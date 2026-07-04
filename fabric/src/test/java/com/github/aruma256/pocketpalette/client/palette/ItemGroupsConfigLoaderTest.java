package com.github.aruma256.pocketpalette.client.palette;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ExtendWith(MinecraftBootstrapExtension.class)
public class ItemGroupsConfigLoaderTest {

    @TempDir
    File tempDir;

    @Test
    public void testConfigExists_returnsFalseWhenNoFile() {
        assertFalse(ItemGroupsConfigLoader.configExists(tempDir));
    }

    @Test
    public void testConfigExists_returnsTrueWhenFileExists() throws IOException {
        File configFile = new File(tempDir, ItemGroupsConfigLoader.CONFIG_FILE_NAME);
        configFile.createNewFile();
        assertTrue(ItemGroupsConfigLoader.configExists(tempDir));
    }

    @Test
    public void testLoad_returnsWarningWhenFileNotFound() {
        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getGroups().isEmpty());
    }

    @Test
    public void testLoad_parsesValidJson() throws IOException {
        String json = """
            {
              "config_version": 1,
              "groups": [
                [
                  [{"id": "minecraft:stone"}, {"id": "minecraft:granite"}, {"id": "minecraft:diorite"}]
                ]
              ]
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertTrue(result.getWarnings().isEmpty(), "Warnings: " + result.getWarnings());
        assertEquals(1, result.getGroups().size());
        assertEquals(1, result.getGroups().get(0).size());
        assertEquals(3, result.getGroups().get(0).get(0).size());
    }

    @Test
    public void testLoad_parsesMultipleGroups() throws IOException {
        String json = """
            {
              "config_version": 1,
              "groups": [
                [
                  [{"id": "minecraft:stone"}, {"id": "minecraft:granite"}]
                ],
                [
                  [{"id": "minecraft:oak_planks"}, {"id": "minecraft:spruce_planks"}]
                ]
              ]
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertTrue(result.getWarnings().isEmpty(), "Warnings: " + result.getWarnings());
        assertEquals(2, result.getGroups().size());
    }

    @Test
    public void testLoad_parsesMultipleCyclesInGroup() throws IOException {
        String json = """
            {
              "config_version": 1,
              "groups": [
                [
                  [{"id": "minecraft:stone"}, {"id": "minecraft:granite"}],
                  [{"id": "minecraft:oak_planks"}, {"id": "minecraft:spruce_planks"}]
                ]
              ]
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertTrue(result.getWarnings().isEmpty(), "Warnings: " + result.getWarnings());
        assertEquals(1, result.getGroups().size());
        assertEquals(2, result.getGroups().get(0).size());
    }

    @Test
    public void testLoad_warnsOnInvalidItemId() throws IOException {
        String json = """
            {
              "config_version": 1,
              "groups": [
                [
                  [{"id": "minecraft:stone"}, {"id": "minecraft:invalid_item_xyz"}, {"id": "minecraft:granite"}]
                ]
              ]
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("invalid_item_xyz")));
        // Cycle should still be created with valid items (stone, granite = 2 items)
        assertEquals(1, result.getGroups().get(0).size());
        assertEquals(2, result.getGroups().get(0).get(0).size());
    }

    @Test
    public void testLoad_warnsOnDuplicateItemInCycle() throws IOException {
        String json = """
            {
              "config_version": 1,
              "groups": [
                [
                  [{"id": "minecraft:stone"}, {"id": "minecraft:granite"}, {"id": "minecraft:stone"}]
                ]
              ]
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("duplicated")));
    }

    @Test
    public void testLoad_warnsOnDuplicateItemAcrossCyclesInSameGroup() throws IOException {
        // Same item (stone) appears in two different cycles within the same group
        String json = """
            {
              "config_version": 1,
              "groups": [
                [
                  [{"id": "minecraft:stone"}, {"id": "minecraft:granite"}],
                  [{"id": "minecraft:stone"}, {"id": "minecraft:diorite"}]
                ]
              ]
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("stone") && w.contains("duplicated")));
    }

    @Test
    public void testLoad_allowsSameItemInDifferentGroups() throws IOException {
        // Same item (stone) appears in different groups - this should be allowed
        String json = """
            {
              "config_version": 1,
              "groups": [
                [
                  [{"id": "minecraft:stone"}, {"id": "minecraft:granite"}]
                ],
                [
                  [{"id": "minecraft:stone"}, {"id": "minecraft:diorite"}]
                ]
              ]
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertTrue(result.getWarnings().isEmpty(), "Should allow same item in different groups. Warnings: " + result.getWarnings());
        assertEquals(2, result.getGroups().size());
        // Both groups should have their cycles intact
        assertEquals(1, result.getGroups().get(0).size());
        assertEquals(1, result.getGroups().get(1).size());
    }

    @Test
    public void testLoad_warnsOnCycleWithLessThan2Items() throws IOException {
        String json = """
            {
              "config_version": 1,
              "groups": [
                [
                  [{"id": "minecraft:stone"}]
                ]
              ]
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("less than 2")));
        // Empty cycles should not be added
        assertTrue(result.getGroups().get(0).isEmpty());
    }

    @Test
    public void testLoad_warnsOnMissingId() throws IOException {
        String json = """
            {
              "config_version": 1,
              "groups": [
                [
                  [{"id": "minecraft:stone"}, {"name": "no id field"}, {"id": "minecraft:granite"}]
                ]
              ]
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("missing 'id'")));
    }

    @Test
    public void testSave_createsValidJson() throws IOException {
        List<List<List<ItemState>>> groups = new ArrayList<>();

        List<List<ItemState>> group0 = new ArrayList<>();
        List<ItemState> cycle0 = new ArrayList<>();
        cycle0.add(new ItemState(new ItemStack(Items.STONE)));
        cycle0.add(new ItemState(new ItemStack(Items.GRANITE)));
        group0.add(cycle0);
        groups.add(group0);

        ItemGroupsConfigLoader.save(tempDir, groups);

        assertTrue(ItemGroupsConfigLoader.configExists(tempDir));

        // Load it back and verify
        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);
        assertTrue(result.getWarnings().isEmpty(), "Warnings: " + result.getWarnings());
        assertEquals(1, result.getGroups().size());
        assertEquals(1, result.getGroups().get(0).size());
        assertEquals(2, result.getGroups().get(0).get(0).size());
    }

    @Test
    public void testSaveAndLoad_roundTrip() throws IOException {
        List<List<List<ItemState>>> originalGroups = new ArrayList<>();

        // Group 0 with 2 cycles
        List<List<ItemState>> group0 = new ArrayList<>();
        List<ItemState> cycle0_0 = new ArrayList<>();
        cycle0_0.add(new ItemState(new ItemStack(Items.STONE)));
        cycle0_0.add(new ItemState(new ItemStack(Items.GRANITE)));
        cycle0_0.add(new ItemState(new ItemStack(Items.DIORITE)));
        group0.add(cycle0_0);

        List<ItemState> cycle0_1 = new ArrayList<>();
        cycle0_1.add(new ItemState(new ItemStack(Items.OAK_PLANKS)));
        cycle0_1.add(new ItemState(new ItemStack(Items.SPRUCE_PLANKS)));
        group0.add(cycle0_1);
        originalGroups.add(group0);

        // Group 1 with 1 cycle
        List<List<ItemState>> group1 = new ArrayList<>();
        List<ItemState> cycle1_0 = new ArrayList<>();
        cycle1_0.add(new ItemState(new ItemStack(Items.WHITE_WOOL)));
        cycle1_0.add(new ItemState(new ItemStack(Items.ORANGE_WOOL)));
        group1.add(cycle1_0);
        originalGroups.add(group1);

        // Save
        ItemGroupsConfigLoader.save(tempDir, originalGroups);

        // Load back
        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertTrue(result.getWarnings().isEmpty(), "Warnings: " + result.getWarnings());
        assertEquals(2, result.getGroups().size());
        assertEquals(2, result.getGroups().get(0).size());
        assertEquals(1, result.getGroups().get(1).size());

        // Verify items
        assertEquals(Items.STONE, result.getGroups().get(0).get(0).get(0).toItemStack().getItem());
        assertEquals(Items.GRANITE, result.getGroups().get(0).get(0).get(1).toItemStack().getItem());
        assertEquals(Items.DIORITE, result.getGroups().get(0).get(0).get(2).toItemStack().getItem());
    }

    @Test
    public void testCreateDefaultGroups_returnsTwoEmptyGroups() {
        List<List<List<ItemState>>> defaults = ItemGroupsConfigLoader.createDefaultGroups();
        assertEquals(2, defaults.size());
        assertTrue(defaults.get(0).isEmpty());
        assertTrue(defaults.get(1).isEmpty());
    }

    // --- Robustness against broken config files ---

    @Test
    public void testLoad_warnsOnMalformedJson() throws IOException {
        writeConfigFile("{ this is not valid json !!");

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getGroups().isEmpty());
    }

    @Test
    public void testLoad_warnsOnConfigVersionMismatch() throws IOException {
        String json = """
            {
              "config_version": 999,
              "groups": []
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("version")));
    }

    @Test
    public void testLoad_warnsOnMissingConfigVersion() throws IOException {
        writeConfigFile("{\"groups\": []}");

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("version")));
    }

    @Test
    public void testLoad_warnsOnMissingGroups() throws IOException {
        writeConfigFile("{\"config_version\": 1}");

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("groups")));
        assertTrue(result.getGroups().isEmpty());
    }

    @Test
    public void testLoad_parsesEmptyGroupsArray() throws IOException {
        String json = """
            {
              "config_version": 1,
              "groups": []
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertTrue(result.getWarnings().isEmpty(), "Warnings: " + result.getWarnings());
        assertTrue(result.getGroups().isEmpty());
    }

    @Test
    public void testLoad_warnsOnMalformedItemId_withoutAbortingLoad() throws IOException {
        // "NOT a valid id" is not parseable as a ResourceLocation; it must be
        // reported per-item, and the rest of the config must still load
        String json = """
            {
              "config_version": 1,
              "groups": [
                [
                  [{"id": "NOT a valid id"}, {"id": "minecraft:stone"}, {"id": "minecraft:granite"}]
                ]
              ]
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("NOT a valid id")));
        assertEquals(1, result.getGroups().size());
        assertEquals(1, result.getGroups().get(0).size());
        assertEquals(2, result.getGroups().get(0).get(0).size());
    }

    // --- Component-aware entries ---

    @Test
    public void testLoad_parsesComponents() throws IOException {
        String json = """
            {
              "config_version": 1,
              "groups": [
                [
                  [
                    {"id": "minecraft:stone", "components": "{\\"minecraft:custom_name\\":\\"Special Stone\\"}"},
                    {"id": "minecraft:granite"}
                  ]
                ]
              ]
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertTrue(result.getWarnings().isEmpty(), "Warnings: " + result.getWarnings());
        ItemState loaded = result.getGroups().get(0).get(0).get(0);
        assertTrue(loaded.hasComponents());
        assertEquals(Component.literal("Special Stone"),
                loaded.toItemStack().get(DataComponents.CUSTOM_NAME));
    }

    @Test
    public void testLoad_warnsOnUnparseableComponents_keepsPlainItem() throws IOException {
        String json = """
            {
              "config_version": 1,
              "groups": [
                [
                  [
                    {"id": "minecraft:stone", "components": "not-snbt-at-all{{{"},
                    {"id": "minecraft:granite"}
                  ]
                ]
              ]
            }
            """;
        writeConfigFile(json);

        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertTrue(result.getWarnings().stream()
                .anyMatch(w -> w.contains("components") || w.contains("Failed to parse")));
        // The item itself is kept (without components), so the cycle stays valid
        assertEquals(2, result.getGroups().get(0).get(0).size());
        assertFalse(result.getGroups().get(0).get(0).get(0).hasComponents());
    }

    @Test
    public void testSaveAndLoad_roundTripWithComponents() throws IOException {
        ItemStack namedStone = new ItemStack(Items.STONE);
        namedStone.set(DataComponents.CUSTOM_NAME, Component.literal("Special Stone"));

        List<List<List<ItemState>>> groups = new ArrayList<>();
        List<List<ItemState>> group0 = new ArrayList<>();
        List<ItemState> cycle = new ArrayList<>();
        cycle.add(new ItemState(namedStone));
        cycle.add(new ItemState(new ItemStack(Items.GRANITE)));
        group0.add(cycle);
        groups.add(group0);

        ItemGroupsConfigLoader.save(tempDir, groups);
        ItemGroupsConfigLoader.LoadResult result = ItemGroupsConfigLoader.load(tempDir);

        assertTrue(result.getWarnings().isEmpty(), "Warnings: " + result.getWarnings());
        ItemState loaded = result.getGroups().get(0).get(0).get(0);
        assertEquals(new ItemState(namedStone), loaded);
        assertEquals(Component.literal("Special Stone"),
                loaded.toItemStack().get(DataComponents.CUSTOM_NAME));
    }

    @Test
    public void testSave_createsMissingDirectory() {
        File nestedDir = new File(tempDir, "nested/config");

        ItemGroupsConfigLoader.save(nestedDir, ItemGroupsConfigLoader.createDefaultGroups());

        assertTrue(ItemGroupsConfigLoader.configExists(nestedDir));
    }

    private void writeConfigFile(String content) throws IOException {
        File configFile = new File(tempDir, ItemGroupsConfigLoader.CONFIG_FILE_NAME);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            writer.write(content);
        }
    }
}
