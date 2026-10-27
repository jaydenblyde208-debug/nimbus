package dev.nimbus.core.setting;

import com.google.gson.JsonElement;
import dev.nimbus.core.Nimbus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Self-describing, serializable setting. The GUI builds a widget from the concrete type,
 * so adding a setting to a module never requires GUI code.
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    private final T defaultValue;
    protected T value;
    private BooleanSupplier visibleWhen = () -> true;
    private final List<Consumer<T>> listeners = new ArrayList<>();

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public final String name() { return name; }
    public final String description() { return description; }
    public final T defaultValue() { return defaultValue; }
    public final T get() { return value; }

    public final void set(T newValue) {
        T clean = sanitize(newValue);
        if (Objects.equals(clean, value)) return;
        value = clean;
        for (Consumer<T> l : listeners) l.accept(clean);
        Nimbus.markConfigDirty();
    }

    public final void reset() { set(defaultValue); }

    public final Setting<T> visibleWhen(BooleanSupplier condition) {
        this.visibleWhen = condition;
        return this;
    }
    public final boolean isVisible() { return visibleWhen.getAsBoolean(); }

    public final Setting<T> onChange(Consumer<T> listener) {
        listeners.add(listener);
        return this;
    }

    /** Clamp / validate an incoming value. Must be pure. */
    protected abstract T sanitize(T raw);
    public abstract JsonElement serialize();
    /** May throw on malformed input; callers isolate failures per setting. */
    public abstract void deserialize(JsonElement json);
}
