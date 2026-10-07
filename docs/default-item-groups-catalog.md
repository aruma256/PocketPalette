# Default Item Groups カタログ（Minecraft 1.21.9）

PocketPalette が同梱する default config の全サイクルと、収録・並び順の設計方針。

サイクルの定義元は [DefaultItemGroupsCatalog.java](../fabric/src/test/java/com/github/aruma256/pocketpalette/client/palette/DefaultItemGroupsCatalog.java)。表の番号（A1〜E4）は実装のコメントと対応する。生成済み JSON は手編集せず、定義元を変更して再生成する。
JSON の再生成: `cd fabric && ./gradlew generateDefaultItemGroups`
同期・網羅性は `DefaultItemGroupsCatalogTest` が検証する（出荷 JSON とカタログの一致、レジストリとの suffix 網羅チェック）。

## 構成と並び順

- **group 1 に全サイクルを集約、group 2 は空**（ユーザーの自作用）
- 並び順: **外側ループ = カラバリ（虹順）、内側ループ = 形状**。原色（無色）があれば先頭
- 虹順: `red, orange, yellow, lime, green, cyan, light_blue, blue, purple, magenta, pink, brown, black, gray, light_gray, white`
- 形状順: `normal, slab, stairs, wall, …`（その後にファミリー固有の形状）
- 原木系・waxed 銅・glazed_terracotta は独立サイクル
- サバイバルで通常入手可能なもののみ。デフォルトのエントリはアイテム ID のみを指定する（components による限定はしない）
- 樹種順は planks の登録順に正規化: `oak, spruce, birch, jungle, acacia, cherry, dark_oak, pale_oak, mangrove, bamboo, crimson, warped`
  （注: バニラのレジストリは log 系のみ pale_oak が dark_oak より先という不整合があるため、planks 順に統一する）

表のアイテム ID は `minecraft:` を省略。`…` は同じ規則での展開、`+slab/stairs/wall` は直前の素材に対応するハーフブロック・階段・塀を表す。「数」はサイクル内のエントリ数。

## A. 色物ファミリー（虹順・原色先頭）

| # | サイクル | 内容 | 数 |
|---|---------|------|----|
| A1 | 羊毛+カーペット | `red_wool, red_carpet, orange_wool, orange_carpet, …` | 32 |
| A2 | コンクリート+パウダー | `red_concrete, red_concrete_powder, orange_concrete, …` | 32 |
| A3 | テラコッタ | `terracotta, red_terracotta, …, white_terracotta` | 17 |
| A4 | 彩釉テラコッタ（独立） | `red_glazed_terracotta, …, white_glazed_terracotta` | 16 |
| A5 | ガラス+板ガラス | `glass, glass_pane, tinted_glass, red_stained_glass, red_stained_glass_pane, …` | 35 |
| A6 | ろうそく | `candle, red_candle, …, white_candle` | 17 |
| A7 | シュルカーボックス | `shulker_box, red_shulker_box, …` | 17 |
| A8 | ベッド | `red_bed, …, white_bed` | 16 |
| A9 | 旗 | `red_banner, …, white_banner` | 16 |

- A5: `tinted_glass`（遮光ガラス）は無色のガラス・板ガラスの直後に置く。
- 不採用（色物だが建材でない）: `*_bundle`, `*_harness`, `*_dye`

## B. 木材

| # | サイクル | 内容 | 数 |
|---|---------|------|----|
| B1 | 原木系 | 樹種ごとに `log, stripped_log, wood, stripped_wood`。oak〜mangrove 9種 + `crimson/warped` (stem/hyphae) + `bamboo_block, stripped_bamboo_block` | 46 |
| B2 | 加工木材メガサイクル | 樹種ごとに `planks, slab, stairs, fence, fence_gate, door, trapdoor, pressure_plate, button, sign, hanging_sign` × 12樹種。bamboo のみ末尾に `bamboo_mosaic, bamboo_mosaic_slab, bamboo_mosaic_stairs` を追加 | 135 |
| B3 | 葉 | `oak_leaves, …, mangrove_leaves`（9種・planks順）`, azalea_leaves, flowering_azalea_leaves` | 11 |
| B4 | 苗木 | `oak_sapling, …, pale_oak_sapling, mangrove_propagule, azalea, flowering_azalea` | 11 |

- B1: 樹種ごとに `log → stripped_log → wood → stripped_wood` の順。原木と木それぞれで、樹皮付きの直後に樹皮を剥いだものを置く。
- B2: boat / chest_boat は乗り物なので不採用。

## C. 石系ファミリー（形状順: normal, slab, stairs, wall, 特殊）

