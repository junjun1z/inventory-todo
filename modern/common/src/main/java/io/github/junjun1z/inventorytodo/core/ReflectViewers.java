package io.github.junjun1z.inventorytodo.core;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * EMI and REI integration through reflection only: nothing of theirs is loaded unless they are installed, and no
 * compile-time dependency is needed. Works across Minecraft versions as long as their public API keeps the names.
 */
public final class ReflectViewers {

    private ReflectViewers() {}

    /** Registers the viewers whose classes are present */
    public static void registerAvailable() {
        if (present("dev.emi.emi.api.EmiApi")) {
            try {
                RecipeViewer.ALL.add(new Emi());
            } catch (Throwable t) {
                Log.warn("EMI integration disabled: " + t);
            }
        }
        if (present("me.shedaniel.rei.api.client.REIRuntime")) {
            try {
                RecipeViewer.ALL.add(new Rei());
            } catch (Throwable t) {
                Log.warn("REI integration disabled: " + t);
            }
        }
    }

    private static boolean present(String cls) {
        try {
            Class.forName(cls, false, ReflectViewers.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Method find(Class<?> c, String name, int params) {
        for (Method m : c.getMethods()) if (m.getName()
            .equals(name) && m.getParameterCount() == params) return m;
        throw new IllegalStateException("No method " + c.getName() + "." + name);
    }

    /** Static factory taking one argument of a type compatible with {@code arg} */
    private static Method factory(Class<?> c, String name, Object arg) {
        for (Method m : c.getMethods()) {
            if (!m.getName()
                .equals(name) || m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0].isInstance(arg)) return m;
        }
        throw new IllegalStateException("No factory " + c.getName() + "." + name + " for " + arg.getClass());
    }

    // ------------------------------------------------------------------ EMI

    static final class Emi implements RecipeViewer {

        private final Class<?> api = Class.forName("dev.emi.emi.api.EmiApi");
        private final Class<?> emiStack = Class.forName("dev.emi.emi.api.stack.EmiStack");
        private final Method getHovered = api.getMethod("getHoveredStack", boolean.class);
        private final Method displayRecipes = find(api, "displayRecipes", 1);
        private final Method displayUses = find(api, "displayUses", 1);

        Emi() throws Exception {}

        @Override
        public String name() {
            return "EMI";
        }

        @Override
        public Object hoveredStack() {
            try {
                Object interaction = getHovered.invoke(null, false);
                if (interaction == null) return null;
                Object ingredient = interaction.getClass()
                    .getMethod("getStack")
                    .invoke(interaction);
                if (ingredient == null) return null;
                List<?> stacks = (List<?>) ingredient.getClass()
                    .getMethod("getEmiStacks")
                    .invoke(ingredient);
                if (stacks == null || stacks.isEmpty()) return null;
                Object st = stacks.get(0);
                Object item = st.getClass()
                    .getMethod("getItemStack")
                    .invoke(st);
                return item == null || Core.mc()
                    .isEmpty(item) ? null : item;
            } catch (Throwable t) {
                return null;
            }
        }

        @Override
        public boolean show(Object stack, boolean usages) {
            try {
                Object es = factory(emiStack, "of", stack).invoke(null, stack);
                (usages ? displayUses : displayRecipes).invoke(null, es);
                return true;
            } catch (Throwable t) {
                Log.debug("EMI show failed: " + t);
                return false;
            }
        }
    }

    // ------------------------------------------------------------------ REI

    static final class Rei implements RecipeViewer {

        private final Class<?> runtime = Class.forName("me.shedaniel.rei.api.client.REIRuntime");
        private final Class<?> entryStacks = Class.forName("me.shedaniel.rei.api.common.util.EntryStacks");
        private final Class<?> viewSearch = Class.forName("me.shedaniel.rei.api.client.view.ViewSearchBuilder");

        Rei() throws Exception {}

        @Override
        public String name() {
            return "REI";
        }

        @Override
        public Object hoveredStack() {
            try {
                Object rt = runtime.getMethod("getInstance")
                    .invoke(null);
                Optional<?> overlay = (Optional<?>) rt.getClass()
                    .getMethod("getOverlay")
                    .invoke(rt);
                if (overlay == null || !overlay.isPresent()) return null;
                Object ov = overlay.get();
                Object list = ov.getClass()
                    .getMethod("getEntryList")
                    .invoke(ov);
                Object focused = list.getClass()
                    .getMethod("getFocusedStack")
                    .invoke(list);
                if (focused == null) {
                    Optional<?> fav = (Optional<?>) ov.getClass()
                        .getMethod("getFavoritesList")
                        .invoke(ov);
                    if (fav != null && fav.isPresent()) focused = fav.get()
                        .getClass()
                        .getMethod("getFocusedStack")
                        .invoke(fav.get());
                }
                if (focused == null) return null;
                Object value = focused.getClass()
                    .getMethod("getValue")
                    .invoke(focused);
                return value != null && value.getClass()
                    .getName()
                    .endsWith("ItemStack") && !Core.mc()
                        .isEmpty(value) ? value : null;
            } catch (Throwable t) {
                return null;
            }
        }

        @Override
        public boolean show(Object stack, boolean usages) {
            try {
                Object entry = factory(entryStacks, "of", stack).invoke(null, stack);
                Object builder = viewSearch.getMethod("builder")
                    .invoke(null);
                Method add = find(builder.getClass(), usages ? "addUsagesFor" : "addRecipesFor", 1);
                add.setAccessible(true);
                builder = add.invoke(builder, entry);
                Method open = builder.getClass()
                    .getMethod("open");
                open.setAccessible(true);
                return Boolean.TRUE.equals(open.invoke(builder)) || true;
            } catch (Throwable t) {
                Log.debug("REI show failed: " + t);
                return false;
            }
        }
    }
}
