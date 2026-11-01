package com.github.aruma256.pocketpalette.client.palette;

import java.util.ArrayList;
import java.util.List;

/**
 * Source of truth for the default item groups shipped as
 * {@code assets/pocketpalette/default-item-groups.json}.
 *
 * <p>Design rationale: docs/default-item-groups-catalog.md（サイクル番号 A1〜E4 は同ドキュメント準拠）
 *
 * <p>Regenerate the JSON with {@code ./gradlew generateDefaultItemGroups}.
 * {@link DefaultItemGroupsCatalogTest} fails if the shipped JSON is out of sync.
 */
public final class DefaultItemGroupsCatalog {

    /** 虹順（こだわりポイント）。バニラの DyeColor 順ではない。 */
    static final List<String> COLORS = List.of(
            "red", "orange", "yellow", "lime", "green", "cyan", "light_blue", "blue",
            "purple", "magenta", "pink", "brown", "black", "gray", "light_gray", "white");

    /** 樹種順は planks の登録順に正規化（バニラは log 系のみ pale_oak が先で不整合）。 */
    static final List<String> WOODS = List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "cherry",
            "dark_oak", "pale_oak", "mangrove", "bamboo", "crimson", "warped");

    /** log/wood 形式を持つオーバーワールドの樹種（oak〜mangrove）。 */
    private static final List<String> LOG_WOODS = WOODS.subList(0, 9);

    private DefaultItemGroupsCatalog() {}

    public static List<List<String>> group1Cycles() {
        List<List<String>> cycles = new ArrayList<>();

        // ---- A. 色物ファミリー（虹順・原色先頭） ----
        cycles.add(colorLoop(List.of(), "*_wool", "*_carpet"));                    // A1
        cycles.add(colorLoop(List.of(), "*_concrete", "*_concrete_powder"));       // A2
        cycles.add(colorLoop(List.of("terracotta"), "*_terracotta"));              // A3
        cycles.add(colorLoop(List.of(), "*_glazed_terracotta"));                   // A4
        cycles.add(colorLoop(List.of("glass", "glass_pane", "tinted_glass"),
                "*_stained_glass", "*_stained_glass_pane"));                       // A5
        cycles.add(colorLoop(List.of("candle"), "*_candle"));                      // A6
        cycles.add(colorLoop(List.of("shulker_box"), "*_shulker_box"));            // A7
        cycles.add(colorLoop(List.of(), "*_bed"));                                 // A8
        cycles.add(colorLoop(List.of(), "*_banner"));                              // A9

        // ---- B. 木材 ----
        cycles.add(logCycle());                                                    // B1
        cycles.add(woodProductsCycle());                                           // B2
        List<String> leaves = new ArrayList<>();                                   // B3
        LOG_WOODS.forEach(w -> leaves.add(w + "_leaves"));
        leaves.add("azalea_leaves");
        leaves.add("flowering_azalea_leaves");
        cycles.add(leaves);
        List<String> saplings = new ArrayList<>();                                 // B4
        WOODS.subList(0, 8).forEach(w -> saplings.add(w + "_sapling"));
        saplings.add("mangrove_propagule");
        saplings.add("azalea");
        saplings.add("flowering_azalea");
        cycles.add(saplings);

        // ---- C. 石系ファミリー（形状順: normal, slab, stairs, wall, 特殊） ----
        List<String> stone = new ArrayList<>(List.of(                              // C1
                "stone", "stone_slab", "stone_stairs", "stone_pressure_plate", "stone_button",
                "smooth_stone", "smooth_stone_slab"));
        for (String s : List.of("granite", "diorite", "andesite")) {
            stone.addAll(List.of(s, s + "_slab", s + "_stairs", s + "_wall",
                    "polished_" + s, "polished_" + s + "_slab", "polished_" + s + "_stairs"));
        }
        cycles.add(stone);
        cycles.add(List.of(                                                        // C2
                "cobblestone", "cobblestone_slab", "cobblestone_stairs", "cobblestone_wall",
                "mossy_cobblestone", "mossy_cobblestone_slab", "mossy_cobblestone_stairs", "mossy_cobblestone_wall"));
        cycles.add(List.of(                                                        // C3
                "stone_bricks", "stone_brick_slab", "stone_brick_stairs", "stone_brick_wall",
                "chiseled_stone_bricks", "cracked_stone_bricks",
                "mossy_stone_bricks", "mossy_stone_brick_slab", "mossy_stone_brick_stairs", "mossy_stone_brick_wall"));
        cycles.add(List.of(                                                        // C4
                "deepslate",
                "cobbled_deepslate", "cobbled_deepslate_slab", "cobbled_deepslate_stairs", "cobbled_deepslate_wall",
                "chiseled_deepslate",
                "polished_deepslate", "polished_deepslate_slab", "polished_deepslate_stairs", "polished_deepslate_wall",
                "deepslate_bricks", "deepslate_brick_slab", "deepslate_brick_stairs", "deepslate_brick_wall",
                "cracked_deepslate_bricks",
                "deepslate_tiles", "deepslate_tile_slab", "deepslate_tile_stairs", "deepslate_tile_wall",
                "cracked_deepslate_tiles"));
        cycles.add(List.of(                                                        // C5
                "tuff", "tuff_slab", "tuff_stairs", "tuff_wall", "chiseled_tuff",
                "polished_tuff", "polished_tuff_slab", "polished_tuff_stairs", "polished_tuff_wall",
                "tuff_bricks", "tuff_brick_slab", "tuff_brick_stairs", "tuff_brick_wall", "chiseled_tuff_bricks"));
        List<String> sandstone = new ArrayList<>();                                // C6
        for (String s : List.of("sandstone", "red_sandstone")) {
            String smooth = "smooth_" + s;
            sandstone.addAll(List.of(s, s + "_slab", s + "_stairs", s + "_wall",
                    "chiseled_" + s, "cut_" + s, "cut_" + s + "_slab",
                    smooth, smooth + "_slab", smooth + "_stairs"));
        }
        cycles.add(sandstone);
        cycles.add(List.of("bricks", "brick_slab", "brick_stairs", "brick_wall")); // C7
        cycles.add(List.of(                                                        // C8
                "mud", "packed_mud", "mud_bricks", "mud_brick_slab", "mud_brick_stairs", "mud_brick_wall"));
        cycles.add(List.of(                                                        // C9
                "nether_bricks", "nether_brick_slab", "nether_brick_stairs", "nether_brick_wall",
                "nether_brick_fence", "chiseled_nether_bricks", "cracked_nether_bricks",
                "red_nether_bricks", "red_nether_brick_slab", "red_nether_brick_stairs", "red_nether_brick_wall"));
        cycles.add(List.of(                                                        // C10
                "blackstone", "blackstone_slab", "blackstone_stairs", "blackstone_wall", "gilded_blackstone",
                "polished_blackstone", "polished_blackstone_slab", "polished_blackstone_stairs", "polished_blackstone_wall",
                "chiseled_polished_blackstone", "polished_blackstone_pressure_plate", "polished_blackstone_button",
                "polished_blackstone_bricks", "polished_blackstone_brick_slab", "polished_blackstone_brick_stairs",
                "polished_blackstone_brick_wall", "cracked_polished_blackstone_bricks"));
        cycles.add(List.of("basalt", "polished_basalt", "smooth_basalt"));         // C11
        cycles.add(List.of(                                                        // C12
                "quartz_block", "quartz_slab", "quartz_stairs", "chiseled_quartz_block", "quartz_pillar",
                "quartz_bricks", "smooth_quartz", "smooth_quartz_slab", "smooth_quartz_stairs"));
        cycles.add(List.of("purpur_block", "purpur_slab", "purpur_stairs", "purpur_pillar")); // C13
        cycles.add(List.of(                                                        // C14
                "end_stone", "end_stone_bricks", "end_stone_brick_slab", "end_stone_brick_stairs", "end_stone_brick_wall"));
        cycles.add(List.of(                                                        // C15
                "prismarine", "prismarine_slab", "prismarine_stairs", "prismarine_wall",
                "prismarine_bricks", "prismarine_brick_slab", "prismarine_brick_stairs",
                "dark_prismarine", "dark_prismarine_slab", "dark_prismarine_stairs"));
        cycles.add(copperCycle(""));                                               // C16
        cycles.add(copperCycle("waxed_"));                                         // C17
        cycles.add(List.of(                                                        // C18
                "resin_block", "resin_bricks", "resin_brick_slab", "resin_brick_stairs", "resin_brick_wall",
                "chiseled_resin_bricks"));

        // ---- D. 自然・その他 ----
        cycles.add(List.of(                                                        // D1
                "grass_block", "dirt", "coarse_dirt", "rooted_dirt", "podzol", "mycelium",
                "crimson_nylium", "warped_nylium"));
        List<String> ores = new ArrayList<>();                                     // D2
        for (String ore : List.of("coal", "iron", "copper", "gold", "redstone", "emerald", "lapis", "diamond")) {
            ores.add(ore + "_ore");
            ores.add("deepslate_" + ore + "_ore");
        }
        ores.addAll(List.of("nether_gold_ore", "nether_quartz_ore", "ancient_debris"));
        cycles.add(ores);
        cycles.add(List.of("raw_iron_block", "raw_copper_block", "raw_gold_block")); // D3
        cycles.add(List.of(                                                        // D4
                "coal_block", "iron_block", "gold_block", "lapis_block", "redstone_block",
                "emerald_block", "diamond_block", "netherite_block", "amethyst_block"));
        cycles.add(List.of(                                                        // D5
                "dandelion", "poppy", "blue_orchid", "allium", "azure_bluet",
                "red_tulip", "orange_tulip", "white_tulip", "pink_tulip",
                "oxeye_daisy", "cornflower", "lily_of_the_valley", "wither_rose",
                "torchflower", "open_eyeblossom", "closed_eyeblossom", "wildflowers", "cactus_flower", "pink_petals"));
        cycles.add(List.of("sunflower", "lilac", "rose_bush", "peony"));           // D6
        cycles.add(List.of("brown_mushroom", "red_mushroom", "crimson_fungus", "warped_fungus")); // D7
        cycles.add(List.of("brown_mushroom_block", "red_mushroom_block", "mushroom_stem")); // D8
        cycles.add(coralCycle("_coral_block"));                                    // D9
        cycles.add(coralCycle("_coral"));                                          // D10
        cycles.add(coralCycle("_coral_fan"));                                      // D11
        cycles.add(List.of("sponge", "wet_sponge"));                               // D12
        cycles.add(List.of("torch", "soul_torch"));                                // D13
        cycles.add(List.of("lantern", "soul_lantern"));                            // D14
        cycles.add(List.of("campfire", "soul_campfire"));                          // D15
        cycles.add(List.of("ochre_froglight", "verdant_froglight", "pearlescent_froglight")); // D16
        cycles.add(List.of("ice", "packed_ice", "blue_ice"));                      // D17
        cycles.add(List.of("snow", "snow_block"));                                 // D18
        cycles.add(List.of("sand", "red_sand", "gravel"));                         // D19
        cycles.add(List.of("netherrack", "nether_wart_block", "warped_wart_block")); // D20
        cycles.add(List.of("soul_sand", "soul_soil"));                             // D21
        cycles.add(List.of("obsidian", "crying_obsidian"));                        // D22
        cycles.add(List.of(                                                        // D23
                "moss_block", "moss_carpet", "pale_moss_block", "pale_moss_carpet", "pale_hanging_moss"));
        cycles.add(List.of("pumpkin", "carved_pumpkin", "jack_o_lantern"));        // D24
        cycles.add(List.of(                                                        // D25
                "short_grass", "tall_grass", "fern", "large_fern", "bush", "dead_bush",
                "firefly_bush", "short_dry_grass", "tall_dry_grass"));
        cycles.add(List.of(                                                        // D26
                "sculk", "sculk_vein", "sculk_catalyst", "sculk_shrieker", "sculk_sensor"));
        cycles.add(List.of("anvil", "chipped_anvil", "damaged_anvil"));            // D27

        // ---- E. 実用系（サバイバル） ----
        cycles.add(foodCycle());                                                   // E1
        cycles.add(toolCycle());                                                   // E2
        cycles.add(List.of(                                                        // E3
                "rail", "powered_rail", "detector_rail", "activator_rail",
                "minecart", "chest_minecart", "furnace_minecart", "tnt_minecart", "hopper_minecart"));
        cycles.add(List.of(                                                        // E4
                "bucket", "water_bucket", "lava_bucket", "milk_bucket", "powder_snow_bucket"));

        return cycles;
    }

    // E1: 食料（カテゴリ順: 肉→魚→主食・菓子→野菜・果物→スープ→特殊、生→焼きペア）。
    // rotten_flesh のみ有害系でも収録。その他の有害系（spider_eye, poisonous_potato,
    // pufferfish, chicken）・components 持ち（suspicious_stew 等）・chorus_fruit は除外。
    private static List<String> foodCycle() {
        List<String> ids = new ArrayList<>();
        // 肉
        ids.addAll(List.of("beef", "cooked_beef", "porkchop", "cooked_porkchop",
                "mutton", "cooked_mutton", "cooked_chicken", "rabbit", "cooked_rabbit", "rotten_flesh"));
        // 魚
        ids.addAll(List.of("cod", "cooked_cod", "salmon", "cooked_salmon", "tropical_fish"));
        // 主食・菓子
        ids.addAll(List.of("bread", "cookie", "pumpkin_pie", "cake"));
        // 野菜・果物
        ids.addAll(List.of("apple", "carrot", "potato", "baked_potato", "beetroot",
                "melon_slice", "sweet_berries", "glow_berries", "dried_kelp"));
        // スープ
        ids.addAll(List.of("mushroom_stew", "beetroot_soup", "rabbit_stew"));
        // 特殊（金系・ハチミツ）
        ids.addAll(List.of("golden_carrot", "golden_apple", "enchanted_golden_apple", "honey_bottle"));
        return ids;
    }

    // E2: 道具セット。外側=素材、内側=道具種別。
    // 所持品フィルタにより実際に持っているティアの道具だけが候補に出る
    private static List<String> toolCycle() {
        List<String> ids = new ArrayList<>();
        for (String tier : List.of("wooden", "stone", "iron", "golden", "diamond", "netherite")) {
            for (String tool : List.of("pickaxe", "shovel", "axe", "hoe")) {
                ids.add(tier + "_" + tool);
            }
        }
        return ids;
    }

    /** 原色（heads）を先頭に、外側=虹順カラバリ・内側=形状（"*" を色名に置換）で展開する。 */
    private static List<String> colorLoop(List<String> heads, String... shapes) {
        List<String> ids = new ArrayList<>(heads);
        for (String color : COLORS) {
            for (String shape : shapes) {
                ids.add(shape.replace("*", color));
            }
        }
        return ids;
    }

    // B1: 樹種ごとに log, stripped_log, wood, stripped_wood（無加工が先）
    private static List<String> logCycle() {
        List<String> ids = new ArrayList<>();
        for (String w : LOG_WOODS) {
            ids.addAll(List.of(w + "_log", "stripped_" + w + "_log", w + "_wood", "stripped_" + w + "_wood"));
        }
        ids.addAll(List.of("bamboo_block", "stripped_bamboo_block"));
        for (String w : List.of("crimson", "warped")) {
            ids.addAll(List.of(w + "_stem", "stripped_" + w + "_stem", w + "_hyphae", "stripped_" + w + "_hyphae"));
        }
        return ids;
    }

    // B2: 加工木材メガサイクル
    private static List<String> woodProductsCycle() {
        List<String> ids = new ArrayList<>();
        for (String w : WOODS) {
            for (String shape : List.of("planks", "slab", "stairs", "fence", "fence_gate",
                    "door", "trapdoor", "pressure_plate", "button", "sign", "hanging_sign")) {
                ids.add(w + "_" + shape);
            }
            if (w.equals("bamboo")) {
                ids.addAll(List.of("bamboo_mosaic", "bamboo_mosaic_slab", "bamboo_mosaic_stairs"));
            }
        }
        return ids;
    }

    // C16/C17: 外側=酸化段階、内側=形状
    private static List<String> copperCycle(String waxed) {
        List<String> ids = new ArrayList<>();
        for (String ox : List.of("", "exposed_", "weathered_", "oxidized_")) {
            ids.add(waxed + (ox.isEmpty() ? "copper_block" : ox + "copper"));
            ids.add(waxed + ox + "chiseled_copper");
            ids.add(waxed + ox + "copper_grate");
            ids.add(waxed + ox + "cut_copper");
            ids.add(waxed + ox + "cut_copper_slab");
            ids.add(waxed + ox + "cut_copper_stairs");
            ids.add(waxed + ox + "copper_bulb");
            ids.add(waxed + ox + "copper_door");
            ids.add(waxed + ox + "copper_trapdoor");
        }
        return ids;
    }

    // D9-D11: 外側=サンゴ種、内側=生死
    private static List<String> coralCycle(String suffix) {
        List<String> ids = new ArrayList<>();
        for (String kind : List.of("tube", "brain", "bubble", "fire", "horn")) {
            ids.add(kind + suffix);
            ids.add("dead_" + kind + suffix);
        }
        return ids;
    }
}