| # | サイクル | 内容 | 数 |
|---|---------|------|----|
| C1 | 石 | `stone, stone_slab, stone_stairs, stone_pressure_plate, stone_button, smooth_stone, smooth_stone_slab,`<br>`granite, granite_slab, granite_stairs, granite_wall, polished_granite, polished_granite_slab, polished_granite_stairs,`<br>`diorite`（granite と同構成）`, andesite`（同） | 28 |
| C2 | 丸石 | `cobblestone, cobblestone_slab, cobblestone_stairs, cobblestone_wall, mossy_cobblestone, +slab/stairs/wall` | 8 |
| C3 | 石レンガ | `stone_bricks, stone_brick_slab, stone_brick_stairs, stone_brick_wall, chiseled_stone_bricks, cracked_stone_bricks, mossy_stone_bricks, +slab/stairs/wall` | 10 |
| C4 | 深層岩 | `deepslate, cobbled_deepslate, +slab/stairs/wall, chiseled_deepslate, polished_deepslate, +slab/stairs/wall, deepslate_bricks, +slab/stairs/wall, cracked_deepslate_bricks, deepslate_tiles, +slab/stairs/wall, cracked_deepslate_tiles` | 20 |
| C5 | 凝灰岩 | `tuff, +slab/stairs/wall, chiseled_tuff, polished_tuff, +slab/stairs/wall, tuff_bricks, +slab/stairs/wall, chiseled_tuff_bricks` | 14 |
| C6 | 砂岩 | `sandstone, sandstone_slab, sandstone_stairs, sandstone_wall, chiseled_sandstone, cut_sandstone, cut_sandstone_slab, smooth_sandstone, smooth_sandstone_slab, smooth_sandstone_stairs` + red_ 同構成 | 20 |
| C7 | レンガ | `bricks, brick_slab, brick_stairs, brick_wall` | 4 |
| C8 | 泥 | `mud, packed_mud, mud_bricks, mud_brick_slab, mud_brick_stairs, mud_brick_wall` | 6 |
| C9 | ネザーレンガ | `nether_bricks, +slab/stairs/wall, nether_brick_fence, chiseled_nether_bricks, cracked_nether_bricks, red_nether_bricks, +slab/stairs/wall` | 11 |
| C10 | ブラックストーン | `blackstone, +slab/stairs/wall, gilded_blackstone, polished_blackstone, +slab/stairs/wall, chiseled_polished_blackstone, polished_blackstone_pressure_plate, polished_blackstone_button, polished_blackstone_bricks, +slab/stairs/wall, cracked_polished_blackstone_bricks` | 17 |
| C11 | 玄武岩 | `basalt, polished_basalt, smooth_basalt` | 3 |
| C12 | クォーツ | `quartz_block, quartz_slab, quartz_stairs, chiseled_quartz_block, quartz_pillar, quartz_bricks, smooth_quartz, smooth_quartz_slab, smooth_quartz_stairs` | 9 |
| C13 | プルプァ | `purpur_block, purpur_slab, purpur_stairs, purpur_pillar` | 4 |
| C14 | エンドストーン | `end_stone, end_stone_bricks, +slab/stairs/wall` | 5 |
| C15 | プリズマリン | `prismarine, +slab/stairs/wall, prismarine_bricks, prismarine_brick_slab, prismarine_brick_stairs, dark_prismarine, dark_prismarine_slab, dark_prismarine_stairs` | 10 |
| C16 | 銅（未ワックス） | 酸化段階 `{none, exposed, weathered, oxidized}` ごとに `copper_block, chiseled_copper, copper_grate, cut_copper, cut_copper_slab, cut_copper_stairs, copper_bulb, copper_door, copper_trapdoor` | 36 |
| C17 | 銅（waxed・独立） | C16 と同構成の waxed 版 | 36 |
| C18 | 樹脂 | `resin_block, resin_bricks, resin_brick_slab, resin_brick_stairs, resin_brick_wall, chiseled_resin_bricks` | 6 |

- C1・C10: 感圧板とボタンは素材別のサイクルに含める。B2 の木材と同様に、石製は C1、磨かれたブラックストーン製は C10 に収録する。
- 壁のみ・階段のみ等の歯抜けはバニラの提供に従う（例: smooth 系に wall はない）。

## D. 自然・その他

