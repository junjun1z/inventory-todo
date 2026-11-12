package io.github.junjun1z.inventorytodo;

import net.minecraft.item.ItemStack;

/** One row of the list: a plain note, or an item with a goal amount. */
public class TodoEntry {

    /** Note text. For item rows, the item name is shown when this is empty. */
    public String text = "";
    /** Goal item (stack size normalised to 1; damage value and NBT are kept) */
    public ItemStack item;
    /** Goal amount. 0 = only show how many you have */
    public int target;
    /** Checked by hand */
    public boolean manualDone;

    /** Current amount in the inventory (recounted regularly, not saved) */
    public transient int have;

    public static TodoEntry text(String text) {
        TodoEntry e = new TodoEntry();
        e.text = text == null ? "" : text;
        return e;
    }

    public static TodoEntry item(ItemStack stack, int target) {
        TodoEntry e = new TodoEntry();
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        e.item = copy;
        e.target = Math.max(0, target);
        return e;
    }

    public boolean isDone() {
        if (manualDone) return true;
        return item != null && target > 0 && have >= target;
    }

    public String displayText() {
        if (text != null && !text.isEmpty()) return text;
        if (item != null) {
            try {
                return item.getDisplayName();
            } catch (Throwable t) {
                return "???";
            }
        }
        return "";
    }
}
