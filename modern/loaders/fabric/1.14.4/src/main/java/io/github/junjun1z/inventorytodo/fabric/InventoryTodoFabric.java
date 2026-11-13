package io.github.junjun1z.inventorytodo.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.junjun1z.inventorytodo.core.Core;
import io.github.junjun1z.inventorytodo.core.ReflectViewers;
import io.github.junjun1z.inventorytodo.mc.McImpl;
import io.github.junjun1z.inventorytodo.mc.ScreenAccess;

/**
 * Fabric entry point (client only). Fabric API for these versions has no screen events, so everything is
 * wired through small mixins ({@link FabricHooks}); the mod needs nothing but Fabric Loader.
 */
public class InventoryTodoFabric implements ClientModInitializer {

    public static final String CATEGORY = "inventorytodo.key.category";
    public static KeyMapping hudKey, addKey;

    @Override
    public void onInitializeClient() {
        // added to the controls screen by OptionsMixin
        hudKey = new KeyMapping("inventorytodo.key.togglehud", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
        addKey = new KeyMapping("inventorytodo.key.addhovered", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
        ReflectViewers.registerAvailable();
        Core.init(new McImpl(FabricLoader.getInstance().getConfigDir().resolve(Core.MODID), new ScreenAccess() {
            @Override
            public int left(AbstractContainerScreen<?> s) {
                return s.leftPos;
            }

            @Override
            public int top(AbstractContainerScreen<?> s) {
                return s.topPos;
            }

            @Override
            public int imageWidth(AbstractContainerScreen<?> s) {
                return s.imageWidth;
            }

            @Override
            public Slot hoveredSlot(AbstractContainerScreen<?> s) {
                return s.hoveredSlot;
            }
        }, addKey, hudKey));
    }
}
