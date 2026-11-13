package io.github.junjun1z.inventorytodo.mc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import io.github.junjun1z.inventorytodo.core.Keys;
import io.github.junjun1z.inventorytodo.core.Log;
import io.github.junjun1z.inventorytodo.core.Mc;

/** {@link Mc} for Minecraft 1.11.2 (MCP names, LWJGL 2). */
public class McImpl implements Mc {

    private final Path configDir;
    private final KeyBinding addKey, hudKey;
    private net.minecraft.client.resources.Locale english;
    private boolean englishFailed;

    public McImpl(Path configDir, KeyBinding addKey, KeyBinding hudKey) {
        this.configDir = configDir;
        this.addKey = addKey;
        this.hudKey = hudKey;
        // LWJGL 2 key codes
        Keys.ESCAPE = Keyboard.KEY_ESCAPE;
        Keys.ENTER = Keyboard.KEY_RETURN;
        Keys.KP_ENTER = Keyboard.KEY_NUMPADENTER;
        Keys.BACKSPACE = Keyboard.KEY_BACK;
        Keys.DELETE = Keyboard.KEY_DELETE;
        Keys.LEFT = Keyboard.KEY_LEFT;
        Keys.RIGHT = Keyboard.KEY_RIGHT;
        Keys.HOME = Keyboard.KEY_HOME;
        Keys.END = Keyboard.KEY_END;
        Keys.A = Keyboard.KEY_A;
        Keys.C = Keyboard.KEY_C;
        Keys.V = Keyboard.KEY_V;
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
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
        return ItemStack.areItemsEqual(s(a), s(b)) && ItemStack.areItemStackTagsEqual(s(a), s(b));
    }

    @Override
    public boolean countsAs(Object target, Object stack, boolean ignoreData) {
        ItemStack t = s(target), x = s(stack);
        if (t.getItem() != x.getItem()) return false;
        boolean anyMeta = t.getMetadata() == 32767 || t.isItemStackDamageable();
        if (!anyMeta && t.getMetadata() != x.getMetadata()) return false;
        if (t.hasTagCompound()) return ItemStack.areItemStackTagsEqual(t, x);
        return ignoreData || !x.hasTagCompound();
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
        return s(stack).getDisplayName();
    }

    @Override
    public String englishName(Object stack) {
        if (english == null && !englishFailed) {
            try {
                net.minecraft.client.resources.Locale loc = new net.minecraft.client.resources.Locale();
                loc.loadLocaleDataFiles(mc().getResourceManager(), Collections.singletonList("en_us"));
                english = loc;
            } catch (Throwable t) {
                englishFailed = true;
                Log.warn("English names unavailable: " + t);
            }
        }
        if (english == null) return null;
        String key = s(stack).getUnlocalizedName() + ".name";
        String v = english.formatMessage(key, new Object[0]);
        return v == null || v.equals(key) ? null : v;
    }

    @Override
    public String registryId(Object stack) {
        ResourceLocation rl = Item.REGISTRY.getNameForObject(s(stack).getItem());
        return rl == null ? null : rl.toString();
    }

    @Override
    public List<Object> allItems() {
        NonNullList<ItemStack> list = NonNullList.create();
        for (Item item : Item.REGISTRY) {
            try {
                item.getSubItems(item, CreativeTabs.SEARCH, list);
            } catch (Throwable t) {
                Log.debug("could not list " + item + ": " + t);
            }
        }
        return new ArrayList<Object>(list);
    }

    @Override
    public Object stackById(String id) {
        String[] parts = id.split(":");
        int meta = 0;
        String name = id;
        if (parts.length == 3 && parts[2].matches("\\d+")) {
            name = parts[0] + ":" + parts[1];
            meta = Integer.parseInt(parts[2]);
        }
        Item item = Item.REGISTRY.getObject(new ResourceLocation(name));
        return item == null ? null : new ItemStack(item, 1, meta);
    }

    @Override
    public String serialize(Object stack) {
        return s(stack).writeToNBT(new NBTTagCompound())
            .toString();
    }

