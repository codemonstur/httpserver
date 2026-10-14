package benchmarks;

/**
 * A fixed-memory latency histogram: 64 linear sub-buckets per power of two, so every recorded
 * value is reproduced with at most ~1.6% relative error. Client threads record into their own
 * instance and the histograms are merged after the run, so the recording path never contends.
 */
final class LatencyHistogram {

    private static final int SUB_BITS = 6;
    private static final int SUB_BUCKETS = 1 << SUB_BITS;

    private final long[] buckets = new long[(Long.SIZE + 1) * SUB_BUCKETS];

    void record(final long nanos) {
        final long value = Math.max(1L, nanos); // nanoTime deltas can be 0
        final int power = 63 - Long.numberOfLeadingZeros(value);
        final int sub = power < SUB_BITS
                ? (int) value // below 64 the buckets hold single nanosecond values
                : (int) ((value - (1L << power)) >>> (power - SUB_BITS));
        buckets[Math.min(buckets.length - 1, power * SUB_BUCKETS + sub)]++;
    }

    void mergeFrom(final LatencyHistogram other) {
        for (int i = 0; i < buckets.length; i++) buckets[i] += other.buckets[i];
    }

    long count() {
        long total = 0;
        for (final long bucket : buckets) total += bucket;
        return total;
    }

    /** The midpoint of the bucket holding the value at the given quantile, in nanoseconds. */
    long quantile(final double quantile) {
        final long target = (long) Math.ceil(quantile * count());
        long cumulative = 0;
        for (int i = 0; i < buckets.length; i++) {
            if (buckets[i] == 0) continue;
            cumulative += buckets[i];
            if (cumulative >= target) return midpointOf(i);
        }
        return 0;
    }

    private static long midpointOf(final int index) {
        final int power = index / SUB_BUCKETS;
        final int sub = index % SUB_BUCKETS;
        if (power < SUB_BITS) return sub; // stored exactly
        final long base = (1L << power) + ((long) sub << (power - SUB_BITS));
        final long half = power == SUB_BITS ? 0 : 1L << (power - SUB_BITS - 1);
        return base + half;
    }

}
