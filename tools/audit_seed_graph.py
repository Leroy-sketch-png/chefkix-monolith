"""Reconstruct source-only ingredient graph statistics from KnowledgeGraphSeeder.java."""

import argparse
import csv
import hashlib
import json
import re
import statistics
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = ROOT / "culinary/src/main/java/com/chefkix/culinary/features/knowledge/service/KnowledgeGraphSeeder.java"
DEFAULT_OUTPUT = ROOT / "evidence/member-ma-seed-graph-source-20261023"
EXPECTED_SOURCE_SHA256 = "cb7befabc03897f45c3d3d5b3a4f29262d93ec82622e9d5a8cf285dd28b15174"
SOURCE_BUILDER = re.compile(r"KnowledgeIngredient\.builder\(\)")
BUILDER_OR_BUILD = re.compile(r"KnowledgeIngredient(?:\.Substitution)?\.builder\(\)|\.build\(\)")
JAVA_STRING = re.compile(r'"((?:\\.|[^"\\])*)"')
EDGE_TYPE = "directed_substitution_statement"


def _decode_java_string(value):
    try:
        return json.loads(f'"{value}"')
    except json.JSONDecodeError as error:
        raise ValueError(f"unsupported Java string escape: {value!r}") from error


def _strings(value):
    return [_decode_java_string(match.group(1)) for match in JAVA_STRING.finditer(value)]


def _field(block, field):
    match = re.search(rf"\.{re.escape(field)}\(\s*\"((?:\\.|[^\"\\])*)\"\s*\)", block)
    if not match:
        raise ValueError(f"ingredient builder is missing {field}(\"...\")")
    return _decode_java_string(match.group(1))


def _ingredient_blocks(source):
    blocks = []
    consumed_until = -1
    for start in SOURCE_BUILDER.finditer(source):
        if start.start() < consumed_until:
            continue
        depth = 0
        for token in BUILDER_OR_BUILD.finditer(source, start.start()):
            if token.group(0).endswith("builder()"):
                depth += 1
            else:
                depth -= 1
                if depth < 0:
                    raise ValueError("unbalanced .build() in ingredient seeder")
                if depth == 0:
                    blocks.append(source[start.start() : token.end()])
                    consumed_until = token.end()
                    break
        else:
            raise ValueError("ingredient builder has no matching .build()")
    if not blocks:
        raise ValueError("no KnowledgeIngredient.builder() records found")
    return blocks


def _normalize(value):
    return " ".join(value.casefold().split())


def _distribution(values):
    if not values:
        return {"min": None, "median": None, "mean": None, "max": None}
    return {
        "min": min(values),
        "median": statistics.median(values),
        "mean": statistics.fmean(values),
        "max": max(values),
    }


