package io.github.junjun1z.inventorytodo.compat.nei;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.API;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerDrawHandler;
import codechicken.nei.guihook.IContainerInputHandler;
import codechicken.nei.guihook.IContainerObjectHandler;
import codechicken.nei.guihook.IContainerTooltipHandler;
import io.github.junjun1z.inventorytodo.TodoPanel;
import io.github.junjun1z.inventorytodo.compat.Compat;

/**
 * Connects the panel to NEI's GUI hooks.
 * <p>
 * Every interface method is implemented here on purpose: the GTNH fork turned several of them into default
 * methods, but the original NEI calls them as abstract methods (AbstractMethodError if missing).
 */
public final class NeiFrontend implements IContainerInputHandler, IContainerDrawHandler, IContainerObjectHandler,
    IContainerTooltipHandler, INEIGuiHandler {

    private final TodoPanel panel = TodoPanel.INSTANCE;

    private NeiFrontend() {}

    public static void register() {
        NeiFrontend f = new NeiFrontend();
        Compat.set(new NeiIntegration());
        // Receive input before NEI itself (so typing "e" does not close the inventory)
        GuiContainerManager.inputHandlers.addFirst(f);
        GuiContainerManager.addDrawHandler(f);
        // Tell NEI which item is under the mouse: NEI's recipe/usage keys and tooltips then work on the panel
        GuiContainerManager.objectHandlers.addFirst(f);
        GuiContainerManager.addTooltipHandler(f);
        // Drag and drop from NEI, and hiding NEI items behind the panel
        API.registerNEIGuiHandler(f);
    }

    // ---------------------------------------------------------------- IContainerInputHandler

    @Override
    public boolean keyTyped(GuiContainer gui, char c, int key) {
        return panel.keyTyped(gui, c, key);
    }

    /** GTNH fork only: NEI hotkeys (bookmark, recipe, ...) take a separate path. Block them while typing. */
    @Override
    public boolean onHotkeyPressed(GuiContainer gui) {
        return panel.isTyping();
    }

    @Override
    public void onKeyTyped(GuiContainer gui, char c, int key) {}

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char c, int key) {
        return false;
    }

    /** NEI already held a dragged item when the current button press started */
    private boolean pressStartedWithNeiItem;

    @Override
    public boolean mouseClicked(GuiContainer gui, int mx, int my, int button) {
        // An item picked up in NEI (dragged out of the item list / bookmarks, or middle-clicked) follows the cursor
        // until the next click. NEI drops it through INEIGuiHandler.handleDragNDrop, but only if this handler (first
        // in the list, so typing works) lets the click through - and the GTNH bookmark area, which can lie under the
        // panel, would take it first. So drop it on the panel here, and otherwise leave the click to NEI.
        ItemStack held = NeiDrag.held();
        pressStartedWithNeiItem = held != null;
        if (held != null) {
            if (panel.dropStack(gui, mx, my, held)) {
                NeiDrag.clear();
                return true;
            }
            return false;
        }
        return panel.mouseClicked(gui, mx, my, button);
    }

    @Override
    public void onMouseClicked(GuiContainer gui, int mx, int my, int button) {}

    @Override
    public void onMouseUp(GuiContainer gui, int mx, int my, int button) {
        // dragged out of NEI and released over the panel (NEI itself would keep it on the cursor until a click)
        ItemStack held = NeiDrag.held();
        if (held != null && !pressStartedWithNeiItem && panel.dropStack(gui, mx, my, held)) NeiDrag.clear();
        pressStartedWithNeiItem = false;
        panel.mouseUp(gui, mx, my, button);
    }

    @Override
    public boolean mouseScrolled(GuiContainer gui, int mx, int my, int scrolled) {
        return panel.mouseScrolled(gui, mx, my, scrolled > 0 ? 1 : -1);
    }

    @Override
    public void onMouseScrolled(GuiContainer gui, int mx, int my, int scrolled) {}

    @Override
    public void onMouseDragged(GuiContainer gui, int mx, int my, int button, long heldTime) {
        panel.mouseDragged(gui, mx, my, button);
    }

    // ---------------------------------------------------------------- IContainerObjectHandler

    @Override
    public void guiTick(GuiContainer gui) {
        panel.tick(gui);
    }

    @Override
    public void refresh(GuiContainer gui) {}

    @Override
    public void load(GuiContainer gui) {
        panel.onGuiOpened(gui);
        // Stay first even if NEI (or another mod) added an input handler to the front later
        if (GuiContainerManager.inputHandlers.peekFirst() != this) {
            GuiContainerManager.inputHandlers.remove(this);
            GuiContainerManager.inputHandlers.addFirst(this);
        }
    }

    @Override
    public ItemStack getStackUnderMouse(GuiContainer gui, int mx, int my) {
        return panel.stackUnderMouse(gui, mx, my);
    }

    @Override
    public boolean objectUnderMouse(GuiContainer gui, int mx, int my) {
        return panel.isOverPanel(gui, mx, my);
    }

    @Override
    public boolean shouldShowTooltip(GuiContainer gui) {
        return true;
    }

    // ---------------------------------------------------------------- IContainerDrawHandler

    @Override
    public void onPreDraw(GuiContainer gui) {}

    @Override
    public void renderObjects(GuiContainer gui, int mx, int my) {}

    @Override
    public void postRenderObjects(GuiContainer gui, int mx, int my) {
        panel.render(gui, mx, my);
    }

    @Override
    public void renderSlotUnderlay(GuiContainer gui, Slot slot) {}

    @Override
    public void renderSlotOverlay(GuiContainer gui, Slot slot) {}

    // ---------------------------------------------------------------- IContainerTooltipHandler

    @Override
    public List<String> handleTooltip(GuiContainer gui, int mx, int my, List<String> tip) {
        if (tip.isEmpty()) tip.addAll(panel.panelTooltip(gui, mx, my));
        return tip;
    }

    @Override
    public List<String> handleItemDisplayName(GuiContainer gui, ItemStack stack, List<String> tip) {
        return tip;
    }

    @Override
    public List<String> handleItemTooltip(GuiContainer gui, ItemStack stack, int mx, int my, List<String> tip) {
        if (stack != null) tip.addAll(panel.itemTooltipExtras(gui, mx, my));
        return tip;
    }

    // ---------------------------------------------------------------- INEIGuiHandler

    @Override
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
        return currentVisibility;
    }

    @Override
    public Iterable<Integer> getItemSpawnSlots(GuiContainer gui, ItemStack item) {
        return Collections.emptyList();
    }

    @Override
    public List<TaggedInventoryArea> getInventoryAreas(GuiContainer gui) {
        return null;
    }

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mx, int my, ItemStack stack, int button) {
        if (!panel.dropStack(gui, mx, my, stack)) return false;
        stack.stackSize = 0; // used up: NEI then stops dragging it (both NEI versions check for 0)
        return true;
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return panel.overlaps(gui, x, y, w, h);
    }
}