    @Override
    public Object deserialize(String data) {
        try {
            NBTTagCompound tag = JsonToNBT.getTagFromJson(data);
            if (Item.REGISTRY.getObject(new ResourceLocation(tag.getString("id"))) == null) return null;
            ItemStack st = new ItemStack(tag);
            return st.isEmpty() ? null : st;
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
        ItemStack target = s(stack);
        for (IRecipe r : CraftingManager.getInstance()
            .getRecipeList()) {
            ItemStack result = r.getRecipeOutput();
            if (result == null || result.isEmpty() || result.getItem() != target.getItem()) continue;
            if (!target.isItemStackDamageable() && result.getMetadata() != target.getMetadata()) continue;
            List<?> inputs = inputsOf(r);
            if (inputs == null) continue;
            return new Recipe(ingredients(inputs), result.getCount());
        }
        return new Recipe(Collections.emptyList(), 1);
    }

    /** Recipe inputs: ItemStack, List of ItemStack (ore dictionary) or empty. Null for unknown recipe types. */
    private static List<?> inputsOf(IRecipe r) {
        if (r instanceof ShapedRecipes) return Arrays.asList(((ShapedRecipes) r).recipeItems);
        if (r instanceof ShapelessRecipes) return ((ShapelessRecipes) r).recipeItems;
        if (r instanceof ShapedOreRecipe) return Arrays.asList(((ShapedOreRecipe) r).getInput());
        if (r instanceof ShapelessOreRecipe) return ((ShapelessOreRecipe) r).getInput();
        return null;
    }

    private static List<Object> ingredients(List<?> list) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, ItemStack> first = new LinkedHashMap<>();
        for (Object in : list) {
            ItemStack o = null;
            if (in instanceof ItemStack) o = (ItemStack) in;
            else if (in instanceof List && !((List<?>) in).isEmpty() && ((List<?>) in).get(0) instanceof ItemStack) o = (ItemStack) ((List<?>) in).get(0);
            if (o == null || o.isEmpty()) continue;
            String key = Item.REGISTRY.getNameForObject(o.getItem()) + ":" + (o.getMetadata() == 32767 ? 0 : o.getMetadata());
            counts.merge(key, 1, Integer::sum);
            first.putIfAbsent(key, o);
        }
        List<Object> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            ItemStack st = first.get(e.getKey())
                .copy();
            if (st.getMetadata() == 32767) st.setItemDamage(0);
            st.setCount(e.getValue());
            out.add(st);
        }
        return out;
    }

    // ------------------------------------------------------------------ player / world

    @Override
    public boolean hasPlayer() {
        return mc().player != null;
    }

    @Override
    public List<Object> playerStacks() {
        InventoryPlayer inv = mc().player.inventory;
        List<Object> out = new ArrayList<Object>(inv.mainInventory);
        out.addAll(inv.offHandInventory);
        return out;
    }

    @Override
    public List<Object> containerStacks(Object screen) {
        List<Object> out = new ArrayList<>();
        if (!(screen instanceof GuiContainer)) return out;
        InventoryPlayer inv = mc().player.inventory;
        for (Slot slot : ((GuiContainer) screen).inventorySlots.inventorySlots) {
            if (slot.inventory != inv && slot.getHasStack()) out.add(slot.getStack());
        }
        return out;
    }

    @Override
    public Object carriedStack() {
        return mc().player == null ? ItemStack.EMPTY : mc().player.inventory.getItemStack();
    }

    @Override
    public String worldKey() {
        Minecraft mc = mc();
        try {
            if (mc.isSingleplayer() && mc.getIntegratedServer() != null) {
                return "sp_" + sanitize(mc.getIntegratedServer()
                    .getFolderName());
            }
            ServerData sd = mc.getCurrentServerData();
            if (sd != null && sd.serverIP != null) return "mp_" + sanitize(sd.serverIP);
        } catch (Throwable ignored) {}
        return "default";
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9._\\-]", "_");
    }

    @Override
    public Path configDir() {
        return configDir;
    }

    // ------------------------------------------------------------------ screen

    @Override
    public boolean isContainerScreen(Object screen) {
        return screen instanceof GuiContainer;
    }

    @Override
    public int guiLeft(Object screen) {
        return ((GuiContainer) screen).getGuiLeft();
    }

    @Override
    public int guiTop(Object screen) {
        return ((GuiContainer) screen).getGuiTop();
    }

    @Override
    public int imageWidth(Object screen) {
        return ((GuiContainer) screen).getXSize();
    }

    @Override
    public Object hoveredSlotStack(Object screen, int mx, int my) {
        Slot slot = ((GuiContainer) screen).getSlotUnderMouse();
        return slot != null && slot.getHasStack() ? slot.getStack() : null;
    }

    @Override
    public int screenWidth() {
        return new ScaledResolution(mc()).getScaledWidth();
    }

    @Override
    public int screenHeight() {
        return new ScaledResolution(mc()).getScaledHeight();
    }

    @Override
    public double mouseX() {
        return Mouse.getX() * (double) screenWidth() / mc().displayWidth;
    }

    @Override
    public double mouseY() {
        int h = screenHeight();
        return h - Mouse.getY() * (double) h / mc().displayHeight - 1;
    }

    @Override
    public boolean isLeftMouseDown() {
        return Mouse.isButtonDown(0);
    }

    @Override
    public boolean isShiftDown() {
        return GuiScreen.isShiftKeyDown();
    }

    @Override
    public boolean isCtrlDown() {
        return GuiScreen.isCtrlKeyDown();
    }

    @Override
    public boolean isAltDown() {
        return GuiScreen.isAltKeyDown();
    }

    // ------------------------------------------------------------------ text

    @Override
    public String tr(String key, Object... args) {
        return I18n.format(key, args);
    }

    @Override
    public String clipboard() {
        return GuiScreen.getClipboardString();
    }

    @Override
    public void setClipboard(String text) {
        GuiScreen.setClipboardString(text);
    }

    @Override
    public String addKeyName() {
        return keyName(addKey);
    }

    @Override
    public String hudKeyName() {
        return keyName(hudKey);
    }

    private static String keyName(KeyBinding k) {
        if (k == null || k.getKeyCode() == 0) return null;
        return k.getDisplayName();
    }
}
