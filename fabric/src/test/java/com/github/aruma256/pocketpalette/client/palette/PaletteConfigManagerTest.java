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
        List<ItemState> newCycle = new ArrayList<>();
        newCycle.add(new ItemState(new ItemStack(Items.STONE)));
        newCycle.add(new ItemState(new ItemStack(Items.GRANITE)));

        // Mutate the live group data the way the palette editor does
        List<List<List<ItemState>>> groupData = ItemPalette.getGroupData();
        groupData.add(new ArrayList<>());
        groupData.get(0).add(newCycle);

        PaletteConfigManager.applyGroupDataAndSave();

        // Palette is updated in memory
        assertTrue(ItemPalette.canCycle(new ItemStack(Items.STONE), 0));
        // ... and persisted to disk
        ItemGroupsConfigLoader.LoadResult saved = ItemGroupsConfigLoader.load(tempDir);
        assertEquals(1, saved.getGroups().get(0).size());
    }
}
