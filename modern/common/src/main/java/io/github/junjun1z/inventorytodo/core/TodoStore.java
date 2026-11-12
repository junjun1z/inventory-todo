package io.github.junjun1z.inventorytodo.core;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Loads and saves the list. Lists are stored per world/server in config/inventorytodo/lists/, or in
 * lists/global.json when the "all worlds" scope is selected.
 */
public final class TodoStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();

    public final List<TodoEntry> entries = new ArrayList<>();
    private String currentKey;

    private static String keyFor(Settings s) {
        return s.scope == Settings.Scope.GLOBAL ? "global"
            : Core.mc()
                .worldKey();
    }

    /** Reloads when the world or the scope changed */
    public void ensureLoaded() {
        if (!Core.mc()
            .hasPlayer()) return;
        String key = keyFor(Core.settings());
        if (key.equals(currentKey)) return;
        currentKey = key;
        load();
    }

    /** Forces a reload on the next {@link #ensureLoaded()} */
    public void invalidate() {
        currentKey = null;
    }

    private Path file() {
        return Core.mc()
            .configDir()
            .resolve("lists")
            .resolve(currentKey + ".json");
    }

    private void load() {
        entries.clear();
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r)
                .getAsJsonObject();
            for (JsonElement el : root.getAsJsonArray("entries")) {
                JsonObject o = el.getAsJsonObject();
                TodoEntry e = new TodoEntry();
                e.text = o.has("text") ? o.get("text")
                    .getAsString() : "";
                e.target = o.has("target") ? o.get("target")
                    .getAsInt() : 0;
                e.manualDone = o.has("done") && o.get("done")
                    .getAsBoolean();
                if (o.has("item")) {
                    String data = o.get("item")
                        .getAsString();
                    try {
                        e.item = Core.mc()
                            .deserialize(data);
                    } catch (Throwable t) {
                        Log.warn("Could not read item " + data + ": " + t);
                    }
                    if (e.item == null) e.missingItemData = data;
                }
                entries.add(e);
            }
        } catch (Exception ex) {
            Log.error("Failed to load the list: " + f, ex);
        }
    }

    public void save() {
        if (currentKey == null) return;
        JsonArray arr = new JsonArray();
        for (TodoEntry e : entries) {
            JsonObject o = new JsonObject();
            o.addProperty("text", e.text == null ? "" : e.text);
            o.addProperty("target", e.target);
            o.addProperty("done", e.manualDone);
            String data = e.item != null ? Core.mc()
                .serialize(e.item) : e.missingItemData;
            if (data != null) o.addProperty("item", data);
            arr.add(o);
        }
        JsonObject root = new JsonObject();
        root.addProperty("format", 1);
        root.add("entries", arr);
        Path f = file();
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            Log.error("Failed to save the list: " + f, ex);
        }
    }
}
