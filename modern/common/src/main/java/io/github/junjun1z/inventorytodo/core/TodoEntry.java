package io.github.junjun1z.inventorytodo.core;

/** One row: a plain note, or an item with a goal amount. */
public final class TodoEntry {

    /** Note text. For item rows the item name is shown when this is empty. */
    public String text = "";
    /** Goal item (count 1), or null for a note */
    public Object item;
    /** Raw saved data of an item that no longer exists (kept so it is not lost) */
    public String missingItemData;
    /** Goal amount; 0 = only show how many you have */
    public int target;
    public boolean manualDone;

    /** Current amount (recounted regularly, not saved) */
    public transient int have;

    public static TodoEntry note(String text) {
        TodoEntry e = new TodoEntry();
        e.text = text == null ? "" : text;
        return e;
    }

    public static TodoEntry item(Object stack, int target) {
        TodoEntry e = new TodoEntry();
        e.item = Core.mc()
            .copyOne(stack);
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
                return Core.mc()
                    .displayName(item);
            } catch (Throwable t) {
                return "???";
            }
        }
        if (missingItemData != null) return Core.mc()
            .tr("inventorytodo.unknown_item") + " " + missingItemData;
        return "";
    }
}
