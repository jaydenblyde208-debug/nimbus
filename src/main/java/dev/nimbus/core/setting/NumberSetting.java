package dev.nimbus.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class NumberSetting extends Setting<Double> {
    private final double min, max, step;
    private final int decimals;

    public NumberSetting(String name, String description, double def, double min, double max, double step) {
        super(name, description, snap(def, min, max, step));
        if (min >= max) throw new IllegalArgumentException("min must be < max for " + name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.decimals = step > 0 ? Math.max(0, BigDecimal.valueOf(step).stripTrailingZeros().scale()) : 3;
    }

    private static double snap(double v, double min, double max, double step) {
        double c = Math.max(min, Math.min(max, v));
        if (step > 0) {
            c = min + Math.round((c - min) / step) * step;
            c = Math.max(min, Math.min(max, c));
        }
        int dec = step > 0 ? Math.max(0, BigDecimal.valueOf(step).stripTrailingZeros().scale()) : 3;
        return BigDecimal.valueOf(c).setScale(dec, RoundingMode.HALF_UP).doubleValue();
    }

    public double min() { return min; }
    public double max() { return max; }
    public double step() { return step; }
    public int decimals() { return decimals; }
    public double dbl() { return value; }
    public float flt() { return value.floatValue(); }
    public int intValue() { return (int) Math.round(value); }
    /** 0..1 position for slider rendering. */
    public double normalized() { return (value - min) / (max - min); }

    @Override protected Double sanitize(Double raw) {
        return raw == null || raw.isNaN() ? defaultValue() : snap(raw, min, max, step);
    }
    @Override public JsonElement serialize() { return new JsonPrimitive(value); }
    @Override public void deserialize(JsonElement json) { set(json.getAsDouble()); }
}
