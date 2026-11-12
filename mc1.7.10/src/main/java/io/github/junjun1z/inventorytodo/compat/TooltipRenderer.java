package io.github.junjun1z.inventorytodo.compat;

import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** Vanilla-style tooltip drawing (GuiScreen's version is protected). */
final class TooltipRenderer extends Gui {

    static final TooltipRenderer INSTANCE = new TooltipRenderer();

    void draw(List<String> lines, int x, int y, int screenW, int screenH, FontRenderer font) {
        if (lines.isEmpty()) return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        int w = 0;
        for (String s : lines) w = Math.max(w, font.getStringWidth(s));
        int tx = x + 12, ty = y - 12;
        int h = 8;
        if (lines.size() > 1) h += 2 + (lines.size() - 1) * 10;
        if (tx + w > screenW) tx -= 28 + w;
        if (tx < 4) tx = 4;
        if (ty + h + 6 > screenH) ty = screenH - h - 6;
        if (ty < 4) ty = 4;
        float oldZ = zLevel;
        zLevel = 300f;
        int bg = 0xF0100010;
        drawGradientRect(tx - 3, ty - 4, tx + w + 3, ty - 3, bg, bg);
        drawGradientRect(tx - 3, ty + h + 3, tx + w + 3, ty + h + 4, bg, bg);
        drawGradientRect(tx - 3, ty - 3, tx + w + 3, ty + h + 3, bg, bg);
        drawGradientRect(tx - 4, ty - 3, tx - 3, ty + h + 3, bg, bg);
        drawGradientRect(tx + w + 3, ty - 3, tx + w + 4, ty + h + 3, bg, bg);
        int b1 = 0x505000FF;
        int b2 = (b1 & 0xFEFEFE) >> 1 | b1 & 0xFF000000;
        drawGradientRect(tx - 3, ty - 2, tx - 2, ty + h + 2, b1, b2);
        drawGradientRect(tx + w + 2, ty - 2, tx + w + 3, ty + h + 2, b1, b2);
        drawGradientRect(tx - 3, ty - 3, tx + w + 3, ty - 2, b1, b1);
        drawGradientRect(tx - 3, ty + h + 2, tx + w + 3, ty + h + 3, b2, b2);
        GL11.glPushMatrix();
        GL11.glTranslatef(0f, 0f, 300f);
        for (int i = 0; i < lines.size(); i++) {
            font.drawStringWithShadow(lines.get(i), tx, ty, 0xFFFFFFFF);
            if (i == 0) ty += 2;
            ty += 10;
        }
        GL11.glPopMatrix();
        zLevel = oldZ;
        GL11.glPopAttrib();
    }
}
