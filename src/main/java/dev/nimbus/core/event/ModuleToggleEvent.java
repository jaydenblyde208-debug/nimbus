package dev.nimbus.core.event;

import dev.nimbus.core.module.Module;

public final class ModuleToggleEvent extends Event {
    public final Module module;
    public final boolean enabled;
    public ModuleToggleEvent(Module module, boolean enabled) {
        this.module = module;
        this.enabled = enabled;
    }
}
