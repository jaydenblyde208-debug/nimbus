package dev.nimbus.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/** Dropdown setting. Value is always one of {@link #options()}. */
public final class ModeSetting extends Setting<String> {
    private final List<String> options;

    public ModeSetting(String name, String description, String def, String... options) {
        super(name, description, def);
        if (options.length == 0 || !List.of(options).contains(def))
            throw new IllegalArgumentException("Default must be one of the options for " + name);
        this.options = List.of(options);
    }

    public List<String> options() { return options; }
    public int index() { return options.indexOf(value); }
    public boolean is(String option) { return value.equals(option); }
    public void cycle() { set(options.get((index() + 1) % options.size())); }

    @Override protected String sanitize(String raw) { return raw != null && options.contains(raw) ? raw : value; }
    @Override public JsonElement serialize() { return new JsonPrimitive(value); }
    @Override public void deserialize(JsonElement json) { set(json.getAsString()); }
}
