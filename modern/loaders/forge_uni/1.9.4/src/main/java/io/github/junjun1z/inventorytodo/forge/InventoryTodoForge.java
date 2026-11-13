package io.github.junjun1z.inventorytodo.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import io.github.junjun1z.inventorytodo.core.Core;
import io.github.junjun1z.inventorytodo.core.Hud;
import io.github.junjun1z.inventorytodo.core.Keys;
import io.github.junjun1z.inventorytodo.core.ReflectViewers;
import io.github.junjun1z.inventorytodo.core.TodoPanel;
import io.github.junjun1z.inventorytodo.mc.GfxImpl;
import io.github.junjun1z.inventorytodo.mc.McImpl;

/** Forge 1.9 - 1.9.4 entry point. Client only (not needed on servers). Uses Forge's GUI input events, no coremod. */
@Mod(modid = Core.MODID, name = "Inventory ToDo", useMetadata = true, clientSideOnly = true, acceptedMinecraftVersions = "[1.9,1.9.4]", acceptableRemoteVersions = "*")
public class InventoryTodoForge {

    static final KeyBinding HUD_KEY = new KeyBinding("inventorytodo.key.togglehud", 0, "inventorytodo.key.category");
    static final KeyBinding ADD_KEY = new KeyBinding("inventorytodo.key.addhovered", 0, "inventorytodo.key.category");
    private static final TodoPanel PANEL = TodoPanel.INSTANCE;
    /** A press went to the panel: swallow the following moves and the release too */
    private boolean captured;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(HUD_KEY);
        ClientRegistry.registerKeyBinding(ADD_KEY);
        ReflectViewers.registerAvailable();
        Core.init(new McImpl(Loader.instance()
            .getConfigDir()
            .toPath()
            .resolve(Core.MODID), ADD_KEY, HUD_KEY));
        MinecraftForge.EVENT_BUS.register(this);
        // tick events go through FML's bus (registering the same object twice is ignored)
        net.minecraftforge.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post e) {
        captured = false;
        if (e.getGui() instanceof GuiContainer) PANEL.onScreenOpened(e.getGui());
    }

    @SubscribeEvent
    public void onOpen(GuiOpenEvent e) {
        if (!(e.getGui() instanceof GuiContainer)) PANEL.onScreenClosed();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre e) {
        GuiScreen gui = e.getGui();
        if (!(gui instanceof GuiContainer)) return;
        int mx = Mouse.getEventX() * gui.width / mc().displayWidth;
        int my = gui.height - Mouse.getEventY() * gui.height / mc().displayHeight - 1;
        int button = Mouse.getEventButton();
        boolean used;
        if (button >= 0) {
            if (Mouse.getEventButtonState()) {
                used = PANEL.mouseClicked(gui, mx, my, button);
                if (used) captured = true;
            } else {
                used = PANEL.mouseReleased(gui, mx, my, button) || captured;
                captured = false;
            }
        } else if (Mouse.getEventDWheel() != 0) {
            used = PANEL.mouseScrolled(gui, mx, my, Mouse.getEventDWheel() > 0 ? 1 : -1);
        } else {
            used = captured;
        }
        if (used) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKey(GuiScreenEvent.KeyboardInputEvent.Pre e) {
        GuiScreen gui = e.getGui();
        if (!(gui instanceof GuiContainer)) return;
        if (!Keyboard.getEventKeyState()) {
            if (PANEL.isTyping()) e.setCanceled(true);
            return;
        }
        int key = Keyboard.getEventKey();
        char c = Keyboard.getEventCharacter();
        if (PANEL.isTyping()) {
            boolean special = key == Keys.ESCAPE || key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.BACKSPACE
                || key == Keys.DELETE || key == Keys.LEFT || key == Keys.RIGHT || key == Keys.HOME || key == Keys.END
                || (GuiScreen.isCtrlKeyDown() && (key == Keys.A || key == Keys.C || key == Keys.V));
            if (special) PANEL.keyPressed(gui, key);
            else if (c >= 32) PANEL.charTyped(gui, c);
            e.setCanceled(true);
            return;
        }
        if (PANEL.keyPressed(gui, key)) {
            e.setCanceled(true);
        } else if (ADD_KEY.getKeyCode() != 0 && ADD_KEY.getKeyCode() == key) {
            McImpl m = (McImpl) Core.mc();
            if (PANEL.addHovered(gui, (int) m.mouseX(), (int) m.mouseY())) e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post e) {
        if (!(e.getGui() instanceof GuiContainer)) return;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            PANEL.render(e.getGui(), new GfxImpl(e.getGui()), e.getMouseX(), e.getMouseY());
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Core.clientTick();
        while (HUD_KEY.isPressed()) Hud.toggle();
        Minecraft mc = mc();
        if (mc.thePlayer == null) return;
        if (mc.currentScreen instanceof GuiContainer) PANEL.tick(mc.currentScreen);
        else if (mc.currentScreen == null) Hud.tick();
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post e) {
        if (e.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = mc();
        if (mc.currentScreen != null || mc.gameSettings.hideGUI) return;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            Hud.render(new GfxImpl(null));
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }
}
