package io.github.junjun1z.inventorytodo.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.junjun1z.inventorytodo.core.Core;
import io.github.junjun1z.inventorytodo.core.Hud;
import io.github.junjun1z.inventorytodo.core.ReflectViewers;
import io.github.junjun1z.inventorytodo.core.TodoPanel;
import io.github.junjun1z.inventorytodo.mc.GfxImpl;
import io.github.junjun1z.inventorytodo.mc.McImpl;
import io.github.junjun1z.inventorytodo.mc.ScreenAccess;

/** Fabric entry point for Minecraft 26.x (client only). Only Fabric API events, no mixins. */
public class InventoryTodoFabric implements ClientModInitializer {

    public static KeyMapping hudKey, addKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Core.MODID, "main"));
        hudKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("inventorytodo.key.togglehud", InputConstants.UNKNOWN.getValue(), category));
        addKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("inventorytodo.key.addhovered", InputConstants.UNKNOWN.getValue(), category));
        ReflectViewers.registerAvailable();
        Core.init(new McImpl(FabricLoader.getInstance().getConfigDir().resolve(Core.MODID), new ScreenAccess() {
            @Override
            public int left(AbstractContainerScreen<?> s) {
                return s.leftPos;
            }

            @Override
            public int top(AbstractContainerScreen<?> s) {
                return s.topPos;
            }

            @Override
            public int imageWidth(AbstractContainerScreen<?> s) {
                return s.imageWidth;
            }

            @Override
            public Slot hoveredSlot(AbstractContainerScreen<?> s) {
                return s.hoveredSlot;
            }
        }, addKey, hudKey));

        TodoPanel panel = TodoPanel.INSTANCE;
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen)) return;
            panel.onScreenOpened(screen);
            ScreenMouseEvents.allowMouseClick(screen).register((s, e) -> !panel.mouseClicked(s, e.x(), e.y(), McImpl.button(e.button())));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, e) -> !panel.mouseReleased(s, e.x(), e.y(), McImpl.button(e.button())));
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mx, my, ha, va) -> va == 0 || !panel.mouseScrolled(s, mx, my, va > 0 ? 1 : -1));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, e) -> {
                if (panel.keyPressed(s, e.key())) return false;
                if (addKey.matches(e)) {
                    McImpl m = (McImpl) Core.mc();
                    if (panel.addHovered(s, (int) m.mouseX(), (int) m.mouseY())) return false;
                }
                return true;
            });
            ScreenKeyboardEvents.allowCharType(screen).register((s, e) -> {
                if (!panel.isTyping()) return true;
                for (char c : Character.toChars(e.codepoint())) panel.charTyped(s, c);
                return false;
            });
            ScreenEvents.afterExtract(screen).register((s, g, mx, my, delta) -> {
                g.pose().pushMatrix();
                panel.render(s, new GfxImpl(g), mx, my);
                g.pose().popMatrix();
            });
            ScreenEvents.afterTick(screen).register(panel::tick);
            ScreenEvents.remove(screen).register(s -> panel.onScreenClosed());
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Core.clientTick();
            while (hudKey.consumeClick()) Hud.toggle();
            if (McImpl.currentScreen() == null && mc.player != null) Hud.tick();
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(Core.MODID, "todo_hud"), (g, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (McImpl.currentScreen() == null && !McImpl.hudHidden()) Hud.render(new GfxImpl(g));
        });
    }
}