def build_seed_graph(source_text, source_path, source_sha256=None):
    """Parse source records and preserve each literal substitution as a directed arc."""
    source_records = []
    seen_canonicals = set()
    for block in _ingredient_blocks(source_text):
        canonical = _field(block, "canonicalName")
        normalized = _normalize(canonical)
        if not normalized:
            raise ValueError("canonicalName cannot be blank")
        if normalized in seen_canonicals:
            raise ValueError(f"duplicate canonicalName: {canonical!r}")
        seen_canonicals.add(normalized)

        category = _field(block, "category")
        if not category.strip():
            raise ValueError(f"category cannot be blank for {canonical!r}")
        aliases_call = re.search(r"\.aliases\(\s*List\.of\((.*?)\)\s*\)", block, re.DOTALL)
        aliases = _strings(aliases_call.group(1)) if aliases_call else []
        alternatives = [
            _decode_java_string(match.group(1))
            for match in re.finditer(r"\.alternative\(\s*\"((?:\\.|[^\"\\])*)\"\s*\)", block)
        ]
        if any(not _normalize(alternative) for alternative in alternatives):
            raise ValueError(f"substitution alternative cannot be blank for {canonical!r}")
        source_records.append(
            {
                "node_id": f"ingredient:{normalized}",
                "canonical_name": canonical,
                "display_name": _field(block, "name"),
                "category": category,
                "aliases": aliases,
                "alternatives": alternatives,
            }
        )

    lookup = {}
    for record in source_records:
        canonical_key = _normalize(record["canonical_name"])
        lookup.setdefault(canonical_key, {}).setdefault(record["node_id"], set()).add("canonical")
        for alias in record["aliases"]:
            key = _normalize(alias)
            if key:
                lookup.setdefault(key, {}).setdefault(record["node_id"], set()).add("alias")

    nodes = [
        {
            "node_id": record["node_id"],
            "node_type": "seed_ingredient",
            "display_name": record["display_name"],
            "canonical_source_record": record["canonical_name"],
            "category": record["category"],
            "in_degree": 0,
            "out_degree": 0,
        }
        for record in source_records
    ]
    node_by_id = {node["node_id"]: node for node in nodes}
    literal_nodes = {}
    edges = []
    resolution_counts = Counter({"canonical": 0, "alias": 0, "unmatched": 0, "ambiguous": 0})

    for record in source_records:
        for alternative in record["alternatives"]:
            key = _normalize(alternative)
            matches = lookup.get(key, {})
            if len(matches) == 1:
                target_id, roles = next(iter(matches.items()))
                resolution = "canonical" if "canonical" in roles else "alias"
            else:
                resolution = "ambiguous" if matches else "unmatched"
                target_id = f"literal_target:{key}"
                if target_id not in literal_nodes:
                    literal_nodes[target_id] = {
                        "node_id": target_id,
                        "node_type": "unresolved_literal_target",
                        "display_name": alternative,
                        "canonical_source_record": "",
                        "category": "",
                        "in_degree": 0,
                        "out_degree": 0,
                    }
            edge = {
                "edge_id": f"statement:{len(edges) + 1:03d}",
                "source_node_id": record["node_id"],
                "target_node_id": target_id,
                "source_name": record["canonical_name"],
                "raw_target": alternative,
                "target_resolution": resolution,
                "edge_type": EDGE_TYPE,
                "statement_index": len(edges) + 1,
            }
            edges.append(edge)
            resolution_counts[resolution] += 1
            node_by_id[record["node_id"]]["out_degree"] += 1
            if resolution in ("canonical", "alias"):
                node_by_id[target_id]["in_degree"] += 1
            else:
                literal_nodes[target_id]["in_degree"] += 1

    nodes.extend(literal_nodes.values())

    for node in nodes:
        node["total_degree"] = node["in_degree"] + node["out_degree"]

    in_degrees = [node["in_degree"] for node in nodes]
    out_degrees = [node["out_degree"] for node in nodes]
    total_degrees = [node["total_degree"] for node in nodes]
    node_type_counts = Counter(node["node_type"] for node in nodes)
    category_counts = Counter(record["category"] for record in source_records)
    edge_type_counts = Counter(edge["edge_type"] for edge in edges)
    degree_sums = {
        "in": sum(in_degrees),
        "out": sum(out_degrees),
        "total": sum(total_degrees),
    }
    edge_count = len(edges)
    summary = {
        "canonical_source_record_count": len(source_records),
        "node_count": len(nodes),
        "edge_count": edge_count,
        "resolved_target_statement_count": resolution_counts["canonical"] + resolution_counts["alias"],
        "resolution_counts": dict(sorted(resolution_counts.items())),
        "node_type_counts": dict(sorted(node_type_counts.items())),
        "source_category_counts": dict(sorted(category_counts.items())),
        "edge_type_counts": dict(sorted(edge_type_counts.items())),
        "degree_distributions": {
            "in": _distribution(in_degrees),
            "out": _distribution(out_degrees),
            "total": _distribution(total_degrees),
        },
        "isolated_node_count": sum(degree == 0 for degree in total_degrees),
        "degree_sums": degree_sums,
        "directed_degree_sum_invariant_pass": degree_sums["in"] == edge_count
        and degree_sums["out"] == edge_count
        and degree_sums["total"] == 2 * edge_count,
    }
    source_sha256 = source_sha256 or hashlib.sha256(source_text.encode("utf-8")).hexdigest()
    return {
        "source_records": source_records,
        "nodes": nodes,
        "edges": edges,
        "summary": summary,
        "provenance": {
            "source_path": source_path.replace("\\", "/"),
            "source_sha256": source_sha256,
            "scope": "tracked KnowledgeGraphSeeder ingredient records and literal substitution statements",
            "graph_interpretation": "directed statement multigraph; repeated statements remain separate arcs",
        },
    }


