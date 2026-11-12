package io.github.junjun1z.inventorytodo;

import java.util.Collections;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Search helper: matches items by their English name and registry name,
 * so players using another language can still search with names from English wikis and guides.
 */
final class EnglishNames {

    private static net.minecraft.client.resources.Locale english;
    private static boolean loadFailed;

    private EnglishNames() {}

    private static net.minecraft.client.resources.Locale english() {
        if (english == null && !loadFailed) {
            try {
                net.minecraft.client.resources.Locale loc = new net.minecraft.client.resources.Locale();
                loc.loadLocaleDataFiles(
                    Minecraft.getMinecraft()
                        .getResourceManager(),
                    Collections.singletonList("en_US"));
                english = loc;
            } catch (Throwable t) {
                loadFailed = true;
                InventoryTodo.LOG.warn("Could not load English names (English name search disabled)", t);
            }
        }
        return english;
    }

    /** Reload after a language change */
    static void reset() {
        english = null;
        loadFailed = false;
    }

    static boolean matches(ItemStack s, String q) {
        Item item = s.getItem();
        if (item == null) return false;
        // Registry name (minecraft:dirt, gregtech:gt.metaitem.01, ...)
        Object reg = Item.itemRegistry.getNameForObject(item);
        if (reg != null && reg.toString()
            .toLowerCase(Locale.ROOT)
            .contains(q)) return true;
        // Name from the English language file
        net.minecraft.client.resources.Locale en = english();
        if (en == null) return false;
        try {
            String key = s.getUnlocalizedName() + ".name";
            String name = en.formatMessage(key, new Object[0]);
            return name != null && !name.equals(key)
                && name.toLowerCase(Locale.ROOT)
                    .contains(q);
        } catch (Throwable t) {
            return false;
        }
    }
}
