package io.github.junjun1z.inventorytodo.devtest;

import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import io.github.junjun1z.inventorytodo.core.TodoPanel;

public class DevInput implements DevInputApi {

    @Override
    public boolean click(Object screen, double x, double y, int button) {
        Screen s = (Screen) screen;
        return !ScreenMouseEvents.allowMouseClick(s).invoker().allowMouseClick(s, new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0)));
    }

    @Override
    public boolean key(Object screen, int key) {
        Screen s = (Screen) screen;
        return !ScreenKeyboardEvents.allowKeyPress(s).invoker().allowKeyPress(s, new KeyEvent(key, 0, 0));
    }

    @Override
    public boolean character(Object screen, char c) {
        // characters arrive through a mixin on KeyboardHandler (not reachable here): same logic as the mixin
        if (!TodoPanel.INSTANCE.isTyping()) return false;
        return TodoPanel.INSTANCE.charTyped(screen, c);
    }
}
