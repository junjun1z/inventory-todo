package io.github.junjun1z.inventorytodo.compat;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

import io.github.junjun1z.inventorytodo.TodoPanel;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Events used in every mode. */
public final class GuiEvents {

    private GuiEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new GuiEvents());
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // Leaving inventory screens: save whatever is being typed
        if (!(event.gui instanceof GuiContainer)) TodoPanel.INSTANCE.onGuiClosed();
    }
}