def _truth_rows(graph):
    summary = graph["summary"]
    rows = []

    def add(group, key, metric, value, unit, meaning):
        rows.append([group, key, metric, value, unit, meaning])

    add("all", "source_only_ingredient_seed", "canonical_source_record_count", summary["canonical_source_record_count"], "records", "Tracked Java source records; runtime insertion is not asserted.")
    add("all", "source_only_ingredient_seed", "node_count", summary["node_count"], "nodes", "Source records plus unresolved literal-target vertices.")
    add("all", "source_only_ingredient_seed", "edge_count", summary["edge_count"], "statements", "Each source substitution statement is one directed arc.")
    add("all", "source_only_ingredient_seed", "isolated_node_count", summary["isolated_node_count"], "nodes", "Vertices with zero in-degree and zero out-degree.")
    add("all", "source_only_ingredient_seed", "directed_degree_sum_invariant_pass", str(summary["directed_degree_sum_invariant_pass"]).lower(), "boolean", "Requires sum(in)=E, sum(out)=E, and sum(total)=2E.")
    for node_type, count in summary["node_type_counts"].items():
        add("node_type", node_type, "node_count", count, "nodes", "Typed vertex count in the source-only graph interpretation.")
    for category, count in summary["source_category_counts"].items():
        add("source_category", category, "source_record_count", count, "records", "Category declared in each canonical source record.")
    for edge_type, count in summary["edge_type_counts"].items():
        add("edge_type", edge_type, "edge_count", count, "statements", "Typed directed substitution statements.")
    for resolution, count in summary["resolution_counts"].items():
        add("target_resolution", resolution, "statement_count", count, "statements", "Literal target resolution against current source canonical names and aliases.")
    for degree_type, values in summary["degree_distributions"].items():
        for statistic, value in values.items():
            add("degree", degree_type, statistic, value, "degree", "All vertices; unresolved literals remain explicitly typed target vertices.")
    for direction, total in summary["degree_sums"].items():
        add("degree_sum", direction, "sum", total, "degree", "Directed multigraph degree sum; not an undirected edge count.")
    return rows


