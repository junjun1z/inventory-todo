package io.github.junjun1z.inventorytodo.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.junjun1z.inventorytodo.fabric.FabricHooks;

/** Client tick and screen changes. */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void inventorytodo$tick(CallbackInfo ci) {
        FabricHooks.clientTick();
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void inventorytodo$setScreen(Screen next, CallbackInfo ci) {
        FabricHooks.onSetScreen(next);
    }
}
