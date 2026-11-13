package io.github.junjun1z.inventorytodo.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The to-do panel drawn next to container screens. Version- and loader-independent: the loader glue code feeds
 * input events and a {@link Gfx} for drawing.
 */
public final class TodoPanel {

    public static final TodoPanel INSTANCE = new TodoPanel();

    // ---- sizes ----
    private static final int HEADER_H = 12;
    private static final int ROW_H = 18;
    private static final int FOOTER_H = 14;
    private static final int MINI = 20;
    private static final int SEARCH_ROWS = 4;
    private static final int SETTINGS_ROW_H = 14;
    private static final int MENU_ROW_H = 12;
    private static final int MIN_DOCKED_W = 100;

    // ---- colours (ARGB) ----
    private static final int C_BG = 0xE0101018;
    private static final int C_BORDER = 0xFF5A5A82;
    private static final int C_HEADER = 0xF0262640;
    private static final int C_ROW_HOVER = 0x30FFFFFF;
    private static final int C_BTN = 0xFF3A3A5C;
    private static final int C_BTN_HOVER = 0xFF5A5A8C;
    private static final int C_TEXT = 0xFFFFFFFF;
    private static final int C_TEXT_DONE = 0xFF808080;
    private static final int C_TEXT_HINT = 0xFF9090A0;

    private final TodoStore store = Core.store();

    // ---- layout (screen coordinates, recomputed every time) ----
    private int px, py, pw, ph;
    private int listTop, rowsVisible;
    private int scroll;
    private int screenW, screenH;

    // ---- dragging ----
    private boolean dragging, dragMoved, dragFromMini;
    private double dragGrabX, dragGrabY, dragStartX, dragStartY;
    private float dragFracX, dragFracY;
    private long dragStartedAt;

    // ---- editing ----
    private TextField field;
    private int editIndex = -1;
    private boolean editTarget;

    // ---- search ----
    private boolean searchOpen;
    private TextField searchField;
    private final List<Object> results = new ArrayList<>();
    private int resultScroll;
    private String computedQuery = "";
    private long queryChangedAt;

    // ---- settings page / context menu ----
    private boolean settingsOpen;
    private Menu menu;

    // ---- feedback ----
    private int flashIndex = -1;
    private long flashUntil;
    private String message;
    private long messageUntil;

    private int tickCounter;
    private Gfx lastGfx;

    private TodoPanel() {}

    // =====================================================================
    // helpers
    // =====================================================================

    private static Mc mc() {
        return Core.mc();
    }

    private static Settings cfg() {
        return Core.settings();
    }

    private static String tr(String key, Object... args) {
        return mc().tr(key, args);
    }

    private static boolean active(Object screen) {
        return screen != null && mc() != null && mc().hasPlayer() && mc().isContainerScreen(screen);
    }

