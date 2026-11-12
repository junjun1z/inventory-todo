package io.github.junjun1z.inventorytodo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import io.github.junjun1z.inventorytodo.compat.Compat;
import io.github.junjun1z.inventorytodo.compat.GuiUtil;

/**
 * The to-do panel drawn over inventory screens (any {@link GuiContainer}).
 * <p>
 * This class has no dependency on other mods. Input and rendering are fed to it by a frontend:
 * {@code VanillaFrontend} (standalone) or {@code NeiFrontend} (when NEI is installed).
 */
public final class TodoPanel {

    public static final TodoPanel INSTANCE = new TodoPanel();

    // ---- sizes ----
    private static final int HEADER_H = 12;
    private static final int ROW_H = 18;
    private static final int FOOTER_H = 14;
    private static final int MINI = 20;
    private static final int SEARCH_ROWS = 4;
    /** Narrowest width when docked next to the inventory (narrower screens may overlap) */
    private static final int MIN_DOCKED_W = 100;

    // ---- colours ----
    private static final int C_BG = 0xE0101018;
    private static final int C_BORDER = 0xFF5A5A82;
    private static final int C_HEADER = 0xF0262640;
    private static final int C_ROW_HOVER = 0x30FFFFFF;
    private static final int C_BTN = 0xFF3A3A5C;
    private static final int C_BTN_HOVER = 0xFF5A5A8C;
    private static final int C_TEXT = 0xFFFFFF;
    private static final int C_TEXT_DONE = 0x808080;
    private static final int C_TEXT_HINT = 0x9090A0;

    private final TodoStore store = new TodoStore();
    private boolean uiLoaded;

    // ---- layout (screen coordinates, recomputed every time) ----
    private int px, py, pw, ph;
    private int listTop, rowsVisible;
    private int scroll;

    // ---- dragging ----
    private boolean dragging, dragMoved, dragFromMini;
    private int dragOffX, dragOffY, dragStartX, dragStartY;
    // sub-pixel tracking of the real cursor every frame (smooth dragging)
    private boolean dragPolled;
    private float dragGrabX, dragGrabY, dragLastRawX, dragLastRawY;
    private float dragFracX, dragFracY;

    // ---- row editing ----
    private GuiTextField field;
    private int editIndex = -1;
    private boolean editTarget;

    // ---- item search ----
    private boolean searchOpen;
    private GuiTextField searchField;
    private final List<ItemStack> results = new ArrayList<>();
    private int resultScroll;
    private String computedQuery = "";
    private long queryChangedAt;

    // ---- highlight after adding ----
    private int flashIndex = -1;
    private long flashUntil;

    private int tickCounter;

    // ---- short status message in the footer ----
    private String status;
    private long statusUntil;

    private TodoPanel() {}

    // =====================================================================
    // helpers
    // =====================================================================

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    private static FontRenderer font() {
        return mc().fontRenderer;
    }

    private static String tr(String key, Object... args) {
        return args.length == 0 ? StatCollector.translateToLocal(key)
            : StatCollector.translateToLocalFormatted(key, args);
    }

    private static boolean active(GuiContainer gui) {
        return gui != null && mc().thePlayer != null;
    }

