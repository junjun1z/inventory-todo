package io.github.junjun1z.inventorytodo.compat.nei;

import java.lang.reflect.Field;

import net.minecraft.item.ItemStack;

import io.github.junjun1z.inventorytodo.InventoryTodo;

/**
 * The item NEI is dragging (picked up from its item list or bookmarks). Read through reflection because the field
 * lives in different places: original NEI 1.0.5 {@code LayoutManager.itemPanel.draggedStack}, GTNH fork
 * {@code ItemPanels.itemPanel.draggedStack} and {@code ItemPanels.bookmarkPanel.draggedStack}.
 */
final class NeiDrag {

    private static Object[] panels;
    private static Field draggedStack;
    private static boolean failed;

    private NeiDrag() {}

    private static boolean init() {
        if (panels != null) return true;
        if (failed) return false;
        try {
            Class<?> holder;
            String[] names;
            try {
                holder = Class.forName("codechicken.nei.ItemPanels"); // GTNH fork
                names = new String[] { "itemPanel", "bookmarkPanel" };
            } catch (ClassNotFoundException e) {
                holder = Class.forName("codechicken.nei.LayoutManager"); // original NEI
                names = new String[] { "itemPanel" };
            }
            Object[] found = new Object[names.length];
            for (int i = 0; i < names.length; i++) found[i] = holder.getField(names[i])
                .get(null);
            if (found[0] == null) return false; // original NEI creates it on first use
            draggedStack = found[0].getClass()
                .getField("draggedStack");
            panels = found;
            return true;
        } catch (Throwable t) {
            failed = true;
            InventoryTodo.LOG.warn("Could not read NEI's dragged item: " + t);
            return false;
        }
    }

    /** The item NEI is dragging, or null */
    static ItemStack held() {
        if (!init()) return null;
        try {
            for (Object p : panels) {
                if (p == null) continue;
                ItemStack s = (ItemStack) draggedStack.get(p);
                if (s != null && s.stackSize > 0) return s;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /** Stops NEI's drag (the item was added to the panel) */
    static void clear() {
        if (!init()) return;
        for (Object p : panels) {
            try {
                if (p != null) draggedStack.set(p, null);
            } catch (Throwable ignored) {}
        }
    }
}
