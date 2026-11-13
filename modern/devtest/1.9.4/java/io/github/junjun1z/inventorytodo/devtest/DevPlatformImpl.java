package io.github.junjun1z.inventorytodo.devtest;

import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;

import org.lwjgl.opengl.Display;

public class DevPlatformImpl implements DevPlatform {

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    @Override
    public boolean inWorld() {
        return mc().thePlayer != null && mc().theWorld != null;
    }

    @Override
    public Object screen() {
        return mc().currentScreen;
    }

    @Override
    public void openInventory() {
        mc().displayGuiScreen(new GuiInventory(mc().thePlayer));
    }

    @Override
    public void closeScreen() {
        mc().displayGuiScreen(null);
    }

    private static EntityPlayerMP player(MinecraftServer server) {
        return server.getPlayerList().getPlayerList().get(0);
    }

    @Override
    public void clearInventory() {
        MinecraftServer server = mc().getIntegratedServer();
        server.addScheduledTask(() -> {
            EntityPlayerMP p = player(server);
            p.inventory.clear();
            p.inventoryContainer.detectAndSendChanges();
        });
    }

    @Override
    public void give(String id, int count) {
        MinecraftServer server = mc().getIntegratedServer();
        server.addScheduledTask(() -> {
            EntityPlayerMP p = player(server);
            p.inventory.addItemStackToInventory(new ItemStack(Item.REGISTRY.getObject(new ResourceLocation(id)), count));
            p.inventoryContainer.detectAndSendChanges();
        });
    }

    @Override
    public void screenshot(String name) {
        ScreenShotHelper.saveScreenshot(mc().mcDataDir, name + ".png", mc().displayWidth, mc().displayHeight, mc().getFramebuffer());
    }

    @Override
    public void stop() {
        mc().shutdown();
    }

    @Override
    public Path gameDir() {
        return mc().mcDataDir.toPath();
    }

    @Override
    public int windowX() {
        return Display.getX();
    }

    @Override
    public int windowY() {
        return Display.getY();
    }

    @Override
    public String versionName() {
        return net.minecraftforge.fml.common.Loader.instance().getMCVersionString().replace("Minecraft ", "");
    }

    @Override
    public void focusWindow() {}

    @Override
    public void loadWorld(String folder) {
        // a new flat world when it does not exist yet (null settings only work for existing worlds)
        boolean exists = new java.io.File(new java.io.File(mc().mcDataDir, "saves"), folder + "/level.dat").exists();
        mc().launchIntegratedServer(folder, folder, exists ? null
            : new net.minecraft.world.WorldSettings(1L, net.minecraft.world.WorldSettings.GameType.SURVIVAL, false, false, net.minecraft.world.WorldType.FLAT));
    }
}
