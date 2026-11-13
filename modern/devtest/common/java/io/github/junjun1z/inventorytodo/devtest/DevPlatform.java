package io.github.junjun1z.inventorytodo.devtest;

import java.nio.file.Path;

/** Version-specific helpers for the self-test (development only). */
public interface DevPlatform {

    boolean inWorld();

    /** Opens a singleplayer world (for versions without quick play). Default: nothing. */
    default void loadWorld(String folder) {}

    Object screen();

    void openInventory();

    void closeScreen();

    /** Empties the player's inventory (integrated server) */
    void clearInventory();

    /** Gives items through the integrated server */
    void give(String id, int count);

    void screenshot(String name);

    void stop();

    Path gameDir();

    /** Window position on the desktop (for java.awt.Robot) */
    int windowX();

    int windowY();

    String versionName();

    /** Daytime, clear weather (screenshots). Default: nothing. */
    default void prepareShowcase() {}

    /** Removes toasts and chat lines before a screenshot. Default: nothing. */
    default void clearOverlays() {}

    /** Brings the game window to the front (so real input reaches it) */
    void focusWindow();
}
