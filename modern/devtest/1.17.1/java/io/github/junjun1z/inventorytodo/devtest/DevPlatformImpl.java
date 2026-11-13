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
            p.getInventory().add(new ItemStack(Registry.ITEM.get(new ResourceLocation(id)), count));
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
        return "1.17.1";
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
    public void loadWorld(String folder) {
        if (mc().getLevelSource().levelExists(folder)) {
            mc().loadLevel(folder);
            return;
        }
        net.minecraft.core.RegistryAccess.RegistryHolder reg = net.minecraft.core.RegistryAccess.builtin();
        net.minecraft.world.level.LevelSettings ls = new net.minecraft.world.level.LevelSettings(folder, net.minecraft.world.level.GameType.CREATIVE, false,
            net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(), net.minecraft.world.level.DataPackConfig.DEFAULT);
        mc().createLevel(folder, ls, reg, net.minecraft.world.level.levelgen.WorldGenSettings.makeDefault(reg.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), reg.registryOrThrow(Registry.BIOME_REGISTRY), reg.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY)));
    }
}
