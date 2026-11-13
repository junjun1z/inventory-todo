package io.github.junjun1z.inventorytodo.mc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;

import io.github.junjun1z.inventorytodo.core.Gfx;

/** {@link Gfx} for 1.9.4 (immediate-mode GL). Tooltips need the current screen. */
public class GfxImpl implements Gfx {

    private final GuiScreen screen;
    private final FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
    private float z;

    public GfxImpl(GuiScreen screen) {
        this.screen = screen;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        Gui.drawRect(x1, y1, x2, y2, argb);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    @Override
    public void text(String s, int x, int y, int argb, boolean shadow) {
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        font.drawString(s, x, y, argb, shadow);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    @Override
    public int textWidth(String s) {
        return font.getStringWidth(s);
    }

    @Override
    public String trim(String s, int width) {
        return font.trimStringToWidth(s, Math.max(0, width));
    }

    @Override
    public void item(Object stack, int x, int y) {
        ItemStack st = (ItemStack) stack;
        RenderItem ri = Minecraft.getMinecraft()
            .getRenderItem();
        float old = ri.zLevel;
        ri.zLevel = z + 100;
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        ri.renderItemAndEffectIntoGUI(st, x, y);
        ri.renderItemOverlays(font, st, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        ri.zLevel = old;
    }

    @Override
    public void raise() {
        z += 300;
        GlStateManager.translate(0, 0, 300);
    }

    @Override
    public void tooltip(List<String> lines, int mx, int my) {
        if (screen == null) return;
        GuiUtils.drawHoveringText(lines, mx, my, screen.width, screen.height, -1, font);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
    }

    @Override
    public void itemTooltip(Object stack, List<String> extra, int mx, int my) {
        if (screen == null) return;
        ItemStack st = (ItemStack) stack;
        Minecraft mc = Minecraft.getMinecraft();
        List<String> lines = new ArrayList<>(st.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
        for (int i = 0; i < lines.size(); i++) {
            lines.set(i, (i == 0 ? st.getRarity().rarityColor : TextFormatting.GRAY) + lines.get(i));
        }
        lines.addAll(extra);
        GuiUtils.drawHoveringText(lines, mx, my, screen.width, screen.height, -1, font);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
    }
}
