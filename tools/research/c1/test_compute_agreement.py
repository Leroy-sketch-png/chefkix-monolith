import math
import unittest

from compute_agreement import cluster_bootstrap, cohens_kappa, linear_quantile, wilson


class AgreementMathTests(unittest.TestCase):
    def test_nominal_kappa_exact_and_chance_agreement(self):
        self.assertEqual(cohens_kappa([("A", "A"), ("C", "C")]), 1.0)
        self.assertAlmostEqual(cohens_kappa([(a, b) for a, b in (("A", "A"), ("A", "C"), ("C", "A"), ("C", "C"))]), 0.0)

    def test_kappa_uses_all_four_categories_and_rejects_unknown(self):
        labels = [("A", "A"), ("C", "C"), ("N", "U"), ("U", "N")]
        self.assertTrue(math.isfinite(cohens_kappa(labels)))
        with self.assertRaises(ValueError):
            cohens_kappa([("A", "other")])

    def test_wilson_interval_has_fixed_denominator(self):
        lo, hi = wilson(8, 10)
        self.assertLess(lo, 0.8)
        self.assertGreater(hi, 0.8)
        self.assertIsNone(wilson(0, 0))

    def test_linear_quantile_uses_interpolation_without_changing_order_statistic(self):
        values = [0.0, 10.0, 20.0, 30.0, 40.0]
        self.assertEqual(linear_quantile(values, 0.0), 0.0)
        self.assertEqual(linear_quantile(values, 0.5), 20.0)
        self.assertEqual(linear_quantile(values, 1.0), 40.0)
        with self.assertRaises(ValueError):
            linear_quantile([], 0.5)
        with self.assertRaises(ValueError):
            linear_quantile(values, 1.1)

    def test_case_cluster_bootstrap_is_deterministic_and_resamples_cases(self):
        clusters = {f"case-{i}": [("A", "A"), ("C", "C"), ("N", "U")] for i in range(30)}
        first = cluster_bootstrap(clusters)
        second = cluster_bootstrap(clusters)
        self.assertEqual(first, second)
        self.assertEqual(first["requested_draws"], 2000)
        self.assertEqual(first["valid_draws"], 2000)
        self.assertIsNotNone(first["interval_95"])
        self.assertIsNotNone(first["interval_95_linear_interpolation_sensitivity"])
        self.assertEqual(len(first["interval_95_linear_interpolation_sensitivity"]), 2)


if __name__ == "__main__":
    unittest.main()
