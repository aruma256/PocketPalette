package com.github.aruma256.pocketpalette.client.palette;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemGroupsConfigLoader {

    public static final String CONFIG_FILE_NAME = "PocketPalette-ItemGroups.json";

    private static final int CURRENT_CONFIG_VERSION = 1;

    /**
     * Get the appropriate RegistryAccess for codec operations.
     * Uses the client's level registry (includes data-driven registries like enchantments)
     * when in a world, otherwise falls back to built-in registries.
     */
    private static RegistryAccess getRegistryAccess() {
        // Minecraft.getInstance() is null in unit tests (no client running)
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.level != null) {
            return mc.level.registryAccess();
        }
        // Fallback for when not in a world (limited - won't have enchantments etc.)
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    /**
     * Create RegistryOps backed by the client's registry access, so that
     * data-driven registries (e.g. enchantments) resolve when available.
     */
    private static RegistryOps<JsonElement> createRegistryOps() {
        return RegistryOps.create(JsonOps.INSTANCE, getRegistryAccess());
    }

    public static class LoadResult {
        private final List<List<List<ItemState>>> groups;
        private final List<String> warnings;

        public LoadResult(List<List<List<ItemState>>> groups, List<String> warnings) {
            this.groups = groups;
            this.warnings = warnings;
        }

        public List<List<List<ItemState>>> getGroups() {
            return groups;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }

    public static boolean configExists(File configDir) {
        return new File(configDir, CONFIG_FILE_NAME).exists();
    }

    public static LoadResult load(File configDir) {
        File file = new File(configDir, CONFIG_FILE_NAME);
        if (!file.exists()) {
            return new LoadResult(new ArrayList<>(), List.of("Config file not found: " + file.getPath()));
        }
        return loadFromJson(file);
    }

    private static LoadResult loadFromJson(File file) {
        List<List<List<ItemState>>> groups = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            validateConfigVersion(root, warnings);

            if (!root.has("groups")) {
                warnings.add("No 'groups' array found in config file.");
                return new LoadResult(groups, warnings);
            }

            JsonArray groupsArray = root.getAsJsonArray("groups");
            for (int groupIndex = 0; groupIndex < groupsArray.size(); groupIndex++) {
                List<List<ItemState>> cyclesInGroup = parseGroup(groupsArray.get(groupIndex).getAsJsonArray(), groupIndex, warnings);
                groups.add(cyclesInGroup);
            }

        } catch (IOException e) {
            warnings.add("Failed to read config file: " + e.getMessage());
            PocketPalette.LOGGER.error("Failed to read config file", e);
        } catch (Exception e) {
            warnings.add("Failed to parse config file: " + e.getMessage());
            PocketPalette.LOGGER.error("Failed to parse config file", e);
        }

        return new LoadResult(groups, warnings);
    }

    private static void validateConfigVersion(JsonObject root, List<String> warnings) {
        int configVersion = root.has("config_version") ? root.get("config_version").getAsInt() : 0;
        if (configVersion != CURRENT_CONFIG_VERSION) {
            warnings.add("Config version mismatch. Expected " + CURRENT_CONFIG_VERSION + ", got " + configVersion);
        }
    }

    private static List<List<ItemState>> parseGroup(JsonArray cyclesArray, int groupIndex, List<String> warnings) {
        List<List<ItemState>> cyclesInGroup = new ArrayList<>();
        Set<ItemState> groupRegistered = new HashSet<>();

        for (int cycleIndex = 0; cycleIndex < cyclesArray.size(); cycleIndex++) {
            JsonArray itemsArray = cyclesArray.get(cycleIndex).getAsJsonArray();
            List<ItemState> itemsInCycle = parseCycle(itemsArray, groupRegistered, groupIndex, cycleIndex, warnings);

            if (itemsInCycle.size() >= 2) {
                cyclesInGroup.add(itemsInCycle);
                groupRegistered.addAll(itemsInCycle);
            } else if (!itemsArray.isEmpty()) {
                warnings.add(String.format("Cycle has less than 2 valid items. (group[%d].cycle[%d])",
                        groupIndex, cycleIndex));
            }
        }

        return cyclesInGroup;
    }

    private static List<ItemState> parseCycle(JsonArray itemsArray, Set<ItemState> groupRegistered,
                                              int groupIndex, int cycleIndex, List<String> warnings) {
        List<ItemState> itemsInCycle = new ArrayList<>();
        Set<ItemState> cycleRegistered = new HashSet<>();
        String location = String.format("group[%d].cycle[%d]", groupIndex, cycleIndex);

        for (JsonElement itemElement : itemsArray) {
            JsonObject itemObj = itemElement.getAsJsonObject();

            ItemState itemState = parseItemState(itemObj, location, warnings);
            if (itemState == null) {
                continue;
            }

            if (groupRegistered.contains(itemState) || cycleRegistered.contains(itemState)) {
                warnings.add(String.format("'%s' is duplicated. (%s)",
                        itemObj.get("id").getAsString(), location));
                continue;
            }

            itemsInCycle.add(itemState);
            cycleRegistered.add(itemState);
        }

        return itemsInCycle;
    }

    private static ItemState parseItemState(JsonObject itemObj, String location, List<String> warnings) {
        if (!itemObj.has("id")) {
            warnings.add(String.format("Item missing 'id' field. (%s)", location));
            return null;
        }

        String itemIdStr = itemObj.get("id").getAsString();
        ResourceLocation resourceLocation = ResourceLocation.tryParse(itemIdStr);
        if (resourceLocation == null) {
            warnings.add(String.format("'%s' is not a valid item. (%s)", itemIdStr, location));
            return null;
        }
        Optional<Holder.Reference<Item>> itemHolder = BuiltInRegistries.ITEM.get(resourceLocation);

        if (itemHolder.isEmpty()) {
            warnings.add(String.format("'%s' is not a valid item. (%s)", itemIdStr, location));
            return null;
        }

        Item item = itemHolder.get().value();
        if (!ItemValidation.isValidItem(item)) {
            warnings.add(String.format("'%s' is not a valid item. (%s)", itemIdStr, location));
            return null;
        }

        ItemStack itemStack = new ItemStack(item);

        if (itemObj.has("components")) {
            String componentsStr = itemObj.get("components").getAsString();
            try {
                CompoundTag nbt = TagParser.parseCompoundFully(componentsStr);
                JsonElement json = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, nbt);
                DataComponentPatch patch = DataComponentPatch.CODEC.parse(createRegistryOps(), json).getOrThrow();
                itemStack.applyComponents(patch);
            } catch (Exception e) {
                warnings.add(String.format("Failed to parse components for '%s': %s (%s)",
                        itemIdStr, e.getMessage(), location));
            }
        }

        return new ItemState(itemStack);
    }

    public static boolean save(File configDir, List<List<List<ItemState>>> groups) {
        File file = new File(configDir, CONFIG_FILE_NAME);
        Gson compactGson = new Gson();
        Path temporary = null;

        try {
            Files.createDirectories(configDir.toPath());
            temporary = Files.createTempFile(configDir.toPath(), CONFIG_FILE_NAME, ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {

                writer.write("{\n");
                writer.write("  \"config_version\": " + CURRENT_CONFIG_VERSION + ",\n");
                writer.write("  \"groups\": [\n");

                for (int groupIdx = 0; groupIdx < groups.size(); groupIdx++) {
                    List<List<ItemState>> cyclesInGroup = groups.get(groupIdx);
                    writer.write("    [\n");

                    for (int cycleIdx = 0; cycleIdx < cyclesInGroup.size(); cycleIdx++) {
                        List<ItemState> itemsInCycle = cyclesInGroup.get(cycleIdx);

                        // Build cycle array inline (single line)
                        JsonArray itemsArray = new JsonArray();
                        for (ItemState itemState : itemsInCycle) {
                            itemsArray.add(serializeItemState(itemState));
                        }
                        String cycleJson = compactGson.toJson(itemsArray);

                        writer.write("      " + cycleJson);
                        if (cycleIdx < cyclesInGroup.size() - 1) {
                            writer.write(",");
                        }
                        writer.write("\n");
                    }

                    writer.write("    ]");
                    if (groupIdx < groups.size() - 1) {
                        writer.write(",");
                    }
                    writer.write("\n");
                }

                writer.write("  ]\n");
                writer.write("}\n");
            }
            Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            PocketPalette.LOGGER.error("Failed to save config file", e);
            return false;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException e) {
                    PocketPalette.LOGGER.warn("Failed to remove temporary config file", e);
                }
            }
        }
    }

    private static JsonObject serializeItemState(ItemState itemState) {
        ItemStack stack = itemState.toItemStack();
        JsonObject obj = new JsonObject();

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        obj.addProperty("id", id.toString());

        if (itemState.hasComponents()) {
            try {
                DataComponentPatch patch = stack.getComponentsPatch();
                JsonElement encoded = DataComponentPatch.CODEC.encodeStart(createRegistryOps(), patch).getOrThrow();

                // Convert JSON to SNBT-like string
                Tag nbt = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, encoded);
                obj.addProperty("components", nbt.toString());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize components for " + id, e);
            }
        }

        return obj;
    }

    public static List<List<List<ItemState>>> createDefaultGroups() {
        List<List<List<ItemState>>> groups = new ArrayList<>();
        // Two empty groups for PRIMARY and SECONDARY
        groups.add(new ArrayList<>());
        groups.add(new ArrayList<>());
        return groups;
    }
}
