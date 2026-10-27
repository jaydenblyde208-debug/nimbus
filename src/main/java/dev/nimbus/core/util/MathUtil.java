package dev.nimbus.core.util;

public final class MathUtil {
    private MathUtil() {}
    public static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    public static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}
