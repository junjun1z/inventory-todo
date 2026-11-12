package io.github.junjun1z.inventorytodo.core;

/**
 * Minimal single-line text field drawn with {@link Gfx}. Own implementation so that it behaves the same on every
 * Minecraft version (vanilla's EditBox API changes between versions).
 */
public final class TextField {

    public int x, y, width, height = 12;
    private final int maxLength;
    private final boolean digitsOnly;
    private String text = "";
    private int cursor;
    private boolean focused = true;
    private long blinkStart = System.currentTimeMillis();

    public TextField(int width, int maxLength, boolean digitsOnly) {
        this.width = width;
        this.maxLength = maxLength;
        this.digitsOnly = digitsOnly;
    }

    public String getText() {
        return text;
    }

    public void setText(String t) {
        text = t == null ? "" : (t.length() > maxLength ? t.substring(0, maxLength) : t);
        cursor = text.length();
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean f) {
        focused = f;
        blinkStart = System.currentTimeMillis();
    }

    private void insert(String s) {
        StringBuilder clean = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c < 32 || c == 127 || c == '§') continue;
            if (digitsOnly && !Character.isDigit(c)) continue;
            clean.append(c);
        }
        if (clean.length() == 0) return;
        int room = maxLength - text.length();
        if (room <= 0) return;
        String add = clean.length() > room ? clean.substring(0, room) : clean.toString();
        text = text.substring(0, cursor) + add + text.substring(cursor);
        cursor += add.length();
        blinkStart = System.currentTimeMillis();
    }

    public boolean charTyped(char c) {
        if (!focused) return false;
        insert(String.valueOf(c));
        return true;
    }

    /** Handles editing keys. Returns true if the key was used. */
    public boolean keyPressed(int key) {
        if (!focused) return false;
        Mc mc = Core.mc();
        boolean ctrl = mc.isCtrlDown();
        blinkStart = System.currentTimeMillis();
        if (key == Keys.BACKSPACE) {
            if (cursor > 0) {
                int from = ctrl ? wordStart(cursor) : cursor - 1;
                text = text.substring(0, from) + text.substring(cursor);
                cursor = from;
            }
            return true;
        }
        if (key == Keys.DELETE) {
            if (cursor < text.length()) {
                int to = ctrl ? wordEnd(cursor) : cursor + 1;
                text = text.substring(0, cursor) + text.substring(to);
            }
            return true;
        }
        if (key == Keys.LEFT) {
            if (cursor > 0) cursor = ctrl ? wordStart(cursor) : cursor - 1;
            return true;
        }
        if (key == Keys.RIGHT) {
            if (cursor < text.length()) cursor = ctrl ? wordEnd(cursor) : cursor + 1;
            return true;
        }
        if (key == Keys.HOME) {
            cursor = 0;
            return true;
        }
        if (key == Keys.END) {
            cursor = text.length();
            return true;
        }
        if (ctrl && key == Keys.V) {
            String clip = mc.clipboard();
            if (clip != null) insert(clip.replace('\n', ' ')
                .replace('\r', ' '));
            return true;
        }
        if (ctrl && key == Keys.C) {
            mc.setClipboard(text);
            return true;
        }
        if (ctrl && key == Keys.A) {
            cursor = text.length();
            return true;
        }
        return false;
    }

    private int wordStart(int from) {
        int i = from;
        while (i > 0 && text.charAt(i - 1) == ' ') i--;
        while (i > 0 && text.charAt(i - 1) != ' ') i--;
        return i;
    }

    private int wordEnd(int from) {
        int i = from;
        while (i < text.length() && text.charAt(i) == ' ') i++;
        while (i < text.length() && text.charAt(i) != ' ') i++;
        return i;
    }

    public boolean contains(int mx, int my) {
        return mx >= x && my >= y && mx < x + width && my < y + height;
    }

    /** Moves the cursor to the clicked character */
    public void click(Gfx g, int mx) {
        if (g == null) {
            cursor = text.length();
            return;
        }
        int rel = mx - (x + 4);
        String visible = visibleText(g);
        int offset = text.length() - visible.length();
        int best = text.length();
        for (int i = 0; i <= visible.length(); i++) {
            if (g.textWidth(visible.substring(0, i)) >= rel) {
                best = offset + i;
                break;
            }
        }
        cursor = Math.max(0, Math.min(text.length(), best));
    }

    /** The tail of the text that fits (the cursor is kept visible by always showing the end) */
    private String visibleText(Gfx g) {
        int avail = width - 8;
        String before = text.substring(0, cursor);
        // keep the cursor visible: drop characters from the start until the part before the cursor fits
        int start = 0;
        while (start < before.length() && g.textWidth(before.substring(start)) > avail) start++;
        String shown = text.substring(start);
        return g.trim(shown, avail);
    }

    public void draw(Gfx g, String placeholder) {
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, focused ? 0xFFE0E0E0 : 0xFFA0A0A0);
        g.fill(x, y, x + width, y + height, 0xFF000000);
        int ty = y + (height - 8) / 2;
        if (text.isEmpty() && !focused && placeholder != null) {
            g.text(g.trim(placeholder, width - 8), x + 4, ty, 0xFF707080, false);
            return;
        }
        int avail = width - 8;
        String before = text.substring(0, cursor);
        int start = 0;
        while (start < before.length() && g.textWidth(before.substring(start)) > avail) start++;
        String shown = g.trim(text.substring(start), avail);
        g.text(shown, x + 4, ty, 0xFFE0E0E0, true);
        if (focused && ((System.currentTimeMillis() - blinkStart) / 500) % 2 == 0) {
            int cx = x + 4 + g.textWidth(text.substring(start, Math.max(start, Math.min(cursor, start + shown.length()))));
            g.fill(cx, ty - 1, cx + 1, ty + 9, 0xFFD0D0D0);
        }
    }
}
