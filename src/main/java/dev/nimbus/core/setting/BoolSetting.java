package dev.nimbus.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String description, boolean def) { super(name, description, def); }
    public boolean on() { return value; }
    public void toggle() { set(!value); }
    @Override protected Boolean sanitize(Boolean raw) { return raw != null && raw; }
    @Override public JsonElement serialize() { return new JsonPrimitive(value); }
    @Override public void deserialize(JsonElement json) { set(json.getAsBoolean()); }
}
