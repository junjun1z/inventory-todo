package io.github.junjun1z.inventorytodo.fabric.mixin;

import java.util.Map;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;

import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.junjun1z.inventorytodo.fabric.InventoryTodoFabric;

/** Adds the key bindings of the mod to the controls screen (no Fabric API needed). */
@Mixin(Options.class)
public class OptionsMixin {

    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "load", at = @At("HEAD"))
    private void inventorytodo$addKeys(CallbackInfo ci) {
        if (InventoryTodoFabric.hudKey == null || ArrayUtils.contains(keyMappings, InventoryTodoFabric.hudKey)) return;
        Map<String, Integer> order = KeyMapping.CATEGORY_SORT_ORDER;
        if (!order.containsKey(InventoryTodoFabric.CATEGORY)) {
            int max = 0;
            for (int v : order.values()) max = Math.max(max, v);
            order.put(InventoryTodoFabric.CATEGORY, max + 1);
        }
        keyMappings = ArrayUtils.addAll(keyMappings, InventoryTodoFabric.hudKey, InventoryTodoFabric.addKey);
    }
}
