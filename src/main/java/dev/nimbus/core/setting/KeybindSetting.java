package dev.nimbus.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** GLFW key code, or {@link #UNBOUND}. */
public final class KeybindSetting extends Setting<Integer> {
    public static final int UNBOUND = -1;

    public KeybindSetting(String name, String description, int defaultKey) { super(name, description, defaultKey); }
    public int key() { return value; }
    public boolean isBound() { return value != UNBOUND; }
    public void clear() { set(UNBOUND); }
    @Override protected Integer sanitize(Integer raw) { return raw == null || raw < 0 ? UNBOUND : raw; }
    @Override public JsonElement serialize() { return new JsonPrimitive(value); }
    @Override public void deserialize(JsonElement json) { set(json.getAsInt()); }
}
