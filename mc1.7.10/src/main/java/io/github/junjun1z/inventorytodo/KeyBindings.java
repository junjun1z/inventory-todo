package io.github.junjun1z.inventorytodo;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;

/** Key bindings (unbound by default so they never clash with other mods; set them in Controls). */
public final class KeyBindings {

    private KeyBindings() {}

    public static final KeyBinding TOGGLE_HUD = new KeyBinding(
        "inventorytodo.key.togglehud",
        Keyboard.KEY_NONE,
        "inventorytodo.key.category");
    public static final KeyBinding ADD_HOVERED = new KeyBinding(
        "inventorytodo.key.addhovered",
        Keyboard.KEY_NONE,
        "inventorytodo.key.category");

    public static void register() {
        ClientRegistry.registerKeyBinding(TOGGLE_HUD);
        ClientRegistry.registerKeyBinding(ADD_HOVERED);
    }
}
