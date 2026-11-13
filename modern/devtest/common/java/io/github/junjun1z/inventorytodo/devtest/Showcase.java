package io.github.junjun1z.inventorytodo.devtest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;

import io.github.junjun1z.inventorytodo.core.Core;
import io.github.junjun1z.inventorytodo.core.Keys;
import io.github.junjun1z.inventorytodo.core.Log;
import io.github.junjun1z.inventorytodo.core.Mc;
import io.github.junjun1z.inventorytodo.core.Settings;
import io.github.junjun1z.inventorytodo.core.TodoEntry;
import io.github.junjun1z.inventorytodo.core.TodoPanel;
import io.github.junjun1z.inventorytodo.core.TodoStore;

/**
 * Development only: sets up a clean example list and saves screenshots for the mod page
 * (selftest.flag containing "showcase").
 */
final class Showcase {

    private final DevPlatform dev;
    private int step, wait, menuTicks;
    private Object screen;

    Showcase(DevPlatform dev) {
        this.dev = dev;
    }

    private static Mc mc() {
        return Core.mc();
    }

    private static TodoPanel panel() {
        return TodoPanel.INSTANCE;
    }

    private int geti(String f) throws Exception {
        panel().isOverPanel(screen, -1, -1);
        Field fl = TodoPanel.class.getDeclaredField(f);
        fl.setAccessible(true);
        return (Integer) fl.get(panel());
    }

    private Object getf(String f) throws Exception {
        Field fl = TodoPanel.class.getDeclaredField(f);
        fl.setAccessible(true);
        return fl.get(panel());
    }

    private void shot(String name) {
        dev.clearOverlays();
        dev.screenshot(name);
    }

    private TodoEntry add(String id, int goal) {
        panel().addItem(mc().stackById(id), goal);
        TodoStore st = Core.store();
        return st.entries.get(st.entries.size() - 1);
    }

    void tick() {
        if (wait > 0) {
            wait--;
            return;
        }
        screen = dev.screen();
        try {
            run();
        } catch (Throwable t) {
            Log.error("SHOWCASE failed at step " + step, t);
            finish();
        }
    }

    private void run() throws Exception {
        Settings cfg = Core.settings();
        TodoStore store = Core.store();
        switch (step) {
            case 0:
                if (!dev.inWorld()) {
                    if (screen != null && ++menuTicks == 100) dev.loadWorld("claude_selftest");
                    return;
                }
                if (screen != null) dev.closeScreen();
                dev.prepareShowcase();
                cfg.mode = Settings.Mode.FOLLOW;
                cfg.minimized = false;
                cfg.hudEnabled = false;
                cfg.hudPos = Settings.HudPos.BOTTOM_RIGHT;
                cfg.save();
                store.ensureLoaded();
                store.entries.clear();
                store.save();
                step++;
                wait = 40;
                return;
            case 1:
                dev.clearInventory();
                dev.give("minecraft:iron_ingot", 32);
                dev.give("minecraft:diamond", 3);
                dev.give("minecraft:oak_log", 64);
                dev.give("minecraft:redstone", 9);
                dev.give("minecraft:cobblestone", 40);
                dev.give("minecraft:torch", 12);
                step++;
                wait = 220; // let the advancement / recipe toasts appear and fade before any screenshot
                return;
            case 2:
                dev.openInventory();
                step++;
                wait = 10;
                return;
            case 3: {
                store.entries.add(TodoEntry.note("Build a nether portal"));
                store.save();
                add("minecraft:iron_ingot", 64);
                add("minecraft:diamond", 5);
                add("minecraft:oak_log", 64);
                add("minecraft:redstone", 16);
                add("minecraft:chest", 2);
                Core.recount(screen);
                Method ing = TodoPanel.class.getDeclaredMethod("addIngredients", int.class);
                ing.setAccessible(true);
                ing.invoke(panel(), store.entries.size() - 1); // chest -> oak planks
                step++;
                wait = 75; // let the "ingredients added" note fade so the new row is visible
                return;
            }
            case 4:
                shot("showcase_panel");
                step++;
                wait = 4;
                return;
            case 5: {
                int chestRow = 5;
                panel().mouseClicked(screen, geti("px") + 70, geti("listTop") + chestRow * 18 + 8, 1);
                step++;
                wait = 4;
                return;
            }
            case 6:
                shot("showcase_row_menu");
                panel().keyPressed(screen, Keys.ESCAPE);
                panel().mouseClicked(screen, geti("px") + geti("pw") - 47 + 3, geti("py") + 4, 0); // settings
                step++;
                wait = 4;
                return;
            case 7:
                shot("showcase_settings");
                panel().keyPressed(screen, Keys.ESCAPE);
                panel().mouseClicked(screen, geti("px") + 56 + 5, geti("py") + geti("ph") - 14 + 5, 0); // + Item
                for (char c : "diamond".toCharArray()) panel().charTyped(screen, c);
                step++;
                wait = 15;
                return;
            case 8:
                shot("showcase_item_search");
                panel().keyPressed(screen, Keys.ESCAPE);
                cfg.hudEnabled = true;
                cfg.save();
                dev.closeScreen();
                step++;
                wait = 25;
                return;
            case 9:
                shot("showcase_hud");
                step++;
                wait = 2;
                return;
            case 10:
                cfg.hudEnabled = false;
                cfg.save();
                finish();
                return;
            default:
                return;
        }
    }

    private void finish() {
        Core.devTick = null;
        try {
            Files.write(dev.gameDir()
                .resolve("selftest_result.txt"), java.util.Collections.singletonList("SHOWCASE DONE step=" + step));
            Files.deleteIfExists(mc().configDir()
                .resolve("selftest.flag"));
        } catch (Exception e) {
            Log.error("SHOWCASE could not write result", e);
        }
        dev.stop();
    }
}
