package io.github.junjun1z.inventorytodo.compat;

import java.lang.reflect.Field;

import net.minecraft.client.gui.inventory.GuiContainer;

import io.github.junjun1z.inventorytodo.InventoryTodo;

import cpw.mods.fml.relauncher.ReflectionHelper;

/** Access to protected GuiContainer fields. */
public final class GuiUtil {

    private static Field fGuiLeft, fGuiTop, fXSize, fYSize;

    static {
        try {
            fGuiLeft = ReflectionHelper.findField(GuiContainer.class, "guiLeft", "field_147003_i");
            fGuiTop = ReflectionHelper.findField(GuiContainer.class, "guiTop", "field_147009_r");
            fXSize = ReflectionHelper.findField(GuiContainer.class, "xSize", "field_146999_f");
            fYSize = ReflectionHelper.findField(GuiContainer.class, "ySize", "field_147000_g");
        } catch (Throwable t) {
            InventoryTodo.LOG.warn("Could not access GuiContainer fields; positions will be estimated", t);
        }
    }

    private GuiUtil() {}

    private static int get(Field f, GuiContainer g, int fallback) {
        try {
            if (f != null) return f.getInt(g);
        } catch (Exception ignored) {}
        return fallback;
    }

    public static int guiLeft(GuiContainer g) {
        return get(fGuiLeft, g, (g.width - xSize(g)) / 2);
    }

    public static int guiTop(GuiContainer g) {
        return get(fGuiTop, g, (g.height - ySize(g)) / 2);
    }

    public static int xSize(GuiContainer g) {
        return get(fXSize, g, 176);
    }

    public static int ySize(GuiContainer g) {
        return get(fYSize, g, 166);
    }
}
