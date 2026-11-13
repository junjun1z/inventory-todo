package io.github.junjun1z.inventorytodo.forge;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLPaths;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.junjun1z.inventorytodo.core.Core;
import io.github.junjun1z.inventorytodo.core.Hud;
import io.github.junjun1z.inventorytodo.core.ReflectViewers;
import io.github.junjun1z.inventorytodo.core.TodoPanel;
import io.github.junjun1z.inventorytodo.mc.GfxImpl;
import io.github.junjun1z.inventorytodo.mc.McImpl;
import io.github.junjun1z.inventorytodo.mc.ScreenAccess;

/** Client-side wiring through Forge's screen events (no mixins). */
public final class ForgeClient {

    static final KeyMapping HUD_KEY = new KeyMapping("inventorytodo.key.togglehud", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "inventorytodo.key.category");
    static final KeyMapping ADD_KEY = new KeyMapping("inventorytodo.key.addhovered", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "inventorytodo.key.category");
    private static final TodoPanel PANEL = TodoPanel.INSTANCE;

    private ForgeClient() {}

    static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.register(HUD_KEY);
            e.register(ADD_KEY);
        });
        ReflectViewers.registerAvailable();
        Core.init(new McImpl(FMLPaths.CONFIGDIR.get().resolve(Core.MODID), new ScreenAccess() {
            @Override
            public int left(AbstractContainerScreen<?> s) {
                return s.getGuiLeft();
            }

            @Override
            public int top(AbstractContainerScreen<?> s) {
                return s.getGuiTop();
            }

            @Override
            public int imageWidth(AbstractContainerScreen<?> s) {
                return s.getXSize();
            }

            @Override
            public Slot hoveredSlot(AbstractContainerScreen<?> s) {
                return s.getSlotUnderMouse();
            }
        }, ADD_KEY, HUD_KEY));

        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener((ScreenEvent.Init.Post e) -> {
            if (e.getScreen() instanceof AbstractContainerScreen) PANEL.onScreenOpened(e.getScreen());
        });
        bus.addListener((ScreenEvent.Closing e) -> PANEL.onScreenClosed());
        bus.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseButtonPressed.Pre e) -> {
            if (PANEL.mouseClicked(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getButton())) e.setCanceled(true);
        });
        bus.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseButtonReleased.Pre e) -> {
            if (PANEL.mouseReleased(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getButton())) e.setCanceled(true);
        });
        bus.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseScrolled.Pre e) -> {
            double d = e.getScrollDelta();
            if (d != 0 && PANEL.mouseScrolled(e.getScreen(), e.getMouseX(), e.getMouseY(), d > 0 ? 1 : -1)) e.setCanceled(true);
        });
        bus.addListener(EventPriority.HIGHEST, (ScreenEvent.KeyPressed.Pre e) -> {
            if (PANEL.keyPressed(e.getScreen(), e.getKeyCode())) {
                e.setCanceled(true);
            } else if (ADD_KEY.matches(e.getKeyCode(), e.getScanCode())) {
                McImpl m = (McImpl) Core.mc();
                if (PANEL.addHovered(e.getScreen(), (int) m.mouseX(), (int) m.mouseY())) e.setCanceled(true);
            }
        });
        bus.addListener(EventPriority.HIGHEST, (ScreenEvent.CharacterTyped.Pre e) -> {
            if (PANEL.isTyping() && PANEL.charTyped(e.getScreen(), e.getCodePoint())) e.setCanceled(true);
        });
        bus.addListener(EventPriority.LOW, (ScreenEvent.Render.Post e) -> {
            var pose = e.getPoseStack();
            pose.pushPose();
            PANEL.render(e.getScreen(), new GfxImpl(pose, e.getScreen()), e.getMouseX(), e.getMouseY());
            pose.popPose();
        });
        bus.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase != TickEvent.Phase.END) return;
            Core.clientTick();
            Minecraft mc = Minecraft.getInstance();
            while (HUD_KEY.consumeClick()) Hud.toggle();
            if (mc.player == null) return;
            if (mc.screen instanceof AbstractContainerScreen) PANEL.tick(mc.screen);
            else if (mc.screen == null) Hud.tick();
        });
        bus.addListener((RenderGuiEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null && !mc.options.hideGui) Hud.render(new GfxImpl(e.getPoseStack(), null));
        });
    }
}
