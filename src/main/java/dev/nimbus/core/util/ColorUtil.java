package dev.nimbus.core.util;

/** Colors are packed ARGB ints (0xAARRGGBB), the same layout the game's draw calls use. */
public final class ColorUtil {
    private ColorUtil() {}

    public static int argb(int a, int r, int g, int b) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }
    public static int alpha(int c) { return c >>> 24; }
    public static int red(int c)   { return (c >> 16) & 255; }
    public static int green(int c) { return (c >> 8) & 255; }
    public static int blue(int c)  { return c & 255; }
    public static int withAlpha(int c, int a) { return (a & 255) << 24 | (c & 0xFFFFFF); }

    public static int lerp(int c1, int c2, float t) {
        t = MathUtil.clamp(t, 0f, 1f);
        return argb(
            Math.round(MathUtil.lerp(alpha(c1), alpha(c2), t)),
            Math.round(MathUtil.lerp(red(c1), red(c2), t)),
            Math.round(MathUtil.lerp(green(c1), green(c2), t)),
            Math.round(MathUtil.lerp(blue(c1), blue(c2), t)));
    }

    /** h, s, v in 0..1. */
    public static int fromHsb(float h, float s, float v, int alpha) {
        h = h - (float) Math.floor(h);
        s = MathUtil.clamp(s, 0f, 1f);
        v = MathUtil.clamp(v, 0f, 1f);
        float hh = h * 6f;
        int i = (int) Math.floor(hh);
        float f = hh - i;
        float p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return argb(alpha, Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
    }

    /** Returns {h, s, v}, each 0..1. */
    public static float[] toHsb(int c) {
        float r = red(c) / 255f, g = green(c) / 255f, b = blue(c) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min, h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) % 6f;
        else if (max == g) h = (b - r) / d + 2f;
        else h = (r - g) / d + 4f;
        h /= 6f;
        if (h < 0) h += 1f;
        return new float[] { h, max == 0 ? 0 : d / max, max };
    }

    /** Accepts "#RRGGBB" or "#AARRGGBB" (the '#' is optional). Throws IllegalArgumentException otherwise. */
    public static int parse(String s) {
        String hex = s.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6 && hex.length() != 8) throw new IllegalArgumentException("Bad color: " + s);
        long v = Long.parseLong(hex, 16);
        return hex.length() == 6 ? (int) (0xFF000000L | v) : (int) v;
    }

    public static String format(int c) { return String.format("#%08X", c); }
}
