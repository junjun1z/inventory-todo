package io.github.junjun1z.inventorytodo.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

import io.github.junjun1z.inventorytodo.core.Core;

/** MinecraftForge 26.x entry point. Client only; does nothing on a dedicated server. */
@Mod(Core.MODID)
public class InventoryTodoForge {

    public InventoryTodoForge(FMLJavaModLoadingContext context) {
        if (FMLLoader.getDist()
            .isClient()) ForgeClient.init();
    }
}
