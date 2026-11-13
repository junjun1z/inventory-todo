package io.github.junjun1z.inventorytodo.fabric.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.junjun1z.inventorytodo.core.TodoPanel;

/** Fabric API 1.21.11 has no "character typed" screen event: deliver typed characters to the panel while it has focus. */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void inventorytodo$charTyped(long window, CharacterEvent event, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return;
        TodoPanel panel = TodoPanel.INSTANCE;
        if (!panel.isTyping()) return;
        for (char c : Character.toChars(event.codepoint())) panel.charTyped(mc.screen, c);
        ci.cancel();
    }
}
