package dev.nimbus.core.module;

import com.google.gson.JsonObject;
import dev.nimbus.core.Nimbus;
import dev.nimbus.core.event.Event;
import dev.nimbus.core.event.EventBus;
import dev.nimbus.core.event.ModuleToggleEvent;
import dev.nimbus.core.event.NotificationEvent;
import dev.nimbus.core.event.Priority;
import dev.nimbus.core.policy.PolicyClass;
import dev.nimbus.core.policy.PolicyService;
import dev.nimbus.core.setting.KeybindSetting;
import dev.nimbus.core.setting.Setting;
import dev.nimbus.core.util.SafeRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base class for every feature. Listeners registered through {@link #listen} are removed
 * automatically on disable, and any exception inside them disables the module instead of
 * crashing the game.
 */
public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private final PolicyClass policyClass;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<EventBus.Subscription> subscriptions = new ArrayList<>();
    private final KeybindSetting keybind = new KeybindSetting("Keybind", "Key that toggles this module", KeybindSetting.UNBOUND);
    private boolean enabled;

    protected Module(String name, String description, Category category, PolicyClass policyClass) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.policyClass = policyClass;
        settings.add(keybind);
    }

    // ---- metadata ----
    public final String name() { return name; }
    public final String description() { return description; }
    public final Category category() { return category; }
    public final PolicyClass policyClass() { return policyClass; }
    public final String id() { return name.toLowerCase().replaceAll("[^a-z0-9]", ""); }
    public final KeybindSetting keybind() { return keybind; }
    public final List<Setting<?>> settings() { return Collections.unmodifiableList(settings); }
    public final boolean isEnabled() { return enabled; }
    /** Override to start enabled on first run (SAFE modules only; GATED modules never auto-enable). */
    public boolean enabledByDefault() { return false; }

    protected final <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    // ---- lifecycle hooks ----
    protected void onEnable() {}
    protected void onDisable() {}

    // ---- toggling ----
    public final void toggle() { setEnabled(!enabled); }

    /** @return true if the module is now in the requested state. */
    public final boolean setEnabled(boolean on) {
        if (on == enabled) return true;
        if (on) {
            PolicyService.Decision d = Nimbus.policy().check(this);
            if (!d.allowed()) {
                Nimbus.notify(NotificationEvent.Level.WARN, d.reason());
                return false;
            }
            enabled = true;
            try {
                onEnable();
            } catch (Throwable t) {
                fault("onEnable", t);
                return false;
            }
        } else {
            disableInternal(true);
        }
        Nimbus.bus().post(new ModuleToggleEvent(this, enabled));
        Nimbus.markConfigDirty();
        return true;
    }

    private void disableInternal(boolean reportErrors) {
        if (!enabled) return;
        enabled = false;
        subscriptions.forEach(EventBus.Subscription::cancel);
        subscriptions.clear();
        try {
            onDisable();
        } catch (Throwable t) {
            if (reportErrors) SafeRunner.report(name + " (onDisable)", t);
        }
    }

    /** Called when a module throws: log, notify, disable. */
    protected final void fault(String where, Throwable t) {
        SafeRunner.report(name + " (" + where + ")", t);
        boolean wasEnabled = enabled;
        disableInternal(false);
        if (wasEnabled) {
            Nimbus.notify(NotificationEvent.Level.ERROR,
                name + " hit an error and was disabled (" + t.getClass().getSimpleName() + "). See latest.log.");
            Nimbus.bus().post(new ModuleToggleEvent(this, false));
            Nimbus.markConfigDirty();
        }
    }

    // ---- events ----
    protected final <E extends Event> void listen(Class<E> type, Consumer<E> handler) {
        listen(type, Priority.NORMAL, handler);
    }

    protected final <E extends Event> void listen(Class<E> type, int priority, Consumer<E> handler) {
        subscriptions.add(Nimbus.bus().subscribe(type, priority, e -> {
            if (!enabled) return;
            try {
                handler.accept(e);
            } catch (Throwable t) {
                fault("event " + type.getSimpleName(), t);
            }
        }));
    }

    // ---- persistence helpers ----
    public final JsonObject settingsToJson() {
        JsonObject o = new JsonObject();
        for (Setting<?> s : settings) {
            try {
                o.add(s.name(), s.serialize());
            } catch (RuntimeException ex) {
                SafeRunner.report(name + " serialize " + s.name(), ex);
            }
        }
        return o;
    }

    public final void settingsFromJson(JsonObject json) {
        for (Setting<?> s : settings) {
            if (!json.has(s.name())) continue;
            try {
                s.deserialize(json.get(s.name()));
            } catch (RuntimeException ex) {
                Nimbus.LOG.warn("Ignoring bad value for {}.{}: {}", name, s.name(), ex.toString());
            }
        }
    }

    public final void resetSettings() {
        for (Setting<?> s : settings) s.reset();
    }
}
