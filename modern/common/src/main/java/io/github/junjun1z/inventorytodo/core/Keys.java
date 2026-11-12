package io.github.junjun1z.inventorytodo.core;

/**
 * Key codes used by the panel. Defaults are GLFW codes (Minecraft up to 1.21.x); Minecraft 26.x uses SDL key codes,
 * so its adapter overwrites these values at start-up.
 */
public final class Keys {

    public static int ESCAPE = 256;
    public static int ENTER = 257;
    public static int BACKSPACE = 259;
    public static int DELETE = 261;
    public static int RIGHT = 262;
    public static int LEFT = 263;
    public static int HOME = 268;
    public static int END = 269;
    public static int KP_ENTER = 335;
    public static int A = 65;
    public static int C = 67;
    public static int V = 86;

    private Keys() {}
}
