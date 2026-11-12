package io.github.junjun1z.inventorytodo;

import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import io.github.junjun1z.inventorytodo.compat.Compat;
import io.github.junjun1z.inventorytodo.compat.GuiUtil;
import io.github.junjun1z.inventorytodo.compat.VanillaFrontend;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * Development-only automated test. Never shipped: only compiled into a separate test jar.
 * Runs when config/inventorytodo/selftest.flag exists. Writes selftest_result.txt and quits.
 */
@Mod(modid = "inventorytodo_selftest", name = "Inventory ToDo SelfTest", version = "0", acceptableRemoteVersions = "*")
public class SelfTestMod {

    private final List<String> log = new ArrayList<>();
    private int menuTicks, joinTicks, waitedForItems;
    private int fails;
    private int step;
    private int wait;
    private GuiContainer gui;
    private boolean nei, gtnhNei;
    private File uiBackup;

    // real-input (java.awt.Robot) state
    private Robot robot;
    private double scaleX, scaleY, originX, originY;
    private int calX1, calY1, calMx1, calMy1, slotX, slotY;
    private int dragBeforeX, dragBeforeY;

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        if (!new File(InventoryTodo.configDir, "selftest.flag").exists()) return;
        nei = Loader.isModLoaded("NotEnoughItems");
        gtnhNei = nei && Loader.instance()
            .getIndexedModList()
            .get("NotEnoughItems")
            .getVersion()
            .contains("GTNH");
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    private void check(String name, boolean ok, Object detail) {
        String s = (ok ? "[OK]   " : "[FAIL] ") + name + (detail == null ? "" : "  -> " + detail);
        log.add(s);
        InventoryTodo.LOG.info("SELFTEST " + s);
        if (!ok) fails++;
    }

    private void info(String s) {
        log.add("[INFO] " + s);
        InventoryTodo.LOG.info("SELFTEST [INFO] " + s);
    }

    // ------------------------------------------------------------------ reflection helpers

    private static Object get(Object o, String f) throws Exception {
        Field fl = o.getClass()
            .getDeclaredField(f);
        fl.setAccessible(true);
        return fl.get(o);
    }

    private int geti(String f) throws Exception {
        if (gui != null) TodoPanel.INSTANCE.isOverPanel(gui, -1, -1); // recompute layout
        return (Integer) get(TodoPanel.INSTANCE, f);
    }

    private static TodoStore store() throws Exception {
        return (TodoStore) get(TodoPanel.INSTANCE, "store");
    }

    private int footerY() throws Exception {
        return geti("py") + geti("ph") - 14;
    }

    // ------------------------------------------------------------------ simulated input (through the frontend)

    private void invokeGui(String mcp, String srg, Class<?>[] types, Object... args) throws Exception {
        Method m = ReflectionHelper.findMethod(GuiContainer.class, gui, new String[] { mcp, srg }, types);
        m.invoke(gui, args);
    }

    private static final Class<?>[] III = { int.class, int.class, int.class };

    /** Press + release, routed like a real click */
    private void click(int x, int y, int b) throws Exception {
        if (nei) {
            // NEI patches GuiContainer.mouseClicked / mouseMovedOrUp to call its handlers
            invokeGui("mouseClicked", "func_73864_a", III, x, y, b);
            if (Minecraft.getMinecraft().currentScreen == gui)
                invokeGui("mouseMovedOrUp", "func_146286_b", III, x, y, b);
        } else {
            VanillaFrontend f = VanillaFrontend.instance;
            if (!f.dispatchMouse(gui, x, y, b, true, 0, false)) invokeGui("mouseClicked", "func_73864_a", III, x, y, b);
            if (Minecraft.getMinecraft().currentScreen == gui && !f.dispatchMouse(gui, x, y, b, false, 0, false))
                invokeGui("mouseMovedOrUp", "func_146286_b", III, x, y, b);
        }
    }

