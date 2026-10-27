package dev.nimbus.core.policy;

import dev.nimbus.core.Nimbus;
import dev.nimbus.core.event.ModuleToggleEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * In AUTHORIZED_TEST mode, appends every module toggle (with a settings snapshot) to
 * config/nimbus/audit/test-session.log so server owners can line it up against their
 * own anticheat/server logs. Does nothing in other modes.
 */
public final class AuditLog {
    private final Path file;

    public AuditLog(Path root) {
        this.file = root.resolve("audit").resolve("test-session.log");
        Nimbus.bus().subscribe(ModuleToggleEvent.class, this::onToggle);
    }

    private void onToggle(ModuleToggleEvent e) {
        if (Nimbus.policy().mode() != PolicyMode.AUTHORIZED_TEST) return;
        String line = Instant.now() + " | " + e.module.name() + " -> " + (e.enabled ? "ON" : "OFF")
            + " | " + e.module.settingsToJson() + System.lineSeparator();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            Nimbus.LOG.warn("Could not write audit log: {}", ex.toString());
        }
    }
}
