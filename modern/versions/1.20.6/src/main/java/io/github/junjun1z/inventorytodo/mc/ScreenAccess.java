package io.github.junjun1z.inventorytodo.mc;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/** Protected container-screen fields; each loader reads them its own way (access widener / Forge getters). */
public interface ScreenAccess {

    int left(AbstractContainerScreen<?> screen);

    int top(AbstractContainerScreen<?> screen);

    int imageWidth(AbstractContainerScreen<?> screen);

    Slot hoveredSlot(AbstractContainerScreen<?> screen);
}
