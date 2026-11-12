package io.github.junjun1z.inventorytodo;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Settings stored in config/inventorytodo/inventorytodo.cfg (also editable in-game: Mods > Config). */
public final class TodoConfig {

    private TodoConfig() {}

    public static final String CATEGORY = Configuration.CATEGORY_GENERAL;

    public static Configuration config;

    /** Modifier key for adding the hovered item with a click: ALT / CTRL / SHIFT */
    public static String addModifier = "ALT";
    /** Maximum number of rows shown at once */
    public static int maxVisibleRows = 10;
    /** Panel width in pixels */
    public static int panelWidth = 160;
    /** When the goal item has no NBT, count inventory items regardless of their NBT */
    public static boolean ignoreNbtWhenTargetHasNone = true;
    /** Show unfinished rows on the HUD while playing (can also be toggled with the "H" button or a key) */
    public static boolean hudEnabled = true;
    /** HUD corner: TOP_LEFT / TOP_RIGHT / BOTTOM_LEFT / BOTTOM_RIGHT */
    public static String hudAnchor = "BOTTOM_RIGHT";
    public static int hudOffsetX = 4;
    public static int hudOffsetY = 4;
    public static int hudMaxRows = 6;
    /** Also count items inside the container that is currently open (chests etc.) */
    public static boolean countOpenContainer = true;
    /** WORLD = one list per world/server, GLOBAL = one list shared everywhere */
    public static String listScope = "WORLD";

    public static void load(File file) {
        config = new Configuration(file);
        config.load();
        sync();
        FMLCommonHandler.instance()
            .bus()
            .register(new ChangeListener());
    }

    /** Re-reads the values after they were edited in the in-game config screen */
    public static final class ChangeListener {

        @SubscribeEvent
        public void onChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (InventoryTodo.MODID.equals(event.modID)) sync();
        }
    }

    private static Property prop(Property p, String langKey) {
        p.setLanguageKey("inventorytodo.config." + langKey);
        return p;
    }

    public static void sync() {
        Configuration cfg = config;
        addModifier = prop(
            cfg.get(
                CATEGORY,
                "addModifier",
                "ALT",
                "Modifier key + click adds the hovered inventory (or recipe viewer) item to the list. ALT / CTRL / SHIFT\n"
                    + "With SHIFT, shift-click adds items instead of moving them while the panel is shown.",
                new String[] { "ALT", "CTRL", "SHIFT" }),
            "addModifier").getString();
        maxVisibleRows = prop(
            cfg.get(CATEGORY, "maxVisibleRows", 10, "Maximum number of rows shown at once", 3, 40),
            "maxVisibleRows").getInt();
        panelWidth = prop(cfg.get(CATEGORY, "panelWidth", 160, "Panel width in pixels", 120, 320), "panelWidth")
            .getInt();
        ignoreNbtWhenTargetHasNone = prop(
            cfg.get(
                CATEGORY,
                "ignoreNbtWhenTargetHasNone",
                true,
                "When a goal item has no NBT, count inventory items regardless of their NBT"),
            "ignoreNbt").getBoolean();
        hudEnabled = prop(
            cfg.get(CATEGORY, "hudEnabled", true, "Show unfinished rows on the screen while playing"),
            "hud").getBoolean();
        hudAnchor = prop(
            cfg.get(
                CATEGORY,
                "hudAnchor",
                "BOTTOM_RIGHT",
                "Screen corner for the HUD list",
                new String[] { "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT" }),
            "hudAnchor").getString();
        hudOffsetX = cfg.get(CATEGORY, "hudOffsetX", 4, "HUD distance from the screen edge (horizontal)", 0, 2000)
            .getInt();
        hudOffsetY = cfg.get(CATEGORY, "hudOffsetY", 4, "HUD distance from the screen edge (vertical)", 0, 2000)
            .getInt();
        hudMaxRows = prop(cfg.get(CATEGORY, "hudMaxRows", 6, "Maximum rows on the HUD", 1, 30), "hudMaxRows").getInt();
        countOpenContainer = prop(
            cfg.get(
                CATEGORY,
                "countOpenContainer",
                true,
                "Also count items inside the container that is open (chests, machines, ...)"),
            "countContainer").getBoolean();
        listScope = prop(
            cfg.get(
                CATEGORY,
                "listScope",
                "WORLD",
                "WORLD = a separate list for each world/server, GLOBAL = one list shared by all worlds",
                new String[] { "WORLD", "GLOBAL" }),
            "listScope").getString();
        if (cfg.hasChanged()) cfg.save();
    }

    /** Saves the HUD on/off state after it was toggled in-game */
    public static void setHudEnabled(boolean on) {
        hudEnabled = on;
        if (config == null) return;
        config.get(CATEGORY, "hudEnabled", true)
            .set(on);
        config.save();
    }
}
