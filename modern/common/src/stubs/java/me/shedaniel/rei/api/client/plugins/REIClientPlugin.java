package me.shedaniel.rei.api.client.plugins;

import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;

/** Compile-time stub (not in the jar): the real interface (with default methods) comes from REI. */
public interface REIClientPlugin {

    void registerScreens(ScreenRegistry registry);

    void registerExclusionZones(ExclusionZones zones);
}
