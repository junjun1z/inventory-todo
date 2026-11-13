package io.github.junjun1z.inventorytodo.devtest;

import net.minecraft.client.gui.screens.Screen;

import io.github.junjun1z.inventorytodo.fabric.FabricHooks;

/** Same entry points the mixins call. */
public class DevInput implements DevInputApi {

    @Override
    public boolean click(Object screen, double x, double y, int button) {
        return FabricHooks.mouseClicked((Screen) screen, x, y, button);
    }

    @Override
    public boolean key(Object screen, int key) {
        return FabricHooks.keyPressed((Screen) screen, key, 0);
    }

    @Override
    public boolean character(Object screen, char c) {
        return FabricHooks.charTyped((Screen) screen, c);
    }
}
