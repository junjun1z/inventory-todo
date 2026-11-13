package io.github.junjun1z.inventorytodo.devtest;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

public class DevInput implements DevInputApi {

    @Override
    public boolean click(Object screen, double x, double y, int button) {
        return MinecraftForge.EVENT_BUS.post(new ScreenEvent.MouseButtonPressed.Pre((Screen) screen, x, y, button));
    }

    @Override
    public boolean key(Object screen, int key) {
        return MinecraftForge.EVENT_BUS.post(new ScreenEvent.KeyPressed.Pre((Screen) screen, key, 0, 0));
    }

    @Override
    public boolean character(Object screen, char c) {
        return MinecraftForge.EVENT_BUS.post(new ScreenEvent.CharacterTyped.Pre((Screen) screen, c, 0));
    }
}