    private void prepare(GuiContainer gui) {
        if (!uiLoaded) {
            store.loadUi();
            uiLoaded = true;
        }
        store.ensureLoaded();
        layout(gui);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private boolean insidePanel(int mx, int my) {
        return inside(mx, my, px, py, pw, ph);
    }

    private static boolean isAddModifierDown() {
        String m = TodoConfig.addModifier.toUpperCase(Locale.ROOT);
        if ("SHIFT".equals(m)) return GuiScreen.isShiftKeyDown();
        if ("CTRL".equals(m)) return GuiScreen.isCtrlKeyDown();
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    /** "Alt" / "Ctrl" / "Shift" for help texts */
    private static String modifierName() {
        String m = TodoConfig.addModifier.toUpperCase(Locale.ROOT);
        if ("SHIFT".equals(m)) return "Shift";
        if ("CTRL".equals(m)) return "Ctrl";
        return "Alt";
    }

    private static boolean hasRecipeViewer() {
        return Compat.get()
            .hasRecipeViewer();
    }

    // =====================================================================
    // layout
    // =====================================================================

    private void layout(GuiContainer gui) {
        int sw = gui.width, sh = gui.height;
        if (store.minimized) {
            pw = MINI;
            ph = MINI;
        } else {
            pw = Math.min(TodoConfig.panelWidth, sw - 4);
            // When docked, shrink to the free space left of the inventory (don't cover armor slots etc.)
            if (store.mode != TodoStore.Mode.FREE) {
                int avail = GuiUtil.guiLeft(gui) - 6;
                if (avail >= MIN_DOCKED_W) pw = Math.min(pw, avail);
            }
            int maxRowsByScreen = Math.max(1, (sh - 8 - HEADER_H - FOOTER_H) / ROW_H);
            int body;
            if (searchOpen) {
                body = 16 + SEARCH_ROWS * ROW_H + 2;
                rowsVisible = 0;
            } else {
                rowsVisible = Math
                    .max(1, Math.min(store.entries.size(), Math.min(TodoConfig.maxVisibleRows, maxRowsByScreen)));
                body = rowsVisible * ROW_H;
            }
            ph = HEADER_H + body + FOOTER_H;
        }

        switch (store.mode) {
            case LEFT:
                px = 2;
                py = GuiUtil.guiTop(gui);
                break;
            case FREE:
                px = store.freeX;
                py = store.freeY;
                break;
            case FOLLOW:
            default:
                px = GuiUtil.guiLeft(gui) - pw - 4;
                if (px < 2) px = 2;
                py = GuiUtil.guiTop(gui);
                break;
        }
        px = clamp(px, 0, Math.max(0, sw - pw));
        py = clamp(py, 0, Math.max(0, sh - ph));
        listTop = py + HEADER_H;
        scroll = clamp(scroll, 0, Math.max(0, store.entries.size() - rowsVisible));
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    // =====================================================================
    // hit testing
    // =====================================================================

    private enum HitType {
        NONE,
        BODY,
        MINI,
        HEADER,
        BTN_HELP,
        BTN_MODE,
        BTN_MIN,
        BTN_HUD,
        ROW_CHECK,
        ROW_ICON,
        ROW_TEXT,
        ROW_COUNT,
        ROW_DEL,
        BTN_ADD_TEXT,
        BTN_ADD_ITEM,
        BTN_SEARCH_CLOSE,
        SEARCH_FIELD,
        SEARCH_RESULT
    }

    private static final class Hit {

        final HitType type;
        final int index;

        Hit(HitType type, int index) {
            this.type = type;
            this.index = index;
        }
    }

    private static final Hit NO_HIT = new Hit(HitType.NONE, -1);

    private int btnMinX() {
        return px + pw - 11;
    }

    private int btnModeX() {
        return px + pw - 23;
    }

    private int btnHelpX() {
        return px + pw - 35;
    }

    private int btnHudX() {
        return px + pw - 47;
    }

    private int footerY() {
        return py + ph - FOOTER_H;
    }

    private static final int ADD_TEXT_X = 3, ADD_TEXT_W = 50;
    private static final int ADD_ITEM_X = 56, ADD_ITEM_W = 62;

    private int counterWidth(TodoEntry e) {
        return font().getStringWidth(counterText(e)) + 4;
    }

    private int searchCols() {
        return Math.max(1, (pw - 6) / 18);
    }

    private int searchGridX() {
        int cols = searchCols();
        return px + 3 + ((pw - 6) - cols * 18) / 2;
    }

    private int searchGridY() {
        return listTop + 16;
    }

    private Hit hitTest(int mx, int my) {
        if (!insidePanel(mx, my)) return NO_HIT;
        if (store.minimized) return new Hit(HitType.MINI, -1);

        if (my < py + HEADER_H) {
            if (inside(mx, my, btnMinX(), py + 1, 10, 10)) return new Hit(HitType.BTN_MIN, -1);
            if (inside(mx, my, btnModeX(), py + 1, 10, 10)) return new Hit(HitType.BTN_MODE, -1);
            if (inside(mx, my, btnHelpX(), py + 1, 10, 10)) return new Hit(HitType.BTN_HELP, -1);
            if (inside(mx, my, btnHudX(), py + 1, 10, 10)) return new Hit(HitType.BTN_HUD, -1);
            return new Hit(HitType.HEADER, -1);
        }

        int fy = footerY();
        if (my >= fy) {
            if (searchOpen) {
                if (inside(mx, my, px + ADD_TEXT_X, fy + 1, ADD_TEXT_W, 12))
                    return new Hit(HitType.BTN_SEARCH_CLOSE, -1);
            } else {
                if (inside(mx, my, px + ADD_TEXT_X, fy + 1, ADD_TEXT_W, 12)) return new Hit(HitType.BTN_ADD_TEXT, -1);
                if (inside(mx, my, px + ADD_ITEM_X, fy + 1, ADD_ITEM_W, 12)) return new Hit(HitType.BTN_ADD_ITEM, -1);
            }
            return new Hit(HitType.BODY, -1);
        }

        if (searchOpen) {
            if (inside(mx, my, px + 4, listTop + 2, pw - 8, 12)) return new Hit(HitType.SEARCH_FIELD, -1);
            int gx = searchGridX(), gy = searchGridY(), cols = searchCols();
            if (mx >= gx && my >= gy && mx < gx + cols * 18 && my < gy + SEARCH_ROWS * 18) {
                int c = (mx - gx) / 18, r = (my - gy) / 18;
                int idx = (resultScroll + r) * cols + c;
                if (idx < results.size()) return new Hit(HitType.SEARCH_RESULT, idx);
            }
            return new Hit(HitType.BODY, -1);
        }

        int r = (my - listTop) / ROW_H;
        int i = scroll + r;
        if (r < 0 || r >= rowsVisible || i >= store.entries.size()) return new Hit(HitType.BODY, -1);
        TodoEntry e = store.entries.get(i);
        int ry = listTop + r * ROW_H;
        if (inside(mx, my, px + pw - 11, ry, 10, ROW_H)) return new Hit(HitType.ROW_DEL, i);
        if (inside(mx, my, px + 2, ry, 12, ROW_H)) return new Hit(HitType.ROW_CHECK, i);
        if (e.item != null) {
            if (inside(mx, my, px + 15, ry + 1, 16, 16)) return new Hit(HitType.ROW_ICON, i);
            int cw = counterWidth(e);
            int cx = px + pw - 12 - cw;
            if (inside(mx, my, cx, ry, cw, ROW_H)) return new Hit(HitType.ROW_COUNT, i);
        }
        return new Hit(HitType.ROW_TEXT, i);
    }

    // =====================================================================
    // data
    // =====================================================================

    private static boolean sameItem(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getItem() != b.getItem() || a.getItemDamage() != b.getItemDamage()) return false;
        return ItemStack.areItemStackTagsEqual(a, b);
    }

    /** Adds an item row. If the item is already listed, that row is highlighted instead. */
    private void addItem(ItemStack stack, int target) {
        if (stack == null || stack.getItem() == null) return;
        for (int i = 0; i < store.entries.size(); i++) {
            if (sameItem(store.entries.get(i).item, stack)) {
                flash(i);
                return;
            }
        }
        int t = target > 0 ? target : Math.max(1, stack.getMaxStackSize());
        store.entries.add(TodoEntry.item(stack, t));
        recount();
        store.saveList();
        int idx = store.entries.size() - 1;
        scrollTo(idx);
        flash(idx);
    }

    private void flash(int idx) {
        flashIndex = idx;
        flashUntil = System.currentTimeMillis() + 1200;
        scrollTo(idx);
    }

    private void scrollTo(int idx) {
        int rv = Math.max(1, rowsVisible);
        if (idx < scroll) scroll = idx;
        else if (idx >= scroll + rv) scroll = idx - rv + 1;
    }

    private void removeEntry(int idx) {
        if (idx < 0 || idx >= store.entries.size()) return;
        cancelEdit();
        store.entries.remove(idx);
        store.saveList();
    }

    private void moveEntry(int idx, int dir) {
        int j = idx + dir;
        if (idx < 0 || j < 0 || idx >= store.entries.size() || j >= store.entries.size()) return;
        cancelEdit();
        Collections.swap(store.entries, idx, j);
        store.saveList();
        scrollTo(j);
    }

    /** Recounts how many of each goal item you have (inventory, plus the open container if enabled) */
    private void recount() {
        recount(mc().currentScreen instanceof GuiContainer ? (GuiContainer) mc().currentScreen : null);
    }

    private void recount(GuiContainer gui) {
        if (mc().thePlayer == null) return;
        ItemStack[] inv = mc().thePlayer.inventory.mainInventory;
        List<ItemStack> extra = new ArrayList<>();
        if (gui != null && TodoConfig.countOpenContainer
            && !(gui instanceof GuiContainerCreative)
            && gui.inventorySlots != null) {
            for (Object o : gui.inventorySlots.inventorySlots) {
                if (!(o instanceof Slot)) continue;
                Slot slot = (Slot) o;
                if (slot.inventory == mc().thePlayer.inventory || slot instanceof SlotCrafting) continue;
                ItemStack s = slot.getStack();
                if (s != null) extra.add(s);
            }
        }
        for (TodoEntry e : store.entries) {
            if (e.item == null) continue;
            int n = 0;
            for (ItemStack s : inv) {
                if (s != null && countsAs(e.item, s)) n += s.stackSize;
            }
            for (ItemStack s : extra) {
                if (countsAs(e.item, s)) n += s.stackSize;
            }
            e.have = n;
        }
    }

    /** Adds the crafting ingredients for the item of a row (amounts for the remaining goal) */
    private void addIngredients(int idx) {
        TodoEntry e = store.entries.get(idx);
        if (e.item == null) return;
        RecipeHelper.Recipe r = RecipeHelper.find(e.item);
        if (r == null) {
            showStatus(tr("inventorytodo.status.norecipe"));
            return;
        }
        int goal = e.target > 0 ? e.target - Math.min(e.have, e.target) : 1;
        if (goal <= 0) goal = Math.max(1, e.target);
        int crafts = (goal + r.outputCount - 1) / r.outputCount;
        int last = -1;
        for (RecipeHelper.Ingredient in : r.ingredients) {
            int amount = (int) Math.min(999_999_999L, (long) in.count * crafts);
            int found = -1;
            for (int i = 0; i < store.entries.size(); i++) {
                if (sameItem(store.entries.get(i).item, in.stack)) {
                    found = i;
                    break;
                }
            }
            if (found >= 0) {
                TodoEntry ex = store.entries.get(found);
                ex.target = (int) Math.min(999_999_999L, (long) ex.target + amount);
                last = found;
            } else {
                store.entries.add(TodoEntry.item(in.stack, amount));
                last = store.entries.size() - 1;
            }
        }
        recount();
        store.saveList();
        if (last >= 0) flash(last);
        showStatus(tr("inventorytodo.status.ingredients", e.displayText()));
    }

    private void showStatus(String s) {
        status = s;
        statusUntil = System.currentTimeMillis() + 2500;
    }

    // ---- HUD access ----

    /** The current list (for the HUD) */
    List<TodoEntry> entriesForHud() {
        if (!uiLoaded) {
            store.loadUi();
            uiLoaded = true;
        }
        store.ensureLoaded();
        return store.entries;
    }

    /** Recount while no screen is open (for the HUD) */
    void refreshForHud() {
        entriesForHud();
        recount(null);
    }

    private static boolean countsAs(ItemStack target, ItemStack s) {
        Item item = target.getItem();
        if (s.getItem() != item) return false;
        boolean anyMeta = target.getItemDamage() == 32767 || item.isDamageable();
        if (!anyMeta && s.getItemDamage() != target.getItemDamage()) return false;
        if (target.hasTagCompound()) return ItemStack.areItemStackTagsEqual(target, s);
        return TodoConfig.ignoreNbtWhenTargetHasNone || !s.hasTagCompound();
    }

    private static String counterText(TodoEntry e) {
        if (e.target > 0) return "[" + e.have + "/" + e.target + "]";
        return "[" + e.have + "]";
    }

    // ---- editing ----

    private void startEdit(int idx, boolean target) {
        commitEdit();
        if (idx < 0 || idx >= store.entries.size()) return;
        TodoEntry e = store.entries.get(idx);
        editIndex = idx;
        editTarget = target;
        int w = target ? 44 : Math.max(30, pw - (e.item != null ? 34 : 15) - 14);
        field = new GuiTextField(font(), 0, 0, w, 12);
        field.setMaxStringLength(target ? 9 : 200);
        field.setText(target ? String.valueOf(e.target) : (e.text == null ? "" : e.text));
        field.setFocused(true);
        field.setCursorPositionEnd();
        scrollTo(idx);
    }

    private void commitEdit() {
        if (field == null) return;
        if (editIndex >= 0 && editIndex < store.entries.size()) {
            TodoEntry e = store.entries.get(editIndex);
            if (editTarget) {
                String digits = field.getText()
                    .replaceAll("[^0-9]", "");
                try {
                    e.target = digits.isEmpty() ? 0 : Math.min(Integer.parseInt(digits), 999_999_999);
                } catch (NumberFormatException ex) {
                    e.target = 999_999_999;
                }
            } else {
                e.text = field.getText();
                // empty note rows are removed
                if (e.item == null && e.text.trim()
                    .isEmpty()) {
                    store.entries.remove(editIndex);
                }
            }
            store.saveList();
        }
        unfocus(field);
        field = null;
        editIndex = -1;
    }

    private void cancelEdit() {
        if (field == null) return;
        if (!editTarget && editIndex >= 0 && editIndex < store.entries.size()) {
            TodoEntry e = store.entries.get(editIndex);
            // a row that was just created and is still empty is removed on cancel
            if (e.item == null && (e.text == null || e.text.trim()
                .isEmpty())) {
                store.entries.remove(editIndex);
                store.saveList();
            }
        }
        unfocus(field);
        field = null;
        editIndex = -1;
    }

    /**
     * Unfocus a text field before dropping it. lwjgl3ify starts/stops OS text input (IME) on focus changes,
     * so it would stay open otherwise.
     */
    private static void unfocus(GuiTextField f) {
        if (f != null && f.isFocused()) f.setFocused(false);
    }

    // ---- search ----

    private void openSearch() {
        commitEdit();
        searchOpen = true;
        searchField = new GuiTextField(font(), 0, 0, Math.max(40, pw - 8), 12);
        searchField.setMaxStringLength(100);
        searchField.setFocused(true);
        results.clear();
        resultScroll = 0;
        computedQuery = "";
    }

    private void closeSearch() {
        searchOpen = false;
        unfocus(searchField);
        searchField = null;
        results.clear();
    }

    private void updateSearchIfNeeded() {
        if (!searchOpen || searchField == null) return;
        String q = searchField.getText();
        if (q.equals(computedQuery)) return;
        // wait a little while typing (don't search huge item lists on every key)
        if (System.currentTimeMillis() - queryChangedAt < 200) return;
        computedQuery = q;
        runSearch(q);
    }

    private void runSearch(String query) {
        results.clear();
        resultScroll = 0;
        String q = query.trim()
            .toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return;

        // "modid:name" or "modid:name:meta"
        String idPart = null;
        int idMeta = -1;
        if (q.indexOf(':') >= 0) {
            String[] parts = q.split(":");
            if (parts.length == 3 && parts[2].matches("\\d+")) {
                idPart = parts[0] + ":" + parts[1];
                idMeta = Integer.parseInt(parts[2]);
            } else {
                idPart = q;
            }
        }

        List<ItemStack> src = Compat.get()
            .itemList();
        if (src == null) return;
        try {
            for (ItemStack s : src) {
                if (results.size() >= 2000) break;
                if (s == null || s.getItem() == null) continue;
                if (idPart != null) {
                    String reg = String.valueOf(Item.itemRegistry.getNameForObject(s.getItem()))
                        .toLowerCase(Locale.ROOT);
                    if (idMeta >= 0) {
                        if (reg.equals(idPart) && s.getItemDamage() == idMeta) results.add(s);
                    } else if (reg.contains(idPart)) {
                        results.add(s);
                    }
                    continue;
                }
                String name;
                try {
                    name = s.getDisplayName();
                } catch (Throwable t) {
                    continue;
                }
                if (name != null && EnumChatFormatting.getTextWithoutFormattingCodes(name)
                    .toLowerCase(Locale.ROOT)
                    .contains(q)) {
                    results.add(s);
                } else if (EnglishNames.matches(s, q)) {
                    // English names (as used by wikis) and registry names work in every language
                    results.add(s);
                }
            }
        } catch (RuntimeException ex) {
            // the item list changed while searching (e.g. NEI reloading); the next key press searches again
            InventoryTodo.LOG.debug("Item list changed during search", ex);
        }

        // Exact ID of an item that is not in any list (hidden/technical items)
        if (results.isEmpty() && idPart != null) {
            Object obj = Item.itemRegistry.getObject(idPart);
            if (obj instanceof Item) results.add(new ItemStack((Item) obj, 1, Math.max(0, idMeta)));
        }
    }

    // =====================================================================
    // input (called by the frontends)
    // =====================================================================

    /** @return true if the key was used (and must not reach the screen) */
    public boolean keyTyped(GuiContainer gui, char c, int key) {
        if (!active(gui)) return false;
        if (field != null) {
            if (key == Keyboard.KEY_ESCAPE) {
                cancelEdit();
            } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                commitEdit();
            } else if (!editTarget || c < 32 || Character.isDigit(c)) {
                field.textboxKeyTyped(c, key);
            }
            return true; // swallow every key while typing ("e" must not close the inventory)
        }
        if (searchOpen && searchField != null && searchField.isFocused()) {
            if (key == Keyboard.KEY_ESCAPE) {
                closeSearch();
            } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                computedQuery = searchField.getText();
                runSearch(computedQuery);
                if (results.size() == 1) {
                    addItem(results.get(0), 0);
                    closeSearch();
                }
            } else {
                searchField.textboxKeyTyped(c, key);
                queryChangedAt = System.currentTimeMillis();
            }
            return true;
        }
        if (searchOpen && key == Keyboard.KEY_ESCAPE) {
            closeSearch();
            return true;
        }
        int addKey = KeyBindings.ADD_HOVERED.getKeyCode();
        if (addKey != 0 && key == addKey) {
            int mx = Mouse.getX() * gui.width / mc().displayWidth;
            int my = gui.height - Mouse.getY() * gui.height / mc().displayHeight - 1;
            prepare(gui);
            ItemStack s = insidePanel(mx, my) ? null
                : Compat.get()
                    .hoveredStack(gui, mx, my);
            if (s != null) {
                addItem(s, 0);
                return true;
            }
        }
        return false;
    }

