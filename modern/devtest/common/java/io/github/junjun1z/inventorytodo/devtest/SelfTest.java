package io.github.junjun1z.inventorytodo.devtest;

import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.inventorytodo.core.Core;
import io.github.junjun1z.inventorytodo.core.Keys;
import io.github.junjun1z.inventorytodo.core.Log;
import io.github.junjun1z.inventorytodo.core.Mc;
import io.github.junjun1z.inventorytodo.core.Settings;
import io.github.junjun1z.inventorytodo.core.TodoEntry;
import io.github.junjun1z.inventorytodo.core.TodoPanel;
import io.github.junjun1z.inventorytodo.core.TodoStore;

/**
 * Automated in-game test (development builds only). Started by Core when config/inventorytodo/selftest.flag exists
 * and the game was launched into a world. Writes selftest_result.txt + screenshots, then quits.
 */
public final class SelfTest {

    private final DevPlatform dev;
    private final List<String> log = new ArrayList<>();
    private int fails, step, wait, waited, menuTicks;
    private Robot robot;
    private double scaleX, scaleY, originX, originY;
    private int calSx, calSy;
    private double calMx, calMy;
    private int slotX = -1, slotY = -1;
    private int dragBeforeX, dragBeforeY;

    private SelfTest(DevPlatform dev) {
        this.dev = dev;
    }

    public static void start() throws Exception {
        DevPlatform dev = (DevPlatform) Class.forName("io.github.junjun1z.inventorytodo.devtest.DevPlatformImpl")
            .getDeclaredConstructor()
            .newInstance();
        String flag = new String(Files.readAllBytes(Core.mc()
            .configDir()
            .resolve("selftest.flag")), StandardCharsets.UTF_8);
        if (flag.contains("showcase")) {
            Showcase sc = new Showcase(dev);
            Core.devTick = sc::tick;
            Log.info("SHOWCASE armed");
            return;
        }
        SelfTest t = new SelfTest(dev);
        Core.devTick = t::tick;
        Log.info("SELFTEST armed");
    }

    private static Mc mc() {
        return Core.mc();
    }

    private static TodoPanel panel() {
        return TodoPanel.INSTANCE;
    }

    private void check(String name, boolean ok, Object detail) {
        String s = (ok ? "[OK]   " : "[FAIL] ") + name + (detail == null ? "" : "  -> " + detail);
        log.add(s);
        Log.info("SELFTEST " + s);
        if (!ok) fails++;
    }

    private void info(String s) {
        log.add("[INFO] " + s);
        Log.info("SELFTEST [INFO] " + s);
    }

    private static int geti(String f) throws Exception {
        panel().isOverPanel(Core.mc() == null ? null : currentScreen, -1, -1); // recompute layout
        Field fl = TodoPanel.class.getDeclaredField(f);
        fl.setAccessible(true);
        return (Integer) fl.get(panel());
    }

    private static Object getf(String f) throws Exception {
        Field fl = TodoPanel.class.getDeclaredField(f);
        fl.setAccessible(true);
        return fl.get(panel());
    }

    private static Object currentScreen;

    private int footerY() throws Exception {
        return geti("py") + geti("ph") - 14;
    }

    private void type(String s) {
        for (char c : s.toCharArray()) panel().charTyped(currentScreen, c);
    }

    // ---------------------------------------------------------------- robot helpers

    private int sx(double gx) {
        return (int) Math.round(originX + gx * scaleX);
    }

    private int sy(double gy) {
        return (int) Math.round(originY + gy * scaleY);
    }

    private void move(double gx, double gy) {
        robot.mouseMove(sx(gx + 0.5), sy(gy + 0.5));
    }

    private void click() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private void key(int vk) {
        robot.keyPress(vk);
        robot.keyRelease(vk);
    }

    // ---------------------------------------------------------------- steps

    private void tick() {
        if (wait > 0) {
            wait--;
            return;
        }
        currentScreen = dev.screen();
        try {
            run();
        } catch (Throwable t) {
            check("Exception at step " + step, false, t);
            Log.error("SELFTEST exception", t);
            finish();
        }
    }

