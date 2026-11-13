package io.github.junjun1z.inventorytodo.forge;

import java.lang.reflect.Field;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.junjun1z.inventorytodo.core.Core;
import io.github.junjun1z.inventorytodo.core.Hud;
import io.github.junjun1z.inventorytodo.core.Log;
import io.github.junjun1z.inventorytodo.core.ReflectViewers;
import io.github.junjun1z.inventorytodo.core.TodoPanel;
import io.github.junjun1z.inventorytodo.mc.GfxImpl;
import io.github.junjun1z.inventorytodo.mc.McImpl;
import io.github.junjun1z.inventorytodo.mc.ScreenAccess;

/** Client wiring through Forge's EventBus 7 screen events (no mixins). */
public final class ForgeClient {

    static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Core.MODID, "main"));
    static final KeyMapping HUD_KEY = new KeyMapping("inventorytodo.key.togglehud", InputConstants.UNKNOWN.getValue(), CATEGORY);
    static final KeyMapping ADD_KEY = new KeyMapping("inventorytodo.key.addhovered", InputConstants.UNKNOWN.getValue(), CATEGORY);
    private static final TodoPanel PANEL = TodoPanel.INSTANCE;

    private ForgeClient() {}

    /** Minecraft 26.x is not obfuscated: these protected fields have the same names in production. */
    private static final class Fields implements ScreenAccess {

        private final Field left, top, width, hovered;

        Fields() throws Exception {
            Class<?> c = AbstractContainerScreen.class;
            left = c.getDeclaredField("leftPos");
            top = c.getDeclaredField("topPos");
            width = c.getDeclaredField("imageWidth");
            hovered = c.getDeclaredField("hoveredSlot");
            for (Field f : new Field[] { left, top, width, hovered }) f.setAccessible(true);
        }

        private int i(Field f, AbstractContainerScreen<?> s) {
            try {
                return f.getInt(s);
            } catch (Exception e) {
                return 0;
            }
        }

        @Override
        public int left(AbstractContainerScreen<?> s) {
            return i(left, s);
        }

        @Override
        public int top(AbstractContainerScreen<?> s) {
            return i(top, s);
        }

        @Override
        public int imageWidth(AbstractContainerScreen<?> s) {
            return i(width, s);
        }

        @Override
        public Slot hoveredSlot(AbstractContainerScreen<?> s) {
            try {
                return (Slot) hovered.get(s);
            } catch (Exception e) {
                return null;
            }
        }
    }

    static void init() {
        RegisterKeyMappingsEvent.BUS.addListener(e -> {
            e.register(HUD_KEY);
            e.register(ADD_KEY);
        });
        ReflectViewers.registerAvailable();
        ScreenAccess access;
        try {
            access = new Fields();
        } catch (Exception e) {
            Log.error("Cannot access container screen fields", e);
            return;
        }
        Core.init(new McImpl(FMLPaths.CONFIGDIR.get()
            .resolve(Core.MODID), access, ADD_KEY, HUD_KEY));

        ScreenEvent.Init.Post.BUS.addListener(e -> {
            if (e.getScreen() instanceof AbstractContainerScreen) PANEL.onScreenOpened(e.getScreen());
        });
        ScreenEvent.Closing.BUS.addListener(e -> PANEL.onScreenClosed());
        // Predicate listeners: returning true cancels the event (the screen never sees it)
        ScreenEvent.MouseButtonPressed.Pre.BUS.addListener(e -> {
            return PANEL.mouseClicked(e.getScreen(), e.getMouseX(), e.getMouseY(), McImpl.button(e.getInfo()
                .button()));
        });
        ScreenEvent.MouseButtonReleased.Pre.BUS.addListener(e -> {
            return PANEL.mouseReleased(e.getScreen(), e.getMouseX(), e.getMouseY(), McImpl.button(e.getButton()));
        });
        ScreenEvent.MouseScrolled.Pre.BUS.addListener(e -> e.getDeltaY() != 0 && PANEL.mouseScrolled(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getDeltaY() > 0 ? 1 : -1));
        ScreenEvent.KeyPressed.Pre.BUS.addListener(e -> {
            if (PANEL.keyPressed(e.getScreen(), e.getInfo()
                .key())) return true;
            if (ADD_KEY.matches(e.getInfo())) {
                McImpl m = (McImpl) Core.mc();
                return PANEL.addHovered(e.getScreen(), (int) m.mouseX(), (int) m.mouseY());
            }
            return false;
        });
        ScreenEvent.CharacterTyped.Pre.BUS.addListener(e -> {
            if (!PANEL.isTyping()) return false;
            for (char c : Character.toChars(e.getInfo()
                .codepoint())) PANEL.charTyped(e.getScreen(), c);
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
            Object screen = McImpl.currentScreen();
            if (screen instanceof AbstractContainerScreen) PANEL.tick(screen);
            else if (screen == null) Hud.tick();
        });
        AddGuiOverlayLayersEvent.BUS.addListener(e -> e.getLayeredDraw()
            .add(Identifier.fromNamespaceAndPath(Core.MODID, "todo_hud"), (g, delta) -> {
                if (McImpl.currentScreen() == null && !McImpl.hudHidden()) Hud.render(new GfxImpl(g));
            }));
    }
}
