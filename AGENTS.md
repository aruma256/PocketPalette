# Project Overview

PocketPalette is a **client-side only** Minecraft mod that brings LotTweaks' Item Palette feature to **Survival mode**. Players hold a key (default: R) and scroll to cycle the selected hotbar item through configurable item groups. Because Survival does not allow overwriting inventory slots, cycling is implemented as **inventory swaps** (SWAP container clicks) — only items that actually exist in the player's inventory are offered as candidates.

Fabric と NeoForge の 2 つの mod loader に対応している。リポジトリは共通層と loader 層に分かれている:
- `common/` - 共通層。単体の Gradle プロジェクトではなく、各 loader の `build.gradle` が `srcDir` で参照してコンパイルする
- `fabric/` - Fabric 層 (Minecraft 26.2)
- `neoforge/` - NeoForge 層 (Minecraft 26.2, NeoForge 26.2.0.x)

移植元は LotTweaks (`../1.21.8_fabric`)。LotTweaks は Creative 専用・server 連携ありだが、PocketPalette は Palette 機能のみを抜き出し、サーバーへのインストール不要。

## 層の分け方

- **依存は loader 層 → 共通層の一方向**。`common/` からは Minecraft（vanilla）の API と Brigadier・GLFW などだけを使い、loader の API や loader 層のクラスを参照しない。NeoForge が vanilla クラスに追加したメソッド（`getHoveredSlot()` など）も loader の API に含む。
- **loader 層の仕事は変換だけ**。loader のイベントやフックを受けて `PocketPaletteClient` を呼ぶ。loader ごとの差は loader 層で吸収し、必要な値（hovered slot など）は引数で渡す。共通層に loader 向けのインターフェースは置かない。
- **loader 層のクラスは `...pocketpalette.fabric` / `...pocketpalette.neoforge` パッケージに置く**。それ以外のパッケージは `common/` だけが持つ。

共通層の変更は両方の loader に効くので、**どの変更でも両方でビルドとテストを通す**。loader 層の API を共通層から誤って使うと、もう片方のコンパイルで検出される。

