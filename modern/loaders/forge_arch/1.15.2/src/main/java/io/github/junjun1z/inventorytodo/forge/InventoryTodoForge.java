package io.github.junjun1z.inventorytodo.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.FMLNetworkConstants;

import org.apache.commons.lang3.tuple.Pair;

import io.github.junjun1z.inventorytodo.core.Core;

/** Forge 1.15.2 entry point. Client only; servers don't need it. */
@Mod(Core.MODID)
public class InventoryTodoForge {

    public InventoryTodoForge() {
        // client-only mod: never required on a server
        ModLoadingContext.get()
            .registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        if (FMLEnvironment.dist == Dist.CLIENT) ForgeClient.init(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
