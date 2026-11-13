package io.github.junjun1z.inventorytodo.fabric.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.junjun1z.inventorytodo.fabric.FabricHooks;

/** Keys and typed characters for the panel (before the screen sees them). */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void inventorytodo$keyPress(long window, int key, int scan, int action, int mods, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (window != mc.getWindow().getWindow() || mc.screen == null) return;
        if (FabricHooks.onKey(key, scan, action)) ci.cancel();
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void inventorytodo$charTyped(long window, int codePoint, int modifiers, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (window != mc.getWindow().getWindow() || mc.screen == null) return;
        boolean used = false;
        for (char c : Character.toChars(codePoint)) used |= FabricHooks.charTyped(mc.screen, c);
        if (used) ci.cancel();
    }
}
