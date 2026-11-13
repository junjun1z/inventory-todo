package io.github.junjun1z.inventorytodo.forge;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.loading.FMLPaths;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.junjun1z.inventorytodo.core.Core;
import io.github.junjun1z.inventorytodo.core.Hud;
import io.github.junjun1z.inventorytodo.core.ReflectViewers;
import io.github.junjun1z.inventorytodo.core.TodoPanel;
import io.github.junjun1z.inventorytodo.mc.GfxImpl;
import io.github.junjun1z.inventorytodo.mc.McImpl;
import io.github.junjun1z.inventorytodo.mc.ScreenAccess;

/** Client wiring through Forge's EventBus 7 screen events (no mixins). Forge 1.21.6 - 1.21.8. */
public final class ForgeClient {

    static final KeyMapping HUD_KEY = new KeyMapping("inventorytodo.key.togglehud", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "inventorytodo.key.category");
    static final KeyMapping ADD_KEY = new KeyMapping("inventorytodo.key.addhovered", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "inventorytodo.key.category");
    private static final TodoPanel PANEL = TodoPanel.INSTANCE;

    private ForgeClient() {}

    /** Forge's public getters for the protected container screen fields */
    private static final class Fields implements ScreenAccess {

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
    }

    static void init(BusGroup modBus) {
        RegisterKeyMappingsEvent.getBus(modBus).addListener(e -> {
            e.register(HUD_KEY);
            e.register(ADD_KEY);
        });
        ReflectViewers.registerAvailable();
        ScreenAccess access = new Fields();
        Core.init(new McImpl(FMLPaths.CONFIGDIR.get()
            .resolve(Core.MODID), access, ADD_KEY, HUD_KEY));

        ScreenEvent.Init.Post.BUS.addListener(e -> {
            if (e.getScreen() instanceof AbstractContainerScreen) PANEL.onScreenOpened(e.getScreen());
        });
        ScreenEvent.Closing.BUS.addListener(e -> PANEL.onScreenClosed());
        // Predicate listeners: returning true cancels the event (the screen never sees it)
        ScreenEvent.MouseButtonPressed.Pre.BUS.addListener(e -> {
            return PANEL.mouseClicked(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getButton());
        });
        ScreenEvent.MouseButtonReleased.Pre.BUS.addListener(e -> {
            return PANEL.mouseReleased(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getButton());
        });
        ScreenEvent.MouseScrolled.Pre.BUS.addListener(e -> e.getDeltaY() != 0 && PANEL.mouseScrolled(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getDeltaY() > 0 ? 1 : -1));
        ScreenEvent.KeyPressed.Pre.BUS.addListener(e -> {
            if (PANEL.keyPressed(e.getScreen(), e.getKeyCode())) return true;
            if (ADD_KEY.matches(e.getKeyCode(), e.getScanCode())) {
                McImpl m = (McImpl) Core.mc();
                return PANEL.addHovered(e.getScreen(), (int) m.mouseX(), (int) m.mouseY());
            }
            return false;
        });
        ScreenEvent.CharacterTyped.Pre.BUS.addListener(e -> {
            if (!PANEL.isTyping()) return false;
            PANEL.charTyped(e.getScreen(), e.getCodePoint());
            return true;
        });
        ScreenEvent.Render.Post.BUS.addListener(e -> {
            var g = e.getGuiGraphics();
            g.pose()
                .pushMatrix();
            PANEL.render(e.getScreen(), new GfxImpl(g), e.getMouseX(), e.getMouseY());
            g.pose()
                .popMatrix();
        });
        TickEvent.ClientTickEvent.Post.BUS.addListener(e -> {
            Core.clientTick();
            Minecraft mc = Minecraft.getInstance();
            while (HUD_KEY.consumeClick()) Hud.toggle();
            if (mc.player == null) return;
            Object screen = mc.screen;
            if (screen instanceof AbstractContainerScreen) PANEL.tick(screen);
            else if (screen == null) Hud.tick();
        });
        AddGuiOverlayLayersEvent.getBus(modBus).addListener(e -> e.getLayeredDraw()
            .add(ResourceLocation.fromNamespaceAndPath(Core.MODID, "todo_hud"), (g, delta) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == null && !mc.options.hideGui) Hud.render(new GfxImpl(g));
            }));
    }
}
