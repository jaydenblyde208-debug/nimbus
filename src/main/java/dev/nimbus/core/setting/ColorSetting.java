package dev.nimbus.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.nimbus.core.util.ColorUtil;

/** Packed ARGB color, serialized as "#AARRGGBB". */
public final class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, String description, int argb) { super(name, description, argb); }
    public int argb() { return value; }
    @Override protected Integer sanitize(Integer raw) { return raw == null ? defaultValue() : raw; }
    @Override public JsonElement serialize() { return new JsonPrimitive(ColorUtil.format(value)); }
    @Override public void deserialize(JsonElement json) { set(ColorUtil.parse(json.getAsString())); }
}
