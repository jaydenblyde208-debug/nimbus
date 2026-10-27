package dev.nimbus.core;

import dev.nimbus.core.config.ConfigManager;
import dev.nimbus.core.event.EventBus;
import dev.nimbus.core.event.NotificationEvent;
import dev.nimbus.core.event.TickEvent;
import dev.nimbus.core.module.ModuleManager;
import dev.nimbus.core.policy.AuditLog;
import dev.nimbus.core.policy.PolicyService;
import dev.nimbus.core.util.SafeRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.function.BooleanSupplier;

/** Service locator for the core singletons. Lifecycle: init() -> register modules -> start() -> tick()* -> shutdown(). */
public final class Nimbus {
    public static final String ID = "nimbus";
    public static final Logger LOG = LoggerFactory.getLogger("Nimbus");

    private static EventBus bus;
    private static ConfigManager config;
    private static PolicyService policy;
    private static ModuleManager modules;
    private static AuditLog audit;

    private Nimbus() {}

    public static void init(Path configDir, BooleanSupplier singleplayer) {
        bus = new EventBus();
        config = new ConfigManager(configDir);
        config.loadGlobal();
        policy = new PolicyService(singleplayer, config.global());
        modules = new ModuleManager();
        audit = new AuditLog(configDir);
    }

    /** Call after all modules are registered. */
    public static void start() {
        config.loadProfile(config.global().activeProfile);
        LOG.info("Nimbus started: {} modules, profile '{}', policy mode {}",
            modules.all().size(), config.global().activeProfile, policy.mode());
    }

    public static void tick(boolean inWorld) {
        bus.post(new TickEvent(inWorld));
        modules.tick();
        config.tick();
    }

    public static void shutdown() {
        SafeRunner.run("shutdown", () -> {
            modules.all().forEach(m -> m.setEnabled(false));
            config.shutdown();
        });
    }

    public static EventBus bus() { return bus; }
    public static ConfigManager config() { return config; }
    public static PolicyService policy() { return policy; }
    public static ModuleManager modules() { return modules; }

    public static void markConfigDirty() { if (config != null) config.markDirty(); }

    public static void notify(NotificationEvent.Level level, String message) {
        switch (level) {
            case ERROR -> LOG.error(message);
            case WARN -> LOG.warn(message);
            case INFO -> LOG.info(message);
        }
        if (bus != null) bus.post(new NotificationEvent(level, message));
    }
}
