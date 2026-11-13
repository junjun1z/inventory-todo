package io.github.junjun1z.inventorytodo.forge;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fmlclient.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;

import io.github.junjun1z.inventorytodo.core.Core;
import io.github.junjun1z.inventorytodo.core.Hud;
import io.github.junjun1z.inventorytodo.core.ReflectViewers;
import io.github.junjun1z.inventorytodo.core.TodoPanel;
import io.github.junjun1z.inventorytodo.mc.GfxImpl;
import io.github.junjun1z.inventorytodo.mc.McImpl;
import io.github.junjun1z.inventorytodo.mc.ScreenAccess;

/** Client-side wiring through Forge 1.17 GUI events (no mixins). */
public final class ForgeClient {

    static final KeyMapping HUD_KEY = new KeyMapping("inventorytodo.key.togglehud", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "inventorytodo.key.category");
    static final KeyMapping ADD_KEY = new KeyMapping("inventorytodo.key.addhovered", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "inventorytodo.key.category");
    private static final TodoPanel PANEL = TodoPanel.INSTANCE;

    private ForgeClient() {}

    static void init(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent e) -> {
            ClientRegistry.registerKeyBinding(HUD_KEY);
            ClientRegistry.registerKeyBinding(ADD_KEY);
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

        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener((GuiScreenEvent.InitGuiEvent.Post e) -> {
            if (e.getGui() instanceof AbstractContainerScreen) PANEL.onScreenOpened(e.getGui());
        });
        bus.addListener((GuiOpenEvent e) -> {
            if (!(e.getGui() instanceof AbstractContainerScreen)) PANEL.onScreenClosed();
        });
        bus.addListener(EventPriority.HIGHEST, (GuiScreenEvent.MouseClickedEvent.Pre e) -> {
            if (PANEL.mouseClicked(e.getGui(), e.getMouseX(), e.getMouseY(), e.getButton())) e.setCanceled(true);
        });
        bus.addListener(EventPriority.HIGHEST, (GuiScreenEvent.MouseReleasedEvent.Pre e) -> {
            if (PANEL.mouseReleased(e.getGui(), e.getMouseX(), e.getMouseY(), e.getButton())) e.setCanceled(true);
        });
        bus.addListener(EventPriority.HIGHEST, (GuiScreenEvent.MouseScrollEvent.Pre e) -> {
            double d = e.getScrollDelta();
            if (d != 0 && PANEL.mouseScrolled(e.getGui(), e.getMouseX(), e.getMouseY(), d > 0 ? 1 : -1)) e.setCanceled(true);
        });
        bus.addListener(EventPriority.HIGHEST, (GuiScreenEvent.KeyboardKeyPressedEvent.Pre e) -> {
            if (PANEL.keyPressed(e.getGui(), e.getKeyCode())) {
                e.setCanceled(true);
            } else if (ADD_KEY.matches(e.getKeyCode(), e.getScanCode())) {
                McImpl m = (McImpl) Core.mc();
                if (PANEL.addHovered(e.getGui(), (int) m.mouseX(), (int) m.mouseY())) e.setCanceled(true);
            }
        });
        bus.addListener(EventPriority.HIGHEST, (GuiScreenEvent.KeyboardCharTypedEvent.Pre e) -> {
            if (PANEL.isTyping() && PANEL.charTyped(e.getGui(), e.getCodePoint())) e.setCanceled(true);
        });
        bus.addListener(EventPriority.LOW, (GuiScreenEvent.DrawScreenEvent.Post e) -> {
            PoseStack pose = e.getMatrixStack();
            pose.pushPose();
            PANEL.render(e.getGui(), new GfxImpl(pose, e.getGui()), e.getMouseX(), e.getMouseY());
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
        bus.addListener((RenderGameOverlayEvent.Post e) -> {
            if (e.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null && !mc.options.hideGui) Hud.render(new GfxImpl(e.getMatrixStack(), null));
        });
    }
}