def _file_sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_artifacts(graph, output_dir):
    output_dir.mkdir(parents=True, exist_ok=True)
    provenance = graph["provenance"]
    truth_path = output_dir / "GRAPH_TRUTH.csv"
    with truth_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, lineterminator="\n")
        writer.writerow(["group", "key", "metric", "value", "unit", "meaning", "provenance_status", "provenance_scope", "source_path", "source_sha256"])
        for row in _truth_rows(graph):
            writer.writerow([*row, "SOURCE_ONLY_STATIC_SOURCE", provenance["scope"], provenance["source_path"], provenance["source_sha256"]])

    with (output_dir / "GRAPH_NODES.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, lineterminator="\n")
        writer.writerow(["node_id", "node_type", "display_name", "canonical_source_record", "category", "in_degree", "out_degree", "total_degree", "source_path", "source_sha256"])
        for node in sorted(graph["nodes"], key=lambda item: item["node_id"]):
            writer.writerow([node["node_id"], node["node_type"], node["display_name"], node["canonical_source_record"], node["category"], node["in_degree"], node["out_degree"], node["total_degree"], provenance["source_path"], provenance["source_sha256"]])

    with (output_dir / "GRAPH_EDGES.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, lineterminator="\n")
        writer.writerow(["edge_id", "source_node_id", "target_node_id", "source_name", "raw_target", "target_resolution", "edge_type", "statement_index", "source_path", "source_sha256"])
        for edge in graph["edges"]:
            writer.writerow([edge["edge_id"], edge["source_node_id"], edge["target_node_id"], edge["source_name"], edge["raw_target"], edge["target_resolution"], edge["edge_type"], edge["statement_index"], provenance["source_path"], provenance["source_sha256"]])

    report = {
        "record_id": "member-ma-seed-graph-source-20261023",
        "provenance": provenance,
        "summary": graph["summary"],
        "coverage_curve_disposition": "NOT_COMPUTED_NO_QUERY_COHORT_DENOMINATOR",
        "round0_disposition": "OPEN_SOURCE_ONLY_INTERPRETATION",
        "limitations": [
            "Static source inspection does not prove the seeder ran or that runtime records match these statements.",
            "Unmatched or ambiguous literal targets are not canonical ingredients or verified real-world substitutions.",
            "No authoritative query cohort or denominator is present, so COVERAGE_CURVES.csv is not produced.",
            "No runtime AI artifact, deployment, rights, Round-0 definition, or model-quality claim is established.",
        ],
        "artifacts": ["GRAPH_TRUTH.csv", "GRAPH_NODES.csv", "GRAPH_EDGES.csv", "seed_graph_report.json", "audit_receipt.json"],
    }
    report_path = output_dir / "seed_graph_report.json"
    report_path.write_text(
        json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    test_path = ROOT / "tools/test_audit_seed_graph.py"
    output_names = ["GRAPH_TRUTH.csv", "GRAPH_NODES.csv", "GRAPH_EDGES.csv", "seed_graph_report.json"]
    receipt = {
        "record_id": "member-ma-seed-graph-source-20261023-audit-receipt",
        "source": provenance,
        "generator": {
            "path": Path(__file__).resolve().relative_to(ROOT).as_posix(),
            "sha256": _file_sha256(Path(__file__)),
        },
        "regression_test": {
            "path": test_path.relative_to(ROOT).as_posix(),
            "sha256": _file_sha256(test_path),
            "command": "python tools/test_audit_seed_graph.py",
        },
        "output_sha256": {name: _file_sha256(output_dir / name) for name in output_names},
        "scope": "Source reconstruction and output inventory only; test result is recorded by the invoking run, not inferred by this generator.",
    }
    (output_dir / "audit_receipt.json").write_text(
        json.dumps(receipt, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE, help="KnowledgeGraphSeeder.java path")
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT, help="Directory for CSV and JSON evidence")
    args = parser.parse_args()
    try:
        source_bytes = args.source.read_bytes()
        source_text = source_bytes.decode("utf-8")
        source_sha256 = hashlib.sha256(source_bytes).hexdigest()
        if args.source.resolve() == DEFAULT_SOURCE.resolve() and source_sha256 != EXPECTED_SOURCE_SHA256:
            raise ValueError("tracked seed source hash changed; review and update the source pin before auditing")
        try:
            source_path = args.source.resolve().relative_to(ROOT).as_posix()
        except ValueError:
            source_path = str(args.source.resolve()).replace("\\", "/")
        graph = build_seed_graph(source_text, source_path, source_sha256)
        if not graph["summary"]["directed_degree_sum_invariant_pass"]:
            raise ValueError("directed degree-sum invariant failed")
        write_artifacts(graph, args.output_dir)
    except (OSError, ValueError) as error:
        parser.error(str(error))
    print(json.dumps({"output_dir": str(args.output_dir), **graph["summary"]}, sort_keys=True))


if __name__ == "__main__":
    main()
