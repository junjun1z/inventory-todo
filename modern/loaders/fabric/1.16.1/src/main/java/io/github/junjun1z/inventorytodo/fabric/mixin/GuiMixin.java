package io.github.junjun1z.inventorytodo.fabric.mixin;

import net.minecraft.client.gui.Gui;

import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.junjun1z.inventorytodo.fabric.FabricHooks;

/** The in-game HUD list. */
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void inventorytodo$render(PoseStack pose, float partialTicks, CallbackInfo ci) {
        FabricHooks.renderHud(pose);
    }
}
