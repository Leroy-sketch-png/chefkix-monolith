import copy
import json
import re
import tempfile
import unittest
from pathlib import Path

from render_reviewer import render, validate_pack


def fixture():
    return {"pack_id": "a" * 64, "reviewer": "R1", "views": [
        {"view_id": f"{i + 1:032x}", "recipe_title": f"SYNTHETIC QA ONLY - Recipe {i + 1}",
         "ingredients": ["Synthetic ingredient one", "Synthetic ingredient two"],
         "target": "Synthetic ingredient one", "candidates": [
             {"blind_candidate": f"{j + 100:032x}", "display_names": [f"Synthetic suggestion {j + 1}"]}
             for j in range(5)]} for i in range(150)]}


class RendererTests(unittest.TestCase):
    def test_valid_pack(self):
        self.assertEqual(len(validate_pack(fixture())["views"]), 150)

    def test_private_fields_at_each_level_rejected(self):
        for level in ("pack", "view", "candidate"):
            pack = fixture()
            target = pack if level == "pack" else pack["views"][0]
            if level == "candidate":
                target = target["candidates"][0]
            target["gold"] = True
            with self.subTest(level=level), self.assertRaises(ValueError):
                validate_pack(pack)

    def test_changed_case_count_rejected(self):
        pack = fixture()
        pack["views"].pop()
        with self.assertRaises(ValueError):
            validate_pack(pack)

    def test_duplicate_view_and_candidate_rejected(self):
        for duplicate_view in (True, False):
            pack = fixture()
            if duplicate_view:
                pack["views"][1]["view_id"] = pack["views"][0]["view_id"]
            else:
                pack["views"][0]["candidates"][1] = copy.deepcopy(pack["views"][0]["candidates"][0])
            with self.assertRaises(ValueError):
                validate_pack(pack)

    def test_missing_candidate_and_raw_ids_rejected(self):
        for mutation in ("candidate", "id"):
            pack = fixture()
            if mutation == "candidate":
                pack["views"][0]["candidates"].pop()
            else:
                pack["views"][0]["view_id"] = "test:123:source-recipe-id"
            with self.assertRaises(ValueError):
                validate_pack(pack)

    def test_html_safe_and_immutable_output(self):
        pack = fixture()
        pack["views"][0]["recipe_title"] = '</script><img src=x onerror="alert(1)">'
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "review.html"
            digest = render(pack, path)
            html = path.read_text(encoding="utf-8")
            self.assertNotIn('</script><img src=x', html)
            self.assertIn('\\u003c/script\\u003e', html)
            self.assertIn(digest, html)
            self.assertNotIn("__PACK_JSON__", html)
            with self.assertRaises(FileExistsError):
                render(pack, path)

    def test_unknown_rater_and_empty_ingredient_rejected(self):
        for mutation in ("reviewer", "ingredient"):
            pack = fixture()
            if mutation == "reviewer":
                pack["reviewer"] = "R3"
            else:
                pack["views"][0]["ingredients"] = []
            with self.assertRaises(ValueError):
                validate_pack(pack)

    def test_source_text_matching_template_marker_is_preserved(self):
        pack = fixture()
        pack["views"][0]["recipe_title"] = "__PACK_BINDING__ / __PACK_JSON__"
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "review.html"
            render(pack, path)
            document = path.read_text(encoding="utf-8")
            embedded = re.search(r'<script id="pack" type="application/json">(.*?)</script>', document, re.S)
            self.assertIsNotNone(embedded)
            self.assertEqual(json.loads(embedded.group(1)), pack)


if __name__ == "__main__":
    unittest.main()
