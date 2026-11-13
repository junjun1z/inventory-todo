package io.github.junjun1z.inventorytodo.mc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import io.github.junjun1z.inventorytodo.core.Gfx;

/** {@link Gfx} on top of GuiGraphics (1.21.6+: deferred GUI rendering with strata). */
public class GfxImpl implements Gfx {

    private final GuiGraphics g;
    private final Font font = Minecraft.getInstance().font;

    public GfxImpl(GuiGraphics g) {
        this.g = g;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        g.fill(x1, y1, x2, y2, argb);
    }

    @Override
    public void text(String s, int x, int y, int argb, boolean shadow) {
        g.drawString(font, s, x, y, argb, shadow);
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
        g.renderItem(st, x, y);
        g.renderItemDecorations(font, st, x, y);
    }

    @Override
    public void raise() {
        g.nextStratum();
    }

    @Override
    public void tooltip(List<String> lines, int mx, int my) {
        List<Component> c = new ArrayList<>();
        for (String l : lines) c.add(Component.literal(l));
        g.setComponentTooltipForNextFrame(font, c, mx, my);
    }

    @Override
    public void itemTooltip(Object stack, List<String> extra, int mx, int my) {
        ItemStack st = (ItemStack) stack;
        List<Component> c = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), st));
        for (String l : extra) c.add(Component.literal(l));
        g.setTooltipForNextFrame(font, c, st.getTooltipImage(), mx, my);
    }
}
