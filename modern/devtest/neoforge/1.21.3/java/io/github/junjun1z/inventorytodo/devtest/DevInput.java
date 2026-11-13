package io.github.junjun1z.inventorytodo.devtest;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public class DevInput implements DevInputApi {

    @Override
    public boolean click(Object screen, double x, double y, int button) {
        return NeoForge.EVENT_BUS.post(new ScreenEvent.MouseButtonPressed.Pre((Screen) screen, x, y, button)).isCanceled();
    }

    @Override
    public boolean key(Object screen, int key) {
        return NeoForge.EVENT_BUS.post(new ScreenEvent.KeyPressed.Pre((Screen) screen, key, 0, 0)).isCanceled();
    }

    @Override
    public boolean character(Object screen, char c) {
        return NeoForge.EVENT_BUS.post(new ScreenEvent.CharacterTyped.Pre((Screen) screen, c, 0)).isCanceled();
    }
}
