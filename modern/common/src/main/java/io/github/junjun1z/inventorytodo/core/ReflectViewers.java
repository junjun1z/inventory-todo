package io.github.junjun1z.inventorytodo.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

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

    /** A non-empty item stack (class names differ between mappings, so compare with a stack we made ourselves) */
    static boolean isItemStack(Object o) {
        if (o == null) return false;
        Object icon = Core.mc()
            .iconStack();
        return icon != null && icon.getClass()
            .isInstance(o) && !Core.mc()
                .isEmpty(o);
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

    // ------------------------------------------------------------------ drag & drop (EMI / REI plugins)
    // EmiEntry / ReiEntry are the plugin classes the viewers load themselves; they hand their registries to the
    // methods below. Everything here is reflection + java.lang.reflect.Proxy: no compile-time dependency.

    /** Called by {@link EmiEntry} (EmiPlugin.register) */
    static void registerEmi(Object registry) {
        try {
            Method addDrop = find(registry.getClass(), "addGenericDragDropHandler", 1);
            Class<?> dropType = addDrop.getParameterTypes()[0];
            addDrop.invoke(registry, proxy(dropType, (self, m, a) -> {
                if (m.getName()
                    .equals("dropStack") && a != null && a.length == 4) {
                    Object item = emiItem(a[1]);
                    return item != null
                        && TodoPanel.INSTANCE.dropStack(a[0], ((Number) a[2]).doubleValue(), ((Number) a[3]).doubleValue(), item);
                }
                return invokeDefault(self, m, a); // render(): nothing extra to draw
            }));
            Log.info("EMI drag & drop target registered");
        } catch (Throwable t) {
            Log.warn("EMI drag & drop disabled: " + t);
        }
        try {
            // keep EMI's panels (favorites, item list) from drawing under the panel, like the JEI exclusion area
            Method addArea = find(registry.getClass(), "addGenericExclusionArea", 1);
            Class<?> areaType = addArea.getParameterTypes()[0];
            Constructor<?> bounds = Class.forName("dev.emi.emi.api.widget.Bounds", false, areaType.getClassLoader())
                .getConstructor(int.class, int.class, int.class, int.class);
            addArea.invoke(registry, proxy(areaType, (self, m, a) -> {
                if (!m.getName()
                    .equals("addExclusionArea") || a == null || a.length != 2) return invokeDefault(self, m, a);
                int[] b = TodoPanel.INSTANCE.bounds(a[0]);
                if (b != null) {
                    @SuppressWarnings("unchecked")
                    Consumer<Object> out = (Consumer<Object>) a[1];
                    out.accept(bounds.newInstance(b[0], b[1], b[2], b[3]));
                }
                return null;
            }));
        } catch (Throwable t) {
            Log.warn("EMI exclusion area disabled: " + t);
        }
    }

    /** First item stack of an EmiIngredient, or null */
    private static Object emiItem(Object ingredient) {
        try {
            List<?> stacks = (List<?>) ingredient.getClass()
                .getMethod("getEmiStacks")
                .invoke(ingredient);
            if (stacks == null || stacks.isEmpty()) return null;
            Object st = stacks.get(0);
            Object item = st.getClass()
                .getMethod("getItemStack")
                .invoke(st);
            return isItemStack(item) ? item : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Called by {@link ReiEntry} (REIClientPlugin.registerScreens) */
    static void registerRei(Object screenRegistry) {
        try {
            Method register = null;
            for (Method m : screenRegistry.getClass()
                .getMethods()) {
                if (m.getName()
                    .equals("registerDraggableStackVisitor") && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].getName()
                        .endsWith(".DraggableStackVisitor"))
                    register = m;
            }
            if (register == null) throw new IllegalStateException("no registerDraggableStackVisitor");
            Class<?> visitorType = register.getParameterTypes()[0];
            ClassLoader cl = visitorType.getClassLoader();
            Class<?> rect = Class.forName("me.shedaniel.math.Rectangle", false, cl);
            Constructor<?> newRect = rect.getConstructor(int.class, int.class, int.class, int.class);
            Method ofRect = Class.forName("me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor$BoundsProvider", false, cl)
                .getMethod("ofRectangle", rect);
            register.invoke(screenRegistry, proxy(visitorType, (self, m, a) -> {
                switch (m.getName()) {
                    case "isHandingScreen":
                        return TodoPanel.INSTANCE.bounds(a[0]) != null;
                    case "acceptDraggedStack": // REI 7+: DraggedAcceptorResult, REI 6: boolean
                    case "acceptDraggedStackWithResult":
                    case "acceptDragged": { // REI 26+: any draggable component
                        boolean ok = reiDrop(a[0], a[1]);
                        Class<?> r = m.getReturnType();
                        if (r == boolean.class) return ok;
                        if (r.isEnum()) return enumConstant(r, ok ? "CONSUMED" : "PASS");
                        return invokeDefault(self, m, a);
                    }
                    case "getDraggableAcceptingBounds": {
                        int[] b = TodoPanel.INSTANCE.bounds(noArg(a[0], "getScreen"));
                        if (b == null) return Stream.empty();
                        return Stream.of(ofRect.invoke(null, newRect.newInstance(b[0], b[1], b[2], b[3])));
                    }
                    case "getPriority":
                        return 0.0;
                    case "compareTo":
                        return Double.compare(0.0, ((Number) noArg(a[0], "getPriority")).doubleValue());
                    default:
                        return invokeDefault(self, m, a);
                }
            }));
            Log.info("REI drag & drop target registered");
        } catch (Throwable t) {
            Log.warn("REI drag & drop disabled: " + t);
        }
    }

    /** Called by {@link ReiEntry} (REIClientPlugin.registerExclusionZones) */
    static void registerReiExclusion(Object zones) {
        try {
            Method register = find(zones.getClass(), "register", 2);
            Class<?> providerType = register.getParameterTypes()[1];
            Constructor<?> newRect = Class.forName("me.shedaniel.math.Rectangle", false, providerType.getClassLoader())
                .getConstructor(int.class, int.class, int.class, int.class);
            // REI matches the class with isAssignableFrom: Object.class = every screen (bounds() is null elsewhere)
            register.invoke(zones, Object.class, proxy(providerType, (self, m, a) -> {
                if (!m.getName()
                    .equals("provide")) return invokeDefault(self, m, a);
                int[] b = TodoPanel.INSTANCE.bounds(a[0]);
                Collection<Object> out = b == null ? Collections.emptyList()
                    : Collections.singletonList(newRect.newInstance(b[0], b[1], b[2], b[3]));
                return out;
            }));
        } catch (Throwable t) {
            Log.warn("REI exclusion zone disabled: " + t);
        }
    }

    /** Adds the item of a dragged REI stack/component at the drop position */
    private static boolean reiDrop(Object context, Object dragged) {
        try {
            Object entry = noArg(dragged, "getStack");
            if (entry == null) entry = noArg(dragged, "get");
            if (entry == null) return false;
            Object item = noArg(entry, "getValue");
            if (!isItemStack(item)) return false;
            Object screen = noArg(context, "getScreen");
            Object p = noArg(context, "getCurrentPosition");
            return TodoPanel.INSTANCE.dropStack(screen, coord(p, "x"), coord(p, "y"), item);
        } catch (Throwable t) {
            Log.debug("REI drop failed: " + t);
            return false;
        }
    }

    private static int coord(Object point, String axis) throws Exception {
        try {
            Field f = point.getClass()
                .getField(axis);
            return ((Number) f.get(point)).intValue();
        } catch (NoSuchFieldException e) {
            return ((Number) point.getClass()
                .getMethod(axis.equals("x") ? "getX" : "getY")
                .invoke(point)).intValue();
        }
    }

    /** Calls a public no-argument method, or returns null if that fails */
    private static Object noArg(Object o, String name) {
        if (o == null) return null;
        try {
            Method m = o.getClass()
                .getMethod(name);
            m.setAccessible(true);
            return m.invoke(o);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object enumConstant(Class<?> type, String name) {
        return Enum.valueOf((Class) type, name);
    }

    private interface Handler {

        Object call(Object self, Method m, Object[] args) throws Throwable;
    }

    /** Implements a viewer interface; Object methods are answered here */
    private static Object proxy(Class<?> type, Handler h) {
        InvocationHandler ih = (self, m, a) -> {
            if (m.getDeclaringClass() == Object.class) {
                switch (m.getName()) {
                    case "equals":
                        return self == a[0];
                    case "hashCode":
                        return System.identityHashCode(self);
                    default:
                        return "InventoryToDo$" + type.getSimpleName();
                }
            }
            return h.call(self, m, a);
        };
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, ih);
    }

    /**
     * Runs an interface default method (InvocationHandler.invokeDefault, Java 16+, called reflectively so the Java 8
     * targets still compile). Abstract methods we do not know answer false / 0 / null.
     */
    private static Object invokeDefault(Object self, Method m, Object[] a) throws Throwable {
        if (!m.isDefault()) {
            Class<?> r = m.getReturnType();
            if (r == boolean.class) return false;
            if (r == int.class) return 0;
            if (r == double.class) return 0.0;
            return null;
        }
        try {
            return InvocationHandler.class.getMethod("invokeDefault", Object.class, Method.class, Object[].class)
                .invoke(null, self, m, a);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
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
                return isItemStack(item) ? item : null;
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
                return isItemStack(value) ? value : null;
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