    /** True while a text field has focus */
    public boolean isTyping() {
        return field != null || (searchOpen && searchField != null && searchField.isFocused());
    }

    /** @return true if the click was used (and must not reach the screen) */
    public boolean mouseClicked(GuiContainer gui, int mx, int my, int button) {
        if (!active(gui)) return false;
        prepare(gui);
        Hit h = hitTest(mx, my);

        // clicking elsewhere while editing saves the edit
        if (field != null) {
            boolean onSameRow = (h.type == HitType.ROW_TEXT || h.type == HitType.ROW_COUNT) && h.index == editIndex;
            if (onSameRow) {
                field.mouseClicked(mx, my, button);
                return true;
            }
            commitEdit();
            layout(gui);
            h = hitTest(mx, my);
        }

        if (h.type == HitType.NONE) {
            // outside the panel: modifier + click adds the hovered inventory / recipe viewer item
            if ((button == 0 || button == 1) && isAddModifierDown()) {
                ItemStack s = Compat.get()
                    .hoveredStack(gui, mx, my);
                if (s != null) {
                    addItem(s, 0);
                    return true;
                }
            }
            if (searchField != null) searchField.setFocused(false);
            return false;
        }

        // clicking the panel while holding an item adds it (the item is not used up)
        ItemStack held = mc().thePlayer.inventory.getItemStack();
        if (held != null && h.type != HitType.MINI) {
            addItem(held, held.stackSize > 1 ? held.stackSize : 0);
            return true;
        }

        switch (h.type) {
            case MINI:
            case HEADER:
                if (button == 0) {
                    dragging = true;
                    dragMoved = false;
                    dragFromMini = h.type == HitType.MINI;
                    dragStartX = mx;
                    dragStartY = my;
                    dragOffX = mx - px;
                    dragOffY = my - py;
                    dragPolled = false;
                    dragFracX = dragFracY = 0f;
                    dragLastRawX = rawMouseX(gui);
                    dragLastRawY = rawMouseY(gui);
                    dragGrabX = dragLastRawX - px;
                    dragGrabY = dragLastRawY - py;
                }
                break;
            case BTN_MODE:
                store.mode = TodoStore.Mode.values()[(store.mode.ordinal() + 1) % TodoStore.Mode.values().length];
                if (store.mode == TodoStore.Mode.FREE) {
                    store.freeX = px;
                    store.freeY = py;
                }
                store.saveUi();
                break;
            case BTN_HUD:
                TodoConfig.setHudEnabled(!TodoConfig.hudEnabled);
                break;
            case BTN_MIN:
                commitEdit();
                closeSearch();
                store.minimized = true;
                store.saveUi();
                break;
            case ROW_CHECK: {
                TodoEntry e = store.entries.get(h.index);
                e.manualDone = !e.manualDone;
                store.saveList();
                break;
            }
            case ROW_ICON: {
                ItemStack s = store.entries.get(h.index).item;
                if (s != null && button == 0 && GuiScreen.isCtrlKeyDown()) {
                    addIngredients(h.index);
                } else if (s != null && hasRecipeViewer() && (button == 0 || button == 1)) {
                    Compat.get()
                        .showRecipes(s, button == 1);
                }
                break;
            }
            case ROW_TEXT:
                if (button == 0) startEdit(h.index, false);
                break;
            case ROW_COUNT:
                if (button == 0) startEdit(h.index, true);
                else if (button == 1) {
                    // right-click: set the goal to the amount you have now
                    TodoEntry e = store.entries.get(h.index);
                    e.target = Math.max(1, e.have);
                    store.saveList();
                }
                break;
            case ROW_DEL:
                removeEntry(h.index);
                break;
            case BTN_ADD_TEXT:
                store.entries.add(TodoEntry.text(""));
                layout(gui);
                startEdit(store.entries.size() - 1, false);
                break;
            case BTN_ADD_ITEM:
                openSearch();
                break;
            case BTN_SEARCH_CLOSE:
                closeSearch();
                break;
            case SEARCH_FIELD:
                if (searchField != null) {
                    if (button == 1) {
                        searchField.setText("");
                        queryChangedAt = 0;
                    }
                    searchField.setFocused(true);
                    searchField.mouseClicked(mx, my, 0);
                }
                break;
            case SEARCH_RESULT:
                addItem(results.get(h.index), 0);
                if (!GuiScreen.isShiftKeyDown()) closeSearch(); // Shift: keep adding
                break;
            default:
                break;
        }
        return true; // clicks on the panel never reach the screen (prevents dropping items by accident)
    }

