package io.github.junjun1z.inventorytodo.core;

import java.util.ArrayList;
import java.util.List;

/**
 * An optional recipe viewer (JEI, EMI, REI...). Implementations must only touch the viewer's classes after it
 * was detected, so nothing is loaded when the viewer is not installed.
 */
public interface RecipeViewer {

    String name();

    /** Item under the mouse in the viewer's overlay (item list, bookmarks, recipe screen), or null */
    Object hoveredStack();

    /** @return true if a recipe screen was opened */
    boolean show(Object stack, boolean usages);

    /** Registered viewers (filled during start-up) */
    List<RecipeViewer> ALL = new ArrayList<>();

    static boolean any() {
        return !ALL.isEmpty();
    }

    static Object hovered() {
        for (RecipeViewer v : ALL) {
            try {
                Object s = v.hoveredStack();
                if (s != null) return s;
            } catch (Throwable t) {
                Log.debug("hovered stack from " + v.name() + " failed: " + t);
            }
        }
        return null;
    }

    static boolean showRecipes(Object stack, boolean usages) {
        for (RecipeViewer v : ALL) {
            try {
                if (v.show(stack, usages)) return true;
            } catch (Throwable t) {
                Log.warn("Could not open " + v.name() + ": " + t);
            }
        }
        return false;
    }
}
