package io.github.junjun1z.inventorytodo.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.junjun1z.inventorytodo.core.Core;
import io.github.junjun1z.inventorytodo.core.Hud;
import io.github.junjun1z.inventorytodo.core.ReflectViewers;
import io.github.junjun1z.inventorytodo.core.TodoPanel;
import io.github.junjun1z.inventorytodo.mc.GfxImpl;
import io.github.junjun1z.inventorytodo.mc.McImpl;
import io.github.junjun1z.inventorytodo.mc.ScreenAccess;

/** Fabric entry point (client only). Uses Fabric API screen events; one mixin delivers typed characters. */
public class InventoryTodoFabric implements ClientModInitializer {

    public static KeyMapping hudKey, addKey;

    @Override
    public void onInitializeClient() {
        hudKey = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("inventorytodo.key.togglehud", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "inventorytodo.key.category"));
        addKey = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("inventorytodo.key.addhovered", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "inventorytodo.key.category"));
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
            ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, b) -> !panel.mouseClicked(s, mx, my, b));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, mx, my, b) -> !panel.mouseReleased(s, mx, my, b));
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mx, my, ha, va) -> va == 0 || !panel.mouseScrolled(s, mx, my, va > 0 ? 1 : -1));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scan, mods) -> {
                if (panel.keyPressed(s, key)) return false;
                if (addKey.matches(key, scan)) {
                    Minecraft mc = Minecraft.getInstance();
                    McImpl m = (McImpl) Core.mc();
                    if (panel.addHovered(s, (int) m.mouseX(), (int) m.mouseY())) return false;
                }
                return true;
            });
            ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> {
                g.pose().pushPose();
                panel.render(s, new GfxImpl(g), mx, my);
                g.pose().popPose();
            });
            ScreenEvents.afterTick(screen).register(panel::tick);
            ScreenEvents.remove(screen).register(s -> panel.onScreenClosed());
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Core.clientTick();
            while (hudKey.consumeClick()) Hud.toggle();
            if (mc.screen == null && mc.player != null) Hud.tick();
        });
        HudRenderCallback.EVENT.register((g, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null && !mc.options.hideGui) Hud.render(new GfxImpl(g));
        });
    }
}
