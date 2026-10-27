package dev.nimbus.core.util;

import dev.nimbus.core.Nimbus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Error boundary: runs code, logs failures (rate-limited per context) and never rethrows. */
public final class SafeRunner {
    private static final int MAX_LOGS_PER_CONTEXT = 5;
    private static final Map<String, AtomicInteger> COUNTS = new ConcurrentHashMap<>();

    private SafeRunner() {}

    public static boolean run(String context, Runnable task) {
        try {
            task.run();
            return true;
        } catch (Throwable t) {
            report(context, t);
            return false;
        }
    }

    public static void report(String context, Throwable t) {
        int n = COUNTS.computeIfAbsent(context, k -> new AtomicInteger()).incrementAndGet();
        if (n <= MAX_LOGS_PER_CONTEXT) {
            Nimbus.LOG.error("Error in {}", context, t);
        } else if (n == MAX_LOGS_PER_CONTEXT + 1) {
            Nimbus.LOG.error("Further errors in '{}' will be suppressed", context);
        }
    }
}
