"""Build the L36 thesis exhibit from the shared aggregate receipt."""
from __future__ import annotations

import argparse
import csv
from dataclasses import dataclass
from decimal import Decimal
from hashlib import sha256
from pathlib import Path
import re
from io import StringIO

import matplotlib
matplotlib.use("Agg")
from matplotlib import pyplot as plt
from matplotlib.patches import Patch
from matplotlib.ticker import PercentFormatter

REPO = Path(__file__).resolve().parents[3]
RECEIPT = REPO / "evidence/l36-conditional-h0-descriptive-point-20261122.md"
RECEIPT_SHA256 = "abda801fe42550ebe40db0c9ec47e6e050b8093b1d89c47833493f825fb2d8f0"
OUTCOMES = ("Strict-A success", "Definite failure", "Unknown")
COLORS = ("#155A73", "#A34426", "#F2C96C")
INK = "#182B36"


@dataclass(frozen=True)
class Stratum:
    key: str
    label: str
    assigned: int
    population: int
    hits: int
    outcomes: tuple[int, int, int]

    @property
    def misses(self) -> int:
        return sum(self.outcomes)

    @property
    def weight(self) -> Decimal:
        return Decimal(self.population) / self.assigned


def fields(pattern: str, text: str) -> tuple[str, ...]:
    match = re.search(pattern, text)
    if match is None:
        raise ValueError(f"Missing expected aggregate receipt field: {pattern}")
    return match.groups()


def read_aggregates() -> tuple[list[Stratum], Decimal, Decimal, Decimal]:
    text = RECEIPT.read_text(encoding="utf-8")
    if sha256(text.encode()).hexdigest() != RECEIPT_SHA256:
        raise ValueError("Shared L36 receipt changed. Review its new aggregates before rebuilding.")
    strata = []
    membership = fields(
        r"membership: (\d+)/(\d+) `unseen_profiled` and (\d+)/(\d+) `unseen_unprofiled`"
        r" \((\d+) total\); observed top-five hits are (\d+) and (\d+) respectively",
        text,
    )
    profile_count, profile_assigned, unprofiled_count, unprofiled_assigned, total, first_hits, second_hits = map(int, membership)
    weights = fields(r"stratum weights ([\d,]+)/(\d+) and ([\d,]+)/(\d+)", text)
    definitions = (
        ("unseen_profiled", "Unseen profiled", profile_count, profile_assigned, first_hits, weights[:2]),
        ("unseen_unprofiled", "Unseen unprofiled", unprofiled_count, unprofiled_assigned, second_hits, weights[2:]),
    )
    for key, label, misses, assigned, hits, (population, sample) in definitions:
        result = tuple(map(int, fields(
            rf"`{key}` — (\d+) success, (\d+) failure, (\d+) unknown", text,
        )))
        if sum(result) != misses or misses + hits != assigned or int(sample) != assigned:
            raise ValueError(f"Case counts or frame weights contradict the frozen {key} stratum.")
        strata.append(Stratum(key, label, assigned, int(population.replace(",", "")), hits, result))
    if sum(item.misses for item in strata) != total:
        raise ValueError("The stratum totals do not match the recorded H=0 denominator.")

    denominator = sum(item.weight * item.misses for item in strata)
    lower = sum(item.weight * item.outcomes[0] for item in strata) / denominator
    upper = sum(item.weight * (item.outcomes[0] + item.outcomes[2]) for item in strata) / denominator
    expected_n, = fields(r"Weighted H=0 denominator: ([\d,.]+)\.", text)
    expected_low, expected_high = fields(r"lower (0\.\d+), upper (0\.\d+)\.", text)
    if (
        denominator != Decimal(expected_n.replace(",", "").rstrip("."))
        or f"{lower:.6f}" != expected_low
        or f"{upper:.6f}" != expected_high
    ):
        raise ValueError("The weighted descriptive result does not match the shared receipt.")
    return strata, denominator, lower, upper


def write_table(path: Path, strata: list[Stratum], denominator: Decimal, lower: Decimal, upper: Decimal) -> None:
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow([
            "stratum", "assigned", "recorded_reference_hits", "h0_misses",
            "strict_a_success", "definite_failure", "unknown", "frame_weight",
            "weighted_h0_denominator", "weighted_descriptive_lower", "weighted_descriptive_upper",
        ])
        for item in strata:
            writer.writerow([
                item.key, item.assigned, item.hits, item.misses, *item.outcomes,
                f"{item.weight:.2f}", f"{item.misses * item.weight:.2f}", "", "",
            ])
        writer.writerow([
            "total", sum(item.assigned for item in strata), sum(item.hits for item in strata),
            sum(item.misses for item in strata), *map(sum, zip(*(item.outcomes for item in strata))),
            "", f"{denominator:.2f}", f"{lower:.6f}", f"{upper:.6f}",
        ])


