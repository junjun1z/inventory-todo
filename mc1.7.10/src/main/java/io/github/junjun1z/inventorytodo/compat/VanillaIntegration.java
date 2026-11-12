package io.github.junjun1z.inventorytodo.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import io.github.junjun1z.inventorytodo.InventoryTodo;

/** Works with Minecraft + Forge only. */
public class VanillaIntegration implements Integration {

    private static final RenderItem RENDER_ITEM = new RenderItem();
    private List<ItemStack> items;

    @Override
    public ItemStack hoveredStack(GuiContainer gui, int mx, int my) {
        if (gui.inventorySlots == null) return null;
        int left = GuiUtil.guiLeft(gui), top = GuiUtil.guiTop(gui);
        for (Object o : gui.inventorySlots.inventorySlots) {
            if (!(o instanceof Slot)) continue;
            Slot slot = (Slot) o;
            int x = left + slot.xDisplayPosition, y = top + slot.yDisplayPosition;
            if (mx >= x - 1 && mx < x + 17 && my >= y - 1 && my < y + 17) {
                return slot.getHasStack() ? slot.getStack() : null;
            }
        }
        return null;
    }

    @Override
    public boolean hasRecipeViewer() {
        return false;
    }

    @Override
    public void showRecipes(ItemStack stack, boolean usage) {}

    /** All items from the creative tabs (built once, on first search). */
    @Override
    @SuppressWarnings("unchecked")
    public List<ItemStack> itemList() {
        if (items != null) return items;
        List<ItemStack> list = new ArrayList<>();
        for (Object o : Item.itemRegistry) {
            if (!(o instanceof Item)) continue;
            Item item = (Item) o;
            try {
                for (CreativeTabs tab : item.getCreativeTabs()) {
                    if (tab == null) continue;
                    item.getSubItems(item, tab, list);
                    break; // items listed in several tabs would show up twice
                }
            } catch (Throwable t) {
                InventoryTodo.LOG.debug("Could not list sub-items of " + Item.itemRegistry.getNameForObject(item), t);
            }
        }
        list.removeIf(s -> s == null || s.getItem() == null);
        items = Collections.unmodifiableList(list);
        InventoryTodo.LOG.info("Item list built: " + items.size() + " items");
        return items;
    }

    @Override
    public void drawItem(int x, int y, ItemStack stack) {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderHelper.enableGUIStandardItemLighting();
            float z = RENDER_ITEM.zLevel;
            RENDER_ITEM.zLevel = 100f;
            RENDER_ITEM.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
            RENDER_ITEM.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
            RENDER_ITEM.zLevel = z;
        } finally {
            RenderHelper.disableStandardItemLighting();
            GL11.glPopAttrib();
        }
    }
}
