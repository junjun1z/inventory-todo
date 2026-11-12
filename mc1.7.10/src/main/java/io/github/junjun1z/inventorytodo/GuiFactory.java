package io.github.junjun1z.inventorytodo;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.config.ConfigElement;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.GuiConfig;

/** In-game settings screen (Mods > Inventory ToDo > Config). */
public class GuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft mc) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ConfigScreen.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class ConfigScreen extends GuiConfig {

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ConfigScreen(GuiScreen parent) {
            super(
                parent,
                new ConfigElement(TodoConfig.config.getCategory(TodoConfig.CATEGORY)).getChildElements(),
                InventoryTodo.MODID,
                false,
                false,
                StatCollector.translateToLocal("inventorytodo.config.title"));
        }
    }
}
