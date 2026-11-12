package io.github.junjun1z.inventorytodo;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import io.github.junjun1z.inventorytodo.compat.Compat;

/** Shows unfinished rows on the screen while playing (no screen open). */
public final class TodoHud {

    private static final int ROW_H = 10;
    private static final int MAX_W = 170;
    private int ticks;

    private TodoHud() {}

    public static void register() {
        TodoHud hud = new TodoHud();
        MinecraftForge.EVENT_BUS.register(hud);
        FMLCommonHandler.instance()
            .bus()
            .register(hud);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        while (KeyBindings.TOGGLE_HUD.isPressed()) TodoConfig.setHudEnabled(!TodoConfig.hudEnabled);
        if (TodoConfig.hudEnabled && mc.currentScreen == null && ++ticks % 10 == 0) {
            TodoPanel.INSTANCE.refreshForHud();
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!TodoConfig.hudEnabled || mc.thePlayer == null
            || mc.currentScreen != null
            || mc.gameSettings.hideGUI
            || mc.gameSettings.showDebugInfo) return;
        List<TodoEntry> all = TodoPanel.INSTANCE.entriesForHud();
        List<TodoEntry> open = new ArrayList<>();
        for (TodoEntry e : all) if (!e.isDone()) open.add(e);
        if (open.isEmpty()) return;

        FontRenderer fr = mc.fontRenderer;
        int rows = Math.min(open.size(), TodoConfig.hudMaxRows);
        String title = StatCollector.translateToLocal(
            "inventorytodo.hud.title") + " " + EnumChatFormatting.GRAY + (all.size() - open.size()) + "/" + all.size();
        int w = fr.getStringWidth(title);
        List<String> texts = new ArrayList<>();
        List<String> counters = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            TodoEntry e = open.get(i);
            String c = e.item == null ? "" : (e.target > 0 ? e.have + "/" + e.target : String.valueOf(e.have));
            String t = e.displayText();
            int iconW = e.item != null ? 10 : 0;
            int cw = c.isEmpty() ? 0 : fr.getStringWidth(c) + 6;
            int maxText = MAX_W - iconW - cw;
            if (fr.getStringWidth(t) > maxText) t = fr.trimStringToWidth(t, maxText - fr.getStringWidth("...")) + "...";
            texts.add(t);
            counters.add(c);
            w = Math.max(w, iconW + fr.getStringWidth(t) + cw);
        }
        boolean more = open.size() > rows;
        String moreText = more ? "+" + (open.size() - rows) : null;
        int h = ROW_H + rows * ROW_H + (more ? ROW_H : 0) + 2;
        w += 6;

        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        String anchor = TodoConfig.hudAnchor;
        boolean right = anchor.endsWith("RIGHT");
        boolean bottom = anchor.startsWith("BOTTOM");
        int x = right ? sr.getScaledWidth() - w - TodoConfig.hudOffsetX : TodoConfig.hudOffsetX;
        int y = bottom ? sr.getScaledHeight() - h - TodoConfig.hudOffsetY : TodoConfig.hudOffsetY;
        x = Math.max(0, x);
        y = Math.max(0, y);

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            Gui.drawRect(x, y, x + w, y + h, 0x90000000);
            fr.drawStringWithShadow(title, x + 3, y + 2, 0xFFFFFF);
            for (int i = 0; i < rows; i++) {
                TodoEntry e = open.get(i);
                int ry = y + ROW_H + 1 + i * ROW_H;
                int tx = x + 3;
                if (e.item != null) {
                    GL11.glPushMatrix();
                    GL11.glTranslatef(tx, ry, 0);
                    GL11.glScalef(0.5f, 0.5f, 1f);
                    try {
                        Compat.get()
                            .drawItem(0, 0, TodoEntry.renderable(e.item));
                    } catch (Throwable ignored) {}
                    GL11.glPopMatrix();
                    GL11.glDisable(GL11.GL_LIGHTING);
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                    tx += 10;
                }
                fr.drawStringWithShadow(texts.get(i), tx, ry, 0xE0E0E0);
                String c = counters.get(i);
                if (!c.isEmpty()) {
                    int col = e.have > 0 ? 0xFFFF55 : 0xFF7777;
                    fr.drawStringWithShadow(c, x + w - 3 - fr.getStringWidth(c), ry, col);
                }
            }
            if (more) fr.drawStringWithShadow(moreText, x + 3, y + ROW_H + 1 + rows * ROW_H, 0x9090A0);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }
}
