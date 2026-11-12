package io.github.junjun1z.inventorytodo.compat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import io.github.junjun1z.inventorytodo.TodoPanel;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Standalone frontend (no NEI).
 * <p>
 * Input: Minecraft only reads GUI input in {@code runTick()}, right after the client tick START event. At that
 * point the mouse and keyboard event queues are drained here: events meant for the panel are consumed, all
 * others are handed to the screen exactly as {@code GuiScreen.handleInput()} would.
 * <p>
 * Rendering: drawn after the screen in {@link GuiScreenEvent.DrawScreenEvent.Post}.
 */
public final class VanillaFrontend {

    private final TodoPanel panel = TodoPanel.INSTANCE;
    /** A press went to the panel: swallow the following moves and the release too */
    private boolean captured;

    /** The registered instance (null when another frontend is used) */
    public static VanillaFrontend instance;

    private VanillaFrontend() {}

    public static void register() {
        VanillaFrontend f = new VanillaFrontend();
        instance = f;
        MinecraftForge.EVENT_BUS.register(f);
        FMLCommonHandler.instance()
            .bus()
            .register(f);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    private static GuiContainer activeGui() {
        Minecraft mc = mc();
        if (mc.thePlayer == null) return null;
        return mc.currentScreen instanceof GuiContainer ? (GuiContainer) mc.currentScreen : null;
    }

    // ------------------------------------------------------------------ input

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        GuiContainer gui = activeGui();
        if (gui == null) {
            captured = false;
            return;
        }
        if (event.phase == TickEvent.Phase.START) drainInput(gui);
        else panel.tick(gui);
    }

    private void drainInput(GuiContainer gui) {
        Minecraft mc = mc();
        if (Mouse.isCreated()) {
            while (mc.currentScreen == gui && Mouse.next()) {
                if (!handleMouse(gui)) gui.handleMouseInput();
            }
        }
        if (Keyboard.isCreated()) {
            while (mc.currentScreen == gui && Keyboard.next()) {
                if (!handleKey(gui)) gui.handleKeyboardInput();
            }
        }
        // Anything left (the screen changed) is processed by the new screen as usual.
    }

    private boolean handleMouse(GuiContainer gui) {
        Minecraft mc = mc();
        int mx = Mouse.getEventX() * gui.width / mc.displayWidth;
        int my = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;
        return dispatchMouse(
            gui,
            mx,
            my,
            Mouse.getEventButton(),
            Mouse.getEventButtonState(),
            Mouse.getEventDWheel(),
            Mouse.isButtonDown(0));
    }

    /**
     * Routes one mouse event to the panel.
     *
     * @param button   pressed/released button, or -1 for move/wheel events
     * @param leftDown whether the left button is held (for drag events)
     * @return true if the event was used by the panel (and must not reach the screen)
     */
    public boolean dispatchMouse(GuiContainer gui, int mx, int my, int button, boolean down, int wheel,
        boolean leftDown) {
        if (button >= 0) {
            if (down) {
                if (panel.mouseClicked(gui, mx, my, button)) {
                    captured = true;
                    return true;
                }
                return false;
            }
            panel.mouseUp(gui, mx, my, button);
            if (captured) {
                captured = false;
                return true;
            }
            return false;
        }
        if (wheel != 0) return panel.mouseScrolled(gui, mx, my, wheel > 0 ? 1 : -1);
        if (captured) {
            if (leftDown) panel.mouseDragged(gui, mx, my, 0);
            return true;
        }
        return false;
    }

    private boolean handleKey(GuiContainer gui) {
        return dispatchKey(gui, Keyboard.getEventKeyState(), Keyboard.getEventCharacter(), Keyboard.getEventKey());
    }

    /** Routes one keyboard event to the panel. @return true if used */
    public boolean dispatchKey(GuiContainer gui, boolean down, char c, int key) {
        if (!down) return panel.isTyping();
        return panel.keyTyped(gui, c, key);
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        captured = false;
        if (event.gui instanceof GuiContainer) panel.onGuiOpened((GuiContainer) event.gui);
    }

    // ------------------------------------------------------------------ rendering

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiContainer) || mc().thePlayer == null) return;
        GuiContainer gui = (GuiContainer) event.gui;
        int mx = event.mouseX, my = event.mouseY;
        panel.render(gui, mx, my);
        if (!panel.isOverPanel(gui, mx, my)) return;

        Minecraft mc = mc();
        ItemStack held = mc.thePlayer.inventory.getItemStack();
        if (held != null) {
            // keep the item on the cursor above the panel
            GL11.glPushMatrix();
            GL11.glTranslatef(0f, 0f, 200f);
            Compat.get()
                .drawItem(mx - 8, my - 8, held);
            GL11.glPopMatrix();
            return;
        }
        List<String> lines;
        FontRenderer font = mc.fontRenderer;
        ItemStack stack = panel.stackUnderMouse(gui, mx, my);
        if (stack != null) {
            lines = itemTooltip(stack);
            lines.addAll(panel.itemTooltipExtras(gui, mx, my));
            FontRenderer itemFont = stack.getItem()
                .getFontRenderer(stack);
            if (itemFont != null) font = itemFont;
        } else {
            lines = panel.panelTooltip(gui, mx, my);
        }
        TooltipRenderer.INSTANCE.draw(lines, mx, my, gui.width, gui.height, font);
    }

    @SuppressWarnings("unchecked")
    private static List<String> itemTooltip(ItemStack stack) {
        Minecraft mc = mc();
        List<String> out = new ArrayList<>();
        try {
            List<String> raw = stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
            for (int i = 0; i < raw.size(); i++) {
                out.add(i == 0 ? stack.getRarity().rarityColor + raw.get(i) : EnumChatFormatting.GRAY + raw.get(i));
            }
        } catch (Throwable t) {
            out.add(stack.getDisplayName());
        }
        return out;
    }

}