    public void mouseUp(GuiContainer gui, int mx, int my, int button) {
        if (!dragging) return;
        dragging = false;
        if (dragFracX >= 0.5f) store.freeX++;
        if (dragFracY >= 0.5f) store.freeY++;
        dragFracX = dragFracY = 0f;
        if (dragMoved) {
            store.saveUi();
        } else if (dragFromMini && store.minimized) {
            store.minimized = false;
            store.saveUi();
        }
    }

    /**
     * @param dir +1 = wheel up, -1 = wheel down
     * @return true if used
     */
    public boolean mouseScrolled(GuiContainer gui, int mx, int my, int dir) {
        if (!active(gui)) return false;
        prepare(gui);
        if (!insidePanel(mx, my) || store.minimized) return false;
        if (searchOpen) {
            int cols = searchCols();
            int totalRows = (results.size() + cols - 1) / cols;
            resultScroll = clamp(resultScroll - dir, 0, Math.max(0, totalRows - SEARCH_ROWS));
            return true;
        }
        Hit h = hitTest(mx, my);
        if (h.type == HitType.ROW_COUNT) {
            TodoEntry e = store.entries.get(h.index);
            int mult = GuiScreen.isCtrlKeyDown() ? 64 : (GuiScreen.isShiftKeyDown() ? 16 : 1);
            e.target = clamp(e.target + dir * mult, 0, 999_999_999);
            store.saveList();
            return true;
        }
        if (GuiScreen.isShiftKeyDown() && h.index >= 0) {
            moveEntry(h.index, -dir); // Shift + wheel: reorder
            return true;
        }
        scroll = clamp(scroll - dir, 0, Math.max(0, store.entries.size() - rowsVisible));
        return true;
    }

