package com.github.aruma256.pocketpalette.client.palette;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ExtendWith(MinecraftBootstrapExtension.class)
public class PaletteConfigManagerTest {

    @org.junit.jupiter.api.io.TempDir
    File tempDir;

    @BeforeEach
    public void setUp() {
        PaletteConfigManager.setConfigDir(tempDir);
        ItemPalette.clear();
    }

    @AfterEach
    public void tearDown() {
        // Restore the production default so other tests are unaffected
        PaletteConfigManager.setConfigDir(new File("config"));
        ItemPalette.clear();
    }

    @Test
    public void testLoadAllFromFile_createsConfigWhenMissing() {
        assertFalse(ItemGroupsConfigLoader.configExists(tempDir));

        PaletteConfigManager.loadAllFromFile();

        // A config file must exist afterwards (copied default or empty default)
        assertTrue(ItemGroupsConfigLoader.configExists(tempDir));
    }

    @Test
    public void testLoadAllFromFile_loadsGroupsIntoPalette() throws IOException {
        String json = """
            {
              "config_version": 1,
              "groups": [
                [
                  [{"id": "minecraft:stone"}, {"id": "minecraft:granite"}]
                ]
              ]
            }
            """;
        Files.writeString(new File(tempDir, ItemGroupsConfigLoader.CONFIG_FILE_NAME).toPath(), json);

        boolean ok = PaletteConfigManager.loadAllFromFile();

        assertTrue(ok, "Warnings: " + PaletteConfigManager.LOG_CONFIG_WARNINGS);
        assertTrue(PaletteConfigManager.LOG_CONFIG_WARNINGS.isEmpty());
        assertTrue(ItemPalette.canCycle(new ItemStack(Items.STONE), 0));
    }

    @Test
    public void testLoadAllFromFile_recordsWarningsAndReturnsFalse() throws IOException {
        Files.writeString(new File(tempDir, ItemGroupsConfigLoader.CONFIG_FILE_NAME).toPath(),
                "{ broken json !!");

        boolean ok = PaletteConfigManager.loadAllFromFile();

        assertFalse(ok);
        assertFalse(PaletteConfigManager.LOG_CONFIG_WARNINGS.isEmpty());
    }

    @Test
    public void testLoadAllFromFile_clearsPreviousWarnings() throws IOException {
        Files.writeString(new File(tempDir, ItemGroupsConfigLoader.CONFIG_FILE_NAME).toPath(),
                "{ broken json !!");
        PaletteConfigManager.loadAllFromFile();
        assertFalse(PaletteConfigManager.LOG_CONFIG_WARNINGS.isEmpty());

        Files.writeString(new File(tempDir, ItemGroupsConfigLoader.CONFIG_FILE_NAME).toPath(), """
            {"config_version": 1, "groups": []}
            """);
        boolean ok = PaletteConfigManager.loadAllFromFile();

        assertTrue(ok);
        assertTrue(PaletteConfigManager.LOG_CONFIG_WARNINGS.isEmpty());
    }

    @Test
    public void testApplyGroupDataAndSave_registersAndPersistsCycle() {
        assertTrue(PaletteConfigManager.loadAllFromFile());
        ItemPalette.clear();
        List<ItemState> newCycle = new ArrayList<>();
        newCycle.add(new ItemState(new ItemStack(Items.STONE)));
        newCycle.add(new ItemState(new ItemStack(Items.GRANITE)));

        // Mutate the live group data the way the palette editor does
        List<List<List<ItemState>>> groupData = ItemPalette.getGroupData();
        groupData.add(new ArrayList<>());
        groupData.get(0).add(newCycle);

        assertTrue(PaletteConfigManager.applyGroupDataAndSave());

        // Palette is updated in memory
        assertTrue(ItemPalette.canCycle(new ItemStack(Items.STONE), 0));
        // ... and persisted to disk
        ItemGroupsConfigLoader.LoadResult saved = ItemGroupsConfigLoader.load(tempDir);
        assertEquals(1, saved.getGroups().get(0).size());
    }

    @Test
    public void failedLoadBlocksWritesUntilSuccessfulReload() throws IOException {
        var file = new File(tempDir, ItemGroupsConfigLoader.CONFIG_FILE_NAME).toPath();
        String valid = """
            {"config_version":1,"groups":[[[{"id":"minecraft:stone"},{"id":"minecraft:granite"}]]]}
            """;
        for (String invalid : List.of("{ broken json", """
                {"config_version":1,"groups":[[[{"id":"minecraft:stone"},{"id":"minecraft:granite"},{"id":"missing:item"}]]]}
                """, """
                {"config_version":1,"groups":[[[{"id":"minecraft:stone","components":"invalid"},{"id":"minecraft:granite"}]]]}
                """)) {
            Files.writeString(file, valid);
            assertTrue(PaletteConfigManager.loadAllFromFile());
            var previous = ItemPalette.getGroupData();
            Files.writeString(file, invalid);
            assertFalse(PaletteConfigManager.loadAllFromFile());
            assertFalse(PaletteConfigManager.isEditingAllowed());
            assertSame(previous, ItemPalette.getGroupData());
            assertFalse(PaletteConfigManager.saveAllToFile());
            assertFalse(PaletteConfigManager.applyGroupDataAndSave());
            assertEquals(invalid, Files.readString(file));

            Files.writeString(file, valid);
            assertTrue(PaletteConfigManager.loadAllFromFile());
            assertTrue(PaletteConfigManager.isEditingAllowed());
            assertTrue(PaletteConfigManager.saveAllToFile());
        }
    }

