package io.github.junjun1z.inventorytodo.mc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.LevelResource;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.junjun1z.inventorytodo.core.Keys;
import io.github.junjun1z.inventorytodo.core.Log;
import io.github.junjun1z.inventorytodo.core.Mc;

/** {@link Mc} for Minecraft 26.x (unobfuscated, SDL input, server-side recipes). */
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
        // 26.x reports SDL key codes
        Keys.ESCAPE = InputConstants.KEY_ESCAPE;
        Keys.ENTER = InputConstants.KEY_RETURN;
        Keys.KP_ENTER = InputConstants.KEY_NUMPADENTER;
        Keys.BACKSPACE = InputConstants.KEY_BACKSPACE;
        Keys.DELETE = InputConstants.KEY_DELETE;
        Keys.LEFT = InputConstants.KEY_LEFT;
        Keys.RIGHT = InputConstants.KEY_RIGHT;
        Keys.HOME = InputConstants.KEY_HOME;
        Keys.END = InputConstants.KEY_END;
        Keys.A = InputConstants.KEY_A;
        Keys.C = InputConstants.KEY_C;
        Keys.V = InputConstants.KEY_V;
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
        String key = s(stack).getItem()
            .getDescriptionId();
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
        Identifier rl = Identifier.tryParse(id);
        if (rl == null) return null;
        return BuiltInRegistries.ITEM.getOptional(rl)
            .filter(i -> i != Items.AIR)
            .map(ItemStack::new)
            .orElse(null);
    }

    @Override
    public String serialize(Object stack) {
        if (mc().level == null) return null;
        return ItemStack.CODEC.encodeStart(
            mc().level.registryAccess()
                .createSerializationContext(NbtOps.INSTANCE),
            s(stack))
            .result()
            .map(Tag::toString)
            .orElse(null);
    }

    @Override
    public Object deserialize(String data) {
        try {
            CompoundTag tag = TagParser.parseCompoundFully(data);
            Identifier id = Identifier.tryParse(tag.getStringOr("id", ""));
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return null;
            if (mc().level == null) return null;
            return ItemStack.CODEC.parse(
                mc().level.registryAccess()
                    .createSerializationContext(NbtOps.INSTANCE),
                tag)
                .result()
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

    static List<Object> ingredients(List<Ingredient> list) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Ingredient ing : list) {
            ing.items()
                .findFirst()
                .ifPresent(holder -> counts.merge(holder.value(), 1, Integer::sum));
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
        List<Object> out = new ArrayList<>(mc().player.getInventory()
            .getNonEquipmentItems());
        out.add(mc().player.getOffhandItem());
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
        return mc().mouseHandler.isLeftPressed();
    }

    @Override
    public boolean isShiftDown() {
        return mc().hasShiftDown();
    }

    @Override
    public boolean isCtrlDown() {
        return mc().hasControlDown();
    }

    @Override
    public boolean isAltDown() {
        return mc().hasAltDown();
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

    // ------------------------------------------------------------------ small differences inside 26.x
    // 26.x is not obfuscated, so these names are the same on every loader.

    private static java.lang.reflect.Method guiScreen, hudIsHidden;
    private static java.lang.reflect.Field guiField, hudField, screenField, hideGuiField;

    /** Current screen: Minecraft.gui.screen() (26.2+) or Minecraft.screen (26.1) */
    public static Object currentScreen() {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (guiField == null && screenField == null) {
                try {
                    guiField = Minecraft.class.getField("gui");
                    guiScreen = guiField.getType().getMethod("screen");
                } catch (NoSuchFieldException | NoSuchMethodException e) {
                    guiField = null;
                    screenField = Minecraft.class.getField("screen");
                }
            }
            if (guiField != null) return guiScreen.invoke(guiField.get(mc));
            return screenField.get(mc);
        } catch (Exception e) {
            return null;
        }
    }

    /** F1 pressed: Minecraft.gui.hud.isHidden() (26.2+) or Options.hideGui (26.1) */
    public static boolean hudHidden() {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (hudField == null && hideGuiField == null) {
                try {
                    java.lang.reflect.Field g = Minecraft.class.getField("gui");
                    hudField = g.getType().getField("hud");
                    hudIsHidden = hudField.getType().getMethod("isHidden");
                    guiField = g;
                } catch (NoSuchFieldException | NoSuchMethodException e) {
                    hudField = null;
                    hideGuiField = mc.options.getClass().getField("hideGui");
                }
            }
            if (hudField != null) return (Boolean) hudIsHidden.invoke(hudField.get(Minecraft.class.getField("gui").get(mc)));
            return hideGuiField.getBoolean(mc.options);
        } catch (Exception e) {
            return false;
        }
    }
}
