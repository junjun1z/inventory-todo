package io.github.junjun1z.inventorytodo.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.junjun1z.inventorytodo.fabric.FabricHooks;

/** Mouse buttons and wheel for the panel (before the screen sees them). */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void inventorytodo$onPress(long window, int button, int action, int mods, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (window != mc.getWindow().getWindow() || mc.screen == null || mc.overlay != null) return;
        if (FabricHooks.onMouseButton(button, action)) ci.cancel();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void inventorytodo$onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (window != mc.getWindow().getWindow() || mc.screen == null || mc.overlay != null) return;
        if (FabricHooks.onScroll(vertical)) ci.cancel();
    }
}
