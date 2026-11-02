package com.github.aruma256.pocketpalette.client.palette;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.github.aruma256.pocketpalette.PocketPalette;

public class PaletteConfigManager {

    private static final String DEFAULT_JSON_RESOURCE = "/assets/pocketpalette/default-item-groups.json";

    private static File configDir = new File("config");
    // Only a complete, warning-free load permits overwriting the source config.
    private static boolean editingAllowed;
    private static boolean savePending;

    public static boolean isEditingAllowed() {
        return editingAllowed;
    }

    public static final List<String> LOG_CONFIG_WARNINGS = new ArrayList<>();

    // Visible for testing: redirect config I/O to a temporary directory
    static void setConfigDir(File dir) {
        configDir = dir;
        editingAllowed = false;
        savePending = false;
    }

    public static boolean loadAllFromFile() {
        LOG_CONFIG_WARNINGS.clear();

        // If config doesn't exist, copy default
        if (!ItemGroupsConfigLoader.configExists(configDir)) {
            PocketPalette.LOGGER.debug("Config file does not exist, copying default.");
            copyDefaultJsonFromResources();
        }

        // Load from JSON
        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(configDir);
        LOG_CONFIG_WARNINGS.addAll(result.warnings());
        for (String warning : result.warnings()) {
            PocketPalette.LOGGER.warn("Config: {}", warning);
        }
        editingAllowed = result.warnings().isEmpty();
        if (editingAllowed) {
            ItemPalette.loadGroups(result.groups());
            savePending = false;
        }
        return editingAllowed;
    }

    private static void copyDefaultJsonFromResources() {
        configDir.mkdirs();
        File targetFile = new File(configDir, ItemGroupsConfigLoader.CONFIG_FILE_NAME);

        try (InputStream is = PaletteConfigManager.class.getResourceAsStream(DEFAULT_JSON_RESOURCE)) {
            if (is == null) {
                PocketPalette.LOGGER.warn("Default JSON resource not found: {}. Creating empty config.", DEFAULT_JSON_RESOURCE);
                // Create default empty groups
                List<List<List<ItemState>>> defaultGroups = ItemGroupsConfigLoader.createDefaultGroups();
                ItemGroupsConfigLoader.save(configDir, defaultGroups);
                return;
            }
            Files.copy(is, targetFile.toPath());
        } catch (IOException e) {
            PocketPalette.LOGGER.error("Failed to copy default config from resources", e);
            // Create default empty groups as fallback
            List<List<List<ItemState>>> defaultGroups = ItemGroupsConfigLoader.createDefaultGroups();
            ItemGroupsConfigLoader.save(configDir, defaultGroups);
        }
    }

    public static boolean saveAllToFile() {
        if (!editingAllowed) {
            return false;
        }
        List<List<List<ItemState>>> groupData = ItemPalette.getGroupData();
        boolean saved = ItemGroupsConfigLoader.save(configDir, groupData);
        savePending = !saved;
        return saved;
    }

    /** Commit an editor session, retrying any previous failed save even without new edits. */
    public static boolean commitAndSave(PaletteEditModel model) {
        if (!editingAllowed) {
            return false;
        }
        if (model.commit()) {
            return applyGroupDataAndSave();
        }
        return !savePending || saveAllToFile();
    }

    /**
     * Rebuild the palette caches from the (mutated) group data and persist it.
     * Used by the palette editor after committing an edit session.
     */
    public static boolean applyGroupDataAndSave() {
        if (!editingAllowed) {
            return false;
        }
        ItemPalette.loadGroups(ItemPalette.getGroupData());
        return saveAllToFile();
    }

}
