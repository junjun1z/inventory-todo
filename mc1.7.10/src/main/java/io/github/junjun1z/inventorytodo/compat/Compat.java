package io.github.junjun1z.inventorytodo.compat;

/** Holds the active {@link Integration}. */
public final class Compat {

    private static Integration current = new VanillaIntegration();

    private Compat() {}

    public static Integration get() {
        return current;
    }

    public static void set(Integration integration) {
        current = integration;
    }

    public static void reset() {
        current = new VanillaIntegration();
    }
}
