package io.github.junjun1z.inventorytodo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Saves and loads the lists and the UI state.
 * Lists are stored per world (or server) in config/inventorytodo/lists/.
 * Items are stored as registry name + damage + NBT (Base64), so numeric ID differences between worlds don't matter.
 */
public final class TodoStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();

    public final List<TodoEntry> entries = new ArrayList<>();
    private String currentKey;

    // ---- UI state (shared by all worlds) ----
    public enum Mode {
        FOLLOW, // next to the left edge of the inventory
        LEFT, // pinned to the left edge of the screen
        FREE // wherever it was dragged
    }

    public Mode mode = Mode.FOLLOW;
    public int freeX = 4, freeY = 30;
    public boolean minimized = false;

    // ------------------------------------------------------------

    /** Key for the current world/server (used as the file name) */
    public static String computeWorldKey() {
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (mc.isSingleplayer() && mc.getIntegratedServer() != null) {
                return "sp_" + sanitize(
                    mc.getIntegratedServer()
                        .getFolderName());
            }
            ServerData sd = mc.func_147104_D();
            if (sd != null && sd.serverIP != null) return "mp_" + sanitize(sd.serverIP);
        } catch (Throwable ignored) {}
        return "default";
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9._\\-]", "_");
    }

    /** Reloads the list when the world has changed */
    public void ensureLoaded() {
        String key = computeWorldKey();
        if (key.equals(currentKey)) return;
        currentKey = key;
        loadList();
    }

    private File listFile() {
        File dir = new File(InventoryTodo.configDir, "lists");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, currentKey + ".json");
    }

    private File uiFile() {
        return new File(InventoryTodo.configDir, "ui.json");
    }

    // ---------------- lists ----------------

    private void loadList() {
        entries.clear();
        File f = listFile();
        if (!f.exists()) return;
        try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
            JsonElement root = new JsonParser().parse(r);
            JsonArray arr = root.getAsJsonObject()
                .getAsJsonArray("entries");
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                TodoEntry e = new TodoEntry();
                e.text = o.has("text") ? o.get("text")
                    .getAsString() : "";
                e.target = o.has("target") ? o.get("target")
                    .getAsInt() : 0;
                e.manualDone = o.has("done") && o.get("done")
                    .getAsBoolean();
                if (o.has("item")) {
                    e.item = readItem(o);
                    // Items that no longer exist (e.g. mod removed) are kept as text rows
                    if (e.item == null && e.text.isEmpty())
                        e.text = StatCollector.translateToLocal("inventorytodo.unknown_item") + " "
                            + o.get("item")
                                .getAsString();
                }
                entries.add(e);
            }
        } catch (Exception ex) {
            InventoryTodo.LOG.error("Failed to load the list: " + f, ex);
        }
    }

    public void saveList() {
        if (currentKey == null) return;
        JsonArray arr = new JsonArray();
        for (TodoEntry e : entries) {
            JsonObject o = new JsonObject();
            o.addProperty("text", e.text == null ? "" : e.text);
            o.addProperty("target", e.target);
            o.addProperty("done", e.manualDone);
            if (e.item != null) writeItem(o, e.item);
            arr.add(o);
        }
        JsonObject root = new JsonObject();
        root.add("entries", arr);
        writeJson(listFile(), root);
    }

    private static void writeItem(JsonObject o, ItemStack stack) {
        Object name = Item.itemRegistry.getNameForObject(stack.getItem());
        o.addProperty("item", String.valueOf(name));
        o.addProperty("meta", stack.getItemDamage());
        if (stack.hasTagCompound()) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                CompressedStreamTools.write(stack.getTagCompound(), new DataOutputStream(bos));
                o.addProperty(
                    "nbt",
                    Base64.getEncoder()
                        .encodeToString(bos.toByteArray()));
            } catch (Exception ex) {
                InventoryTodo.LOG.warn("Failed to save NBT", ex);
            }
        }
    }

    private static ItemStack readItem(JsonObject o) {
        Object obj = Item.itemRegistry.getObject(
            o.get("item")
                .getAsString());
        if (!(obj instanceof Item)) return null;
        int meta = o.has("meta") ? o.get("meta")
            .getAsInt() : 0;
        ItemStack stack = new ItemStack((Item) obj, 1, meta);
        if (o.has("nbt")) {
            try {
                byte[] data = Base64.getDecoder()
                    .decode(
                        o.get("nbt")
                            .getAsString());
                NBTTagCompound tag = CompressedStreamTools.read(new DataInputStream(new ByteArrayInputStream(data)));
                stack.setTagCompound(tag);
            } catch (Exception ex) {
                InventoryTodo.LOG.warn("Failed to read NBT", ex);
            }
        }
        return stack;
    }

    // ---------------- UI state ----------------

    public void loadUi() {
        File f = uiFile();
        if (!f.exists()) return;
        try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
            JsonObject o = new JsonParser().parse(r)
                .getAsJsonObject();
            if (o.has("mode")) mode = Mode.valueOf(
                o.get("mode")
                    .getAsString());
            if (o.has("x")) freeX = o.get("x")
                .getAsInt();
            if (o.has("y")) freeY = o.get("y")
                .getAsInt();
            if (o.has("minimized")) minimized = o.get("minimized")
                .getAsBoolean();
        } catch (Exception ex) {
            InventoryTodo.LOG.warn("Failed to load the UI state", ex);
        }
    }

    public void saveUi() {
        JsonObject o = new JsonObject();
        o.addProperty("mode", mode.name());
        o.addProperty("x", freeX);
        o.addProperty("y", freeY);
        o.addProperty("minimized", minimized);
        writeJson(uiFile(), o);
    }

    private static void writeJson(File f, JsonObject root) {
        try {
            File tmp = new File(f.getPath() + ".tmp");
            try (Writer w = new OutputStreamWriter(Files.newOutputStream(tmp.toPath()), StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            if (f.exists() && !f.delete()) InventoryTodo.LOG.warn("Could not delete the old file: " + f);
            if (!tmp.renameTo(f)) InventoryTodo.LOG.warn("Could not replace the save file: " + f);
        } catch (Exception ex) {
            InventoryTodo.LOG.error("Failed to save: " + f, ex);
        }
    }
}
