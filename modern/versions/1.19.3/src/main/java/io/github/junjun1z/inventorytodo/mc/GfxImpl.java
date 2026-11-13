package io.github.junjun1z.inventorytodo.mc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.junjun1z.inventorytodo.core.Gfx;

/** {@link Gfx} for 1.19.x and older (PoseStack + GuiComponent). Tooltips need the current screen. */
public class GfxImpl implements Gfx {

    private final PoseStack pose;
    private final Screen screen;
    private final Font font = Minecraft.getInstance().font;
    private float z;

    public GfxImpl(PoseStack pose, Screen screen) {
        this.pose = pose;
        this.screen = screen;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        GuiComponent.fill(pose, x1, y1, x2, y2, argb);
    }

    @Override
    public void text(String s, int x, int y, int argb, boolean shadow) {
        if (shadow) font.drawShadow(pose, s, x, y, argb);
        else font.draw(pose, s, x, y, argb);
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
        ItemRenderer ir = Minecraft.getInstance().getItemRenderer();
        float old = ir.blitOffset;
        ir.blitOffset = z + 100;
        ir.renderAndDecorateItem(st, x, y);
        ir.renderGuiItemDecorations(font, st, x, y);
        ir.blitOffset = old;
    }

    @Override
    public void raise() {
        z += 300;
        pose.translate(0, 0, 300);
    }

    @Override
    public void tooltip(List<String> lines, int mx, int my) {
        if (screen == null) return;
        List<Component> c = new ArrayList<>();
        for (String l : lines) c.add(Component.literal(l));
        screen.renderComponentTooltip(pose, c, mx, my);
    }

    @Override
    public void itemTooltip(Object stack, List<String> extra, int mx, int my) {
        if (screen == null) return;
        ItemStack st = (ItemStack) stack;
        List<Component> c = new ArrayList<>(screen.getTooltipFromItem(st));
        for (String l : extra) c.add(Component.literal(l));
        screen.renderTooltip(pose, c, st.getTooltipImage(), mx, my);
    }
}
