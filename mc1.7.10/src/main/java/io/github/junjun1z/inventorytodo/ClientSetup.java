package io.github.junjun1z.inventorytodo;

import cpw.mods.fml.common.Loader;
import io.github.junjun1z.inventorytodo.compat.Compat;
import io.github.junjun1z.inventorytodo.compat.GuiEvents;
import io.github.junjun1z.inventorytodo.compat.VanillaFrontend;

/** Client initialisation: picks the input/render frontend depending on which mods are installed. */
public final class ClientSetup {

    private ClientSetup() {}

    public static void init() {
        GuiEvents.register();
        KeyBindings.register();
        TodoHud.register();
        if (Loader.isModLoaded("NotEnoughItems")) {
            try {
                // Loaded by name so that NEI classes are never touched when NEI is absent.
                Class.forName("io.github.junjun1z.inventorytodo.compat.nei.NeiFrontend")
                    .getMethod("register")
                    .invoke(null);
                InventoryTodo.LOG.info("NEI found: using NEI integration");
                return;
            } catch (Throwable t) {
                InventoryTodo.LOG.error("NEI integration failed, falling back to standalone mode", t);
                Compat.reset();
            }
        }
        VanillaFrontend.register();
        InventoryTodo.LOG.info("Running in standalone mode (no recipe viewer)");
    }
}
