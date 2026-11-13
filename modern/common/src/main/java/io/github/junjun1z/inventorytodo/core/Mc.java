package io.github.junjun1z.inventorytodo.core;

import java.nio.file.Path;
import java.util.List;

/**
 * Everything the version-independent code needs from Minecraft.
 * <p>
 * Item stacks are passed around as {@code Object} so that this layer compiles against every Minecraft version.
 * Each supported Minecraft version provides one implementation.
 */
public interface Mc {

    // ------------------------------------------------------------------ items

    boolean isEmpty(Object stack);

    /** Copy with count 1 */
    Object copyOne(Object stack);

    /** Same item and same data (NBT / components) */
    boolean sameItem(Object a, Object b);

    /** True if {@code stack} counts towards the goal {@code target} */
    boolean countsAs(Object target, Object stack, boolean ignoreDataWhenTargetHasNone);

    int count(Object stack);

    int maxStackSize(Object stack);

    String displayName(Object stack);

    /** English name if it can be resolved, otherwise null */
    String englishName(Object stack);

    /** Registry id such as {@code minecraft:iron_ingot} */
    String registryId(Object stack);

    /** All searchable items */
    List<Object> allItems();

    /** Stack of the item with exactly this id, or null */
    Object stackById(String id);

    /** Serialize to a string (item id + data), or null */
    String serialize(Object stack);

    /** Inverse of {@link #serialize}; null if the item no longer exists */
    Object deserialize(String data);

    /** Icon for the minimized panel */
    Object iconStack();

    /**
     * Ingredients of the first crafting-table recipe that makes this item.
     *
     * @return null if recipes cannot be read here (e.g. on a server in newer versions), empty if none found
     */
    Recipe recipeFor(Object stack);

    /** One crafting recipe: what one craft needs and how many items it makes */
    final class Recipe {

        public final List<Object> ingredients; // one stack per distinct ingredient, count = amount per craft
        public final int resultCount;

        public Recipe(List<Object> ingredients, int resultCount) {
            this.ingredients = ingredients;
            this.resultCount = Math.max(1, resultCount);
        }
    }

    // ------------------------------------------------------------------ player / world

    boolean hasPlayer();

    /** Main inventory + hotbar + offhand */
    List<Object> playerStacks();

    /** Stacks in the non-player slots of the open container screen (empty if none) */
    List<Object> containerStacks(Object screen);

    /** Item held on the mouse cursor, or an empty stack */
    Object carriedStack();

    /** Key for the current world or server, safe as a file name */
    String worldKey();

    Path configDir();

    // ------------------------------------------------------------------ screen

    boolean isContainerScreen(Object screen);

    int guiLeft(Object screen);

    int guiTop(Object screen);

    int imageWidth(Object screen);

    /** Item in the inventory slot under the mouse, or null */
    Object hoveredSlotStack(Object screen, int mx, int my);

    /** GUI-scaled screen size */
    int screenWidth();

    int screenHeight();

    /** Mouse position in GUI coordinates (sub-pixel), read directly from the window */
    double mouseX();

    double mouseY();

    boolean isLeftMouseDown();

    /**
     * Tells the game that the panel's text field wants keyboard text (Minecraft 26.x only enables OS text input
     * while a text field has focus). Default: nothing to do.
     */
    default void setTextInputActive(Object owner, boolean active) {}

    boolean isShiftDown();

    boolean isCtrlDown();

    boolean isAltDown();

    // ------------------------------------------------------------------ text

    String tr(String key, Object... args);

    String clipboard();

    void setClipboard(String text);

    /** Display name of the "add hovered item" key binding, or null if unbound */
    String addKeyName();

    /** Display name of the "toggle HUD" key binding, or null if unbound */
    String hudKeyName();
}