    @Test
    public void failedSerializationPreservesOriginalFile() throws IOException {
        assertTrue(PaletteConfigManager.loadAllFromFile());
        var file = new File(tempDir, ItemGroupsConfigLoader.CONFIG_FILE_NAME).toPath();
        String original = Files.readString(file);
        List<ItemState> invalidCycle = new ArrayList<>();
        invalidCycle.add(null);
        assertFalse(ItemGroupsConfigLoader.save(tempDir, List.of(List.of(invalidCycle))));
        assertEquals(original, Files.readString(file));
        try (var files = Files.list(tempDir.toPath())) {
            assertEquals(1, files.count());
        }
        assertTrue(PaletteConfigManager.isEditingAllowed());
    }

    @Test
    public void unchangedEditorRetriesFailedSaveUntilItSucceeds() throws IOException {
        var file = new File(tempDir, ItemGroupsConfigLoader.CONFIG_FILE_NAME).toPath();
        String original = """
            {"config_version":1,"groups":[[[{"id":"minecraft:stone"},{"id":"minecraft:granite"}]]]}
            """;
        Files.writeString(file, original);
        assertTrue(PaletteConfigManager.loadAllFromFile());
        var editor = new PaletteEditModel(ItemPalette.getGroupData(), new ItemStack(Items.STONE));
        assertEquals(PaletteEditModel.AddResult.ADDED, editor.add(0, new ItemStack(Items.DIORITE)));

        Files.delete(file);
        Files.createDirectory(file);
        Files.writeString(file.resolve("obstruction"), "keep");
        assertFalse(PaletteConfigManager.commitAndSave(editor));
        assertTrue(ItemPalette.canCycle(new ItemStack(Items.DIORITE), 0));
        var reopened = new PaletteEditModel(ItemPalette.getGroupData(), new ItemStack(Items.STONE));
        assertFalse(PaletteConfigManager.commitAndSave(reopened));
        assertEquals("keep", Files.readString(file.resolve("obstruction")));

        Files.delete(file.resolve("obstruction"));
        Files.delete(file);
        Files.writeString(file, original);
        assertTrue(PaletteConfigManager.commitAndSave(reopened));
        var saved = ItemGroupsConfigLoader.load(tempDir);
        assertTrue(saved.getWarnings().isEmpty());
        assertEquals(3, saved.getGroups().getFirst().getFirst().size());

        // After a successful retry, an untouched editor must not write again.
        Files.writeString(file, "external change");
        assertTrue(PaletteConfigManager.commitAndSave(
                new PaletteEditModel(ItemPalette.getGroupData(), new ItemStack(Items.STONE))));
        assertEquals("external change", Files.readString(file));
    }

    @Test
    public void successfulReloadClearsPendingSave() throws IOException {
        assertTrue(PaletteConfigManager.loadAllFromFile());
        var file = new File(tempDir, ItemGroupsConfigLoader.CONFIG_FILE_NAME).toPath();
        Files.delete(file);
        Files.createDirectory(file);
        Files.writeString(file.resolve("obstruction"), "keep");
        assertFalse(PaletteConfigManager.applyGroupDataAndSave());
        Files.delete(file.resolve("obstruction"));
        Files.delete(file);
        assertTrue(PaletteConfigManager.loadAllFromFile());
        Files.writeString(file, "external change");
        assertTrue(PaletteConfigManager.commitAndSave(
                new PaletteEditModel(ItemPalette.getGroupData(), new ItemStack(Items.STONE))));
        assertEquals("external change", Files.readString(file));
    }

    @Test
    public void failedReloadBlocksPendingSaveAndEditorCommit() throws IOException {
        assertTrue(PaletteConfigManager.loadAllFromFile());
        var file = new File(tempDir, ItemGroupsConfigLoader.CONFIG_FILE_NAME).toPath();
        Files.delete(file);
        Files.createDirectory(file);
        Files.writeString(file.resolve("obstruction"), "keep");
        assertFalse(PaletteConfigManager.applyGroupDataAndSave());
        Files.delete(file.resolve("obstruction"));
        Files.delete(file);
        Files.writeString(file, "broken json");
        var editor = new PaletteEditModel(ItemPalette.getGroupData(), new ItemStack(Items.STONE));
        var originalCycle = List.copyOf(editor.getCycle(0));
        assertTrue(editor.remove(0, 0));
        assertFalse(PaletteConfigManager.loadAllFromFile());
        assertFalse(PaletteConfigManager.commitAndSave(editor));
        assertEquals(originalCycle, new PaletteEditModel(
                ItemPalette.getGroupData(), new ItemStack(Items.STONE)).getCycle(0));
        assertEquals("broken json", Files.readString(file));
    }

    @Test
    public void saveIoFailureIsReportedWithoutBlockingEditing() throws IOException {
        assertTrue(PaletteConfigManager.loadAllFromFile());
        var file = new File(tempDir, ItemGroupsConfigLoader.CONFIG_FILE_NAME).toPath();
        Files.delete(file);
        Files.createDirectory(file);
        Files.writeString(file.resolve("obstruction"), "keep");
        assertFalse(PaletteConfigManager.applyGroupDataAndSave());
        assertTrue(PaletteConfigManager.isEditingAllowed());
        assertEquals("keep", Files.readString(file.resolve("obstruction")));
    }
}
