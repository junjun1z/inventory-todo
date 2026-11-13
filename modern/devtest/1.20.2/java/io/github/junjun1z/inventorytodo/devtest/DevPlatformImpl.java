package io.github.junjun1z.inventorytodo.devtest;

import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class DevPlatformImpl implements DevPlatform {

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Override
    public boolean inWorld() {
        return mc().player != null && mc().level != null;
    }

    @Override
    public Object screen() {
        return mc().screen;
    }

    @Override
    public void openInventory() {
        mc().setScreen(new InventoryScreen(mc().player));
    }

    @Override
    public void closeScreen() {
        mc().setScreen(null);
    }

    @Override
    public void give(String id, int count) {
        MinecraftServer server = mc().getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            p.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(id)), count));
            p.inventoryMenu.broadcastChanges();
        });
    }

    @Override
    public void screenshot(String name) {
        Screenshot.grab(mc().gameDirectory, name + ".png", mc().getMainRenderTarget(), msg -> {});
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
        return net.minecraft.SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public void focusWindow() {
        long h = mc().getWindow().getWindow();
        org.lwjgl.glfw.GLFW.glfwRestoreWindow(h);
        org.lwjgl.glfw.GLFW.glfwFocusWindow(h);
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
            for (String cmd : new String[] { "difficulty peaceful", "time set 1000", "weather clear 100000", "gamerule doDaylightCycle false", "gamerule announceAdvancements false" }) {
                try {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
                } catch (Throwable ignored) {}
            }
        });
    }

    @Override
    public void clearOverlays() {
        try {
            mc().getToasts().clear();
            mc().gui.getChat().clearMessages(false);
        } catch (Throwable t) {
            io.github.junjun1z.inventorytodo.core.Log.warn("clearOverlays: " + t);
        }
    }
}