    public void mouseDragged(GuiContainer gui, int mx, int my, int button) {
        if (!dragging) return;
        if (!dragMoved && Math.abs(mx - dragStartX) + Math.abs(my - dragStartY) > 2) dragMoved = true;
        // once the real cursor is tracked, pollDrag owns the position (two writers would jitter)
        if (dragMoved && !dragPolled) {
            store.mode = TodoStore.Mode.FREE;
            store.freeX = Math.max(0, mx - dragOffX);
            store.freeY = Math.max(0, my - dragOffY);
        }
    }

    private float rawMouseX(GuiContainer gui) {
        return Mouse.getX() * gui.width / (float) mc().displayWidth;
    }

    private float rawMouseY(GuiContainer gui) {
        return gui.height - Mouse.getY() * gui.height / (float) mc().displayHeight;
    }

    /** Moves the panel to the real cursor position every frame (sub-pixel), without waiting for mouse events */
    private void pollDrag(GuiContainer gui) {
        if (!dragging) return;
        if (!Mouse.isButtonDown(0)) {
            // the release happened somewhere we did not see it
            mouseUp(gui, 0, 0, 0);
            return;
        }
        float rx = rawMouseX(gui), ry = rawMouseY(gui);
        if (rx == dragLastRawX && ry == dragLastRawY) return;
        dragLastRawX = rx;
        dragLastRawY = ry;
        float nx = rx - dragGrabX, ny = ry - dragGrabY;
        if (!dragMoved && Math.abs(nx - px) + Math.abs(ny - py) <= 2f) return;
        dragMoved = true;
        dragPolled = true;
        store.mode = TodoStore.Mode.FREE;
        nx = Math.max(0f, Math.min(nx, Math.max(0, gui.width - pw)));
        ny = Math.max(0f, Math.min(ny, Math.max(0, gui.height - ph)));
        store.freeX = (int) Math.floor(nx);
        store.freeY = (int) Math.floor(ny);
        dragFracX = nx - store.freeX;
        dragFracY = ny - store.freeY;
    }

