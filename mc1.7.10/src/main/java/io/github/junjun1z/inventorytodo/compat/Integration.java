package io.github.junjun1z.inventorytodo.compat;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

/** What the panel needs from the environment. Implemented without dependencies, and again for NEI. */
public interface Integration {

    /** The item under the mouse in the screen (inventory slot, recipe viewer list, ...), or null */
    ItemStack hoveredStack(GuiContainer gui, int mx, int my);

    /** True if recipes/usages can be shown */
    boolean hasRecipeViewer();

    /** Opens the recipes (usage == false) or usages (usage == true) of the item */
    void showRecipes(ItemStack stack, boolean usage);

    /** Every item that can be searched for */
    List<ItemStack> itemList();

    /** Draws an item icon at the given screen position */
    void drawItem(int x, int y, ItemStack stack);
}
