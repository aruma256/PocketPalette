package com.github.aruma256.pocketpalette.client.palette;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Regenerates the shipped default config from {@link DefaultItemGroupsCatalog}.
 * Run with {@code ./gradlew generateDefaultItemGroups}.
 *
 * <p>NeoForge needs FML to bootstrap the registries, so this runs as a JUnit test
 * inside the NeoForge test environment instead of a plain {@code main} method.
 * The test is skipped unless the Gradle task passes the target path.
 *
 * <p>Serialization is delegated to {@link ItemGroupsConfigLoader#save} so that the
 * shipped JSON is byte-identical to what the mod itself writes at runtime.
 */
@ExtendWith(MinecraftBootstrapExtension.class)
public class DefaultItemGroupsGenerator {

    private static final String TARGET_PROPERTY = "pocketpalette.defaultItemGroupsTarget";

    @Test
    @EnabledIfSystemProperty(named = TARGET_PROPERTY, matches = ".+")
    void writeDefaultItemGroups() throws IOException {
        Path target = Path.of(System.getProperty(TARGET_PROPERTY));
        Files.writeString(target, generateJson(), StandardCharsets.UTF_8);
        System.out.println("Wrote " + target.toAbsolutePath());
    }

    /** Builds the groups from the catalog and renders them via the production serializer. */
    public static String generateJson() throws IOException {
        Path tempDir = Files.createTempDirectory("pocketpalette-default-gen");
        try {
            ItemGroupsConfigLoader.save(tempDir.toFile(), buildGroups());
            Path written = tempDir.resolve(ItemGroupsConfigLoader.CONFIG_FILE_NAME);
            String json = Files.readString(written, StandardCharsets.UTF_8);
            Files.delete(written);
            return json;
        } finally {
            Files.delete(tempDir);
        }
    }

    public static List<List<List<ItemState>>> buildGroups() {
        List<List<ItemState>> group1 = new ArrayList<>();
        for (List<String> cycle : DefaultItemGroupsCatalog.group1Cycles()) {
            List<ItemState> states = new ArrayList<>();
            for (String id : cycle) {
                states.add(new ItemState(new ItemStack(resolve(id))));
            }
            group1.add(states);
        }
        List<List<List<ItemState>>> groups = new ArrayList<>();
        groups.add(group1);
        groups.add(new ArrayList<>()); // group 2 はユーザーの自作用に空けておく
        return groups;
    }

    private static Item resolve(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown item id in catalog: " + id))
                .value();
    }
}
