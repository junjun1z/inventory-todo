package io.github.junjun1z.inventorytodo.core;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.forge.REIPluginClient;

/**
 * REI client plugin: drag & drop onto the panel and keeping REI's panels away from it. Only REI loads this class
 * (Fabric: "rei_client" entrypoint, Forge/NeoForge: {@code @REIPluginClient} scan), so nothing happens without REI.
 * <p>
 * Compiled against tiny stubs (common/src/stubs, never in the jar); every other REIClientPlugin method keeps REI's
 * default implementation. The registries are only used through reflection ({@link ReflectViewers#registerRei}).
 */
@REIPluginClient
public class ReiEntry implements REIClientPlugin {

    @Override
    public void registerScreens(ScreenRegistry registry) {
        ReflectViewers.registerRei(registry);
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        ReflectViewers.registerReiExclusion(zones);
    }
}