    // =====================================================================
    // lifecycle
    // =====================================================================

    public void tick(GuiContainer gui) {
        if (!active(gui)) return;
        store.ensureLoaded();
        if (++tickCounter % 5 == 0) recount();
        if (field != null) field.updateCursorCounter();
        if (searchField != null) searchField.updateCursorCounter();
    }

    /** A container screen was opened (or resized) */
    public void onGuiOpened(GuiContainer gui) {
        onGuiClosed();
        if (active(gui)) {
            store.ensureLoaded();
            recount();
        }
    }

    /** Leaving the screen: save the edit in progress */
    public void onGuiClosed() {
        commitEdit();
        closeSearch();
        dragging = false;
        dragFracX = dragFracY = 0f;
    }

    // =====================================================================
    // queries (called by the frontends)
    // =====================================================================

    public boolean isOverPanel(GuiContainer gui, int mx, int my) {
        if (!active(gui)) return false;
        prepare(gui);
        return insidePanel(mx, my);
    }

    /** The item shown under the mouse on the panel, or null */
    public ItemStack stackUnderMouse(GuiContainer gui, int mx, int my) {
        if (!active(gui)) return null;
        prepare(gui);
        Hit h = hitTest(mx, my);
        switch (h.type) {
            case ROW_ICON:
            case ROW_TEXT:
            case ROW_COUNT: {
                ItemStack s = store.entries.get(h.index).item;
                return s == null ? null : s.copy();
            }
            case SEARCH_RESULT:
                return results.get(h.index)
                    .copy();
            default:
                return null;
        }
    }

    /** An item was dropped on the screen (e.g. dragged from a recipe viewer) */
    public boolean dropStack(GuiContainer gui, int mx, int my, ItemStack stack) {
        if (!active(gui) || stack == null) return false;
        prepare(gui);
        if (!insidePanel(mx, my)) return false;
        addItem(stack, 0);
        return true;
    }

    /** True if the rectangle overlaps the panel */
    public boolean overlaps(GuiContainer gui, int x, int y, int w, int h) {
        if (!active(gui)) return false;
        prepare(gui);
        return x < px + pw && x + w > px && y < py + ph && y + h > py;
    }

    /** Tooltip for panel controls (when no item is under the mouse) */
    public List<String> panelTooltip(GuiContainer gui, int mx, int my) {
        List<String> tip = new ArrayList<>();
        if (!active(gui)) return tip;
        prepare(gui);
        Hit h = hitTest(mx, my);
        switch (h.type) {
            case MINI:
                tip.add(tr("inventorytodo.tip.mini"));
                tip.add(EnumChatFormatting.GRAY + tr("inventorytodo.tip.mini.remaining", remainingCount()));
                break;
            case BTN_MIN:
                tip.add(tr("inventorytodo.tip.minimize"));
                break;
            case BTN_HUD:
                tip.add(
                    tr(
                        "inventorytodo.tip.hud",
                        tr(TodoConfig.hudEnabled ? "inventorytodo.state.on" : "inventorytodo.state.off")));
                tip.add(EnumChatFormatting.GRAY + tr("inventorytodo.tip.hud.desc"));
                break;
            case BTN_MODE:
                tip.add(
                    tr(
                        "inventorytodo.tip.mode",
                        tr(
                            "inventorytodo.mode." + store.mode.name()
                                .toLowerCase(Locale.ROOT))));
                tip.add(EnumChatFormatting.GRAY + tr("inventorytodo.tip.mode.desc"));
                break;
            case BTN_HELP:
                tip.add(tr("inventorytodo.help.title"));
                addHelp(tip, "inventorytodo.help.edit");
                addHelp(tip, "inventorytodo.help.modifier", modifierName());
                addHelp(tip, "inventorytodo.help.hold");
                if (hasRecipeViewer()) addHelp(tip, "inventorytodo.help.drop");
                addHelp(tip, "inventorytodo.help.wheel");
                addHelp(tip, "inventorytodo.help.auto");
                if (hasRecipeViewer()) addHelp(tip, "inventorytodo.help.recipe");
                addHelp(tip, "inventorytodo.help.ingredients");
                addHelp(tip, "inventorytodo.help.hud");
                addHelp(tip, "inventorytodo.help.reorder");
                addHelp(tip, "inventorytodo.help.move");
                break;
            case HEADER:
                tip.add(EnumChatFormatting.GRAY + tr("inventorytodo.tip.drag"));
                break;
            case ROW_CHECK:
                tip.add(tr("inventorytodo.tip.check"));
                break;
            case ROW_DEL:
                tip.add(tr("inventorytodo.tip.delete"));
                break;
            case BTN_ADD_TEXT:
                tip.add(tr("inventorytodo.tip.addtext"));
                break;
            case BTN_ADD_ITEM:
                tip.add(tr("inventorytodo.tip.additem"));
                tip.add(EnumChatFormatting.GRAY + tr("inventorytodo.tip.additem.desc", modifierName()));
                if (hasRecipeViewer()) tip.add(EnumChatFormatting.GRAY + tr("inventorytodo.tip.additem.drop"));
                break;
            case SEARCH_FIELD:
                tip.add(EnumChatFormatting.GRAY + tr("inventorytodo.tip.search"));
                break;
            default:
                break;
        }
        return tip;
    }

