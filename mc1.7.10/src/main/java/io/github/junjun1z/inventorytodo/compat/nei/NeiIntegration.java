package io.github.junjun1z.inventorytodo.compat.nei;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import codechicken.nei.ItemList;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import io.github.junjun1z.inventorytodo.compat.VanillaIntegration;

/** NEI-backed integration. Only uses APIs present in both the original NEI 1.0.5.x and the GTNH fork. */
public class NeiIntegration extends VanillaIntegration {

    @Override
    public ItemStack hoveredStack(GuiContainer gui, int mx, int my) {
        ItemStack slot = super.hoveredStack(gui, mx, my);
        if (slot != null) return slot;
        // NEI item list, bookmarks and recipe screens
        return GuiContainerManager.getStackMouseOver(gui);
    }

    @Override
    public boolean hasRecipeViewer() {
        return true;
    }

    @Override
    public void showRecipes(ItemStack stack, boolean usage) {
        if (usage) GuiUsageRecipe.openRecipeGui("item", stack.copy());
        else GuiCraftingRecipe.openRecipeGui("item", stack.copy());
    }

    @Override
    public List<ItemStack> itemList() {
        List<ItemStack> items = ItemList.items;
        // NEI builds its list in the background; use our own until it is ready
        return items == null || items.isEmpty() ? super.itemList() : items;
    }

    @Override
    public void drawItem(int x, int y, ItemStack stack) {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        try {
            GuiContainerManager.drawItem(x, y, stack);
        } finally {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
    }
}
