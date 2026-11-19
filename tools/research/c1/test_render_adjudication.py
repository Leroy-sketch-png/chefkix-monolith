import json
import tempfile
import unittest
from pathlib import Path

from render_adjudication import render, validate_packet


def packet():
    return {
        "format": "c1-l36-adjudication-packet-v1",
        "specification_id": "iron-chef-l36-case-rule-v1",
        "items": [{
            "item_id": f"{i:024x}", "recipe_title": "Soup <script>alert(1)</script>",
            "ingredients": ["water", "herb"], "target": "herb",
            "candidate_names": ["leaf", "leaf / seed"],
            "unordered_raw_labels": ["A", "C"], "final_label": None, "rationale": None,
        } for i in range(150)],
    }


class AdjudicationRendererTests(unittest.TestCase):
    def test_valid_packet_renders_self_contained_html_with_fixed_binding(self):
        with tempfile.TemporaryDirectory() as d:
            source, target = Path(d) / "packet.json", Path(d) / "review.html"
            source.write_text(json.dumps(packet()), encoding="utf-8")
            packet_sha, html_sha = render(source, target)
            html = target.read_text(encoding="utf-8")
            self.assertEqual(len(packet_sha), 64)
            self.assertEqual(len(html_sha), 64)
            self.assertIn("c1-l36-adjudication-return-v1", html)
            self.assertIn("Restore saved return", html)
            self.assertIn("Next unanswered", html)
            self.assertIn("localStorage", html)
            self.assertIn("final_label", html)
            self.assertNotIn("<script>alert(1)</script>", html)
            self.assertNotIn("src=\"http", html)
            with self.assertRaises(FileExistsError):
                render(source, target)

    def test_rejects_wrong_denominator_and_private_fields(self):
        p = packet()
        p["items"].pop()
        with self.assertRaises(ValueError):
            validate_packet(p)
        p = packet()
        p["items"][0]["case_id"] = "private"
        with self.assertRaises(ValueError):
            validate_packet(p)

    def test_rejects_invalid_or_prepopulated_adjudication(self):
        p = packet()
        p["items"][0]["unordered_raw_labels"] = ["A", "A"]
        with self.assertRaises(ValueError):
            validate_packet(p)
        p = packet()
        p["items"][0]["final_label"] = "A"
        with self.assertRaises(ValueError):
            validate_packet(p)


if __name__ == "__main__":
    unittest.main()
