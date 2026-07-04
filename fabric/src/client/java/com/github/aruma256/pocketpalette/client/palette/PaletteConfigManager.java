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

    private static final File CONFIG_DIR = new File("config");

    public static final List<String> LOG_CONFIG_WARNINGS = new ArrayList<>();

    public static boolean loadAllFromFile() {
        LOG_CONFIG_WARNINGS.clear();

        // If config doesn't exist, copy default
        if (!ItemGroupsConfigLoader.configExists(CONFIG_DIR)) {
            PocketPalette.LOGGER.debug("Config file does not exist, copying default.");
            copyDefaultJsonFromResources();
        }

        // Load from JSON
        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(CONFIG_DIR);
        LOG_CONFIG_WARNINGS.addAll(result.getWarnings());
        ItemPalette.loadGroups(result.getGroups());

        return result.getWarnings().isEmpty();
    }

    private static void copyDefaultJsonFromResources() {
        CONFIG_DIR.mkdirs();
        File targetFile = new File(CONFIG_DIR, ItemGroupsConfigLoader.CONFIG_FILE_NAME);

        try (InputStream is = PaletteConfigManager.class.getResourceAsStream(DEFAULT_JSON_RESOURCE)) {
            if (is == null) {
                PocketPalette.LOGGER.warn("Default JSON resource not found: {}. Creating empty config.", DEFAULT_JSON_RESOURCE);
                // Create default empty groups
                List<List<List<ItemState>>> defaultGroups = ItemGroupsConfigLoader.createDefaultGroups();
                ItemGroupsConfigLoader.save(CONFIG_DIR, defaultGroups);
                return;
            }
            Files.copy(is, targetFile.toPath());
        } catch (IOException e) {
            PocketPalette.LOGGER.error("Failed to copy default config from resources", e);
            // Create default empty groups as fallback
            List<List<List<ItemState>>> defaultGroups = ItemGroupsConfigLoader.createDefaultGroups();
            ItemGroupsConfigLoader.save(CONFIG_DIR, defaultGroups);
        }
    }

    public static void saveAllToFile() {
        List<List<List<ItemState>>> groupData = ItemPalette.getGroupData();
        ItemGroupsConfigLoader.save(CONFIG_DIR, groupData);
    }

    public static boolean tryToAddItemGroup(List<ItemState> newCycle, int groupIndex) {
        List<List<List<ItemState>>> groupData = ItemPalette.getGroupData();

        // Ensure we have enough groups
        while (groupData.size() <= groupIndex) {
            groupData.add(new ArrayList<>());
        }

        // Add the new cycle
        groupData.get(groupIndex).add(newCycle);

        // Reload to validate
        ItemPalette.loadGroups(groupData);

        // Save to file
        saveAllToFile();
        return true;
    }
}
