# Project Overview

PocketPalette is a **client-side only** Minecraft mod that brings LotTweaks' Item Palette feature to **Survival mode**. Players hold a key (default: R) and scroll to cycle the selected hotbar item through configurable item groups. Because Survival does not allow overwriting inventory slots, cycling is implemented as **inventory swaps** (SWAP container clicks) — only items that actually exist in the player's inventory are offered as candidates.

The repository は mod loader ごとにサブディレクトリへ実装を分ける構成（LotTweaks と同じ流儀）:
- `fabric/` - Fabric mod loader (Minecraft 1.21.11) **← 現在の主な作業対象**

移植元は LotTweaks (`../1.21.8_fabric`)。LotTweaks は Creative 専用・server 連携ありだが、PocketPalette は Palette 機能のみを抜き出し、サーバーへのインストール不要（`fabric.mod.json` の `environment: "client"`）。

# Development Environment

This is a Windows native environment. Run Git and Gradle directly:
```cmd
cd fabric
.\gradlew build
git status
```

# Build Commands

```bash
cd fabric
./gradlew build      # Full build
./gradlew test       # Run tests
./gradlew jar        # Build JAR only
./gradlew runClient  # Launch dev client
./gradlew generateDefaultItemGroups  # default-item-groups.json を再生成
```

Default config（`assets/pocketpalette/default-item-groups.json`）は手編集禁止。
`src/test/.../palette/DefaultItemGroupsCatalog.java` が正で、上記タスクで JSON に焼き込む
（設計: `docs/default-item-groups-catalog.md`）。カタログと JSON がズレると
`DefaultItemGroupsCatalogTest` が落ちる。

Fabric uses Java 21（Minecraft 1.21.11 の要件）、official Mojang mappings を使用。
Loom 1.18 で Gradle を実行するには JDK 25 以上が必要。Mod のコンパイル対象は Java 21 のまま。

# Minecraft バージョンアップ手順

1. 対象バージョンのブランチ `mc<version>`（例: `mc1.21.8`）を作成する。
2. Fabric 公式の開発ページ・API・Maven メタデータで対応バージョンを確認し、`fabric/gradle.properties`、`fabric.mod.json` などの依存関係・バージョン表記を更新する。必要に応じて Loom / Gradle / Java も更新する。
3. `fabric` で `.\gradlew build` を実行し、API・Mixin などの変更に追従してビルドを通す。
4. 新ブロック・アイテムの追加候補とグループ分けをユーザーに確認する。合意後に `DefaultItemGroupsCatalog.java` を更新し、`.\gradlew generateDefaultItemGroups` で default config を再生成する。
5. `.\gradlew test` と `.\gradlew build` を通す。
6. ビルド済み JAR を案内してユーザーに動作確認を依頼し、問題がなければ完了とする。

# Testing

```bash
cd fabric
./gradlew test
```

Tests use JUnit Jupiter with Minecraft Bootstrap for game environment initialization (`MinecraftBootstrapExtension`).

# Architecture

## Fabric Source Structure (split environment)
- `src/main/` - Shared constants only (package: `com.github.aruma256.pocketpalette`)
  - `PocketPalette.java` - MODID / LOGGER / CONFIG constants. **No server entry point** (client-only mod)
- `src/client/` - All mod logic (package: `com.github.aruma256.pocketpalette.client`)
  - `PocketPaletteClient.java` - Client entry point (`ClientModInitializer`), keybinding registration, config reload on world join, direct scroll/render callbacks from mixins to `PaletteKey`
  - `PocketPaletteCommand.java` - `/pocketpalette reload`
  - `gui/` - In-game palette editor
    - `PaletteEditorOpener.java` - Opens the editor when the palette key is pressed while hovering an item in a container screen (REI/JEI-style hover + key)
    - `PaletteEditScreen.java` - Editor screen: cycle list (reorder ↑↓ / delete ✕, group 1/2 toggle) on top, player inventory below (click = append base item). Auto-saves on close; cycles with <2 items are discarded
  - `keybinding/` - Key handling
    - `KeyBase.java` - Base class with press/release/double-tap detection
    - `PaletteKey.java` - R key: hold + scroll to cycle items; candidates are filtered to items present in the inventory
    - `InventorySwapper.java` - **Survival-mode core**: finds a matching inventory slot and swaps it with the selected hotbar slot via `gameMode.handleInventoryMouseClick(..., ClickType.SWAP, ...)`
  - `palette/` - Item group cycling system (ported from LotTweaks, component-aware):
    - `ItemPalette.java` - Main palette logic with 2-stage lookup (exact component match → fallback to base item)
    - `ItemState.java` - Component-aware ItemStack wrapper (HashMap key compatible)
    - `ItemGroupsConfigLoader.java` - JSON config read/write (`config/PocketPalette-ItemGroups.json`)
    - `PaletteConfigManager.java` - Config lifecycle management
    - `PaletteEditModel.java` - GUI-independent editing session for the palette editor (cycle resolution, add/reorder/remove, commit rules)
    - `ItemValidation.java` - Item/stack validity checks
  - `mixin/` - `MouseScrollRedirector` (intercept scroll), `HotbarRendererHook` (candidate ring rendering hook), `AbstractContainerScreenAccessor` (expose `hoveredSlot` for the editor opener)
  - `render/` - `ItemStackRenderer` (circular candidate display above hotbar)
- `src/test/` - JUnit tests for the palette package

## Key Design Decisions
- **Survival swap instead of Creative overwrite**: LotTweaks uses `handleCreativeModeItemAdd` (Creative only). PocketPalette sends a SWAP container click on the player's `InventoryMenu`, which the server executes — works in Survival and Creative, no server-side mod needed. Swaps are skipped while another container is open (`player.containerMenu != player.inventoryMenu`).
- **Candidates are inventory-limited**: on key press, `ItemPalette` resolves the configured cycle (exact component match first, then base-item fallback), and `InventorySwapper.findSlotsMatchingPaletteEntry` finds physical stacks. A componentless config entry matches every stack of the same base item; an entry with components requires an exact match. See `docs/palette-component-matching.md`.
- **Config format**: same JSON schema as LotTweaks latest (`config_version: 1`, `groups` → cycles → `{id, components}`). No legacy text migration (new mod), so LotTweaks' `ConfigMigrator` / `ItemGroupParser` / `PaletteGroup` were intentionally not ported.