何がどの層にあるかは [Architecture](#architecture) を参照。

# Development Environment

This is a Windows native environment. Run Git and Gradle directly:
```cmd
cd fabric
.\gradlew build
git status
```
（`neoforge/` も同様）

# Build Commands

`fabric/` と `neoforge/` のどちらでも同じタスク名で動く:
```bash
./gradlew build      # Full build
./gradlew test       # Run tests
./gradlew jar        # Build JAR only
./gradlew runClient  # Launch dev client
./gradlew generateDefaultItemGroups  # default-item-groups.json を再生成
```

どちらも Java 25（Minecraft 26.2 の要件）を使用。26.1 以降は難読化が廃止され、mappings 指定は不要。
Fabric は Loom 1.18 の `net.fabricmc.fabric-loom` プラグインを使用し、Gradle の実行・Mod のコンパイルともに JDK 25 以上が必要。

Default config（`common/src/main/resources/assets/pocketpalette/default-item-groups.json`）は手編集禁止。
`common/src/test/.../palette/DefaultItemGroupsCatalog.java` が正で、上記タスクで JSON に焼き込む
（設計: `docs/default-item-groups-catalog.md`）。カタログと JSON がズレると
`DefaultItemGroupsCatalogTest` が落ちる。カタログも JSON も 1 つなので、再生成はどちらかの loader で 1 回実行すればよい。

`DefaultItemGroupsGenerator` は JUnit テストとして動き（素の `main` では NeoForge の `Bootstrap` が FML 未初期化で落ちるため）、`generateDefaultItemGroups` が target パスをシステムプロパティで渡す。通常の `test` では skip されるので、skipped 1 件は正常。

# Minecraft バージョンアップ手順

1. 対象バージョンのブランチ `mc<version>`（例: `mc1.21.8`）を作成する。
2. 各 loader の公式情報で対応バージョンを確認し、依存関係・バージョン表記を更新する。
   - Fabric: 公式の開発ページ・API・Maven メタデータを確認し、`fabric/gradle.properties`、`fabric.mod.json` などを更新する。必要に応じて Loom / Gradle / Java も更新する。
   - NeoForge: 公式の Maven メタデータ・MDK を確認し、`neoforge/gradle.properties`（`neo_version`、`minecraft_version_range`、`archives_base_name` など）を更新する。必要に応じて ModDevGradle / Gradle / Java も更新する。
3. `fabric` と `neoforge` でそれぞれ `.\gradlew build` を実行し、API・Mixin・イベントなどの変更に追従してビルドを通す。
4. 新ブロック・アイテムの追加候補とグループ分けをユーザーに確認する。合意後に `common/` の `DefaultItemGroupsCatalog.java` を更新し、`.\gradlew generateDefaultItemGroups` で default config を再生成する。
5. 両方で `.\gradlew test` と `.\gradlew build` を通す。
6. 両方のビルド済み JAR を案内してユーザーに動作確認を依頼し、問題がなければ完了とする。

# Testing

```bash
./gradlew test   # fabric/ と neoforge/ のそれぞれで実行
```

Tests use JUnit Jupiter with Minecraft Bootstrap for game environment initialization (`MinecraftBootstrapExtension`). テストコードは `common/src/test/` にあり、各 loader のテスト環境で実行される。loader ごとのテスト環境の違いは各 loader の節を参照。

実際のクライアントを起動する Client game test（`./gradlew runClientGameTest`）は loader ごとにある。シナリオは揃えているが、テスト基盤が違うのでコードは共有していない。挙動を変えたら両方を更新する。

# Architecture

## 共通層（`common/`）

`src/main/java`、`src/main/resources`、`src/test/java` を持つ。

`com.github.aruma256.pocketpalette`:
- `PocketPalette.java` - MODID / LOGGER / CONFIG constants

`com.github.aruma256.pocketpalette.client`:
- `PocketPaletteClient.java` - loader 層からの入口。キー、tick、スクロール、描画、コンテナ画面の入力、ワールド参加時の config 読み込みを受ける
- `PocketPaletteCommand.java` - `/pocketpalette reload`。loader ごとに異なる command source に合わせて generic にしてある
- `compat/LotTweaksCompat.java` - Creative で LotTweaks の Palette キーと同じキーなら LotTweaks に譲る。登録済みの `key.lottweaks.palette` を見るので loader の API は使わない
- `gui/` - In-game palette editor
  - `PaletteEditorOpener.java` - Opens the editor when the palette key is pressed while hovering an item in a container screen (REI/JEI-style hover + key)
  - `PaletteEditScreen.java` - Editor screen: cycle list (reorder ↑↓ / delete ✕, group 1/2 toggle) on top, player inventory below (click = append base item). Auto-saves on close; cycles with <2 items are discarded
- `keybinding/` - Key handling
  - `KeyBase.java` - Base class with press/release/double-tap detection
  - `PaletteKey.java` - R key: hold + scroll to cycle items; candidates are filtered to items present in the inventory
  - `PaletteSession.java` - Candidate ring and inventory tracking for one held-key session; inventory reads and swaps are supplied by the caller
  - `InventorySwapper.java` - **Survival-mode core**: finds a matching inventory slot and swaps it with the selected hotbar slot via `gameMode.handleContainerInput(..., ContainerInput.SWAP, ...)`
- `palette/` - Item group cycling system (ported from LotTweaks, component-aware):
  - `ItemPalette.java` - Main palette logic with 2-stage lookup (exact component match → fallback to base item)
  - `ItemState.java` - Component-aware ItemStack wrapper (HashMap key compatible)
  - `ItemGroupsConfigLoader.java` - JSON config read/write (`config/PocketPalette-ItemGroups.json`)
  - `PaletteConfigManager.java` - Config lifecycle management
  - `PaletteEditModel.java` - GUI-independent editing session for the palette editor (cycle resolution, add/reorder/remove, commit rules)
  - `ItemValidation.java` - Item/stack validity checks
- `render/` - `ItemStackRenderer` (circular candidate display above hotbar)

Resources（`assets/pocketpalette/` の `default-item-groups.json`、`lang/`、`icon.png`）も `common/` にある。

### Key Design Decisions
- **Survival swap instead of Creative overwrite**: LotTweaks uses `handleCreativeModeItemAdd` (Creative only). PocketPalette sends a SWAP container click on the player's `InventoryMenu`, which the server executes — works in Survival and Creative, no server-side mod needed. Swaps are skipped while another container is open (`player.containerMenu != player.inventoryMenu`).
- **Candidates are inventory-limited**: on key press, `ItemPalette` resolves the configured cycle (exact component match first, then base-item fallback), and `InventorySwapper.findMatchingSlots` finds physical stacks. A componentless config entry matches every stack of the same base item; an entry with components requires an exact match. See `docs/palette-component-matching.md`.
- **Text input takes priority over the editor key**: there is no API for "is the user typing", so the editor opens only for keys that reach `AbstractContainerScreen.keyPressed` unhandled. Container screens with text fields must consume typed keys before that point (otherwise E would close them while typing). Fabric hooks that point with `ContainerKeyHook`, NeoForge uses `ScreenEvent.KeyPressed.Post`. A key that also has a container action (Q, E, number keys) opens the editor on Fabric but keeps the vanilla action on NeoForge. Mouse bindings skip this, since buttons do not type.
- **Config format**: same JSON schema as LotTweaks latest (`config_version: 1`, `groups` → cycles → `{id, components}`). No legacy text migration (new mod), so LotTweaks' `ConfigMigrator` / `ItemGroupParser` / `PaletteGroup` were intentionally not ported.

### LotTweaks からの移植時の注意（26.2 API）
LotTweaks HEAD はより新しい Minecraft 26.x API を使用しているため、コードを持ち込む際は以下の読み替えが必要:
- `KeyMapping.Category` は `Identifier` で一度だけ登録する。登録は loader 層で行う（Fabric: `KeyMapping.Category.register`、NeoForge: `RegisterKeyMappingsEvent.registerCategory`。NeoForge では前者は非推奨）
- キー・マウス入力は `KeyEvent` / `MouseButtonEvent` を使用。`mouseClicked` は追加で `boolean doubleClick` を受け取る

LotTweaks は Fabric 実装なので、NeoForge では Fabric API の呼び出しを次節のイベントに置き換える。

## Fabric 層（`fabric/`）

- **Source structure**: split environment。`common/` は Minecraft のクライアントクラスを使うので `client` ソースセットに `srcDir` で取り込む。`src/main/` は `fabric.mod.json` だけで、クラスを持たない（このため dev 起動時に `build/classes/java/main` が無いという Loader の警告が出るが、実害はない）。`src/client/` は下記の loader 層のクラスと mixin 設定を持つ。
- **Client-only**: `fabric.mod.json` の `environment: "client"`。エントリは `fabric/PocketPaletteFabricClient`（`ClientModInitializer`）。Fabric 層のクラスには `@Environment(EnvType.CLIENT)` を付ける。共通層には付けない。
- **Mixin**（`fabric/mixin/`）:
  - `MouseScrollRedirector` - スクロール横取り
  - `HotbarRendererHook` - 候補リング描画のフック
  - `AbstractContainerScreenAccessor` - エディタ起動用に `hoveredSlot` を公開
  - `ContainerKeyHook` - `AbstractContainerScreen.keyPressed` 内の `super.keyPressed` を包み、未処理のキーでエディタを開く。vanilla は 1.21.10 まで未処理でも `true` を返し、Fabric API の `afterKeyPress` も無条件に発火するので、イベントでは「画面が処理しなかったキー」を判定できない
- **Fabric API**: `ClientTickEvents`、`KeyMappingHelper`、`ClientPlayConnectionEvents.JOIN`、`ClientCommandRegistrationCallback`、`ScreenEvents` / `ScreenMouseEvents`。
- **Test**: 上記の JUnit に加え、Client game test（`src/gametest/` の `PaletteClientGameTest`、`./gradlew runClientGameTest`）がある。

## NeoForge 層（`neoforge/`）

- **Source structure**: `src/main/` のみ（split なし）。`common/` は `main` ソースセットに `srcDir` で取り込む。`neoforge.mods.toml` は `src/main/templates/META-INF/` に置き、`generateModMetadata` で展開する。Gradle plugin は ModDevGradle（`net.neoforged.moddev`）で、Java 25 は Gradle toolchain で解決される。
- **Client-only**: `neoforge/PocketPaletteNeoForgeClient` を `@Mod(dist = Dist.CLIENT)` のエントリにする。
- **Mixin は不使用**。NeoForge のイベントと GUI レイヤーで実現している:
  - `InputEvent.MouseScrollingEvent` - スクロール横取り。Spectator では何もしない。
  - `RegisterGuiLayersEvent.registerAbove(VanillaGuiLayers.HOTBAR, ...)` - 候補リング描画。追加レイヤーには vanilla の表示条件が付かないので、`gui.hud.isHidden()` を自前で確認する。
  - `ScreenEvent.KeyPressed.Post` / `MouseButtonPressed.Pre` + `getHoveredSlot()` - エディタ起動。
  - `ClientTickEvent.Post`、`RegisterKeyMappingsEvent`、`RegisterClientCommandsEvent`、`ClientPlayerNetworkEvent.LoggingIn`。
- **Test**: ModDevGradle の `unitTest` により NeoForge 環境で実行する。Client game test は `src/clientGameTest/` の `PaletteClientGameTest`（`./gradlew runClientGameTest`）で、別 mod（`pocketpalette_test`）として読み込む。NeoForge にはクライアント用のテスト API が無いので、`ClientGameTestContext` がテスト用スレッドからクライアントを操作する。Fabric と違いゲームは待ち時間の間も進むので、結果は `waitFor` で待つ。実行中はマウスを掴むことがあるが、実際の入力は無視する。`generateDefaultItemGroups` は `test` タスクの設定を引き継いだ `Test` タスクにする。