    private void scroll(int x, int y, int dir) {
        if (nei) TodoPanel.INSTANCE.mouseScrolled(gui, x, y, dir);
        else VanillaFrontend.instance.dispatchMouse(gui, x, y, -1, false, dir * 120, false);
    }

    private void type(char c, int key) throws Exception {
        if (nei) {
            // lwjgl3ify (GTNH) delivers characters through OS text input into TextFieldHandler.textBuffer
            if (c >= 32) {
                try {
                    Class<?> h = Class.forName("me.eigenraven.lwjgl3ify.client.TextFieldHandler");
                    ((StringBuilder) h.getField("textBuffer")
                        .get(null)).append(c);
                } catch (ClassNotFoundException ignored) {}
            }
            NeiHelper.keyTyped(gui, c, key);
        } else if (!VanillaFrontend.instance.dispatchKey(gui, true, c, key)) {
            invokeGui("keyTyped", "func_73869_a", new Class<?>[] { char.class, int.class }, c, key);
        }
    }

    private void typeString(String s) throws Exception {
        for (char c : s.toCharArray()) type(c, c == 'e' ? Keyboard.KEY_E : 0);
    }

    private static final class NeiHelper {

        static void keyTyped(GuiContainer gui, char c, int key) {
            codechicken.nei.guihook.GuiContainerManager.getManager(gui)
                .keyTyped(c, key);
        }

        static boolean hotkeyBlocked(GuiContainer gui) {
            return codechicken.nei.guihook.GuiContainerManager.getManager(gui)
                .onHotkeyPressed();
        }
    }

    // ------------------------------------------------------------------ real input (java.awt.Robot)

    private int screenX(double gx) {
        Minecraft mc = Minecraft.getMinecraft();
        return (int) Math.round(originX + gx * mc.displayWidth / gui.width * scaleX);
    }

    private int screenY(double gy) {
        Minecraft mc = Minecraft.getMinecraft();
        return (int) Math.round(originY + gy * mc.displayHeight / gui.height * scaleY);
    }

    private void robotMoveGui(double gx, double gy) {
        robot.mouseMove(screenX(gx + 0.5), screenY(gy + 0.5));
    }

    private static int windowMouseTop() {
        return Minecraft.getMinecraft().displayHeight - 1 - Mouse.getY();
    }

    private void robotKey(int vk) {
        robot.keyPress(vk);
        robot.keyRelease(vk);
    }

    // ------------------------------------------------------------------ misc

    private void shot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(
            mc.mcDataDir,
            "selftest_" + name + ".png",
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
    }

