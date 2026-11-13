package io.github.junjun1z.inventorytodo.devtest;

import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class DevPlatformImpl implements DevPlatform {

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    // 26.2 moved screen / toasts / chat into Minecraft.gui; 26.1 still has them on Minecraft (reflection: one devtest for all 26.x)
    private static void setScreen(net.minecraft.client.gui.screens.Screen s) {
        try {
            Object gui = Minecraft.class.getField("gui").get(mc());
            try {
                gui.getClass().getMethod("setScreen", net.minecraft.client.gui.screens.Screen.class).invoke(gui, s);
                return;
            } catch (NoSuchMethodException e) {
                Minecraft.class.getMethod("setScreen", net.minecraft.client.gui.screens.Screen.class).invoke(mc(), s);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Follows a dotted path of fields / no-arg methods; tries each alternative path in turn */
    private static Object call(Object root, String... paths) {
        for (String path : paths) {
            try {
                Object o = root;
                for (String name : path.split("\.")) {
                    try {
                        o = o.getClass().getField(name).get(o);
                    } catch (NoSuchFieldException e) {
                        o = o.getClass().getMethod(name).invoke(o);
                    }
                }
                return o;
            } catch (Exception ignored) {}
        }
        throw new IllegalStateException("none of " + String.join(", ", paths));
    }

    @Override
    public boolean inWorld() {
        return mc().player != null && mc().level != null;
    }

    @Override
    public Object screen() {
        return io.github.junjun1z.inventorytodo.mc.McImpl.currentScreen();
    }

    @Override
    public void openInventory() {
        setScreen(new InventoryScreen(mc().player));
    }

    @Override
    public void closeScreen() {
        setScreen(null);
    }

    @Override
    public void give(String id, int count) {
        MinecraftServer server = mc().getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            p.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)), count));
            p.inventoryMenu.broadcastChanges();
        });
    }

    @Override
    public void screenshot(String name) {
        Screenshot.grab(mc().gameDirectory, name + ".png", (com.mojang.blaze3d.pipeline.RenderTarget) call(mc(), "gameRenderer.mainRenderTarget", "getMainRenderTarget"), 1, msg -> {});
    }

    @Override
    public void stop() {
        mc().stop();
    }

    @Override
    public Path gameDir() {
        return mc().gameDirectory.toPath();
    }

    @Override
    public int windowX() {
        return mc().getWindow().getX();
    }

    @Override
    public int windowY() {
        return mc().getWindow().getY();
    }

    @Override
    public String versionName() {
        return net.minecraft.SharedConstants.getCurrentVersion().name();
    }

    @Override
    public void focusWindow() {
        try {
            Class.forName("org.lwjgl.sdl.SDLVideo").getMethod("SDL_RaiseWindow", long.class).invoke(null, mc().getWindow().handle());
        } catch (Throwable t) {
            io.github.junjun1z.inventorytodo.core.Log.warn("focusWindow failed: " + t);
        }
    }

    @Override
    public void clearInventory() {
        MinecraftServer server = mc().getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            p.getInventory().clearContent();
            p.inventoryMenu.broadcastChanges();
        });
    }

    @Override
    public void prepareShowcase() {
        MinecraftServer server = mc().getSingleplayerServer();
        server.execute(() -> {
            for (String cmd : new String[] { "difficulty peaceful", "time set 1000", "weather clear 100000", "gamerule advance_time false", "gamerule doDaylightCycle false", "gamerule show_advancement_messages false", "gamerule announceAdvancements false" }) {
                try {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
                } catch (Throwable ignored) {}
            }
        });
    }

    @Override
    public void clearOverlays() {
        try {
            ((net.minecraft.client.gui.components.toasts.ToastManager) call(mc(), "gui.toastManager", "getToastManager")).clear();
            ((net.minecraft.client.gui.components.ChatComponent) call(mc(), "gui.hud.getChat", "gui.getChat")).clearMessages(false);
        } catch (Throwable t) {
            io.github.junjun1z.inventorytodo.core.Log.warn("clearOverlays: " + t);
        }
    }
}
