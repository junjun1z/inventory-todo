package io.github.junjun1z.inventorytodo.core;

import java.util.List;

/**
 * Drawing operations for one frame. Colours are ARGB (alpha is required for text).
 * Strings may contain legacy formatting codes (section sign).
 */
public interface Gfx {

    void fill(int x1, int y1, int x2, int y2, int argb);

    void text(String s, int x, int y, int argb, boolean shadow);

    int textWidth(String s);

    String trim(String s, int width);

    void item(Object stack, int x, int y);

    /** Draws the following elements above everything drawn so far */
    void raise();

    /** Shows a tooltip with plain lines */
    void tooltip(List<String> lines, int mx, int my);

    /** Shows the item's normal tooltip followed by extra lines */
    void itemTooltip(Object stack, List<String> extra, int mx, int my);
}
