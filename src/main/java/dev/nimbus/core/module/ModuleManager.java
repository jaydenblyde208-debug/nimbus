package dev.nimbus.core.module;

import dev.nimbus.core.Nimbus;
import dev.nimbus.core.event.KeyEvent;
import dev.nimbus.core.event.NotificationEvent;
import dev.nimbus.core.policy.PolicyService;
import dev.nimbus.core.util.SafeRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ModuleManager {
    private static final int POLICY_CHECK_INTERVAL_TICKS = 20;

    private final Map<String, Module> modules = new LinkedHashMap<>();
    private int tickCounter;

    public ModuleManager() {
        Nimbus.bus().subscribe(KeyEvent.class, this::onKey);
    }

    public void register(Module module) {
        if (modules.putIfAbsent(module.id(), module) != null)
            throw new IllegalStateException("Duplicate module id: " + module.id());
    }

    public List<Module> all() { return Collections.unmodifiableList(new ArrayList<>(modules.values())); }

    public List<Module> byCategory(Category category) {
        return modules.values().stream().filter(m -> m.category() == category).toList();
    }

    public Optional<Module> get(String id) { return Optional.ofNullable(modules.get(id)); }

    @SuppressWarnings("unchecked")
    public <T extends Module> Optional<T> get(Class<T> type) {
        return modules.values().stream().filter(type::isInstance).map(m -> (T) m).findFirst();
    }

    /** Live search used by the ClickGUI search bar: names, descriptions, setting names. */
    public List<Module> search(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return all();
        return modules.values().stream()
            .map(m -> Map.entry(m, score(m, q)))
            .filter(e -> e.getValue() > 0)
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .map(Map.Entry::getKey)
            .toList();
    }

    private static int score(Module m, String q) {
        String n = m.name().toLowerCase(Locale.ROOT);
        if (n.equals(q)) return 100;
        if (n.startsWith(q)) return 80;
        if (n.contains(q)) return 60;
        if (m.description().toLowerCase(Locale.ROOT).contains(q)) return 30;
        for (var s : m.settings()) if (s.name().toLowerCase(Locale.ROOT).contains(q)) return 20;
        return 0;
    }

    public void tick() {
        if (++tickCounter % POLICY_CHECK_INTERVAL_TICKS == 0) enforcePolicy();
    }

    /** Disables gated modules if the policy mode changed (e.g. the player joined a server). */
    public void enforcePolicy() {
        PolicyService policy = Nimbus.policy();
        for (Module m : all()) {
            if (m.isEnabled() && !policy.check(m).allowed()) {
                m.setEnabled(false);
                Nimbus.notify(NotificationEvent.Level.WARN, m.name() + " was disabled by the current policy mode.");
            }
        }
    }

    private void onKey(KeyEvent e) {
        if (e.action != KeyEvent.PRESS || e.screenOpen) return;
        for (Module m : all()) {
            if (m.keybind().isBound() && m.keybind().key() == e.key) {
                SafeRunner.run("keybind " + m.name(), m::toggle);
            }
        }
    }
}
