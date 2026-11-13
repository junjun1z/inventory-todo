package io.github.junjun1z.inventorytodo.devtest;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public class DevInput implements DevInputApi {

    @Override
    public boolean click(Object screen, double x, double y, int button) {
        return NeoForge.EVENT_BUS.post(new ScreenEvent.MouseButtonPressed.Pre((Screen) screen, new MouseButtonEvent(x, y, new MouseButtonInfo(io.github.junjun1z.inventorytodo.mc.McImpl.sdlButton(button), 0)), false)).isCanceled();
    }

    @Override
    public boolean key(Object screen, int key) {
        return NeoForge.EVENT_BUS.post(new ScreenEvent.KeyPressed.Pre((Screen) screen, new KeyEvent(key, 0, 0))).isCanceled();
    }

    @Override
    public boolean character(Object screen, char c) {
        return NeoForge.EVENT_BUS.post(new ScreenEvent.CharacterTyped.Pre((Screen) screen, new CharacterEvent(c))).isCanceled();
    }
}
