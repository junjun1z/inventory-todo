package io.github.junjun1z.inventorytodo;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** Settings stored in config/inventorytodo/inventorytodo.cfg. */
public final class TodoConfig {

    private TodoConfig() {}

    /** Modifier key for adding the hovered item with a click: ALT / CTRL / SHIFT */
    public static String addModifier = "ALT";
    /** Maximum number of rows shown at once */
    public static int maxVisibleRows = 10;
    /** Panel width in pixels */
    public static int panelWidth = 160;
    /** When the goal item has no NBT, count inventory items regardless of their NBT */
    public static boolean ignoreNbtWhenTargetHasNone = true;

    public static void load(File file) {
        Configuration cfg = new Configuration(file);
        cfg.load();
        addModifier = cfg.getString(
            "addModifier",
            "general",
            "ALT",
            "Modifier key + click adds the hovered inventory (or NEI) item to the list. ALT / CTRL / SHIFT\n"
                + "With SHIFT, shift-click adds items instead of moving them while the panel is shown.",
            new String[] { "ALT", "CTRL", "SHIFT" });
        maxVisibleRows = cfg.getInt("maxVisibleRows", "general", 10, 3, 40, "Maximum number of rows shown at once");
        panelWidth = cfg.getInt("panelWidth", "general", 160, 120, 320, "Panel width in pixels");
        ignoreNbtWhenTargetHasNone = cfg.getBoolean(
            "ignoreNbtWhenTargetHasNone",
            "general",
            true,
            "When a goal item has no NBT, count inventory items regardless of their NBT");
        if (cfg.hasChanged()) cfg.save();
    }
}
