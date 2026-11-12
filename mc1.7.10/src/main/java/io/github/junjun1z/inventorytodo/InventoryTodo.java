package io.github.junjun1z.inventorytodo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * Inventory ToDo: a to-do / item goal list shown next to inventory screens.
 * Client-side only. Works on its own; integrates with NEI (original or GTNH fork) when present.
 */
@Mod(
    modid = InventoryTodo.MODID,
    name = "Inventory ToDo",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*",
    dependencies = "after:NotEnoughItems",
    guiFactory = "io.github.junjun1z.inventorytodo.GuiFactory")
public class InventoryTodo {

    public static final String MODID = "inventorytodo";
    public static final Logger LOG = LogManager.getLogger(MODID);

    public static File configDir;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (FMLCommonHandler.instance()
            .getSide() != Side.CLIENT) return;
        File root = event.getModConfigurationDirectory();
        configDir = new File(root, MODID);
        migrateFromGtnhTodo(new File(root, "gtnhtodo"), configDir);
        if (!configDir.exists()) configDir.mkdirs();
        TodoConfig.load(new File(configDir, MODID + ".cfg"));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (FMLCommonHandler.instance()
            .getSide() != Side.CLIENT) return;
        ClientSetup.init();
    }

    /** Copies lists and settings from the old "GTNH ToDo" mod the first time. */
    private static void migrateFromGtnhTodo(File oldDir, File newDir) {
        // migrate unless this mod already has lists (the folder itself may exist, e.g. created for the config)
        if (new File(newDir, "lists").exists() || !oldDir.isDirectory()) return;
        try {
            newDir.mkdirs();
            File oldLists = new File(oldDir, "lists");
            File[] files = oldLists.listFiles();
            if (files != null) {
                File newLists = new File(newDir, "lists");
                newLists.mkdirs();
                for (File f : files) {
                    if (f.isFile()) Files.copy(f.toPath(), new File(newLists, f.getName()).toPath());
                }
            }
            File ui = new File(newDir, "ui.json");
            if (!ui.exists()) copyIfExists(new File(oldDir, "ui.json"), ui);
            File cfg = new File(newDir, MODID + ".cfg");
            if (!cfg.exists()) copyIfExists(new File(oldDir, "gtnhtodo.cfg"), cfg);
            LOG.info("Imported lists and settings from config/gtnhtodo");
        } catch (IOException e) {
            LOG.warn("Could not import data from config/gtnhtodo", e);
        }
    }

    private static void copyIfExists(File from, File to) throws IOException {
        if (from.isFile()) Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
