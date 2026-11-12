package io.github.junjun1z.inventorytodo.core;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * User settings and panel state, stored in config/inventorytodo/settings.json.
 * Most of them can be changed from the settings page of the panel.
 */
public final class Settings {

    public enum Mode {
        FOLLOW, // next to the left edge of the inventory
        LEFT, // pinned to the left edge of the screen
        FREE // wherever it was dragged
    }

    public enum Scope {
        WORLD,
        GLOBAL
    }

    public enum HudPos {
        BOTTOM_RIGHT,
        RIGHT_CENTER,
        TOP_RIGHT,
        TOP_LEFT,
        BOTTOM_LEFT
    }

    // ---- options ----
    /** Modifier for "modifier + click adds the hovered item": ALT / CTRL / SHIFT */
    public String addModifier = "ALT";
    public int maxVisibleRows = 10;
    public int panelWidth = 160;
    public boolean ignoreDataWhenTargetHasNone = true;
    /** Also count items in the container that is open (chest, backpack...) */
    public boolean countOpenContainer = true;
    public Scope scope = Scope.WORLD;
    public boolean hudEnabled = false;
    public HudPos hudPos = HudPos.BOTTOM_RIGHT;
    public int hudMaxRows = 6;
    /** Hide finished rows on the HUD */
    public boolean hudHideDone = true;

    // ---- panel state ----
    public Mode mode = Mode.FOLLOW;
    public int freeX = 4, freeY = 30;
    public boolean minimized = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();

    private static Path file() {
        return Core.mc()
            .configDir()
            .resolve("settings.json");
    }

    public static Settings load() {
        Path f = file();
        if (Files.exists(f)) {
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                Settings s = GSON.fromJson(r, Settings.class);
                if (s != null) return s.sanitize();
            } catch (Exception e) {
                Log.warn("Could not read " + f + ", using defaults", e);
            }
        }
        Settings s = new Settings();
        s.save();
        return s;
    }

    private Settings sanitize() {
        if (mode == null) mode = Mode.FOLLOW;
        if (scope == null) scope = Scope.WORLD;
        if (hudPos == null) hudPos = HudPos.BOTTOM_RIGHT;
        if (addModifier == null) addModifier = "ALT";
        maxVisibleRows = Math.max(3, Math.min(40, maxVisibleRows));
        panelWidth = Math.max(120, Math.min(320, panelWidth));
        hudMaxRows = Math.max(1, Math.min(20, hudMaxRows));
        return this;
    }

    public void save() {
        Path f = file();
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Log.warn("Could not save " + f, e);
        }
    }
}
