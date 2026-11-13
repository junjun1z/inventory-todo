package io.github.junjun1z.inventorytodo.devtest;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraftforge.client.event.ScreenEvent;

public class DevInput implements DevInputApi {

    @Override
    public boolean click(Object screen, double x, double y, int button) {
        return ScreenEvent.MouseButtonPressed.Pre.BUS.post(new ScreenEvent.MouseButtonPressed.Pre((Screen) screen, x, y, new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0))));
    }

    @Override
    public boolean key(Object screen, int key) {
        return ScreenEvent.KeyPressed.Pre.BUS.post(new ScreenEvent.KeyPressed.Pre((Screen) screen, new KeyEvent(key, 0, 0)));
    }

    @Override
    public boolean character(Object screen, char c) {
        return ScreenEvent.CharacterTyped.Pre.BUS.post(new ScreenEvent.CharacterTyped.Pre((Screen) screen, new CharacterEvent(c)));
    }
}
