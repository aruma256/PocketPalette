package com.github.aruma256.pocketpalette.client.palette;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class DefaultItemGroupsCatalogTest {

    private static final String DEFAULT_JSON_RESOURCE = "/assets/pocketpalette/default-item-groups.json";

    /**
     * バニラ全アイテムに対する網羅性チェック用の suffix と、設計上の意図的な除外。
     * 新しい Minecraft バージョンでアイテムが増えたとき、カタログの取りこぼしをここで検出する。
     */
    private static final Map<String, Set<String>> COVERAGE_SUFFIXES = Map.ofEntries(
            Map.entry("_planks", Set.of()),
            Map.entry("_wool", Set.of()),
            Map.entry("_carpet", Set.of()),
            Map.entry("_terracotta", Set.of()),
            Map.entry("_concrete", Set.of()),
            Map.entry("_concrete_powder", Set.of()),
            Map.entry("_stained_glass", Set.of()),
            Map.entry("_stained_glass_pane", Set.of()),
            Map.entry("_shulker_box", Set.of()),
            Map.entry("_bed", Set.of()),
            Map.entry("_banner", Set.of()),
            Map.entry("_candle", Set.of()),
            Map.entry("_bricks", Set.of(
                    "infested_stone_bricks", "infested_mossy_stone_bricks",
                    "infested_cracked_stone_bricks", "infested_chiseled_stone_bricks")),
            Map.entry("_log", Set.of()),
            Map.entry("_wood", Set.of()),
            Map.entry("_leaves", Set.of()),
            Map.entry("_sapling", Set.of()),
            Map.entry("_slab", Set.of("petrified_oak_slab")),
            Map.entry("_stairs", Set.of()),
            Map.entry("_wall", Set.of()),
            Map.entry("_fence", Set.of()),
            Map.entry("_fence_gate", Set.of()),
            Map.entry("_door", Set.of("iron_door")),
            Map.entry("_trapdoor", Set.of("iron_trapdoor")),
            Map.entry("_button", Set.of()),
            Map.entry("_pressure_plate", Set.of(
                    "light_weighted_pressure_plate", "heavy_weighted_pressure_plate")),
            Map.entry("_sign", Set.of()),
            Map.entry("_ore", Set.of()),
            Map.entry("_copper", Set.of("raw_copper")),
            Map.entry("_froglight", Set.of()),
            Map.entry("_coral", Set.of()),
            Map.entry("_coral_block", Set.of()),
            Map.entry("_coral_fan", Set.of()),
            Map.entry("_pillar", Set.of()),
            Map.entry("_torch", Set.of("redstone_torch")),
            Map.entry("_lantern", Set.of("sea_lantern")),
            Map.entry("_pickaxe", Set.of()),
            Map.entry("_shovel", Set.of()),
            Map.entry("_axe", Set.of()),
            Map.entry("_hoe", Set.of()),
            Map.entry("_rail", Set.of()),
            Map.entry("_bucket", Set.of(
                    "cod_bucket", "salmon_bucket", "tropical_fish_bucket", "pufferfish_bucket",
                    "axolotl_bucket", "tadpole_bucket")),
            Map.entry("_minecart", Set.of("command_block_minecart")));

    private static List<String> allCatalogIds() {
        List<String> ids = new ArrayList<>();
        DefaultItemGroupsCatalog.group1Cycles().forEach(ids::addAll);
        return ids;
    }

    @Test
    void allCatalogIdsExistInRegistry() {
        List<String> unknown = new ArrayList<>();
        for (String id : allCatalogIds()) {
            Optional<Holder.Reference<Item>> holder =
                    BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(id));
            if (holder.isEmpty() || !ItemValidation.isValidItem(holder.get().value())) {
                unknown.add(id);
            }
        }
        assertTrue(unknown.isEmpty(), "Unknown item ids in catalog: " + unknown);
    }

    @Test
    void noDuplicateIdsWithinGroup1() {
        Set<String> seen = new HashSet<>();
        List<String> duplicated = new ArrayList<>();
        for (String id : allCatalogIds()) {
            if (!seen.add(id)) {
                duplicated.add(id);
            }
        }
        assertTrue(duplicated.isEmpty(), "Duplicated item ids in group 1: " + duplicated);
    }

    @Test
    void everyCycleHasAtLeastTwoItems() {
        List<List<String>> cycles = DefaultItemGroupsCatalog.group1Cycles();
        for (int i = 0; i < cycles.size(); i++) {
            assertTrue(cycles.get(i).size() >= 2, "Cycle " + i + " has fewer than 2 items");
        }
    }

    @Test
    void registryCoverageBySuffix() {
        Set<String> included = new HashSet<>(allCatalogIds());
        List<String> missing = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            for (Map.Entry<String, Set<String>> entry : COVERAGE_SUFFIXES.entrySet()) {
                if (path.endsWith(entry.getKey())
                        && !included.contains(path)
                        && !entry.getValue().contains(path)) {
                    missing.add(path + " (suffix " + entry.getKey() + ")");
                    break;
                }
            }
        }
        assertTrue(missing.isEmpty(),
                "Items missing from catalog (add to a cycle or to the exclusion list): " + missing);
    }

    @Test
    void shippedJsonMatchesCatalog() throws IOException {
        String shipped;
        try (InputStream is = getClass().getResourceAsStream(DEFAULT_JSON_RESOURCE)) {
            assertTrue(is != null, "Default JSON resource not found: " + DEFAULT_JSON_RESOURCE);
            shipped = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        String generated = DefaultItemGroupsGenerator.generateJson();
        assertEquals(normalize(generated), normalize(shipped),
                "default-item-groups.json is out of sync with DefaultItemGroupsCatalog. "
                        + "Run: ./gradlew generateDefaultItemGroups");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
