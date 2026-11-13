package io.github.junjun1z.inventorytodo.mc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.LevelResource;

import org.lwjgl.glfw.GLFW;

import io.github.junjun1z.inventorytodo.core.Log;
import io.github.junjun1z.inventorytodo.core.Mc;

/** {@link Mc} for Minecraft 1.21.2 - 1.21.4 (server-side recipes). */
public class McImpl implements Mc {

    private final Path configDir;
    private final ScreenAccess access;
    private final KeyMapping addKey, hudKey;
    private ClientLanguage english;
    private boolean englishFailed;

    public McImpl(Path configDir, ScreenAccess access, KeyMapping addKey, KeyMapping hudKey) {
        this.configDir = configDir;
        this.access = access;
        this.addKey = addKey;
        this.hudKey = hudKey;
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static ItemStack s(Object o) {
        return (ItemStack) o;
    }

    // ------------------------------------------------------------------ items

    @Override
    public boolean isEmpty(Object stack) {
        return stack == null || s(stack).isEmpty();
    }

    @Override
    public Object copyOne(Object stack) {
        ItemStack c = s(stack).copy();
        c.setCount(1);
        return c;
    }

    @Override
    public boolean sameItem(Object a, Object b) {
        return ItemStack.isSameItemSameComponents(s(a), s(b));
    }

    @Override
    public boolean countsAs(Object target, Object stack, boolean ignoreData) {
        ItemStack t = s(target), x = s(stack);
        if (t.getItem() != x.getItem()) return false;
        if (t.isDamageableItem()) return true; // any damage counts
        if (!t.getComponentsPatch()
            .isEmpty()) return ItemStack.isSameItemSameComponents(t, x);
        return ignoreData || x.getComponentsPatch()
            .isEmpty();
    }

    @Override
    public int count(Object stack) {
        return s(stack).getCount();
    }

    @Override
    public int maxStackSize(Object stack) {
        return s(stack).getMaxStackSize();
    }

    @Override
    public String displayName(Object stack) {
        return s(stack).getHoverName()
            .getString();
    }

    @Override
    public String englishName(Object stack) {
        if (english == null && !englishFailed) {
            try {
                english = ClientLanguage.loadFrom(mc().getResourceManager(), Collections.singletonList("en_us"), false);
            } catch (Throwable t) {
                englishFailed = true;
                Log.warn("English names unavailable: " + t);
            }
        }
        if (english == null) return null;
        String key = s(stack).getItem().getDescriptionId();
        String v = english.getOrDefault(key, key);
        return v.equals(key) ? null : v;
    }

    @Override
    public String registryId(Object stack) {
        return BuiltInRegistries.ITEM.getKey(s(stack).getItem())
            .toString();
    }

    @Override
    public List<Object> allItems() {
        List<Object> out = new ArrayList<>();
        Minecraft mc = mc();
        try {
            if (mc.player != null && mc.level != null) {
                CreativeModeTabs.tryRebuildTabContents(
                    mc.player.connection.enabledFeatures(),
                    true,
                    mc.level.registryAccess());
            }
            out.addAll(CreativeModeTabs.searchTab()
                .getDisplayItems());
        } catch (Throwable t) {
            Log.debug("creative search tab unavailable: " + t);
        }
        if (out.isEmpty()) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item != Items.AIR) out.add(new ItemStack(item));
            }
        }
        return out;
    }

    @Override
    public Object stackById(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        return BuiltInRegistries.ITEM.getOptional(rl)
            .filter(i -> i != Items.AIR)
            .map(ItemStack::new)
            .orElse(null);
    }

    @Override
    public String serialize(Object stack) {
        if (mc().level == null) return null;
        return s(stack).save(mc().level.registryAccess())
            .toString();
    }

    @Override
    public Object deserialize(String data) {
        try {
            CompoundTag tag = TagParser.parseTag(data);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return null;
            if (mc().level == null) return null;
            return ItemStack.parse(mc().level.registryAccess(), tag)
                .filter(st -> !st.isEmpty())
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Object iconStack() {
        return new ItemStack(Items.WRITABLE_BOOK);
    }

    @Override
    public Recipe recipeFor(Object stack) {
        // Since 1.21.2 clients no longer receive recipes; they can only be read in singleplayer
        MinecraftServer server = mc().getSingleplayerServer();
        if (server == null || mc().level == null) return null;
        ItemStack target = s(stack);
        var ctx = SlotDisplayContext.fromLevel(server.overworld());
        for (RecipeHolder<?> h : server.getRecipeManager()
            .getRecipes()) {
            if (!(h.value() instanceof CraftingRecipe r)) continue;
            for (RecipeDisplay d : r.display()) {
                ItemStack result = d.result()
                    .resolveForFirstStack(ctx);
                if (result.isEmpty() || result.getItem() != target.getItem()) continue;
                return new Recipe(ingredients(r.placementInfo()
                    .ingredients()), result.getCount());
            }
        }
        return new Recipe(Collections.emptyList(), 1);
    }

    @SuppressWarnings("unchecked")
    static List<Object> ingredients(List<Ingredient> list) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Ingredient ing : list) {
            // items() is a List in 1.21.2/1.21.3 and a Stream in 1.21.4
            Object items = ing.items();
            java.util.stream.Stream<?> st = items instanceof List<?> l ? l.stream() : (java.util.stream.Stream<?>) items;
            st.findFirst()
                .ifPresent(h -> counts.merge(((net.minecraft.core.Holder<Item>) h).value(), 1, Integer::sum));
        }
        List<Object> out = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : counts.entrySet()) out.add(new ItemStack(e.getKey(), e.getValue()));
        return out;
    }

    // ------------------------------------------------------------------ player / world

    @Override
    public boolean hasPlayer() {
        return mc().player != null;
    }

    @Override
    public List<Object> playerStacks() {
        Inventory inv = mc().player.getInventory();
        List<Object> out = new ArrayList<>(inv.items);
        out.addAll(inv.offhand);
        return out;
    }

    @Override
    public List<Object> containerStacks(Object screen) {
        List<Object> out = new ArrayList<>();
        if (!(screen instanceof AbstractContainerScreen<?> s)) return out;
        Inventory inv = mc().player.getInventory();
        for (Slot slot : s.getMenu().slots) {
            if (slot.container != inv && slot.hasItem()) out.add(slot.getItem());
        }
        return out;
    }

    @Override
    public Object carriedStack() {
        return mc().player == null ? ItemStack.EMPTY : mc().player.containerMenu.getCarried();
    }

    @Override
    public String worldKey() {
        Minecraft mc = mc();
        try {
            if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
                Path root = mc.getSingleplayerServer()
                    .getWorldPath(LevelResource.ROOT)
                    .toAbsolutePath()
                    .normalize();
                return "sp_" + sanitize(root.getFileName()
                    .toString());
            }
            ServerData sd = mc.getCurrentServer();
            if (sd != null && sd.ip != null) return "mp_" + sanitize(sd.ip);
        } catch (Throwable ignored) {}
        return "default";
    }

    static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9._\\-]", "_");
    }

    @Override
    public Path configDir() {
        return configDir;
    }

    // ------------------------------------------------------------------ screen

    @Override
    public boolean isContainerScreen(Object screen) {
        return screen instanceof AbstractContainerScreen;
    }

    @Override
    public int guiLeft(Object screen) {
        return access.left((AbstractContainerScreen<?>) screen);
    }

    @Override
    public int guiTop(Object screen) {
        return access.top((AbstractContainerScreen<?>) screen);
    }

    @Override
    public int imageWidth(Object screen) {
        return access.imageWidth((AbstractContainerScreen<?>) screen);
    }

    @Override
    public Object hoveredSlotStack(Object screen, int mx, int my) {
        Slot slot = access.hoveredSlot((AbstractContainerScreen<?>) screen);
        return slot != null && slot.hasItem() ? slot.getItem() : null;
    }

    @Override
    public int screenWidth() {
        return mc().getWindow()
            .getGuiScaledWidth();
    }

    @Override
    public int screenHeight() {
        return mc().getWindow()
            .getGuiScaledHeight();
    }

    @Override
    public double mouseX() {
        Minecraft mc = mc();
        return mc.mouseHandler.xpos() * mc.getWindow()
            .getGuiScaledWidth() / mc.getWindow()
                .getScreenWidth();
    }

    @Override
    public double mouseY() {
        Minecraft mc = mc();
        return mc.mouseHandler.ypos() * mc.getWindow()
            .getGuiScaledHeight() / mc.getWindow()
                .getScreenHeight();
    }

    @Override
    public boolean isLeftMouseDown() {
        return GLFW.glfwGetMouseButton(
            mc().getWindow()
                .getWindow(),
            GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean isShiftDown() {
        return Screen.hasShiftDown();
    }

    @Override
    public boolean isCtrlDown() {
        return Screen.hasControlDown();
    }

    @Override
    public boolean isAltDown() {
        return Screen.hasAltDown();
    }

    // ------------------------------------------------------------------ text

    @Override
    public String tr(String key, Object... args) {
        return I18n.get(key, args);
    }

    @Override
    public String clipboard() {
        return mc().keyboardHandler.getClipboard();
    }

    @Override
    public void setClipboard(String text) {
        mc().keyboardHandler.setClipboard(text);
    }

    @Override
    public String addKeyName() {
        return keyName(addKey);
    }

    @Override
    public String hudKeyName() {
        return keyName(hudKey);
    }

    private static String keyName(KeyMapping k) {
        if (k == null || k.isUnbound()) return null;
        return k.getTranslatedKeyMessage()
            .getString();
    }
}