| # | サイクル | 内容 | 数 |
|---|---------|------|----|
| D1 | 土 | `grass_block, dirt, coarse_dirt, rooted_dirt, podzol, mycelium, crimson_nylium, warped_nylium` | 8 |
| D2 | 鉱石 | `coal_ore, deepslate_coal_ore, iron_ore, deepslate_iron_ore, copper_ore, …, deepslate_diamond_ore`（登録順=交互）`, nether_gold_ore, nether_quartz_ore, ancient_debris` | 19 |
| D3 | 原石ブロック | `raw_iron_block, raw_copper_block, raw_gold_block` | 3 |
| D4 | 鉱物ブロック | `coal_block, iron_block, gold_block, lapis_block, redstone_block, emerald_block, diamond_block, netherite_block, amethyst_block` | 9 |
| D5 | 小さな花 | `dandelion, poppy, blue_orchid, allium, azure_bluet, red_tulip, orange_tulip, white_tulip, pink_tulip, oxeye_daisy, cornflower, lily_of_the_valley, wither_rose, torchflower, open_eyeblossom, closed_eyeblossom, wildflowers, cactus_flower, pink_petals` | 19 |
| D6 | 大きな花 | `sunflower, lilac, rose_bush, peony` | 4 |
| D7 | キノコ・菌類 | `brown_mushroom, red_mushroom, crimson_fungus, warped_fungus` | 4 |
| D8 | キノコブロック | `brown_mushroom_block, red_mushroom_block, mushroom_stem` | 3 |
| D9 | サンゴブロック | 種類ごとに `生, dead`: `tube_coral_block, dead_tube_coral_block, brain_coral_block, …`（外=種類、内=生死のルール適用） | 10 |
| D10 | サンゴ | 同上 | 10 |
| D11 | サンゴのうちわ | 同上 | 10 |
| D12 | スポンジ | `sponge, wet_sponge` | 2 |
| D13 | たいまつ | `torch, soul_torch, copper_torch` | 3 |
| D14 | ランタン | `lantern, soul_lantern` | 2 |
| D15 | 焚き火 | `campfire, soul_campfire` | 2 |
| D16 | フロッグライト | `ochre_froglight, verdant_froglight, pearlescent_froglight` | 3 |
| D17 | 氷 | `ice, packed_ice, blue_ice` | 3 |
| D18 | 雪 | `snow, snow_block` | 2 |
| D19 | 砂・砂利 | `sand, red_sand, gravel` | 3 |
| D20 | ネザー土壌 | `netherrack, nether_wart_block, warped_wart_block` | 3 |
| D21 | ソウル系土壌 | `soul_sand, soul_soil` | 2 |
| D22 | 黒曜石 | `obsidian, crying_obsidian` | 2 |
| D23 | 苔 | `moss_block, moss_carpet, pale_moss_block, pale_moss_carpet, pale_hanging_moss` | 5 |
| D24 | カボチャ | `pumpkin, carved_pumpkin, jack_o_lantern` | 3 |
| D25 | 草・下草 | `short_grass, tall_grass, fern, large_fern, bush, dead_bush, firefly_bush, short_dry_grass, tall_dry_grass` | 9 |
| D26 | スカルク | `sculk, sculk_vein, sculk_catalyst, sculk_shrieker, sculk_sensor` | 5 |
| D27 | 金床 | `anvil, chipped_anvil, damaged_anvil` | 3 |

## E. 実用系（サバイバル）

候補がインベントリ所持品でフィルタされる PocketPalette では、大きな「寄せ集め」サイクルでも
リングには持っている物しか出ないため、ホットバー枠の節約手段として成立する。

| # | サイクル | 内容 | 数 |
|---|---------|------|----|
| E1 | 食料 | カテゴリ順（肉→魚→主食・菓子→野菜・果物→スープ→特殊）、生→焼きペア。`rotten_flesh` は収録。その他の有害系（`spider_eye, poisonous_potato, pufferfish, chicken`）・components 持ち（`suspicious_stew` 等）・`chorus_fruit` は除外 | 35 |
| E2 | 道具セット | 素材（`wooden, stone, copper, iron, golden, diamond, netherite`）ごとに `pickaxe, shovel, axe, hoe`。エンチャ・耐久値は base-item フォールバック + スタック丸ごとスワップで保持される | 28 |
| E3 | レール・トロッコ | `rail, powered_rail, detector_rail, activator_rail, minecart, chest_minecart, furnace_minecart, tnt_minecart, hopper_minecart` | 9 |
| E4 | バケツ | `bucket, water_bucket, lava_bucket, milk_bucket, powder_snow_bucket` | 5 |

- E3: `command_block_minecart` はサバイバル入手不可のため除外。
- E4: 生物入りバケツは生物運搬という別用途のため除外。

## Minecraft 1.21.9 の追加方針

- 採用: `copper_torch` を D13 に、銅の `pickaxe, shovel, axe, hoe` を E2 の石と鉄の間に追加する。
- 今回は見送り: 銅の鉄格子・鎖・ランタン・銅ゴーレム像（酸化段階・waxed 版を含む）。C16/C17 と D14 の構成は維持する。
- 棚・銅チェスト・避雷針の酸化バリエーションも今回の採用対象外。武器・防具・スポーンエッグ・銅塊は既存方針に従い収録しない。
- 銅ランタン8種は、カタログ網羅性テストの `_lantern` の意図的な除外に明記する。

## 不採用の方針

- **機能ブロック**（作業台・かまど・チェスト・ピストン・ホッパー等）: 建材サイクルの対象外。必要な人は group 2 に自作
- **武器・防具・ポーション・スポーンエッグ・音楽ディスク・スケルトンの頭等**: 対象外（道具・食料・レール系は E で採用）
- **サバイバル入手不可/非正規**: `petrified_oak_slab`, `infested_*`, `reinforced_deepslate`, `budding_amethyst`, `suspicious_sand/gravel`, `bedrock`, `spawner`, `trial_spawner`, `vault`, `command_block` 系, `barrier`, `light`, `structure_*`, `jigsaw`, `test_*`, `farmland`, `dirt_path`
- **単品でサイクルにならないもの**: `dragon_egg`, `calcite`, `dripstone_block`, `magma_block`, `glowstone`, `sea_lantern`, `shroomlight`, `bone_block`, `hay_block` 等。光源や収納といった用途だけで異素材をまとめる建材サイクルは設けない。
