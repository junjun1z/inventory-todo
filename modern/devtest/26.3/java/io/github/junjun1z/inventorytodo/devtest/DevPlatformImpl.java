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

    @Override
    public boolean inWorld() {
        return mc().player != null && mc().level != null;
    }

    @Override
    public Object screen() {
        return mc().gui.screen();
    }

    @Override
    public void openInventory() {
        mc().gui.setScreen(new InventoryScreen(mc().player));
    }

    @Override
    public void closeScreen() {
        mc().gui.setScreen(null);
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
        Screenshot.grab(mc().gameDirectory, name + ".png", mc().gameRenderer.mainRenderTarget(), 1, msg -> {});
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
        return "26.3";
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
}
