package io.github.junjun1z.inventorytodo.mc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import io.github.junjun1z.inventorytodo.core.Core;
import io.github.junjun1z.inventorytodo.core.Log;
import io.github.junjun1z.inventorytodo.core.RecipeViewer;
import io.github.junjun1z.inventorytodo.core.TodoPanel;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;

/**
 * JEI integration. Only JEI ever loads this class (Forge/NeoForge: @JeiPlugin scan, Fabric: "jei_mod_plugin"
 * entrypoint), so nothing happens when JEI is not installed.
 */
@JeiPlugin
public class JeiCompat implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Core.MODID, "jei");
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // keep JEI's item list from drawing under the panel
        registration.addGenericGuiContainerHandler(AbstractContainerScreen.class, new IGuiContainerHandler<AbstractContainerScreen<?>>() {

            @Override
            public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> screen) {
                List<Rect2i> out = new ArrayList<>();
                int[] b = TodoPanel.INSTANCE.bounds(screen);
                if (b != null) out.add(new Rect2i(b[0], b[1], b[2], b[3]));
                return out;
            }
        });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        RecipeViewer.ALL.removeIf(v -> v instanceof Viewer);
        RecipeViewer.ALL.add(0, new Viewer(runtime));
        Log.info("JEI integration active");
    }

    private static final class Viewer implements RecipeViewer {

        private final IJeiRuntime rt;

        Viewer(IJeiRuntime rt) {
            this.rt = rt;
        }

        @Override
        public String name() {
            return "JEI";
        }

        @Override
        public Object hoveredStack() {
            ItemStack s = rt.getIngredientListOverlay()
                .getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
            if (s == null) s = rt.getBookmarkOverlay()
                .getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
            if (s == null) s = rt.getRecipesGui()
                .getIngredientUnderMouse(VanillaTypes.ITEM_STACK)
                .orElse(null);
            return s == null || s.isEmpty() ? null : s;
        }

        @Override
        public boolean show(Object stack, boolean usages) {
            rt.getRecipesGui()
                .show(
                    rt.getJeiHelpers()
                        .getFocusFactory()
                        .createFocus(usages ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, (ItemStack) stack));
            return true;
        }
    }
}
