package io.queueflux.engine.api;

/**
 * A 95% confidence interval computed using the t-distribution for small samples.
 *
 * @param metric      name of the metric
 * @param method      statistical method used (e.g. "t-distribution, 95% CI")
 * @param n           sample size (number of replications)
 * @param mean        sample mean
 * @param lower       lower bound of the confidence interval
 * @param upper       upper bound of the confidence interval
 */
public record ConfidenceInterval(String metric, String method, int n, double mean, double lower, double upper) {

    private static final double[] T_CRIT_95 = {
        0, 0,          // n=0,1 — undefined
        12.706,        // n=2
        4.303,         // n=3
        3.182,         // n=4
        2.776,         // n=5
        2.571,         // n=6
        2.447,         // n=7
        2.365,         // n=8
        2.306,         // n=9
        2.262,         // n=10
        2.228,         // n=11
        2.201,         // n=12
        2.179,         // n=13
        2.160,         // n=14
        2.145,         // n=15
        2.131,         // n=16
        2.120,         // n=17
        2.110,         // n=18
        2.101,         // n=19
        2.093,         // n=20
        2.086,         // n=21
        2.080,         // n=22
        2.074,         // n=23
        2.069,         // n=24
        2.064,         // n=25
        2.060,         // n=26
        2.056,         // n=27
        2.052,         // n=28
        2.048,         // n=29
        2.045,         // n=30
    };

    /**
     * Builds a 95% CI from raw samples using the t-distribution.
     * Returns null if n < 2.
     */
    public static ConfidenceInterval of(String metric, double[] values) {
        int n = values.length;
        if (n < 2) return null;
        double mean = mean(values);
        double variance = 0;
        for (double v : values) variance += (v - mean) * (v - mean);
        double stddev = Math.sqrt(variance / (n - 1));
        double tCrit = n < T_CRIT_95.length ? T_CRIT_95[n] : 1.960; // z for large n
        double margin = tCrit * stddev / Math.sqrt(n);
        return new ConfidenceInterval(metric, "t-distribution, 95% CI", n, mean, mean - margin, mean + margin);
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }
}
