package io.github.junjun1z.inventorytodo.mc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.platform.GlStateManager;

import io.github.junjun1z.inventorytodo.core.Gfx;

/** {@link Gfx} for 1.14.4 (GL matrix stack through GlStateManager). Tooltips need the current screen. */
public class GfxImpl implements Gfx {

    private final Screen screen;
    private final Font font = Minecraft.getInstance().font;
    private float z;

    public GfxImpl(Screen screen) {
        this.screen = screen;
    }

    /** Wraps a render pass in its own matrix (the panel translates along z) */
    public static void push() {
        GlStateManager.pushMatrix();
    }

    public static void pop() {
        GlStateManager.popMatrix();
        GlStateManager.color4f(1f, 1f, 1f, 1f);
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        GuiComponent.fill(x1, y1, x2, y2, argb);
        GlStateManager.color4f(1f, 1f, 1f, 1f);
    }

    @Override
    public void text(String s, int x, int y, int argb, boolean shadow) {
        if (shadow) font.drawShadow(s, x, y, argb);
        else font.draw(s, x, y, argb);
        GlStateManager.color4f(1f, 1f, 1f, 1f);
    }

    @Override
    public int textWidth(String s) {
        return font.width(s);
    }

    @Override
    public String trim(String s, int width) {
        return font.substrByWidth(s, Math.max(0, width));
    }

    @Override
    public void item(Object stack, int x, int y) {
        ItemStack st = (ItemStack) stack;
        ItemRenderer ir = Minecraft.getInstance().getItemRenderer();
        float old = ir.blitOffset;
        ir.blitOffset = z + 100;
        GlStateManager.enableDepthTest();
        com.mojang.blaze3d.platform.Lighting.turnOnGui();
        ir.renderAndDecorateItem(st, x, y);
        ir.renderGuiItemDecorations(font, st, x, y);
        ir.blitOffset = old;
        com.mojang.blaze3d.platform.Lighting.turnOff();
        GlStateManager.disableLighting();
        GlStateManager.color4f(1f, 1f, 1f, 1f);
    }

    @Override
    public void raise() {
        z += 300;
        GlStateManager.translatef(0, 0, 300);
    }

    @Override
    public void tooltip(List<String> lines, int mx, int my) {
        if (screen == null) return;
        screen.renderTooltip(lines, mx, my);
        GlStateManager.disableLighting();
        GlStateManager.color4f(1f, 1f, 1f, 1f);
    }

    @Override
    public void itemTooltip(Object stack, List<String> extra, int mx, int my) {
        if (screen == null) return;
        List<String> lines = new ArrayList<>(screen.getTooltipFromItem((ItemStack) stack));
        lines.addAll(extra);
        screen.renderTooltip(lines, mx, my);
        GlStateManager.disableLighting();
        GlStateManager.color4f(1f, 1f, 1f, 1f);
    }
}