def draw_figure(strata: list[Stratum], lower: Decimal, upper: Decimal) -> plt.Figure:
    plt.rcParams.update({
        "font.family": "DejaVu Sans", "font.size": 10, "svg.hashsalt": RECEIPT_SHA256,
        "text.color": INK, "axes.labelcolor": INK, "xtick.color": INK, "ytick.color": INK,
    })
    fig = plt.figure(figsize=(8.5, 5.1), facecolor="white")
    assigned = sum(item.assigned for item in strata)
    n_misses = sum(item.misses for item in strata)
    fig.text(.06, .94, "Strict-A validity among recorded-reference misses", fontsize=18, weight="bold")
    fig.text(.06, .88, f"Frozen L36 audit  |  {n_misses} observed H = 0 cases of {assigned} assigned unseen cases")
    fig.text(.06, .80, "A  Adjudicated case outcomes", fontsize=12, weight="bold")
    fig.legend(
        [Patch(color=color) for color in COLORS], OUTCOMES, frameon=False,
        ncols=3, loc="upper left", bbox_to_anchor=(.053, .773), fontsize=9.5,
    )

    bars = fig.add_axes([.23, .49, .70, .21])
    for index, item in enumerate(strata):
        start = 0
        for count, color, foreground in zip(item.outcomes, COLORS, ("white", "white", INK)):
            bars.barh(index, count, left=start, height=.64, color=color, zorder=2)
            bars.text(start + count / 2, index, str(count), ha="center", va="center",
                      color=foreground, fontsize=9, weight="bold", zorder=3)
            start += count
    bars.set(xlim=(0, 50), yticks=range(len(strata)), yticklabels=[item.label for item in strata])
    bars.set_ylim(len(strata) - .5, -.5)
    bars.set_xlabel("Observed H = 0 cases")
    bars.tick_params(axis="both", length=0)
    bars.grid(axis="x", color="#DCE4EB", zorder=0)
    for spine in bars.spines.values():
        spine.set_visible(False)

    fig.text(.06, .36, "B  Weighted descriptive bounds", fontsize=12, weight="bold")
    interval = fig.add_axes([.23, .17, .70, .15])
    lo, hi = float(lower), float(upper)
    interval.hlines(.32, lo, hi, linewidth=3, color=COLORS[0], zorder=2)
    interval.scatter([lo, hi], [.32, .32], c=[COLORS[0], COLORS[2]], edgecolor=COLORS[0], s=60, zorder=3)
    for value, label in ((lo, "Strict-A only"), (hi, "Strict-A + unknown")):
        interval.text(value, .57, f"{value:.2%}\n{label}", ha="center", va="bottom", weight="bold", fontsize=9)
    interval.set(xlim=(.68, .78), ylim=(0, 1.2), yticks=[])
    interval.set_xticks([.68, .70, .72, .74, .76, .78])
    interval.xaxis.set_major_formatter(PercentFormatter(xmax=1))
    interval.set_xlabel("Weighted strict-A proportion within H = 0")
    interval.tick_params(axis="x", length=0)
    for key, spine in interval.spines.items():
        spine.set_visible(key == "bottom")
        spine.set_color("#ACBBC4")

    fig.text(.06, .045, "The bounds reflect unknown outcomes. Sampling uncertainty and the primary H1 decision remain open.",
             fontsize=9, color="#405461")
    return fig


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output_directory", type=Path)
    output = parser.parse_args().output_directory
    strata, denominator, lower, upper = read_aggregates()
    output.mkdir(parents=True, exist_ok=True)
    write_table(output / "l36-h0-outcomes.csv", strata, denominator, lower, upper)
    fig = draw_figure(strata, lower, upper)
    buffer = StringIO()
    fig.savefig(buffer, format="svg", metadata={
        "Date": None,
        "Title": "L36 strict-A validity among recorded-reference misses",
        "Description": "Frozen audit miss cases by stratum. The weighted descriptive bounds reflect unknown outcomes.",
    })
    plt.close(fig)
    svg = "\n".join(line.rstrip() for line in buffer.getvalue().splitlines()) + "\n"
    (output / "l36-h0-outcomes.svg").write_text(svg, encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
