package io.github.junjun1z.inventorytodo.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import io.github.junjun1z.inventorytodo.core.Core;
import io.github.junjun1z.inventorytodo.core.Hud;
import io.github.junjun1z.inventorytodo.core.TodoPanel;
import io.github.junjun1z.inventorytodo.mc.GfxImpl;
import io.github.junjun1z.inventorytodo.mc.McImpl;

import com.mojang.blaze3d.vertex.PoseStack;

/** Called from the mixins. Each input hook returns true when the panel consumed the event. */
public final class FabricHooks {

    private static final TodoPanel PANEL = TodoPanel.INSTANCE;

    private FabricHooks() {}

    private static Screen containerScreen() {
        Screen s = Minecraft.getInstance().screen;
        return s instanceof AbstractContainerScreen ? s : null;
    }

    private static McImpl m() {
        return (McImpl) Core.mc();
    }

    public static boolean mouseClicked(Screen s, double x, double y, int button) {
        return PANEL.mouseClicked(s, x, y, button);
    }

    public static boolean mouseReleased(Screen s, double x, double y, int button) {
        return PANEL.mouseReleased(s, x, y, button);
    }

    public static boolean keyPressed(Screen s, int key, int scan) {
        if (PANEL.keyPressed(s, key)) return true;
        return InventoryTodoFabric.addKey.matches(key, scan) && PANEL.addHovered(s, (int) m().mouseX(), (int) m().mouseY());
    }

    public static boolean charTyped(Screen s, char c) {
        return PANEL.isTyping() && PANEL.charTyped(s, c);
    }

    /** Raw mouse button from MouseHandler (action 1 = press, 0 = release) */
    public static boolean onMouseButton(int button, int action) {
        Screen s = containerScreen();
        if (s == null) return false;
        double x = m().mouseX(), y = m().mouseY();
        return action == 1 ? mouseClicked(s, x, y, button) : action == 0 && mouseReleased(s, x, y, button);
    }

    public static boolean onScroll(double vertical) {
        Screen s = containerScreen();
        if (s == null || vertical == 0) return false;
        return PANEL.mouseScrolled(s, m().mouseX(), m().mouseY(), vertical > 0 ? 1 : -1);
    }

    /** Raw key from KeyboardHandler (action 1 = press, 2 = repeat) */
    public static boolean onKey(int key, int scan, int action) {
        Screen s = containerScreen();
        return s != null && action != 0 && keyPressed(s, key, scan);
    }

    public static void onScreenInit(Screen s) {
        PANEL.onScreenOpened(s);
    }

    public static void onSetScreen(Screen next) {
        if (!(next instanceof AbstractContainerScreen)) PANEL.onScreenClosed();
    }

    public static void afterScreenRender() {
        Screen s = containerScreen();
        if (s == null) return;
        PoseStack pose = new PoseStack();
        PANEL.render(s, new GfxImpl(pose, s), (int) m().mouseX(), (int) m().mouseY());
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        Core.clientTick();
        while (InventoryTodoFabric.hudKey.consumeClick()) Hud.toggle();
        if (mc.player == null) return;
        if (mc.screen instanceof AbstractContainerScreen) PANEL.tick(mc.screen);
        else if (mc.screen == null) Hud.tick();
    }

    public static void renderHud(PoseStack pose) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui) return;
        pose.pushPose();
        Hud.render(new GfxImpl(pose, null));
        pose.popPose();
    }
}
