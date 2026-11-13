package io.github.junjun1z.inventorytodo.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fmllegacy.network.FMLNetworkConstants;

import io.github.junjun1z.inventorytodo.core.Core;

/** Forge 1.17.1 entry point. Client only; servers don't need it. */
@Mod(Core.MODID)
public class InventoryTodoForge {

    public InventoryTodoForge() {
        // client-only mod: never required on a server
        ModLoadingContext.get()
            .registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        if (FMLEnvironment.dist == Dist.CLIENT) ForgeClient.init(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
