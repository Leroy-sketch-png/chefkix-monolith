from __future__ import annotations

import sys
import os
import hashlib
import subprocess
import tempfile
import types
import unittest
from decimal import Decimal
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))
import estimate_c2_headline_cost as forecast_module  # noqa: E402
from estimate_c2_headline_cost import ForecastError, forecast_cost, load_offline_encoder  # noqa: E402


class C2ForecastTests(unittest.TestCase):
    def test_full_package_scenario_arithmetic_is_explicit_and_nonbinding(self):
        report = forecast_cost(tokenizer_proxy_tokens=4_158_391, request_count=10_499, output_cap_tokens=256)
        self.assertEqual(report["estimated_input_tokens_with_assumptions"], 8_925_331)
        self.assertEqual(report["output_tokens_at_cap"], 2_687_744)
        self.assertEqual(report["estimated_input_cost_usd"], "22.32")
        self.assertEqual(report["estimated_output_cost_usd"], "26.88")
        self.assertEqual(report["estimated_subtotal_usd"], "49.20")
        self.assertEqual(report["forecast_with_reserve_usd"], "59.04")
        self.assertEqual(report["scenario_residual_budget_at_protocol_80_percent_threshold_usd"], "73.80")
        self.assertFalse(report["forecast_is_hard_upper_bound"])
        self.assertFalse(report["framing_allowance_is_published_bound"])

    def test_zero_request_costs_are_zero_and_have_no_budget_gate(self):
        report = forecast_cost(tokenizer_proxy_tokens=0, request_count=0, output_cap_tokens=256)
        self.assertEqual(report["estimated_input_tokens_with_assumptions"], 0)
        self.assertEqual(report["output_tokens_at_cap"], 0)
        self.assertEqual(report["forecast_with_reserve_usd"], "0.00")
        self.assertEqual(report["scenario_residual_budget_at_protocol_80_percent_threshold_usd"], "0.00")

    def test_boolean_and_negative_counts_rejected(self):
        for value in (True, -1, 1.5):
            with self.subTest(value=value), self.assertRaises(ForecastError):
                forecast_cost(tokenizer_proxy_tokens=value, request_count=1, output_cap_tokens=256)

    def test_rates_are_injectable_for_independent_fixture(self):
        report = forecast_cost(
            tokenizer_proxy_tokens=1_000,
            request_count=2,
            output_cap_tokens=256,
            input_rate=Decimal("1"),
            output_rate=Decimal("2"),
        )
        self.assertEqual(report["estimated_input_tokens_with_assumptions"], 2_012)
        self.assertEqual(report["output_tokens_at_cap"], 512)
        self.assertEqual(report["estimated_input_cost_usd"], "0.01")
        self.assertEqual(report["estimated_output_cost_usd"], "0.01")

    def test_offline_encoder_requires_an_explicit_cache(self):
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaisesRegex(ForecastError, "explicit pre-staged offline cache"):
                load_offline_encoder()

    def test_offline_encoder_rejects_missing_or_unpinned_resource_before_loading(self):
        with tempfile.TemporaryDirectory() as cache_dir, patch.dict(os.environ, {"TIKTOKEN_CACHE_DIR": cache_dir}, clear=True):
            with self.assertRaisesRegex(ForecastError, "refusing network fallback"):
                load_offline_encoder()

    def test_offline_encoder_refuses_actual_cache_miss_network_branch(self):
        url = "https://example.invalid/pinned-test-resource"
        data = b"pinned local test encoding"
        digest = hashlib.sha256(data).hexdigest()
        cache_key = hashlib.sha1(url.encode("utf-8")).hexdigest()
        attempted_reads = []
        fake_load = types.ModuleType("tiktoken.load")
        fake_load.read_file = lambda requested_url: attempted_reads.append(requested_url) or b"network data"
        fake_tiktoken = types.ModuleType("tiktoken")
        fake_tiktoken.__version__ = forecast_module.TOKENIZER_PACKAGE_VERSION
        fake_tiktoken.load = fake_load
        fake_tiktoken.get_encoding = lambda name: fake_load.read_file(url)

        with tempfile.TemporaryDirectory() as cache_dir:
            (Path(cache_dir) / cache_key).write_bytes(data)
            with (
                patch.object(forecast_module, "TOKENIZER_BLOB_URL", url),
                patch.object(forecast_module, "TOKENIZER_BLOB_SHA256", digest),
                patch.dict(os.environ, {"TIKTOKEN_CACHE_DIR": cache_dir}, clear=True),
                patch.dict(sys.modules, {"tiktoken": fake_tiktoken, "tiktoken.load": fake_load}),
            ):
                with self.assertRaisesRegex(ForecastError, "network read; offline guard refused"):
                    load_offline_encoder()
        self.assertEqual(attempted_reads, [])

    def test_frozen_serializer_is_hash_verified_and_restored_after_validation_scope(self):
        previous = forecast_module._request_package.serializer
        with forecast_module.use_frozen_package_serializer():
            frozen = forecast_module._request_package.serializer
            frozen_path = Path(frozen.__file__)
            self.assertNotEqual(frozen, previous)
            self.assertEqual(
                hashlib.sha256(frozen_path.read_bytes()).hexdigest(),
                forecast_module.FROZEN_SERIALIZER_SHA256,
            )
            self.assertTrue(frozen_path.is_file())
        self.assertIs(forecast_module._request_package.serializer, previous)
        self.assertFalse(frozen_path.exists())

    def test_unavailable_frozen_serializer_git_object_uses_hash_pinned_bundle_snapshot(self):
        failed = subprocess.CompletedProcess(
            args=["git", "show"], returncode=1, stdout=b"", stderr=b"missing object"
        )
        with patch.object(forecast_module.subprocess, "run", return_value=failed) as run:
            with forecast_module.use_frozen_package_serializer():
                frozen = forecast_module._request_package.serializer
                self.assertEqual(
                    hashlib.sha256(Path(frozen.__file__).read_bytes()).hexdigest(),
                    forecast_module.FROZEN_SERIALIZER_SHA256,
                )
        self.assertEqual(run.call_args.kwargs["env"]["GIT_NO_LAZY_FETCH"], "1")

    def test_tampered_frozen_serializer_git_object_fails_closed(self):
        tampered = subprocess.CompletedProcess(
            args=["git", "show"], returncode=0, stdout=b"not the pinned source", stderr=b""
        )
        with patch.object(forecast_module.subprocess, "run", return_value=tampered):
            with self.assertRaisesRegex(ForecastError, "failed its SHA-256 check"):
                with forecast_module.use_frozen_package_serializer():
                    self.fail("must not enter validation with a mismatched source")


if __name__ == "__main__":
    unittest.main()
