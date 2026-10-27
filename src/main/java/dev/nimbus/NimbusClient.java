package dev.nimbus;

import dev.nimbus.core.Nimbus;
import dev.nimbus.core.policy.CompatibilityChecker;
import dev.nimbus.core.util.SafeRunner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class NimbusClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Nimbus.init(
            FabricLoader.getInstance().getConfigDir().resolve("nimbus"),
            () -> Minecraft.getInstance().isSingleplayer());

        CompatibilityChecker.run();

        // Phase 2+: Nimbus.modules().register(new ...Module());

        Nimbus.start();

        ClientTickEvents.END_CLIENT_TICK.register(mc ->
            SafeRunner.run("client tick", () -> Nimbus.tick(mc.player != null && mc.level != null)));
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> Nimbus.shutdown());
    }
}
