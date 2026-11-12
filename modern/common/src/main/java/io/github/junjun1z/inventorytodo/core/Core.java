package io.github.junjun1z.inventorytodo.core;

/** Global state shared by the panel, the HUD and the loader glue code. */
public final class Core {

    public static final String MODID = "inventorytodo";

    private static Mc mc;
    private static Settings settings;
    private static final TodoStore STORE = new TodoStore();

    private Core() {}

    /** Called once by the loader entry point */
    public static void init(Mc impl) {
        mc = impl;
        Log.info("Inventory ToDo initialised" + (RecipeViewer.any() ? " (recipe viewers: " + viewerNames() + ")" : ""));
        startSelfTestIfRequested();
    }

    /** Development only: runs once per client tick when a self-test is active (never set in normal use) */
    public static Runnable devTick;

    /** Called by the loader glue at the end of every client tick */
    public static void clientTick() {
        if (devTick != null) devTick.run();
    }

    /**
     * Development builds (built with -Pselftest) contain an automated test; it starts only when
     * config/inventorytodo/selftest.flag exists. Release jars don't contain the test class at all.
     */
    private static void startSelfTestIfRequested() {
        try {
            if (!java.nio.file.Files.exists(mc.configDir()
                .resolve("selftest.flag"))) return;
            Class.forName("io.github.junjun1z.inventorytodo.devtest.SelfTest")
                .getMethod("start")
                .invoke(null);
        } catch (ClassNotFoundException e) {
            // release build: no self-test
        } catch (Throwable t) {
            Log.warn("Self-test could not start: " + t);
        }
    }

    private static String viewerNames() {
        StringBuilder sb = new StringBuilder();
        for (RecipeViewer v : RecipeViewer.ALL) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(v.name());
        }
        return sb.toString();
    }

    public static Mc mc() {
        return mc;
    }

    public static Settings settings() {
        if (settings == null) settings = Settings.load();
        return settings;
    }

    public static TodoStore store() {
        return STORE;
    }

    /** Recounts every item row (inventory + optionally the open container) */
    public static void recount(Object screen) {
        if (mc == null || !mc.hasPlayer()) return;
        STORE.ensureLoaded();
        java.util.List<Object> stacks = new java.util.ArrayList<>(mc.playerStacks());
        if (screen != null && settings().countOpenContainer && mc.isContainerScreen(screen)) {
            stacks.addAll(mc.containerStacks(screen));
        }
        boolean ignore = settings().ignoreDataWhenTargetHasNone;
        for (TodoEntry e : STORE.entries) {
            if (e.item == null) continue;
            int n = 0;
            for (Object s : stacks) {
                if (!mc.isEmpty(s) && mc.countsAs(e.item, s, ignore)) n += mc.count(s);
            }
            e.have = n;
        }
    }
}
