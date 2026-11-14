package io.github.junjun1z.inventorytodo.mc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import io.github.junjun1z.inventorytodo.core.Gfx;
import io.github.junjun1z.inventorytodo.core.Log;

/** {@link Gfx} on top of GuiGraphicsExtractor (26.x: deferred GUI rendering with strata). */
public class GfxImpl implements Gfx {

    private final GuiGraphicsExtractor g;
    private final Font font = Minecraft.getInstance().font;

    public GfxImpl(GuiGraphicsExtractor g) {
        this.g = g;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        g.fill(x1, y1, x2, y2, argb);
    }

    @Override
    public void text(String s, int x, int y, int argb, boolean shadow) {
        g.text(font, s, x, y, argb, shadow);
    }

    @Override
    public int textWidth(String s) {
        return font.width(s);
    }

    @Override
    public String trim(String s, int width) {
        return font.plainSubstrByWidth(s, Math.max(0, width));
    }

    @Override
    public void item(Object stack, int x, int y) {
        ItemStack st = (ItemStack) stack;
        g.item(st, x, y);
        g.itemDecorations(font, st, x, y);
    }

    @Override
    public void raise() {
        g.nextStratum();
    }

    // setTooltipForNextFrame() is only drawn by the screen's own render pass, and the loaders' "after render"
    // events (Fabric afterRender/afterExtract, NeoForge/Forge ScreenEvent.Render.Post) come after it: a tooltip
    // set there was silently dropped. Draw it right away on a new stratum instead (what the deferred pass does).
    /** @return false if the tooltip could not be drawn directly (the caller then falls back) */
    private boolean show(List<ClientTooltipComponent> lines, int mx, int my) {
        if (lines.isEmpty()) return true;
        Method m = tooltipMethod();
        if (m == null) return false;
        try {
            g.nextStratum();
            if (m.getParameterCount() == 7) m.invoke(g, font, lines, mx, my, DefaultTooltipPositioner.INSTANCE, null, false);
            else m.invoke(g, font, lines, mx, my, DefaultTooltipPositioner.INSTANCE, null);
            return true;
        } catch (Throwable t) {
            disable("drawing failed: " + t);
            return false;
        }
    }

    private static Method tooltip;
    private static boolean tooltipBroken;

    private static void disable(String why) {
        if (!tooltipBroken) Log.warn("Panel tooltips: direct drawing disabled (" + why + "), using the deferred tooltip instead");
        tooltipBroken = true;
    }

    /** GuiGraphicsExtractor.tooltip(Font, List, x, y, positioner, style[, extraSpaceAfterFirstLine]): 26.3 added the flag. Null if unusable. */
    private static Method tooltipMethod() {
        if (tooltipBroken) return null;
        if (tooltip == null) {
            try {
                for (Method m : GuiGraphicsExtractor.class.getMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (m.getName().equals("tooltip") && (p.length == 6 || p.length == 7) && p[0] == Font.class && p[1] == List.class) tooltip = m;
                }
            } catch (Throwable t) {
                tooltip = null;
            }
            if (tooltip == null) disable("GuiGraphicsExtractor.tooltip not found");
        }
        return tooltip;
    }

    private static List<ClientTooltipComponent> components(List<Component> lines) {
        List<ClientTooltipComponent> out = new ArrayList<>();
        for (Component l : lines) out.add(ClientTooltipComponent.create(l.getVisualOrderText()));
        return out;
    }

    @Override
    public void tooltip(List<String> lines, int mx, int my) {
        List<Component> c = new ArrayList<>();
        for (String l : lines) c.add(Component.literal(l));
        if (!show(components(c), mx, my)) g.setComponentTooltipForNextFrame(font, c, mx, my);
    }

    @Override
    public void itemTooltip(Object stack, List<String> extra, int mx, int my) {
        ItemStack st = (ItemStack) stack;
        List<Component> c = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), st));
        for (String l : extra) c.add(Component.literal(l));
        List<ClientTooltipComponent> out = components(c);
        st.getTooltipImage().ifPresent(img -> out.add(out.isEmpty() ? 0 : 1, ClientTooltipComponent.create(img)));
        if (!show(out, mx, my)) g.setTooltipForNextFrame(font, c, st.getTooltipImage(), mx, my);
    }
}
