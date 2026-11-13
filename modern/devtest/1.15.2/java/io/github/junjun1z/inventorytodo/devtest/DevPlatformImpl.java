package io.github.junjun1z.inventorytodo.devtest;

import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Registry;
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
            p.inventory.add(new ItemStack(Registry.ITEM.get(new ResourceLocation(id)), count));
            p.inventoryMenu.broadcastChanges();
        });
    }

    @Override
    public void screenshot(String name) {
        Screenshot.grab(mc().gameDirectory, name + ".png", mc().getWindow().getWidth(), mc().getWindow().getHeight(), mc().getMainRenderTarget(), msg -> {});
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
        return "1.15.2";
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
            p.inventory.clearContent();
            p.inventoryMenu.broadcastChanges();
        });
    }

    @Override
    public void loadWorld(String folder) {
        boolean exists = mc().getLevelSource().levelExists(folder);
        mc().selectLevel(folder, folder, exists ? null
            : new net.minecraft.world.level.LevelSettings(0L, net.minecraft.world.level.GameType.CREATIVE, true, false, net.minecraft.world.level.LevelType.NORMAL));
    }
}