    private void prepare(Object screen) {
        store.ensureLoaded();
        layout(screen);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private boolean insidePanel(double mx, double my) {
        return inside(mx, my, px, py, pw, ph);
    }

    private static boolean isAddModifierDown() {
        String m = cfg().addModifier.toUpperCase(Locale.ROOT);
        if ("SHIFT".equals(m)) return mc().isShiftDown();
        if ("CTRL".equals(m)) return mc().isCtrlDown();
        return mc().isAltDown();
    }

    private static String modifierName() {
        String m = cfg().addModifier.toUpperCase(Locale.ROOT);
        if ("SHIFT".equals(m)) return "Shift";
        if ("CTRL".equals(m)) return "Ctrl";
        return "Alt";
    }

    private void showMessage(String s) {
        message = s;
        messageUntil = System.currentTimeMillis() + 3000;
    }

    // =====================================================================
    // layout
    // =====================================================================

    private void layout(Object screen) {
        Settings s = cfg();
        int sw = mc().screenWidth(), sh = mc().screenHeight();
        screenW = sw;
        screenH = sh;
        if (s.minimized) {
            pw = MINI;
            ph = MINI;
        } else {
            pw = Math.min(s.panelWidth, sw - 4);
            if (s.mode != Settings.Mode.FREE) {
                int avail = mc().guiLeft(screen) - 6;
                if (avail >= MIN_DOCKED_W) pw = Math.min(pw, avail);
            }
            int maxRowsByScreen = Math.max(1, (sh - 8 - HEADER_H - FOOTER_H) / ROW_H);
            int body;
            if (settingsOpen) {
                body = settingsRows().size() * SETTINGS_ROW_H + 4;
                rowsVisible = 0;
            } else if (searchOpen) {
                body = 16 + SEARCH_ROWS * ROW_H + 2;
                rowsVisible = 0;
            } else {
                rowsVisible = Math
                    .max(1, Math.min(store.entries.size(), Math.min(s.maxVisibleRows, maxRowsByScreen)));
                body = rowsVisible * ROW_H;
            }
            ph = HEADER_H + body + FOOTER_H;
        }
        switch (s.mode) {
            case LEFT:
                px = 2;
                py = mc().guiTop(screen);
                break;
            case FREE:
                px = s.freeX;
                py = s.freeY;
                break;
            case FOLLOW:
            default:
                px = mc().guiLeft(screen) - pw - 4;
                if (px < 2) px = 2;
                py = mc().guiTop(screen);
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
        BTN_SETTINGS,
        BTN_HUD,
        BTN_MODE,
        BTN_MIN,
        ROW_CHECK,
        ROW_ICON,
        ROW_TEXT,
        ROW_COUNT,
        ROW_DEL,
        BTN_ADD_TEXT,
        BTN_ADD_ITEM,
        BTN_BACK,
        SEARCH_FIELD,
        SEARCH_RESULT,
        SETTING
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

    private int btnHudX() {
        return px + pw - 35;
    }

    private int btnSettingsX() {
        return px + pw - 47;
    }

    private int btnHelpX() {
        return px + pw - 59;
    }

    private int footerY() {
        return py + ph - FOOTER_H;
    }

    private static final int ADD_TEXT_X = 3, ADD_TEXT_W = 50;
    private static final int ADD_ITEM_X = 56, ADD_ITEM_W = 62;

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

    private int counterWidth(Gfx g, TodoEntry e) {
        return (g == null ? 6 * counterText(e).length() : g.textWidth(counterText(e))) + 4;
    }

    private Hit hitTest(double mx, double my) {
        if (!insidePanel(mx, my)) return NO_HIT;
        if (cfg().minimized) return new Hit(HitType.MINI, -1);

        if (my < py + HEADER_H) {
            if (inside(mx, my, btnMinX(), py + 1, 10, 10)) return new Hit(HitType.BTN_MIN, -1);
            if (inside(mx, my, btnModeX(), py + 1, 10, 10)) return new Hit(HitType.BTN_MODE, -1);
            if (inside(mx, my, btnHudX(), py + 1, 10, 10)) return new Hit(HitType.BTN_HUD, -1);
            if (inside(mx, my, btnSettingsX(), py + 1, 10, 10)) return new Hit(HitType.BTN_SETTINGS, -1);
            if (inside(mx, my, btnHelpX(), py + 1, 10, 10)) return new Hit(HitType.BTN_HELP, -1);
            return new Hit(HitType.HEADER, -1);
        }

        int fy = footerY();
        if (my >= fy) {
            if (searchOpen || settingsOpen) {
                if (inside(mx, my, px + ADD_TEXT_X, fy + 1, ADD_TEXT_W, 12)) return new Hit(HitType.BTN_BACK, -1);
            } else {
                if (inside(mx, my, px + ADD_TEXT_X, fy + 1, ADD_TEXT_W, 12)) return new Hit(HitType.BTN_ADD_TEXT, -1);
                if (inside(mx, my, px + ADD_ITEM_X, fy + 1, ADD_ITEM_W, 12)) return new Hit(HitType.BTN_ADD_ITEM, -1);
            }
            return new Hit(HitType.BODY, -1);
        }

        if (settingsOpen) {
            int r = (int) Math.floor((my - listTop - 2) / SETTINGS_ROW_H);
            if (r >= 0 && r < settingsRows().size()) return new Hit(HitType.SETTING, r);
            return new Hit(HitType.BODY, -1);
        }

        if (searchOpen) {
            if (inside(mx, my, px + 4, listTop + 2, pw - 8, 12)) return new Hit(HitType.SEARCH_FIELD, -1);
            int gx = searchGridX(), gy = searchGridY(), cols = searchCols();
            if (mx >= gx && my >= gy && mx < gx + cols * 18 && my < gy + SEARCH_ROWS * 18) {
                int c = (int) ((mx - gx) / 18), r = (int) ((my - gy) / 18);
                int idx = (resultScroll + r) * cols + c;
                if (idx < results.size()) return new Hit(HitType.SEARCH_RESULT, idx);
            }
            return new Hit(HitType.BODY, -1);
        }

        int r = (int) Math.floor((my - listTop) / ROW_H);
        int i = scroll + r;
        if (r < 0 || r >= rowsVisible || i >= store.entries.size()) return new Hit(HitType.BODY, -1);
        TodoEntry e = store.entries.get(i);
        int ry = listTop + r * ROW_H;
        if (inside(mx, my, px + pw - 11, ry, 10, ROW_H)) return new Hit(HitType.ROW_DEL, i);
        if (inside(mx, my, px + 2, ry, 12, ROW_H)) return new Hit(HitType.ROW_CHECK, i);
        if (e.item != null) {
            if (inside(mx, my, px + 15, ry + 1, 16, 16)) return new Hit(HitType.ROW_ICON, i);
            int cw = counterWidth(lastGfx, e);
            int cx = px + pw - 12 - cw;
            if (inside(mx, my, cx, ry, cw, ROW_H)) return new Hit(HitType.ROW_COUNT, i);
        }
        return new Hit(HitType.ROW_TEXT, i);
    }

    // =====================================================================
    // data
    // =====================================================================

    /** Adds an item row; if the item is already listed, that row is highlighted instead. */
    public void addItem(Object stack, int target) {
        if (stack == null || mc().isEmpty(stack)) return;
        store.ensureLoaded();
        for (int i = 0; i < store.entries.size(); i++) {
            Object it = store.entries.get(i).item;
            if (it != null && mc().sameItem(it, stack)) {
                flash(i);
                return;
            }
        }
        int t = target > 0 ? target : Math.max(1, mc().maxStackSize(stack));
        store.entries.add(TodoEntry.item(stack, t));
        Core.recount(null);
        store.save();
        flash(store.entries.size() - 1);
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
        store.save();
    }

    private void moveEntry(int idx, int dir) {
        int j = idx + dir;
        if (idx < 0 || j < 0 || idx >= store.entries.size() || j >= store.entries.size()) return;
        cancelEdit();
        Collections.swap(store.entries, idx, j);
        store.save();
        scrollTo(j);
    }

    /** Adds the crafting ingredients of an item row (one level), scaled to its goal */
    private void addIngredients(int idx) {
        TodoEntry e = store.entries.get(idx);
        if (e.item == null) return;
        Mc.Recipe recipe;
        try {
            recipe = mc().recipeFor(e.item);
        } catch (Throwable t) {
            Log.warn("Recipe lookup failed", t);
            recipe = null;
        }
        if (recipe == null) {
            showMessage(tr("inventorytodo.msg.recipes_unavailable"));
            return;
        }
        if (recipe.ingredients.isEmpty()) {
            showMessage(tr("inventorytodo.status.norecipe"));
            return;
        }
        int missing = e.target > 0 ? e.target - e.have : 1;
        if (missing <= 0) {
            showMessage(tr("inventorytodo.status.ingredients", e.displayText()));
            return;
        }
        int crafts = (missing + recipe.resultCount - 1) / recipe.resultCount;
        int added = 0;
        for (Object ing : recipe.ingredients) {
            int need = mc().count(ing) * crafts;
            int found = -1;
            for (int i = 0; i < store.entries.size(); i++) {
                Object it = store.entries.get(i).item;
                if (it != null && mc().sameItem(it, ing)) found = i;
            }
            if (found >= 0) {
                TodoEntry ex = store.entries.get(found);
                ex.target = Math.min(999_999_999, ex.target + need);
            } else {
                store.entries.add(idx + 1 + added, TodoEntry.item(ing, need));
            }
            added++;
        }
        Core.recount(null);
        store.save();
        showMessage(tr("inventorytodo.status.ingredients", e.displayText()));
        flash(Math.min(store.entries.size() - 1, idx + 1));
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
        field = new TextField(w, target ? 9 : 200, target);
        field.setText(target ? String.valueOf(e.target) : (e.text == null ? "" : e.text));
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
                if (e.item == null && e.missingItemData == null
                    && e.text.trim()
                        .isEmpty())
                    store.entries.remove(editIndex);
            }
            store.save();
        }
        field = null;
        editIndex = -1;
    }

    private void cancelEdit() {
        if (field == null) return;
        if (!editTarget && editIndex >= 0 && editIndex < store.entries.size()) {
            TodoEntry e = store.entries.get(editIndex);
            if (e.item == null && e.missingItemData == null
                && (e.text == null || e.text.trim()
                    .isEmpty())) {
                store.entries.remove(editIndex);
                store.save();
            }
        }
        field = null;
        editIndex = -1;
    }

    // ---- search ----

    private void openSearch() {
        commitEdit();
        settingsOpen = false;
        searchOpen = true;
        searchField = new TextField(Math.max(40, pw - 8), 100, false);
        results.clear();
        resultScroll = 0;
        computedQuery = "";
    }

    private void closeSearch() {
        searchOpen = false;
        searchField = null;
        results.clear();
    }

    private void updateSearchIfNeeded() {
        if (!searchOpen || searchField == null) return;
        String q = searchField.getText();
        if (q.equals(computedQuery)) return;
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
        boolean idQuery = q.indexOf(':') >= 0;
        List<Object> src;
        try {
            src = mc().allItems();
        } catch (Throwable t) {
            Log.warn("Could not list items", t);
            return;
        }
        for (Object s : src) {
            if (results.size() >= 2000) break;
            try {
                if (s == null || mc().isEmpty(s)) continue;
                String id = mc().registryId(s);
                if (idQuery) {
                    if (id != null && id.contains(q)) results.add(s);
                    continue;
                }
                String name = stripFormatting(mc().displayName(s)).toLowerCase(Locale.ROOT);
                if (name.contains(q) || (id != null && id.contains(q))) {
                    results.add(s);
                    continue;
                }
                String en = mc().englishName(s);
                if (en != null && en.toLowerCase(Locale.ROOT)
                    .contains(q)) results.add(s);
            } catch (Throwable ignored) {
                // one broken item must not stop the search
            }
        }
        if (results.isEmpty() && idQuery) {
            Object exact = mc().stackById(q);
            if (exact != null) results.add(exact);
        }
    }

    private static String stripFormatting(String s) {
        return s == null ? "" : s.replaceAll("§.", "");
    }

    // =====================================================================
    // settings page
    // =====================================================================

    /** Settings rows as {label, value} */
    private List<String[]> settingsRows() {
        Settings s = cfg();
        List<String[]> rows = new ArrayList<>();
        rows.add(row("inventorytodo.config.hud", onOff(s.hudEnabled)));
        rows.add(row("inventorytodo.config.hudAnchor", tr("inventorytodo.hud_pos." + s.hudPos.name()
            .toLowerCase(Locale.ROOT))));
        rows.add(row("inventorytodo.config.hudHideDone", onOff(s.hudHideDone)));
        rows.add(row("inventorytodo.config.hudMaxRows", String.valueOf(s.hudMaxRows)));
        rows.add(row("inventorytodo.config.countContainer", onOff(s.countOpenContainer)));
        rows.add(row("inventorytodo.config.listScope", tr("inventorytodo.scope." + s.scope.name()
            .toLowerCase(Locale.ROOT))));
        rows.add(row("inventorytodo.config.addModifier", modifierName()));
        rows.add(row("inventorytodo.config.maxVisibleRows", String.valueOf(s.maxVisibleRows)));
        rows.add(row("inventorytodo.config.panelWidth", String.valueOf(s.panelWidth)));
        rows.add(row("inventorytodo.config.ignoreNbt", onOff(s.ignoreDataWhenTargetHasNone)));
        return rows;
    }

    /** Label without a trailing "%s" placeholder / colon, plus the value */
    private static String[] row(String key, String value) {
        String label = tr(key).replace("%s", "")
            .trim();
        while (label.endsWith(":") || label.endsWith("\uff1a")) label = label.substring(0, label.length() - 1)
            .trim();
        return new String[] { label, value };
    }

    private static String onOff(boolean b) {
        return tr(b ? "inventorytodo.state.on" : "inventorytodo.state.off");
    }

    private static final int[] ROW_CHOICES = { 5, 8, 10, 12, 15, 20 };
    private static final int[] WIDTH_CHOICES = { 120, 140, 160, 200, 240, 320 };
    private static final int[] HUD_ROW_CHOICES = { 3, 4, 5, 6, 8, 10, 15 };

    private static int next(int[] choices, int current, int dir) {
        int idx = 0;
        for (int i = 0; i < choices.length; i++) if (choices[i] <= current) idx = i;
        return choices[(idx + dir + choices.length) % choices.length];
    }

    private void clickSetting(int row, int button) {
        Settings s = cfg();
        int dir = button == 1 ? -1 : 1;
        switch (row) {
            case 0:
                s.hudEnabled = !s.hudEnabled;
                break;
            case 1:
                s.hudPos = cycle(Settings.HudPos.values(), s.hudPos, dir);
                break;
            case 2:
                s.hudHideDone = !s.hudHideDone;
                break;
            case 3:
                s.hudMaxRows = next(HUD_ROW_CHOICES, s.hudMaxRows, dir);
                break;
            case 4:
                s.countOpenContainer = !s.countOpenContainer;
                break;
            case 5:
                commitEdit();
                s.scope = cycle(Settings.Scope.values(), s.scope, dir);
                store.invalidate();
                store.ensureLoaded();
                break;
            case 6: {
                String[] mods = { "ALT", "CTRL", "SHIFT" };
                int i = 0;
                for (int k = 0; k < mods.length; k++) if (mods[k].equalsIgnoreCase(s.addModifier)) i = k;
                s.addModifier = mods[(i + dir + mods.length) % mods.length];
                break;
            }
            case 7:
                s.maxVisibleRows = next(ROW_CHOICES, s.maxVisibleRows, dir);
                break;
            case 8:
                s.panelWidth = next(WIDTH_CHOICES, s.panelWidth, dir);
                break;
            case 9:
                s.ignoreDataWhenTargetHasNone = !s.ignoreDataWhenTargetHasNone;
                break;
            default:
                return;
        }
        s.save();
    }

    private static <T extends Enum<T>> T cycle(T[] values, T current, int dir) {
        return values[(current.ordinal() + dir + values.length) % values.length];
    }

    // =====================================================================
    // context menu
    // =====================================================================

    private enum Action {
        INGREDIENTS("inventorytodo.menu.ingredients"),
        RECIPES("inventorytodo.menu.recipes"),
        USAGES("inventorytodo.menu.usages"),
        SET_CURRENT("inventorytodo.menu.set_current"),
        TOGGLE_DONE("inventorytodo.menu.toggle_done"),
        EDIT("inventorytodo.menu.edit"),
        MOVE_UP("inventorytodo.menu.move_up"),
        MOVE_DOWN("inventorytodo.menu.move_down"),
        DELETE("inventorytodo.menu.delete");

        final String key;

        Action(String key) {
            this.key = key;
        }
    }

    private static final class Menu {

        final int entry;
        final List<Action> actions;
        int x, y, w, h;

        Menu(int entry, List<Action> actions) {
            this.entry = entry;
            this.actions = actions;
        }

        int indexAt(double mx, double my) {
            if (mx < x || mx >= x + w || my < y + 2 || my >= y + h - 2) return -1;
            int i = (int) ((my - y - 2) / MENU_ROW_H);
            return i >= 0 && i < actions.size() ? i : -1;
        }
    }

    private void openMenu(int entry, int mx, int my) {
        TodoEntry e = store.entries.get(entry);
        List<Action> a = new ArrayList<>();
        if (e.item != null) {
            a.add(Action.INGREDIENTS);
            if (RecipeViewer.any()) {
                a.add(Action.RECIPES);
                a.add(Action.USAGES);
            }
            a.add(Action.SET_CURRENT);
        }
        a.add(Action.TOGGLE_DONE);
        a.add(Action.EDIT);
        if (entry > 0) a.add(Action.MOVE_UP);
        if (entry < store.entries.size() - 1) a.add(Action.MOVE_DOWN);
        a.add(Action.DELETE);
        menu = new Menu(entry, a);
        int w = 60;
        if (lastGfx != null) for (Action ac : a) w = Math.max(w, lastGfx.textWidth(tr(ac.key)) + 10);
        menu.w = w;
        menu.h = a.size() * MENU_ROW_H + 4;
        menu.x = Math.max(0, Math.min(mx, screenW - menu.w));
        menu.y = Math.max(0, Math.min(my, screenH - menu.h));
    }

    private void runAction(Action a, int idx) {
        if (idx < 0 || idx >= store.entries.size()) return;
        TodoEntry e = store.entries.get(idx);
        switch (a) {
            case INGREDIENTS:
                addIngredients(idx);
                break;
            case RECIPES:
            case USAGES:
                if (e.item != null) RecipeViewer.showRecipes(mc().copyOne(e.item), a == Action.USAGES);
                break;
            case SET_CURRENT:
                e.target = Math.max(1, e.have);
                store.save();
                break;
            case TOGGLE_DONE:
                e.manualDone = !e.manualDone;
                store.save();
                break;
            case EDIT:
                startEdit(idx, false);
                break;
            case MOVE_UP:
                moveEntry(idx, -1);
                break;
            case MOVE_DOWN:
                moveEntry(idx, 1);
                break;
            case DELETE:
                removeEntry(idx);
                break;
            default:
                break;
        }
    }

    // =====================================================================
    // input (called by the loader glue)
    // =====================================================================

    private TextField textInputOwner;

    /** Keeps the game's text input (IME / character events) enabled exactly while a panel text field has focus */
    private void syncTextInput(boolean screenOpen) {
        TextField want = !screenOpen ? null
            : field != null ? field : (searchOpen && searchField != null && searchField.isFocused() ? searchField : null);
        if (want == textInputOwner) return;
        try {
            if (textInputOwner != null) mc().setTextInputActive(textInputOwner, false);
            if (want != null) mc().setTextInputActive(want, true);
        } catch (Throwable t) {
            Log.debug("text input toggle failed: " + t);
        }
        textInputOwner = want;
    }

    /** True while a text field has focus (all keys go to the panel) */
    public boolean isTyping() {
        return field != null || (searchOpen && searchField != null && searchField.isFocused());
    }

    /** @return true if the key was used and must not reach the screen */
    public boolean keyPressed(Object screen, int key) {
        if (!active(screen)) return false;
        if (menu != null && key == Keys.ESCAPE) {
            menu = null;
            return true;
        }
        if (field != null) {
            if (key == Keys.ESCAPE) cancelEdit();
            else if (key == Keys.ENTER || key == Keys.KP_ENTER) commitEdit();
            else field.keyPressed(key);
            return true; // swallow everything while typing (inventory key, hotbar keys, recipe viewer keys)
        }
        if (searchOpen && searchField != null && searchField.isFocused()) {
            if (key == Keys.ESCAPE) {
                closeSearch();
            } else if (key == Keys.ENTER || key == Keys.KP_ENTER) {
                computedQuery = searchField.getText();
                runSearch(computedQuery);
                if (results.size() == 1) {
                    addItem(results.get(0), 0);
                    closeSearch();
                }
            } else {
                searchField.keyPressed(key);
                queryChangedAt = System.currentTimeMillis();
            }
            return true;
        }
        if ((searchOpen || settingsOpen) && key == Keys.ESCAPE) {
            closeSearch();
            settingsOpen = false;
            return true;
        }
        return false;
    }

    /** @return true if the character was used */
    public boolean charTyped(Object screen, char c) {
        if (!active(screen)) return false;
        if (field != null) {
            field.charTyped(c);
            return true;
        }
        if (searchOpen && searchField != null && searchField.isFocused()) {
            searchField.charTyped(c);
            queryChangedAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /** "Add hovered item" key binding pressed in a container screen */
    public boolean addHovered(Object screen, int mx, int my) {
        if (!active(screen) || isTyping()) return false;
        prepare(screen);
        Object s = hoveredForAdd(screen, mx, my);
        if (s == null) return false;
        addItem(s, 0);
        return true;
    }

    private Object hoveredForAdd(Object screen, int mx, int my) {
        if (insidePanel(mx, my)) return stackUnderMouse(screen, mx, my);
        Object s = RecipeViewer.hovered();
        if (s == null) s = mc().hoveredSlotStack(screen, mx, my);
        return s == null || mc().isEmpty(s) ? null : s;
    }

    /** The last press went to the panel: its release must not reach the screen either */
    private boolean pressConsumed;

    /** @return true if the click was used and must not reach the screen */
    public boolean mouseClicked(Object screen, double dmx, double dmy, int button) {
        pressConsumed = click(screen, dmx, dmy, button);
        return pressConsumed;
    }

    private boolean click(Object screen, double dmx, double dmy, int button) {
        if (!active(screen)) return false;
        prepare(screen);
        int mx = (int) Math.floor(dmx), my = (int) Math.floor(dmy);

        if (menu != null) {
            int i = menu.indexAt(mx, my);
            Menu m = menu;
            menu = null;
            if (i >= 0) runAction(m.actions.get(i), m.entry);
            return true;
        }

        Hit h = hitTest(mx, my);

        if (field != null) {
            boolean onSameRow = (h.type == HitType.ROW_TEXT || h.type == HitType.ROW_COUNT) && h.index == editIndex;
            if (onSameRow && field.contains(mx, my)) {
                field.click(lastGfx, mx);
                return true;
            }
            commitEdit();
            layout(screen);
            h = hitTest(mx, my);
        }

        if (h.type == HitType.NONE) {
            if ((button == 0 || button == 1) && isAddModifierDown()) {
                Object s = hoveredForAdd(screen, mx, my);
                if (s != null) {
                    addItem(s, 0);
                    return true;
                }
            }
            if (searchField != null) searchField.setFocused(false);
            return false;
        }

        // clicking the panel while holding an item adds it (the item is not used up)
        Object held = mc().carriedStack();
        if (held != null && !mc().isEmpty(held) && h.type != HitType.MINI) {
            int n = mc().count(held);
            addItem(held, n > 1 ? n : 0);
            return true;
        }

        Settings s = cfg();
        switch (h.type) {
            case MINI:
            case HEADER:
                if (button == 0) {
                    dragging = true;
                    dragMoved = false;
                    dragFromMini = h.type == HitType.MINI;
                    dragStartedAt = System.currentTimeMillis();
                    dragStartX = mc().mouseX();
                    dragStartY = mc().mouseY();
                    dragGrabX = dragStartX - px;
                    dragGrabY = dragStartY - py;
                    dragFracX = dragFracY = 0f;
                }
                break;
            case BTN_MODE:
                s.mode = cycle(Settings.Mode.values(), s.mode, 1);
                if (s.mode == Settings.Mode.FREE) {
                    s.freeX = px;
                    s.freeY = py;
                }
                s.save();
                break;
            case BTN_MIN:
                commitEdit();
                closeSearch();
                settingsOpen = false;
                s.minimized = true;
                s.save();
                break;
            case BTN_SETTINGS:
                commitEdit();
                closeSearch();
                settingsOpen = !settingsOpen;
                break;
            case BTN_HUD:
                Hud.toggle();
                break;
            case BTN_HELP:
                break;
            case ROW_CHECK: {
                TodoEntry e = store.entries.get(h.index);
                e.manualDone = !e.manualDone;
                store.save();
                break;
            }
            case ROW_ICON: {
                TodoEntry e = store.entries.get(h.index);
                if (e.item != null && button == 0 && mc().isCtrlDown()) {
                    addIngredients(h.index);
                } else if (e.item != null && (button == 0 || button == 1)) {
                    if (RecipeViewer.any()) RecipeViewer.showRecipes(mc().copyOne(e.item), button == 1);
                    else if (button == 1) openMenu(h.index, mx, my);
                }
                break;
            }
            case ROW_TEXT:
                if (button == 0) startEdit(h.index, false);
                else if (button == 1) openMenu(h.index, mx, my);
                break;
            case ROW_COUNT:
                if (button == 0) startEdit(h.index, true);
                else if (button == 1) {
                    TodoEntry e = store.entries.get(h.index);
                    e.target = Math.max(1, e.have);
                    store.save();
                }
                break;
            case ROW_DEL:
                removeEntry(h.index);
                break;
            case BTN_ADD_TEXT:
                store.entries.add(TodoEntry.note(""));
                layout(screen);
                startEdit(store.entries.size() - 1, false);
                break;
            case BTN_ADD_ITEM:
                openSearch();
                break;
            case BTN_BACK:
                closeSearch();
                settingsOpen = false;
                break;
            case SEARCH_FIELD:
                if (searchField != null) {
                    if (button == 1) {
                        searchField.setText("");
                        queryChangedAt = 0;
                    }
                    searchField.setFocused(true);
                    searchField.click(lastGfx, mx);
                }
                break;
            case SEARCH_RESULT:
                addItem(results.get(h.index), 0);
                if (!mc().isShiftDown()) closeSearch();
                break;
            case SETTING:
                clickSetting(h.index, button);
                break;
            default:
                break;
        }
        return true; // clicks on the panel never reach the screen (no dropped items by accident)
    }

    /** @return true if the release belonged to the panel */
    public boolean mouseReleased(Object screen, double mx, double my, int button) {
        boolean consumed = pressConsumed;
        pressConsumed = false;
        if (dragging) {
            endDrag();
            return true;
        }
        // e.g. older versions drop the held item on a release outside the inventory
        return consumed;
    }

    private void endDrag() {
        dragging = false;
        Settings s = cfg();
        if (dragFracX >= 0.5f) s.freeX++;
        if (dragFracY >= 0.5f) s.freeY++;
        dragFracX = dragFracY = 0f;
        if (dragMoved) {
            s.save();
        } else if (dragFromMini && s.minimized) {
            s.minimized = false;
            s.save();
        }
    }

    /**
     * @param dir +1 = wheel up, -1 = wheel down
     * @return true if used
     */
    public boolean mouseScrolled(Object screen, double mx, double my, int dir) {
        if (!active(screen)) return false;
        prepare(screen);
        if (menu != null) return true;
        if (!insidePanel(mx, my) || cfg().minimized) return false;
        if (searchOpen) {
            int cols = searchCols();
            int totalRows = (results.size() + cols - 1) / cols;
            resultScroll = clamp(resultScroll - dir, 0, Math.max(0, totalRows - SEARCH_ROWS));
            return true;
        }
        if (settingsOpen) return true;
        Hit h = hitTest(mx, my);
        if (h.type == HitType.ROW_COUNT) {
            TodoEntry e = store.entries.get(h.index);
            int mult = mc().isCtrlDown() ? 64 : (mc().isShiftDown() ? 16 : 1);
            e.target = clamp(e.target + dir * mult, 0, 999_999_999);
            store.save();
            return true;
        }
        if (mc().isShiftDown() && h.index >= 0) {
            moveEntry(h.index, -dir);
            return true;
        }
        scroll = clamp(scroll - dir, 0, Math.max(0, store.entries.size() - rowsVisible));
        return true;
    }

    /** Follows the real cursor every frame while dragging (sub-pixel smooth) */
    private void pollDrag() {
        if (!dragging) return;
        // The drag ends with the release event (always delivered by the loaders: the OS captures the mouse while a
        // button is held). Polling the button is only a fallback after a while, for platforms that report it.
        if (System.currentTimeMillis() - dragStartedAt > 400 && !dragMoved && !mc().isLeftMouseDown()) {
            endDrag();
            return;
        }
        double rx = mc().mouseX(), ry = mc().mouseY();
        if (!dragMoved && Math.abs(rx - dragStartX) + Math.abs(ry - dragStartY) <= 2) return;
        dragMoved = true;
        Settings s = cfg();
        s.mode = Settings.Mode.FREE;
        double nx = Math.max(0, Math.min(rx - dragGrabX, Math.max(0, screenW - pw)));
        double ny = Math.max(0, Math.min(ry - dragGrabY, Math.max(0, screenH - ph)));
        s.freeX = (int) Math.floor(nx);
        s.freeY = (int) Math.floor(ny);
        dragFracX = (float) (nx - s.freeX);
        dragFracY = (float) (ny - s.freeY);
    }

    // =====================================================================
    // lifecycle
    // =====================================================================

    public void tick(Object screen) {
        if (!active(screen)) return;
        store.ensureLoaded();
        if (++tickCounter % 5 == 0) Core.recount(screen);
    }

    /** A container screen was opened or resized */
    public void onScreenOpened(Object screen) {
        onScreenClosed();
        if (active(screen)) {
            store.ensureLoaded();
            Core.recount(screen);
        }
    }

    /** Leaving the screen: keep what was typed */
    public void onScreenClosed() {
        commitEdit();
        closeSearch();
        menu = null;
        settingsOpen = false;
        if (dragging) endDrag();
        syncTextInput(false);
    }

    // =====================================================================
    // queries
    // =====================================================================

    public boolean isOverPanel(Object screen, double mx, double my) {
        if (!active(screen)) return false;
        prepare(screen);
        return insidePanel(mx, my) || (menu != null && menu.indexAt(mx, my) >= -1 && inside(mx, my, menu.x, menu.y, menu.w, menu.h));
    }

    /** Item shown under the mouse on the panel, or null */
    public Object stackUnderMouse(Object screen, double mx, double my) {
        if (!active(screen)) return null;
        prepare(screen);
        if (menu != null) return null;
        Hit h = hitTest(mx, my);
        switch (h.type) {
            case ROW_ICON:
            case ROW_TEXT:
            case ROW_COUNT: {
                Object s = store.entries.get(h.index).item;
                return s == null ? null : mc().copyOne(s);
            }
            case SEARCH_RESULT:
                return mc().copyOne(results.get(h.index));
            default:
                return null;
        }
    }

    /** Something was dropped on the screen (e.g. dragged from a recipe viewer) */
    public boolean dropStack(Object screen, double mx, double my, Object stack) {
        if (!active(screen) || stack == null) return false;
        prepare(screen);
        if (!insidePanel(mx, my)) return false;
        addItem(stack, 0);
        return true;
    }

    /** Panel bounds {x, y, w, h} for recipe viewers' exclusion zones, or null */
    public int[] bounds(Object screen) {
        if (!active(screen)) return null;
        prepare(screen);
        return new int[] { px - 1, py - 1, pw + 2, ph + 2 };
    }

    private List<String> panelTooltip(Hit h) {
        List<String> tip = new ArrayList<>();
        Settings s = cfg();
        switch (h.type) {
            case MINI:
                tip.add(tr("inventorytodo.tip.mini"));
                tip.add("§7" + tr("inventorytodo.tip.mini.remaining", remainingCount()));
                break;
            case BTN_MIN:
                tip.add(tr("inventorytodo.tip.minimize"));
                break;
            case BTN_SETTINGS:
                tip.add(tr("inventorytodo.tip.settings"));
                break;
            case BTN_HUD:
                tip.add(tr("inventorytodo.tip.hud", onOff(s.hudEnabled)));
                tip.add("§7" + tr("inventorytodo.tip.hud.desc"));
                break;
            case BTN_MODE:
                tip.add(
                    tr(
                        "inventorytodo.tip.mode",
                        tr(
                            "inventorytodo.mode." + s.mode.name()
                                .toLowerCase(Locale.ROOT))));
                tip.add("§7" + tr("inventorytodo.tip.mode.desc"));
                break;
            case BTN_HELP:
                tip.add(tr("inventorytodo.help.title"));
                help(tip, "inventorytodo.help.edit");
                help(tip, "inventorytodo.help.modifier", modifierName());
                String key = mc().addKeyName();
                if (key != null) help(tip, "inventorytodo.help.addkey", key);
                help(tip, "inventorytodo.help.hold");
                help(tip, "inventorytodo.help.wheel");
                help(tip, "inventorytodo.help.auto");
                help(tip, "inventorytodo.help.menu");
                help(tip, "inventorytodo.help.ingredients");
                if (RecipeViewer.any()) help(tip, "inventorytodo.help.recipe");
                help(tip, "inventorytodo.help.reorder");
                help(tip, "inventorytodo.help.move");
                String hud = mc().hudKeyName();
                help(tip, "inventorytodo.help.hud", hud != null ? hud : tr("inventorytodo.tip.settings"));
                break;
            case HEADER:
                tip.add("§7" + tr("inventorytodo.tip.drag"));
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
                tip.add("§7" + tr("inventorytodo.tip.additem.desc", modifierName()));
                break;
            case SEARCH_FIELD:
                tip.add("§7" + tr("inventorytodo.tip.search"));
                break;
            case SETTING: {
                List<String[]> rows = settingsRows();
                if (h.index >= 0 && h.index < rows.size()) tip.add(rows.get(h.index)[0] + ": §e" + rows.get(h.index)[1]);
                tip.add("§7" + tr("inventorytodo.tip.setting"));
                break;
            }
            case ROW_TEXT:
                if (h.index >= 0 && h.index < store.entries.size() && store.entries.get(h.index).item == null)
                    tip.add("§8" + tr("inventorytodo.tip.row.menu"));
                break;
            default:
                break;
        }
        return tip;
    }

    private static void help(List<String> tip, String key, Object... args) {
        tip.add("§7- " + tr(key, args));
    }

    private List<String> itemTooltipExtras(Hit h) {
        List<String> tip = new ArrayList<>();
        if (!searchOpen && h.index >= 0 && h.index < store.entries.size()) {
            TodoEntry e = store.entries.get(h.index);
            if (e.item != null) {
                tip.add("§b" + tr("inventorytodo.tip.progress", e.have, e.target > 0 ? String.valueOf(e.target) : "-"));
                if (RecipeViewer.any()) tip.add("§8" + tr("inventorytodo.tip.row.icon"));
                tip.add("§8" + tr("inventorytodo.tip.row.ingredients"));
                tip.add("§8" + tr("inventorytodo.tip.row.count"));
                tip.add("§8" + tr("inventorytodo.tip.row.menu"));
            }
        } else if (searchOpen && h.type == HitType.SEARCH_RESULT) {
            tip.add("§8" + tr("inventorytodo.tip.result"));
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

    /** Draws the panel (and its tooltip). Called after the container screen has been drawn. */
    public void render(Object screen, Gfx g, int mx, int my) {
        if (!active(screen)) return;
        lastGfx = g;
        syncTextInput(true);
        pollDrag();
        prepare(screen);
        updateSearchIfNeeded();
        g.raise();
        try {
            if (cfg().minimized) drawMini(g, mx, my);
            else drawPanel(g, mx, my);
            if (menu != null) drawMenu(g, mx, my);
        } catch (Throwable t) {
            Log.error("Error while drawing the panel", t);
        }
        if (dragging || menu != null) return;
        if (!insidePanel(mx, my)) return;
        Object held = mc().carriedStack();
        if (held != null && !mc().isEmpty(held)) return;
        Hit h = hitTest(mx, my);
        Object stack = stackUnderMouse(screen, mx, my);
        if (stack != null) g.itemTooltip(stack, itemTooltipExtras(h), mx, my);
        else {
            List<String> tip = panelTooltip(h);
            if (!tip.isEmpty()) g.tooltip(tip, mx, my);
        }
    }

    private void drawMini(Gfx g, int mx, int my) {
        boolean hover = insidePanel(mx, my);
        g.fill(px, py, px + MINI, py + MINI, C_BORDER);
        g.fill(px + 1, py + 1, px + MINI - 1, py + MINI - 1, hover ? C_BTN_HOVER : C_HEADER);
        g.item(mc().iconStack(), px + 2, py + 2);
        int rem = remainingCount();
        if (rem > 0) {
            g.raise();
            String s = String.valueOf(rem);
            g.text(s, px + MINI - 1 - g.textWidth(s), py + MINI - 9, 0xFFFFFF55, true);
        }
    }

    private void drawPanel(Gfx g, int mx, int my) {
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, C_BORDER);
        g.fill(px, py, px + pw, py + ph, C_BG);
        g.fill(px, py, px + pw, py + HEADER_H, C_HEADER);

        int done = store.entries.size() - remainingCount();
        String title = settingsOpen ? tr("inventorytodo.tip.settings")
            : tr("inventorytodo.title") + " §7" + done + "/" + store.entries.size();
        g.text(g.trim(title, pw - 64), px + 3, py + 2, C_TEXT, true);
        drawButton(g, btnHelpX(), py + 1, 10, 10, "?", mx, my);
        drawButton(g, btnHudX(), py + 1, 10, 10, tr("inventorytodo.btn.hud.short"), mx, my);
        if (cfg().hudEnabled) g.fill(btnHudX() + 1, py + 10, btnHudX() + 9, py + 11, 0xFF55FF55);
        drawButton(g, btnSettingsX(), py + 1, 10, 10, "≡", mx, my);
        drawButton(
            g,
            btnModeX(),
            py + 1,
            10,
            10,
            tr(
                "inventorytodo.mode." + cfg().mode.name()
                    .toLowerCase(Locale.ROOT) + ".short"),
            mx,
            my);
        drawButton(g, btnMinX(), py + 1, 10, 10, "_", mx, my);

        if (settingsOpen) drawSettings(g, mx, my);
        else if (searchOpen) drawSearch(g, mx, my);
        else drawRows(g, mx, my);

        int fy = footerY();
        g.fill(px, fy, px + pw, fy + 1, 0x60FFFFFF);
        String info;
        if (searchOpen || settingsOpen) {
            drawButton(g, px + ADD_TEXT_X, fy + 1, ADD_TEXT_W, 12, tr("inventorytodo.btn.close"), mx, my);
            info = searchOpen && !results.isEmpty() ? tr("inventorytodo.search.count", results.size()) : "";
        } else {
            drawButton(g, px + ADD_TEXT_X, fy + 1, ADD_TEXT_W, 12, tr("inventorytodo.btn.addtext"), mx, my);
            drawButton(g, px + ADD_ITEM_X, fy + 1, ADD_ITEM_W, 12, tr("inventorytodo.btn.additem"), mx, my);
            info = store.entries.size() > rowsVisible
                ? (scroll + 1) + "-" + Math.min(store.entries.size(), scroll + rowsVisible) + "/" + store.entries.size()
                : "";
        }
        int infoX = px + ADD_ITEM_X + ADD_ITEM_W + 3;
        if (message != null && System.currentTimeMillis() < messageUntil && !searchOpen && !settingsOpen) {
            // feedback messages are shown above the footer so they are never cut off
            String m = g.trim(message, Math.max(20, pw - 6));
            g.raise(); // above the row icons and text
            g.fill(px, fy - 12, px + pw, fy, 0xF0303050);
            g.text(m, px + 3, fy - 10, 0xFFFFFF55, true);
        }
        if (!info.isEmpty() && px + pw - 3 - g.textWidth(info) >= infoX) {
            g.text(info, px + pw - 3 - g.textWidth(info), fy + 3, C_TEXT_HINT, false);
        }
    }

    private void drawRows(Gfx g, int mx, int my) {
        if (store.entries.isEmpty()) {
            g.text(g.trim(tr("inventorytodo.empty"), pw - 8), px + 4, listTop + 5, C_TEXT_HINT, false);
            return;
        }
        long now = System.currentTimeMillis();
        for (int r = 0; r < rowsVisible; r++) {
            int i = scroll + r;
            if (i >= store.entries.size()) break;
            TodoEntry e = store.entries.get(i);
            int ry = listTop + r * ROW_H;
            if (i == flashIndex && now < flashUntil && (now / 150) % 2 == 0) {
                g.fill(px, ry, px + pw, ry + ROW_H, 0x5055FF55);
            } else if (inside(mx, my, px, ry, pw, ROW_H) && menu == null) {
                g.fill(px, ry, px + pw, ry + ROW_H, C_ROW_HOVER);
            }
            if (r > 0) g.fill(px + 2, ry, px + pw - 2, ry + 1, 0x18FFFFFF);
            int cx = px + 4, cy = ry + 5;
            g.fill(cx, cy, cx + 8, cy + 8, 0xFFA0A0B0);
            g.fill(cx + 1, cy + 1, cx + 7, cy + 7, e.isDone() ? 0xFF3CB043 : 0xFF202028);
            if (e.item != null) g.item(e.item, px + 15, ry + 1);
        }
        for (int r = 0; r < rowsVisible; r++) {
            int i = scroll + r;
            if (i >= store.entries.size()) break;
            TodoEntry e = store.entries.get(i);
            int ry = listTop + r * ROW_H;
            int tx = px + (e.item != null ? 34 : 15);
            int right = px + pw - 12;
            if (e.item != null) {
                String ct = counterText(e);
                int cw = g.textWidth(ct);
                int col = e.target > 0 && e.have >= e.target ? 0xFF55FF55 : (e.have > 0 ? 0xFFFFFF55 : 0xFFFF7777);
                if (!(field != null && editTarget && editIndex == i)) g.text(ct, right - cw - 2, ry + 5, col, true);
                right = right - cw - 6;
            }
            if (field != null && editIndex == i) {
                field.x = editTarget ? px + pw - 12 - 46 : tx - 2;
                field.y = ry + 3;
                continue;
            }
            String text = e.displayText();
            String shown = g.trim(text, Math.max(0, right - tx));
            if (shown.length() < text.length()) shown = g.trim(text, Math.max(0, right - tx - g.textWidth("..."))) + "...";
            if (e.isDone()) g.text("§m" + shown, tx, ry + 5, C_TEXT_DONE, false);
            else g.text(shown, tx, ry + 5, C_TEXT, true);
            boolean delHover = inside(mx, my, px + pw - 11, ry, 10, ROW_H) && menu == null;
            g.text("x", px + pw - 8, ry + 5, delHover ? 0xFFFF5555 : 0xFF707080, false);
        }
        if (field != null && editIndex >= scroll && editIndex < scroll + rowsVisible) field.draw(g, null);
    }

    private void drawSearch(Gfx g, int mx, int my) {
        if (searchField != null) {
            searchField.x = px + 4;
            searchField.y = listTop + 2;
            searchField.width = pw - 8;
            searchField.draw(g, tr("inventorytodo.search.hint"));
        }
        int gx = searchGridX(), gy = searchGridY(), cols = searchCols();
        if (results.isEmpty()) {
            String msg = (searchField == null || searchField.getText()
                .trim()
                .isEmpty()) ? tr("inventorytodo.search.prompt") : tr("inventorytodo.search.none");
            g.text(g.trim(msg, pw - 8), px + 4, gy + 5, C_TEXT_HINT, false);
            return;
        }
        for (int r = 0; r < SEARCH_ROWS; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = (resultScroll + r) * cols + c;
                if (idx >= results.size()) break;
                int x = gx + c * 18, y = gy + r * 18;
                g.fill(x, y, x + 18, y + 18, inside(mx, my, x, y, 18, 18) ? 0x60FFFFFF : 0x20FFFFFF);
            }
        }
        for (int r = 0; r < SEARCH_ROWS; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = (resultScroll + r) * cols + c;
                if (idx >= results.size()) break;
                g.item(results.get(idx), gx + c * 18 + 1, gy + r * 18 + 1);
            }
        }
    }

    private void drawSettings(Gfx g, int mx, int my) {
        List<String[]> rows = settingsRows();
        for (int i = 0; i < rows.size(); i++) {
            int y = listTop + 2 + i * SETTINGS_ROW_H;
            if (inside(mx, my, px, y, pw, SETTINGS_ROW_H)) g.fill(px, y, px + pw, y + SETTINGS_ROW_H, C_ROW_HOVER);
            String value = rows.get(i)[1];
            int vw = g.textWidth(value);
            String label = rows.get(i)[0];
            int avail = pw - 14 - vw;
            String shown = g.trim(label, avail);
            if (shown.length() < label.length()) shown = g.trim(label, Math.max(0, avail - g.textWidth("..."))) + "...";
            g.text(shown, px + 5, y + 3, C_TEXT, true);
            g.text(value, px + pw - 5 - vw, y + 3, 0xFFFFFF55, true);
        }
    }

    private void drawMenu(Gfx g, int mx, int my) {
        g.raise();
        Menu m = menu;
        g.fill(m.x - 1, m.y - 1, m.x + m.w + 1, m.y + m.h + 1, C_BORDER);
        g.fill(m.x, m.y, m.x + m.w, m.y + m.h, 0xF0181828);
        int hover = m.indexAt(mx, my);
        for (int i = 0; i < m.actions.size(); i++) {
            int y = m.y + 2 + i * MENU_ROW_H;
            if (i == hover) g.fill(m.x + 1, y, m.x + m.w - 1, y + MENU_ROW_H, C_BTN_HOVER);
            Action a = m.actions.get(i);
            int col = a == Action.DELETE ? 0xFFFF8080 : C_TEXT;
            g.text(tr(a.key), m.x + 5, y + 2, col, true);
        }
    }

    private void drawButton(Gfx g, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h) && menu == null;
        g.fill(x, y, x + w, y + h, hover ? C_BTN_HOVER : C_BTN);
        String s = g.trim(label, w - 2);
        if (s.isEmpty()) s = label;
        g.text(s, x + (w - g.textWidth(s)) / 2 + 1, y + (h - 8) / 2 + 1, 0xFFFFFFFF, true);
    }

    /** Sub-pixel offset while dragging (applied by renderers that support it) */
    public float dragOffsetX() {
        return dragging ? dragFracX : 0f;
    }

    public float dragOffsetY() {
        return dragging ? dragFracY : 0f;
    }
}
