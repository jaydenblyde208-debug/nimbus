package dev.nimbus.core.policy;

import dev.nimbus.core.config.ConfigManager;
import dev.nimbus.core.module.Module;

import java.util.function.BooleanSupplier;

/**
 * Decides which modules may run. AUTHORIZED_TEST requires BOTH the override and an explicit
 * acknowledgement flag in the global config, so it can't be reached by accident.
 */
public final class PolicyService {
    public record Decision(boolean allowed, String reason) {
        static final Decision ALLOW = new Decision(true, "");
    }

    private final BooleanSupplier singleplayer;
    private final ConfigManager.GlobalConfig config;

    public PolicyService(BooleanSupplier singleplayer, ConfigManager.GlobalConfig config) {
        this.singleplayer = singleplayer;
        this.config = config;
    }

    public PolicyMode mode() {
        return switch (config.policyOverride) {
            case RESTRICTED -> PolicyMode.RESTRICTED;
            case AUTHORIZED_TEST -> config.authorizedAck ? PolicyMode.AUTHORIZED_TEST : PolicyMode.RESTRICTED;
            case AUTO -> singleplayer.getAsBoolean() ? PolicyMode.SINGLEPLAYER : PolicyMode.RESTRICTED;
        };
    }

    public Decision check(Module module) {
        if (module.policyClass() == PolicyClass.SAFE) return Decision.ALLOW;
        if (mode() != PolicyMode.RESTRICTED) return Decision.ALLOW;
        return new Decision(false, module.name() + " is disabled in Restricted mode (multiplayer default). "
            + "Use a single-player world, or on a server you are authorized to test, set "
            + "policyOverride=AUTHORIZED_TEST and authorizedAck=true in config/nimbus/config.json.");
    }
}
