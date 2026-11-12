package io.github.junjun1z.inventorytodo.core;

import java.util.ArrayList;
import java.util.List;

/** Small list of unfinished rows drawn on the in-game HUD (no screen open). */
public final class Hud {

    private static final int ROW_H = 12;
    private static int tick;

    private Hud() {}

    /** Called every client tick while no screen is open */
    public static void tick() {
        if (!Core.settings().hudEnabled) return;
        if (++tick % 10 == 0) Core.recount(null);
    }

    public static void toggle() {
        Settings s = Core.settings();
        s.hudEnabled = !s.hudEnabled;
        s.save();
        if (s.hudEnabled) Core.recount(null);
    }

    public static void render(Gfx g) {
        Settings s = Core.settings();
        Mc mc = Core.mc();
        if (!s.hudEnabled || mc == null || !mc.hasPlayer()) return;
        TodoStore store = Core.store();
        store.ensureLoaded();
        List<TodoEntry> rows = new ArrayList<>();
        int done = 0;
        for (TodoEntry e : store.entries) {
            if (e.isDone()) {
                done++;
                if (s.hudHideDone) continue;
            }
            rows.add(e);
        }
        if (rows.isEmpty()) return;
        int more = Math.max(0, rows.size() - s.hudMaxRows);
        if (more > 0) rows = rows.subList(0, s.hudMaxRows);

        String title = mc.tr("inventorytodo.hud.title") + " §7" + done + "/" + store.entries.size();
        int w = g.textWidth(title);
        List<String> texts = new ArrayList<>();
        List<String> counters = new ArrayList<>();
        for (TodoEntry e : rows) {
            String t = e.displayText();
            String c = e.item == null ? "" : (e.target > 0 ? e.have + "/" + e.target : String.valueOf(e.have));
            t = g.trim(t, 130);
            texts.add(t);
            counters.add(c);
            w = Math.max(w, (e.item != null ? 12 : 0) + g.textWidth(t) + (c.isEmpty() ? 0 : 6 + g.textWidth(c)));
        }
        String moreText = more > 0 ? mc.tr("inventorytodo.hud.more", more) : null;
        if (moreText != null) w = Math.max(w, g.textWidth(moreText));
        w += 8;
        int h = 14 + rows.size() * ROW_H + (moreText != null ? 10 : 0) + 2;

        int sw = mc.screenWidth(), sh = mc.screenHeight();
        int x, y;
        switch (s.hudPos) {
            case TOP_LEFT:
                x = 4;
                y = 4;
                break;
            case BOTTOM_LEFT:
                x = 4;
                y = sh - h - 4;
                break;
            case BOTTOM_RIGHT:
                x = sw - w - 4;
                y = sh - h - 4;
                break;
            case TOP_RIGHT:
                x = sw - w - 4;
                y = 4;
                break;
            case RIGHT_CENTER:
            default:
                // right side, below where minimaps usually are
                x = sw - w - 4;
                y = Math.max(4, Math.min(sh / 2 - h / 2, sh - h - 40));
                break;
        }
        g.fill(x, y, x + w, y + h, 0x90000000);
        g.text(title, x + 4, y + 3, 0xFFFFFFFF, true);
        int ry = y + 14;
        for (int i = 0; i < rows.size(); i++) {
            TodoEntry e = rows.get(i);
            int tx = x + 4;
            if (e.item != null) {
                // half-size icon area: draw a coloured marker instead of a full item to keep the HUD compact
                g.fill(tx, ry + 2, tx + 8, ry + 10, e.isDone() ? 0xFF3CB043 : (e.have > 0 ? 0xFFE0C040 : 0xFFB04040));
                tx += 12;
            }
            int col = e.isDone() ? 0xFF808080 : 0xFFE0E0E0;
            g.text((e.isDone() ? "§m" : "") + texts.get(i), tx, ry + 2, col, true);
            String c = counters.get(i);
            if (!c.isEmpty()) {
                int ccol = e.isDone() ? 0xFF55FF55 : (e.have > 0 ? 0xFFFFFF55 : 0xFFFF7777);
                g.text(c, x + w - 4 - g.textWidth(c), ry + 2, ccol, true);
            }
            ry += ROW_H;
        }
        if (moreText != null) g.text(moreText, x + 4, ry + 1, 0xFF9090A0, true);
    }
}
