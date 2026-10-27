package dev.nimbus.core.policy;

import dev.nimbus.core.Nimbus;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

/** Startup environment checks. Never throws; problems are logged and returned. */
public final class CompatibilityChecker {
    public enum Severity { INFO, WARN, ERROR }
    public record Issue(Severity severity, String message) {}

    public static final String TARGET_MC = "1.21.11";
    /** Mods that change entity/world rendering and can affect overlay modules. */
    private static final String[] RENDER_MODS = { "sodium", "iris", "immediatelyfast", "entityculling" };

    private CompatibilityChecker() {}

    public static List<Issue> run() {
        List<Issue> issues = new ArrayList<>();
        try {
            FabricLoader loader = FabricLoader.getInstance();
            if (Runtime.version().feature() < 21)
                issues.add(new Issue(Severity.ERROR, "Java 21+ required, running " + Runtime.version()));

            String mc = loader.getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
            if (!TARGET_MC.equals(mc))
                issues.add(new Issue(Severity.WARN, "Built for Minecraft " + TARGET_MC + ", running " + mc));

            for (String id : RENDER_MODS) {
                if (loader.isModLoaded(id))
                    issues.add(new Issue(Severity.WARN, "'" + id + "' is installed; if overlays render oddly, "
                        + "test with it removed (see docs/TROUBLESHOOTING.md)."));
            }
        } catch (Throwable t) {
            issues.add(new Issue(Severity.WARN, "Compatibility check failed: " + t));
        }
        for (Issue i : issues) {
            switch (i.severity()) {
                case ERROR -> Nimbus.LOG.error(i.message());
                case WARN -> Nimbus.LOG.warn(i.message());
                case INFO -> Nimbus.LOG.info(i.message());
            }
        }
        return issues;
    }
}