    private static void addHelp(List<String> tip, String key, Object... args) {
        tip.add(EnumChatFormatting.GRAY + "- " + tr(key, args));
    }

    /** Extra lines appended to the tooltip of an item shown on the panel */
    public List<String> itemTooltipExtras(GuiContainer gui, int mx, int my) {
        List<String> tip = new ArrayList<>();
        if (!active(gui)) return tip;
        prepare(gui);
        if (!insidePanel(mx, my)) return tip;
        Hit h = hitTest(mx, my);
        if (!searchOpen && h.index >= 0 && h.index < store.entries.size()) {
            TodoEntry e = store.entries.get(h.index);
            if (e.item != null) {
                tip.add(
                    EnumChatFormatting.AQUA
                        + tr("inventorytodo.tip.progress", e.have, e.target > 0 ? String.valueOf(e.target) : "-"));
                if (hasRecipeViewer()) tip.add(EnumChatFormatting.DARK_GRAY + tr("inventorytodo.tip.row.icon"));
                tip.add(EnumChatFormatting.DARK_GRAY + tr("inventorytodo.tip.row.ingredients"));
                tip.add(EnumChatFormatting.DARK_GRAY + tr("inventorytodo.tip.row.count"));
            }
        } else if (searchOpen && h.type == HitType.SEARCH_RESULT) {
            tip.add(EnumChatFormatting.DARK_GRAY + tr("inventorytodo.tip.result"));
        }
        return tip;
    }

    private int remainingCount() {
        int n = 0;
        for (TodoEntry e : store.entries) if (!e.isDone()) n++;
        return n;
    }

    // =====================================================================
    // rendering
    // =====================================================================