    private void openInv() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
        gui = (GuiContainer) mc.currentScreen;
    }

    private int[] slotCenterOf(net.minecraft.item.Item item) {
        for (Object o : gui.inventorySlots.inventorySlots) {
            Slot s = (Slot) o;
            if (s.getHasStack() && s.getStack()
                .getItem() == item) {
                return new int[] { GuiUtil.guiLeft(gui) + s.xDisplayPosition + 8,
                    GuiUtil.guiTop(gui) + s.yDisplayPosition + 8 };
            }
        }
        return null;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (wait > 0) {
            wait--;
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            switch (step) {
                case 0:
                    if (mc.theWorld == null && mc.currentScreen != null
                        && (mc.currentScreen instanceof GuiMainMenu || ++menuTicks > 200)) {
                        info(
                            "mode=" + (nei ? (gtnhNei ? "NEI (GTNH fork)" : "NEI (original)") : "standalone")
                                + " integration="
                                + Compat.get()
                                    .getClass()
                                    .getSimpleName());
                        File lists = new File(InventoryTodo.configDir, "lists");
                        new File(lists, "sp_claude_selftest.json").delete();
                        File ui = new File(InventoryTodo.configDir, "ui.json");
                        if (ui.exists()) {
                            uiBackup = new File(InventoryTodo.configDir, "ui.json.selftest-backup");
                            Files.move(ui.toPath(), uiBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                        WorldSettings ws = new WorldSettings(
                            1L,
                            WorldSettings.GameType.SURVIVAL,
                            false,
                            false,
                            WorldType.FLAT);
                        mc.launchIntegratedServer("claude_selftest", "claude_selftest", ws);
                        step++;
                    }
                    return;
                case 1:
                    if (mc.thePlayer != null && mc.theWorld != null
                        && (mc.currentScreen == null || ++joinTicks > 400)) {
                        if (mc.currentScreen != null) mc.displayGuiScreen(null);
                        step++;
                        wait = 60;
                    }
                    return;
                case 2: {
                    EntityPlayerMP sp = (EntityPlayerMP) MinecraftServer.getServer()
                        .getConfigurationManager().playerEntityList.get(0);
                    sp.inventory.addItemStackToInventory(new ItemStack(Items.iron_ingot, 32));
                    sp.inventory.addItemStackToInventory(new ItemStack(Blocks.cobblestone, 10));
                    sp.inventoryContainer.detectAndSendChanges();
                    step++;
                    wait = 20;
                    return;
                }
                case 3:
                    openInv();
                    step++;
                    wait = 10;
                    return;
                case 4: {
                    shot("01_empty");
                    int guiLeft = GuiUtil.guiLeft(gui);
                    check(
                        "Docked panel does not cover the inventory",
                        geti("px") + geti("pw") <= guiLeft,
                        "px=" + geti("px") + " pw=" + geti("pw") + " guiLeft=" + guiLeft);
                    click(geti("px") + 3 + 5, footerY() + 5, 0);
                    check("+ Note starts editing", get(TodoPanel.INSTANCE, "field") != null, null);
                    if (gtnhNei) check("NEI hotkeys blocked while typing", NeiHelper.hotkeyBlocked(gui), null);
                    typeString("鉄 Iron e");
                    check("Typing 'e' does not close the inventory", mc.currentScreen == gui, mc.currentScreen);
                    type((char) 13, Keyboard.KEY_RETURN);
                    TodoStore st = store();
                    check(
                        "Note saved (incl. CJK)",
                        st.entries.size() == 1 && "鉄 Iron e".equals(st.entries.get(0).text),
                        st.entries.size() == 1 ? st.entries.get(0).text : st.entries.size());
                    if (gtnhNei) check("NEI hotkeys work when not typing", !NeiHelper.hotkeyBlocked(gui), null);
                    boolean dnd = TodoPanel.INSTANCE
                        .dropStack(gui, geti("px") + 20, geti("py") + 20, new ItemStack(Items.iron_ingot, 5));
                    check(
                        "Dropped item is added",
                        dnd && st.entries.size() == 2 && st.entries.get(1).item.getItem() == Items.iron_ingot,
                        st.entries.size());
                    TodoPanel.INSTANCE
                        .dropStack(gui, geti("px") + 20, geti("py") + 20, new ItemStack(Items.iron_ingot));
                    check("Same item is not added twice", st.entries.size() == 2, st.entries.size());
                    mc.thePlayer.inventory.setItemStack(new ItemStack(Blocks.cobblestone, 7));
                    click(geti("px") + 60, geti("py") + 20, 0);
                    boolean stillHeld = mc.thePlayer.inventory.getItemStack() != null;
                    mc.thePlayer.inventory.setItemStack(null);
                    check(
                        "Click with held item adds it (goal = held amount)",
                        st.entries.size() == 3 && st.entries.get(2).target == 7,
                        st.entries.size() == 3 ? st.entries.get(2).target : st.entries.size());
                    check("Held item is not dropped or used up", stillHeld, null);
                    ItemStack named = new ItemStack(Items.diamond_sword);
                    named.setStackDisplayName("Test Sword");
                    TodoPanel.INSTANCE.dropStack(gui, geti("px") + 20, geti("py") + 20, named);
                    check(
                        "Item with NBT added",
                        st.entries.size() == 4 && st.entries.get(3).item.hasTagCompound(),
                        null);
                    step++;
                    wait = 10;
                    return;
                }
                case 5: {
                    TodoStore st = store();
                    TodoEntry iron = st.entries.get(1);
                    check("Counts items in inventory (32 iron)", iron.have == 32, iron.have);
                    check("Default goal = max stack size 64", iron.target == 64, iron.target);
                    check("Not done below goal", !iron.isDone(), null);
                    shot("02_items");
                    int ry = geti("listTop") + 18;
                    int cw = mc.fontRenderer.getStringWidth("[32/64]") + 4;
                    int cx = geti("px") + geti("pw") - 12 - cw + 2;
                    scroll(cx, ry + 8, -1);
                    check("Wheel down over count: goal -1", iron.target == 63, iron.target);
                    click(cx, ry + 8, 0);
                    check(
                        "Click on count opens number field",
                        get(TodoPanel.INSTANCE, "field") != null && (Boolean) get(TodoPanel.INSTANCE, "editTarget"),
                        null);
                    for (int i = 0; i < 5; i++) type((char) 8, Keyboard.KEY_BACK);
                    typeString("3a2");
                    type((char) 13, Keyboard.KEY_RETURN);
                    check("Goal typed as 32 (non-digits ignored)", iron.target == 32, iron.target);
                    check("Done automatically at goal", iron.isDone(), null);
                    ItemStack under = TodoPanel.INSTANCE.stackUnderMouse(gui, geti("px") + 20, ry + 8);
                    check("Item under mouse is reported", under != null && under.getItem() == Items.iron_ingot, under);
                    check(
                        "isOverPanel on the panel",
                        TodoPanel.INSTANCE.isOverPanel(gui, geti("px") + 20, ry + 8),
                        null);
                    check(
                        "Overlap test for recipe viewer slots",
                        TodoPanel.INSTANCE.overlaps(gui, geti("px") + 5, geti("py") + 5, 16, 16),
                        null);
                    int[] slot = slotCenterOf(Items.iron_ingot);
                    ItemStack hov = slot == null ? null
                        : Compat.get()
                            .hoveredStack(gui, slot[0], slot[1]);
                    check(
                        "Hovered inventory slot item is found (for modifier+click)",
                        hov != null && hov.getItem() == Items.iron_ingot,
                        hov);
                    List<String> extra = TodoPanel.INSTANCE.itemTooltipExtras(gui, geti("px") + 20, ry + 8);
                    check("Item tooltip has progress line", !extra.isEmpty(), extra);
                    List<String> help = TodoPanel.INSTANCE
                        .panelTooltip(gui, geti("px") + geti("pw") - 35 + 3, geti("py") + 4);
                    check("Help tooltip", help.size() >= 8, help.size());
                    step++;
                    wait = 5;
                    return;
                }
                case 6: {
                    shot("03_done");
                    TodoStore st = store();
                    click(geti("px") + 6, geti("listTop") + 8, 0);
                    check("Check box toggles done", st.entries.get(0).manualDone, null);
                    click(geti("px") + 20, geti("listTop") + 18 + 8, 0);
                    String scr = mc.currentScreen == null ? "null"
                        : mc.currentScreen.getClass()
                            .getSimpleName();
                    if (nei) check("Icon click opens NEI recipes", scr.contains("Recipe"), scr);
                    else check("Icon click without recipe viewer keeps the inventory", mc.currentScreen == gui, scr);
                    step++;
                    wait = 10;
                    return;
                }
                case 7:
                    shot("04_recipe");
                    openInv();
                    step++;
                    wait = 10;
                    return;
                case 8: {
                    if (nei && ++waitedForItems < 15) {
                        wait = 20; // give NEI time to build its item list
                        return;
                    }
                    click(geti("px") + 56 + 5, footerY() + 5, 0);
                    check("+ Item opens search", (Boolean) get(TodoPanel.INSTANCE, "searchOpen"), null);
                    typeString("dirt");
                    step++;
                    wait = 15;
                    return;
                }
                case 9: {
                    @SuppressWarnings("unchecked")
                    List<ItemStack> res = (List<ItemStack>) get(TodoPanel.INSTANCE, "results");
                    check("Name search finds results (dirt)", !res.isEmpty(), res.size());
                    info(
                        "searchable items: " + Compat.get()
                            .itemList()
                            .size());
                    shot("05_search");
                    int before = store().entries.size();
                    Method gx = TodoPanel.class.getDeclaredMethod("searchGridX");
                    gx.setAccessible(true);
                    Method gy = TodoPanel.class.getDeclaredMethod("searchGridY");
                    gy.setAccessible(true);
                    click((Integer) gx.invoke(TodoPanel.INSTANCE) + 8, (Integer) gy.invoke(TodoPanel.INSTANCE) + 8, 0);
                    check("Clicking a result adds it", store().entries.size() == before + 1, store().entries.size());
                    click(geti("px") + 56 + 5, footerY() + 5, 0);
                    typeString("minecraft:wool:14");
                    step++;
                    wait = 15;
                    return;
                }
                case 10: {
                    @SuppressWarnings("unchecked")
                    List<ItemStack> res = (List<ItemStack>) get(TodoPanel.INSTANCE, "results");
                    check(
                        "ID search minecraft:wool:14 gives one result",
                        res.size() == 1 && res.get(0)
                            .getItemDamage() == 14,
                        res.size());
                    type((char) 13, Keyboard.KEY_RETURN);
                    check("Enter adds the only result", !(Boolean) get(TodoPanel.INSTANCE, "searchOpen"), null);
                    click(geti("px") + 56 + 5, footerY() + 5, 0);
                    type((char) 0, Keyboard.KEY_ESCAPE);
                    check(
                        "Esc in search closes only the search",
                        mc.currentScreen == gui && !(Boolean) get(TodoPanel.INSTANCE, "searchOpen"),
                        mc.currentScreen);
                    TodoStore st = store();
                    click(geti("px") + geti("pw") - 23 + 3, geti("py") + 4, 0);
                    check(
                        "Mode button: pinned left",
                        st.mode == TodoStore.Mode.LEFT && geti("px") == 2,
                        st.mode + " px=" + geti("px"));
                    step++;
                    wait = 5;
                    return;
                }
                case 11: {
                    shot("06_left");
                    TodoStore st = store();
                    int hx = geti("px") + 20, hy = geti("py") + 5;
                    TodoPanel.INSTANCE.mouseClicked(gui, hx, hy, 0);
                    TodoPanel.INSTANCE.mouseDragged(gui, hx + 100, hy + 30, 0);
                    TodoPanel.INSTANCE.mouseUp(gui, hx + 100, hy + 30, 0);
                    check(
                        "Header drag moves the panel (free position)",
                        st.mode == TodoStore.Mode.FREE && st.freeX == 102 && st.freeY > 0,
                        st.mode + " " + st.freeX + "," + st.freeY);
                    click(geti("px") + geti("pw") - 11 + 3, geti("py") + 4, 0);
                    check("Minimize", st.minimized, null);
                    step++;
                    wait = 5;
                    return;
                }
                case 12: {
                    shot("07_mini");
                    TodoStore st = store();
                    click(geti("px") + 5, geti("py") + 5, 0);
                    check("Clicking the icon restores the panel", !st.minimized, null);
                    int n = st.entries.size();
                    click(geti("px") + geti("pw") - 7, geti("listTop") + 8, 0);
                    check("x deletes a row", st.entries.size() == n - 1, st.entries.size());
                    Field ck = TodoStore.class.getDeclaredField("currentKey");
                    ck.setAccessible(true);
                    ck.set(st, null);
                    st.ensureLoaded();
                    boolean nbtOk = false, metaOk = false;
                    for (TodoEntry en : st.entries) {
                        if (en.item == null) continue;
                        if (en.item.hasTagCompound() && "Test Sword".equals(en.item.getDisplayName())) nbtOk = true;
                        if (en.item.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.wool)
                            && en.item.getItemDamage() == 14) metaOk = true;
                    }
                    check("Save + reload keeps the rows", st.entries.size() == n - 1, st.entries.size());
                    check("NBT (named sword) survives save/reload", nbtOk, null);
                    check("Damage value (red wool 14) survives save/reload", metaOk, null);
                    step = 20;
                    wait = 5;
                    return;
                }

                // ---------------- real OS input via java.awt.Robot ----------------
                case 20: {
                    shot("08_before_robot");
                    try {
                        System.setProperty("java.awt.headless", "false");
                        robot = new Robot();
                        robot.setAutoDelay(15);
                    } catch (Throwable t) {
                        info("Robot unavailable, real-input tests skipped: " + t);
                        step = 90;
                        return;
                    }
                    // two-point calibration between screen and window coordinates
                    // start inside the window (Display position is a good first guess)
                    int wx = 300, wy = 200;
                    try {
                        wx = org.lwjgl.opengl.Display.getX();
                        wy = org.lwjgl.opengl.Display.getY();
                    } catch (Throwable ignored) {}
                    calX1 = wx + 60;
                    calY1 = wy + 80;
                    robot.mouseMove(calX1, calY1);
                    step++;
                    wait = 3;
                    return;
                }
                case 21: {
                    calMx1 = Mouse.getX();
                    calMy1 = windowMouseTop();
                    robot.mouseMove(calX1 + 200, calY1 + 150);
                    step++;
                    wait = 3;
                    return;
                }
                case 22: {
                    int sx1 = calX1, mx1 = calMx1, sy1 = calY1, my1 = calMy1;
                    int mx2 = Mouse.getX(), my2 = windowMouseTop();
                    if (mx2 == mx1 || my2 == my1) {
                        info("Mouse did not move with Robot (window not focused?), real-input tests skipped");
                        step = 90;
                        return;
                    }
                    scaleX = 200.0 / (mx2 - mx1);
                    scaleY = 150.0 / (my2 - my1);
                    originX = sx1 - mx1 * scaleX;
                    originY = sy1 - my1 * scaleY;
                    info(
                        String
                            .format("calibration scale=%.3f,%.3f origin=%.0f,%.0f", scaleX, scaleY, originX, originY));
                    // focus click on an empty area right of the inventory
                    robotMoveGui(gui.width - 5, 5);
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    step++;
                    wait = 5;
                    return;
                }
                case 23: {
                    check("Real click outside keeps the inventory open", mc.currentScreen == gui, mc.currentScreen);
                    // real click on "+ Note"
                    robotMoveGui(geti("px") + 3 + 5, footerY() + 5);
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    step++;
                    wait = 5;
                    return;
                }
                case 24: {
                    check("Real click on + Note starts editing", get(TodoPanel.INSTANCE, "field") != null, null);
                    robotKey(KeyEvent.VK_R);
                    robotKey(KeyEvent.VK_E);
                    robotKey(KeyEvent.VK_A);
                    robotKey(KeyEvent.VK_L);
                    step++;
                    wait = 5;
                    return;
                }
                case 25: {
                    check("Real 'e' key does not close the inventory", mc.currentScreen == gui, mc.currentScreen);
                    robotKey(KeyEvent.VK_ENTER);
                    step++;
                    wait = 5;
                    return;
                }
                case 26: {
                    TodoStore st = store();
                    String last = st.entries.isEmpty() ? null : st.entries.get(st.entries.size() - 1).text;
                    check("Real typing saved the note 'real'", "real".equals(last), last);
                    // real click on an inventory slot must still reach the inventory (pass-through)
                    int[] slot = slotCenterOf(net.minecraft.item.Item.getItemFromBlock(Blocks.cobblestone));
                    if (slot == null) {
                        check("Cobblestone slot found", false, null);
                        step = 30;
                        return;
                    }
                    robotMoveGui(slot[0], slot[1]);
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    slotX = slot[0];
                    slotY = slot[1];
                    step++;
                    wait = 5;
                    return;
                }
                case 27: {
                    ItemStack held = mc.thePlayer.inventory.getItemStack();
                    check(
                        "Real click on a slot picks the item up (input passes through)",
                        held != null && held.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.cobblestone),
                        held);
                    // real click on the panel while holding it: added, not dropped
                    robotMoveGui(geti("px") + 60, geti("listTop") + 8);
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    step++;
                    wait = 5;
                    return;
                }
                case 28: {
                    ItemStack held = mc.thePlayer.inventory.getItemStack();
                    check("Real click on panel with held item does not drop it", held != null, held);
                    shot("09_robot_held");
                    // put it back
                    robotMoveGui(slotX, slotY);
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    step++;
                    wait = 5;
                    return;
                }
                case 29: {
                    check("Item put back", mc.thePlayer.inventory.getItemStack() == null, null);
                    // real drag of the header: press, move while held
                    TodoStore st = store();
                    dragBeforeX = st.freeX;
                    dragBeforeY = st.freeY;
                    robotMoveGui(geti("px") + 20, geti("py") + 5);
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    step = 31;
                    wait = 3;
                    return;
                }
                case 31: {
                    robotMoveGui(geti("px") + 20 + 40, geti("py") + 5 + 25);
                    step++;
                    wait = 5;
                    return;
                }
                case 32: {
                    TodoStore st = store();
                    check(
                        "Real drag follows the cursor while held",
                        st.freeX > dragBeforeX + 20 && st.freeY > dragBeforeY + 10,
                        dragBeforeX + "," + dragBeforeY + " -> " + st.freeX + "," + st.freeY);
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    step++;
                    wait = 5;
                    return;
                }
                case 33: {
                    check("Drag ends on release", !(Boolean) get(TodoPanel.INSTANCE, "dragging"), null);
                    // hover a row item so the tooltip shows in the screenshot
                    robotMoveGui(geti("px") + 22, geti("listTop") + 18 + 8);
                    step = 90;
                    wait = 5;
                    return;
                }
                case 30:
                    step = 90;
                    return;

                case 90:
                    shot("10_final_tooltip");
                    finish();
                    step = 999;
                    return;
                default:
                    return;
            }
        } catch (Throwable t) {
            check("Exception at step " + step, false, t);
            InventoryTodo.LOG.error("SELFTEST exception", t);
            finish();
            step = 999;
        }
    }

    private void finish() {
        try {
            if (robot != null) robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } catch (Throwable ignored) {}
        try {
            File out = new File(Minecraft.getMinecraft().mcDataDir, "selftest_result.txt");
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8))) {
                for (String s : log) w.println(s);
                w.println(fails == 0 ? "ALL PASSED" : ("FAILED: " + fails));
            }
            new File(new File(InventoryTodo.configDir, "lists"), "sp_claude_selftest.json").delete();
            File ui = new File(InventoryTodo.configDir, "ui.json");
            if (uiBackup != null) Files.move(uiBackup.toPath(), ui.toPath(), StandardCopyOption.REPLACE_EXISTING);
            else ui.delete();
        } catch (Exception ex) {
            InventoryTodo.LOG.error("result write failed", ex);
        }
        new File(InventoryTodo.configDir, "selftest.flag").delete();
        Minecraft.getMinecraft()
            .shutdown();
    }
}
