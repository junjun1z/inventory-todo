package io.github.junjun1z.inventorytodo.fabric.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.junjun1z.inventorytodo.fabric.FabricHooks;

/** Container screen opened / resized. */
@Mixin(AbstractContainerScreen.class)
public class ContainerScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void inventorytodo$init(CallbackInfo ci) {
        FabricHooks.onScreenInit((Screen) (Object) this);
    }
}