    public void render(GuiContainer gui, int mx, int my) {
        if (!active(gui)) return;
        pollDrag(gui);
        prepare(gui);
        updateSearchIfNeeded();

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            if (dragging) GL11.glTranslatef(dragFracX, dragFracY, 0f);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (store.minimized) drawMini(mx, my);
            else drawPanel(mx, my);
        } catch (Throwable t) {
            InventoryTodo.LOG.error("Error while drawing the panel", t);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    private void drawMini(int mx, int my) {
        boolean hover = insidePanel(mx, my);
        Gui.drawRect(px, py, px + MINI, py + MINI, C_BORDER);
        Gui.drawRect(px + 1, py + 1, px + MINI - 1, py + MINI - 1, hover ? C_BTN_HOVER : C_HEADER);
        drawItem(px + 2, py + 2, new ItemStack(Items.writable_book));
        int rem = remainingCount();
        if (rem > 0) {
            String s = String.valueOf(rem);
            GL11.glTranslatef(0, 0, 300);
            font().drawStringWithShadow(s, px + MINI - 1 - font().getStringWidth(s), py + MINI - 9, 0xFFFF55);
            GL11.glTranslatef(0, 0, -300);
        }
    }

    private void drawPanel(int mx, int my) {
        FontRenderer fr = font();
        // frame and background
        Gui.drawRect(px - 1, py - 1, px + pw + 1, py + ph + 1, C_BORDER);
        Gui.drawRect(px, py, px + pw, py + ph, C_BG);
        Gui.drawRect(px, py, px + pw, py + HEADER_H, C_HEADER);

        // header
        int done = store.entries.size() - remainingCount();
        String title = tr("inventorytodo.title") + " " + EnumChatFormatting.GRAY + done + "/" + store.entries.size();
        fr.drawStringWithShadow(fr.trimStringToWidth(title, pw - 52), px + 3, py + 2, C_TEXT);
        drawButton(btnHudX(), py + 1, 10, 10, tr("inventorytodo.btn.hud.short"), mx, my);
        if (TodoConfig.hudEnabled) Gui.drawRect(btnHudX() + 1, py + 10, btnHudX() + 9, py + 11, 0xFF55FF55);
        drawButton(btnHelpX(), py + 1, 10, 10, "?", mx, my);
        drawButton(
            btnModeX(),
            py + 1,
            10,
            10,
            tr(
                "inventorytodo.mode." + store.mode.name()
                    .toLowerCase(Locale.ROOT) + ".short"),
            mx,
            my);
        drawButton(btnMinX(), py + 1, 10, 10, "_", mx, my);

        if (searchOpen) drawSearch(mx, my);
        else drawRows(mx, my);

        // footer
        int fy = footerY();
        Gui.drawRect(px, fy, px + pw, fy + 1, 0x60FFFFFF);
        if (searchOpen) {
            drawButton(px + ADD_TEXT_X, fy + 1, ADD_TEXT_W, 12, tr("inventorytodo.btn.close"), mx, my);
            String info = results.isEmpty() ? "" : tr("inventorytodo.search.count", results.size());
            fr.drawString(info, px + pw - 3 - fr.getStringWidth(info), fy + 3, C_TEXT_HINT);
        } else {
            drawButton(px + ADD_TEXT_X, fy + 1, ADD_TEXT_W, 12, tr("inventorytodo.btn.addtext"), mx, my);
            drawButton(px + ADD_ITEM_X, fy + 1, ADD_ITEM_W, 12, tr("inventorytodo.btn.additem"), mx, my);
            if (status != null && System.currentTimeMillis() < statusUntil) {
                // drawn just above the footer so the buttons do not cut it off
                String s = fr.trimStringToWidth(status, pw - 6);
                Gui.drawRect(px, fy - 11, px + pw, fy, 0xE0303050);
                fr.drawStringWithShadow(s, px + 3, fy - 10, 0xFFFF80);
            }
            if (store.entries.size() > rowsVisible) {
                String s = (scroll + 1) + "-"
                    + Math.min(store.entries.size(), scroll + rowsVisible)
                    + "/"
                    + store.entries.size();
                fr.drawString(s, px + pw - 3 - fr.getStringWidth(s), fy + 3, C_TEXT_HINT);
            }
        }
    }

    private void drawRows(int mx, int my) {
        FontRenderer fr = font();
        if (store.entries.isEmpty()) {
            fr.drawString(fr.trimStringToWidth(tr("inventorytodo.empty"), pw - 8), px + 4, listTop + 5, C_TEXT_HINT);
            return;
        }
        long now = System.currentTimeMillis();
        // pass 1: backgrounds, check boxes, icons
        for (int r = 0; r < rowsVisible; r++) {
            int i = scroll + r;
            if (i >= store.entries.size()) break;
            TodoEntry e = store.entries.get(i);
            int ry = listTop + r * ROW_H;
            if (i == flashIndex && now < flashUntil && (now / 150) % 2 == 0) {
                Gui.drawRect(px, ry, px + pw, ry + ROW_H, 0x5055FF55);
            } else if (inside(mx, my, px, ry, pw, ROW_H)) {
                Gui.drawRect(px, ry, px + pw, ry + ROW_H, C_ROW_HOVER);
            }
            if (r > 0) Gui.drawRect(px + 2, ry, px + pw - 2, ry + 1, 0x18FFFFFF);
            int cx = px + 4, cy = ry + 5;
            Gui.drawRect(cx, cy, cx + 8, cy + 8, 0xFFA0A0B0);
            Gui.drawRect(cx + 1, cy + 1, cx + 7, cy + 7, e.isDone() ? 0xFF3CB043 : 0xFF202028);
            if (e.item != null) drawItem(px + 15, ry + 1, e.item);
        }
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        // pass 2: text
        for (int r = 0; r < rowsVisible; r++) {
            int i = scroll + r;
            if (i >= store.entries.size()) break;
            TodoEntry e = store.entries.get(i);
            int ry = listTop + r * ROW_H;
            boolean doneRow = e.isDone();

            int tx = px + (e.item != null ? 34 : 15);
            int right = px + pw - 12;
            if (e.item != null) {
                String ct = counterText(e);
                int cw = fr.getStringWidth(ct);
                int col = e.target > 0 && e.have >= e.target ? 0x55FF55 : (e.have > 0 ? 0xFFFF55 : 0xFF7777);
                if (!(field != null && editTarget && editIndex == i)) {
                    fr.drawStringWithShadow(ct, right - cw - 2, ry + 5, col);
                }
                right = right - cw - 6;
            }
            if (field != null && editIndex == i) {
                if (editTarget) {
                    field.xPosition = px + pw - 12 - 46;
                } else {
                    field.xPosition = tx - 2;
                }
                field.yPosition = ry + 3;
                continue; // the text field is drawn last
            }
            String text = e.displayText();
            String shown = fr.trimStringToWidth(text, Math.max(0, right - tx));
            if (shown.length() < text.length()) {
                shown = fr.trimStringToWidth(text, Math.max(0, right - tx - fr.getStringWidth("..."))) + "...";
            }
            if (doneRow) {
                fr.drawString(EnumChatFormatting.STRIKETHROUGH + shown, tx, ry + 5, C_TEXT_DONE);
            } else {
                fr.drawStringWithShadow(shown, tx, ry + 5, C_TEXT);
            }
            boolean delHover = inside(mx, my, px + pw - 11, ry, 10, ROW_H);
            fr.drawString("x", px + pw - 8, ry + 5, delHover ? 0xFF5555 : 0x707080);
        }
        if (field != null && editIndex >= scroll && editIndex < scroll + rowsVisible) {
            field.drawTextBox();
        }
    }

    private void drawSearch(int mx, int my) {
        FontRenderer fr = font();
        if (searchField != null) {
            searchField.xPosition = px + 4;
            searchField.yPosition = listTop + 2;
            searchField.drawTextBox();
            if (searchField.getText()
                .isEmpty() && !searchField.isFocused()) {
                fr.drawString(tr("inventorytodo.search.hint"), px + 8, listTop + 4, C_TEXT_HINT);
            }
        }
        int gx = searchGridX(), gy = searchGridY(), cols = searchCols();
        if (results.isEmpty()) {
            String msg = (searchField == null || searchField.getText()
                .trim()
                .isEmpty()) ? tr("inventorytodo.search.prompt") : tr("inventorytodo.search.none");
            fr.drawString(fr.trimStringToWidth(msg, pw - 8), px + 4, gy + 5, C_TEXT_HINT);
            return;
        }
        for (int r = 0; r < SEARCH_ROWS; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = (resultScroll + r) * cols + c;
                if (idx >= results.size()) break;
                int x = gx + c * 18, y = gy + r * 18;
                Gui.drawRect(x, y, x + 18, y + 18, inside(mx, my, x, y, 18, 18) ? 0x60FFFFFF : 0x20FFFFFF);
            }
        }
        for (int r = 0; r < SEARCH_ROWS; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = (resultScroll + r) * cols + c;
                if (idx >= results.size()) break;
                drawItem(gx + c * 18 + 1, gy + r * 18 + 1, results.get(idx));
            }
        }
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private void drawButton(int x, int y, int w, int h, String label, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        Gui.drawRect(x, y, x + w, y + h, hover ? C_BTN_HOVER : C_BTN);
        FontRenderer fr = font();
        String s = fr.trimStringToWidth(label, w - 2);
        if (s.isEmpty()) s = label; // e.g. a single CJK character would be trimmed away completely
        fr.drawStringWithShadow(s, x + (w - fr.getStringWidth(s)) / 2 + 1, y + (h - 8) / 2 + 1, 0xFFFFFF);
    }

    private static void drawItem(int x, int y, ItemStack stack) {
        try {
            Compat.get()
                .drawItem(x, y, TodoEntry.renderable(stack));
        } catch (Throwable t) {
            // one broken item must not stop the whole panel
        } finally {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
    }
}