    private void run() throws Exception {
        Settings cfg = Core.settings();
        TodoStore store = Core.store();
        switch (step) {
            case 0:
                if (!dev.inWorld()) {
                    // versions without quick play: open the test world from the title screen ourselves
                    if (++waited % 100 == 1) Log.info("SELFTEST waiting for world, screen=" + currentScreen);
                    if (currentScreen != null && ++menuTicks == 100) dev.loadWorld("claude_selftest");
                    return;
                }
                if (currentScreen != null && ++waited < 200) {
                    dev.closeScreen();
                    return;
                }
                info("version=" + dev.versionName());
                dev.prepareShowcase(); // daytime + peaceful: nothing interrupts the test
                cfg.mode = Settings.Mode.FOLLOW;
                cfg.minimized = false;
                cfg.hudEnabled = false;
                cfg.scope = Settings.Scope.WORLD;
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
                dev.give("minecraft:cobblestone", 10);
                dev.give("minecraft:oak_planks", 3);
                dev.give("minecraft:redstone", 5);
                step++;
                wait = 20;
                return;
            case 2:
                dev.openInventory();
                step++;
                wait = 10;
                return;
            case 3: {
                Object scr = currentScreen;
                check("Inventory screen open", mc().isContainerScreen(scr), scr);
                int guiLeft = mc().guiLeft(scr);
                check("Docked panel does not cover the inventory", geti("px") + geti("pw") <= guiLeft,
                    "px=" + geti("px") + " pw=" + geti("pw") + " guiLeft=" + guiLeft);
                panel().mouseClicked(scr, geti("px") + 8, footerY() + 5, 0);
                check("+ Note starts editing", getf("field") != null, null);
                type("Iron e 鉄");
                check("Typing 'e' keeps the inventory open", dev.screen() == scr, dev.screen());
                panel().keyPressed(scr, Keys.ENTER);
                check("Note saved (incl. CJK)", store.entries.size() == 1 && "Iron e 鉄".equals(store.entries.get(0).text),
                    store.entries.isEmpty() ? "-" : store.entries.get(0).text);
                Object iron = mc().stackById("minecraft:iron_ingot");
                check("Dropped item added", panel().dropStack(scr, geti("px") + 20, geti("py") + 20, iron) && store.entries.size() == 2, store.entries.size());
                panel().dropStack(scr, geti("px") + 20, geti("py") + 20, iron);
                check("Same item not added twice", store.entries.size() == 2, store.entries.size());
                panel().addItem(mc().stackById("minecraft:chest"), 2);
                panel().addItem(mc().stackById("minecraft:torch"), 16);
                panel().addItem(mc().stackById("minecraft:redstone"), 16);
                check("Item rows added", store.entries.size() == 5, store.entries.size());
                step++;
                wait = 10;
                return;
            }
            case 4: {
                Object scr = currentScreen;
                TodoEntry iron = store.entries.get(1);
                check("Counts inventory (32 iron)", iron.have == 32, iron.have);
                check("Default goal = max stack 64", iron.target == 64, iron.target);
                TodoEntry red = store.entries.get(4);
                check("Partial amount counted (5 redstone)", red.have == 5, red.have);
                // Ctrl+click on the chest icon -> ingredients (needs Ctrl held; use the menu path instead)
                int rowY = geti("listTop") + 2 * 18;
                panel().mouseClicked(scr, geti("px") + 60, rowY + 8, 1);
                Object menu = getf("menu");
                check("Right-click opens the row menu", menu != null, null);
                step = 50;
                wait = 3;
                return;
            }
            case 50: {
                Object scr = currentScreen;
                dev.screenshot("selftest_menu");
                Object menu = getf("menu");
                if (menu != null) {
                    Field mx = menu.getClass().getDeclaredField("x");
                    Field my = menu.getClass().getDeclaredField("y");
                    mx.setAccessible(true);
                    my.setAccessible(true);
                    int x = (Integer) mx.get(menu), y = (Integer) my.get(menu);
                    panel().mouseClicked(scr, x + 5, y + 2 + 6, 0); // first entry: add ingredients
                }
                TodoEntry planks = null;
                for (TodoEntry e : store.entries)
                    if (e.item != null && String.valueOf(mc().registryId(e.item)).contains("planks")) planks = e;
                Mc.Recipe r = mc().recipeFor(mc().stackById("minecraft:chest"));
                info("chest recipe: " + (r == null ? "unavailable" : r.ingredients.size() + " ingredient(s)"));
                if (r != null && !r.ingredients.isEmpty()) {
                    check("Ingredients added (chest x2 -> 16 planks)", planks != null && planks.target == 16, planks == null ? "missing" : planks.target);
                }
                step = 5;
                wait = 10;
                return;
            }
            case 5: {
                dev.screenshot("selftest_panel");
                Object scr = currentScreen;
                // count wheel + typing a goal
                TodoEntry iron = store.entries.get(1);
                panel().mouseScrolled(scr, geti("px") + geti("pw") - 14, geti("listTop") + 18 + 8, -1);
                check("Wheel over count changes goal", iron.target == 63, iron.target);
                panel().mouseClicked(scr, geti("px") + geti("pw") - 14, geti("listTop") + 18 + 8, 0);
                check("Click on count edits the goal", getf("field") != null && (Boolean) getf("editTarget"), null);
                for (int i = 0; i < 4; i++) panel().keyPressed(scr, Keys.BACKSPACE);
                type("3a2");
                panel().keyPressed(scr, Keys.ENTER);
                check("Goal typed (digits only) = 32", iron.target == 32, iron.target);
                check("Done automatically at goal", iron.isDone(), null);
                // settings page
                panel().mouseClicked(scr, geti("px") + geti("pw") - 47 + 3, geti("py") + 4, 0);
                check("Settings page opens", (Boolean) getf("settingsOpen"), null);
                step++;
                wait = 5;
                return;
            }
            case 6: {
                dev.screenshot("selftest_settings");
                Object scr = currentScreen;
                panel().mouseClicked(scr, geti("px") + 20, geti("listTop") + 2 + 4 * 14 + 4, 0); // count open chests
                check("Setting toggled (count containers)", !cfg.countOpenContainer, cfg.countOpenContainer);
                panel().mouseClicked(scr, geti("px") + 20, geti("listTop") + 2 + 4 * 14 + 4, 0);
                panel().keyPressed(scr, Keys.ESCAPE);
                check("Esc closes settings only", dev.screen() == scr && !(Boolean) getf("settingsOpen"), dev.screen());
                // HUD button
                panel().mouseClicked(scr, geti("px") + geti("pw") - 35 + 3, geti("py") + 4, 0);
                check("H button turns the HUD on", cfg.hudEnabled, null);
                // search
                panel().mouseClicked(scr, geti("px") + 56 + 5, footerY() + 5, 0);
                type("iron");
                step++;
                wait = 15;
                return;
            }
            case 7: {
                @SuppressWarnings("unchecked")
                List<Object> res = (List<Object>) getf("results");
                check("Name search finds items (iron)", !res.isEmpty(), res.size());
                info("searchable items: " + mc().allItems().size());
                dev.screenshot("selftest_search");
                panel().keyPressed(currentScreen, Keys.ESCAPE);
                // persistence
                int n = store.entries.size();
                store.invalidate();
                store.ensureLoaded();
                boolean chest = false;
                for (TodoEntry e : store.entries)
                    if (e.item != null && "minecraft:chest".equals(mc().registryId(e.item))) chest = true;
                check("Save + reload keeps rows and items", store.entries.size() == n && chest, store.entries.size());
                step = 20;
                return;
            }
            // ---------------- real OS input (java.awt.Robot) ----------------
            case 20: {
                try {
                    System.setProperty("java.awt.headless", "false");
                    robot = new Robot();
                    robot.setAutoDelay(20);
                } catch (Throwable t) {
                    info("Robot unavailable (" + t + ") - checking input through the loader events instead");
                    step = 60;
                    return;
                }
                // Windows only lets a process take the foreground right after a key event: tap Alt first
                robot.keyPress(KeyEvent.VK_ALT);
                dev.focusWindow();
                robot.keyRelease(KeyEvent.VK_ALT);
                step = 19;
                wait = 10;
                return;
            }
            case 19: {
                // Window position is in physical pixels; Robot uses DPI-scaled desktop coordinates (Java 9+)
                double dpi = 1.0;
                try {
                    dpi = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDefaultConfiguration()
                        .getDefaultTransform()
                        .getScaleX();
                } catch (Throwable ignored) {}
                info("desktop scale " + dpi + ", window at " + dev.windowX() + "," + dev.windowY());
                calSx = (int) Math.round(dev.windowX() / dpi) + 80;
                calSy = (int) Math.round(dev.windowY() / dpi) + 80;
                robot.mouseMove(calSx, calSy);
                step = 21;
                wait = 4;
                return;
            }
            case 21:
                calMx = mc().mouseX();
                calMy = mc().mouseY();
                robot.mouseMove(calSx + 200, calSy + 150);
                step++;
                wait = 4;
                return;
            case 22: {
                double mx2 = mc().mouseX(), my2 = mc().mouseY();
                if (Math.abs(mx2 - calMx) < 1 || Math.abs(my2 - calMy) < 1) {
                    info("Robot cannot reach the window - checking input through the loader events instead");
                    step = 60;
                    return;
                }
                scaleX = 200.0 / (mx2 - calMx);
                scaleY = 150.0 / (my2 - calMy);
                if (scaleX < 0.2 || scaleX > 6 || scaleY < 0.2 || scaleY > 6 || Math.abs(scaleX - scaleY) > 0.3) {
                    info(String.format("Robot calibration looks wrong (%.3f, %.3f) - checking input through the loader events instead", scaleX, scaleY));
                    step = 60;
                    return;
                }
                originX = calSx - calMx * scaleX;
                originY = calSy - calMy * scaleY;
                info(String.format("calibration %.3f %.3f origin %.0f,%.0f", scaleX, scaleY, originX, originY));
                move(mc().screenWidth() - 4, 4); // empty corner: focuses the window
                click();
                step++;
                wait = 5;
                return;
            }
            case 23:
                check("Real click outside keeps the inventory", dev.screen() == currentScreen && mc().isContainerScreen(currentScreen), dev.screen());
                move(geti("px") + 8, footerY() + 5);
                click();
                step++;
                wait = 5;
                return;
            case 24:
                check("Real click on + Note starts editing", getf("field") != null, null);
                key(KeyEvent.VK_R);
                key(KeyEvent.VK_E);
                key(KeyEvent.VK_A);
                key(KeyEvent.VK_L);
                step++;
                wait = 5;
                return;
            case 25:
                check("Real 'e' key keeps the inventory open", dev.screen() == currentScreen && mc().isContainerScreen(currentScreen), dev.screen());
                key(KeyEvent.VK_ENTER);
                step++;
                wait = 5;
                return;
            case 26: {
                String last = store.entries.isEmpty() ? null : store.entries.get(store.entries.size() - 1).text;
                check("Real typing saved the note", "real".equals(last), last);
                // find a cobblestone slot: move the real cursor over the slots and ask which item is hovered
                Object scr = currentScreen;
                int left = mc().guiLeft(scr), top = mc().guiTop(scr);
                // hoveredSlotStack uses the real hovered slot, so probe in the next ticks
                probeX = left + 8;
                probeY = top + 84;
                move(probeX, probeY);
                step++;
                wait = 2;
                return;
            }
            case 27: {
                Object scr = currentScreen;
                int left = mc().guiLeft(scr), top = mc().guiTop(scr);
                Object hov = mc().hoveredSlotStack(scr, probeX, probeY);
                if (hov != null && "minecraft:cobblestone".equals(mc().registryId(hov))) {
                    slotX = probeX;
                    slotY = probeY;
                    click();
                    step++;
                    wait = 5;
                    return;
                }
                probeX += 18;
                if (probeX > left + 170) {
                    probeX = left + 8;
                    probeY += 18;
                }
                if (probeY > top + 170) {
                    check("Cobblestone slot found", false, null);
                    step = 30;
                    return;
                }
                move(probeX, probeY);
                wait = 2;
                return;
            }
            case 28: {
                Object held = mc().carriedStack();
                check("Real click on a slot reaches the inventory", held != null && !mc().isEmpty(held), held);
                move(geti("px") + 60, geti("listTop") + 8);
                click();
                step++;
                wait = 5;
                return;
            }
            case 29: {
                Object held = mc().carriedStack();
                check("Real click on the panel with a held item keeps it", held != null && !mc().isEmpty(held), held);
                move(slotX, slotY);
                click();
                step = 30;
                wait = 5;
                return;
            }
            case 30: {
                Object held = mc().carriedStack();
                check("Item put back", held == null || mc().isEmpty(held), held);
                dragBeforeX = geti("px");
                dragBeforeY = geti("py");
                move(geti("px") + 20, geti("py") + 5);
                step = 35; // press a little later: SDL may handle the press before the move otherwise
                wait = 3;
                return;
            }
            case 35:
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                step = 31;
                wait = 3;
                return;
            case 31:
                info(String.format("drag start: mouse=%.1f,%.1f down=%s dragging=%s startedAt=%s pressConsumed=%s carried=%s screen=%s", mc().mouseX(), mc().mouseY(), mc().isLeftMouseDown(), getf("dragging"), getf("dragStartedAt"), getf("pressConsumed"), mc().carriedStack(), dev.screen()));
                move(dragBeforeX + 20 + 30, dragBeforeY + 5 + 20);
                step++;
                wait = 5;
                return;
            case 32:
                info(String.format("drag end: mouse=%.1f,%.1f down=%s dragging=%s moved=%s", mc().mouseX(), mc().mouseY(), mc().isLeftMouseDown(), getf("dragging"), getf("dragMoved")));
                check("Real drag moves the panel", geti("px") > dragBeforeX + 10 && geti("py") > dragBeforeY + 5,
                    dragBeforeX + "," + dragBeforeY + " -> " + geti("px") + "," + geti("py"));
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                step++;
                wait = 5;
                return;
            case 33:
                check("Drag ends on release", !(Boolean) getf("dragging"), null);
                cfg.mode = Settings.Mode.FOLLOW;
                cfg.save();
                move(geti("px") + 22, geti("listTop") + 2 * 18 + 8); // hover the chest row for the tooltip
                step++;
                wait = 6;
                return;
            case 34:
                dev.screenshot("selftest_tooltip");
                step = 40;
                return;
            // ---------------- input through the loader's own event system ----------------
            case 60: {
                DevInputApi in;
                try {
                    in = (DevInputApi) Class.forName("io.github.junjun1z.inventorytodo.devtest.DevInput")
                        .getDeclaredConstructor()
                        .newInstance();
                } catch (ClassNotFoundException e) {
                    info("No loader event injector for this target");
                    step = 40;
                    return;
                }
                Object scr = currentScreen;
                cfg.mode = Settings.Mode.FOLLOW;
                cfg.save();
                boolean c1 = in.click(scr, geti("px") + 8, footerY() + 5, 0);
                check("Loader click event on + Note is consumed", c1, null);
                check("... and starts editing", getf("field") != null, null);
                boolean k1 = true;
                for (char c : "real e".toCharArray()) k1 &= in.character(scr, c);
                check("Loader char events are consumed while typing", k1, null);
                boolean eKey = in.key(scr, Keys.ENTER == 257 ? 69 : io.github.junjun1z.inventorytodo.core.Keys.A + 4);
                check("Inventory key is blocked while typing", eKey && dev.screen() == scr, dev.screen());
                check("Loader Enter key event is consumed", in.key(scr, Keys.ENTER), null);
                String last = store.entries.isEmpty() ? null : store.entries.get(store.entries.size() - 1).text;
                check("Note typed through loader events", "real e".equals(last), last);
                int left = mc().guiLeft(scr), top = mc().guiTop(scr);
                check("Click on the inventory is NOT consumed", !in.click(scr, left + 90, top + 100, 0), null);
                check("Keys pass through when not typing", !in.key(scr, Keys.ENTER), null);
                step = 34;
                return;
            }
            case 40:
                dev.closeScreen();
                step++;
                wait = 20;
                return;
            case 41:
                check("HUD stays enabled after closing", cfg.hudEnabled, null);
                dev.screenshot("selftest_hud");
                step++;
                wait = 2;
                return;
            case 42:
                cfg.hudEnabled = false;
                cfg.save();
                finish();
                return;
            default:
                return;
        }
    }

    private int probeX, probeY;

    private void finish() {
        Core.devTick = null;
        try {
            if (robot != null) robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } catch (Throwable ignored) {}
        try {
            List<String> out = new ArrayList<>(log);
            out.add(fails == 0 ? "ALL PASSED" : "FAILED: " + fails);
            Files.write(dev.gameDir().resolve("selftest_result.txt"), out, StandardCharsets.UTF_8);
            Files.deleteIfExists(mc().configDir().resolve("selftest.flag"));
        } catch (Exception e) {
            Log.error("SELFTEST could not write results", e);
        }
        dev.stop();
    }
}
