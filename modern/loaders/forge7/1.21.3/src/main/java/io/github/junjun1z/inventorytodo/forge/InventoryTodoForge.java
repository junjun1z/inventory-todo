package io.github.junjun1z.inventorytodo.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import io.github.junjun1z.inventorytodo.core.Core;

/** Forge entry point. Client only; does nothing on a dedicated server. */
@Mod(Core.MODID)
public class InventoryTodoForge {

    public InventoryTodoForge(FMLJavaModLoadingContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ForgeClient.init(context.getModEventBus());
    }
}
