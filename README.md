# PocketPalette

A **client-side only** Fabric mod that lets builders cycle through related items in the hotbar — **even in Survival mode**.

Hold the Palette key (default: <kbd>R</kbd>) and scroll to swap the selected hotbar item with related items from your inventory (e.g. stone → granite → diorite → andesite). Since Survival mode doesn't allow creating items, PocketPalette works by **swapping items within your inventory** — only items you actually have are offered.

This mod is a Survival-focused spin-off of the Palette feature from [LotTweaks](https://github.com/aruma256/LotTweaks).

## Features

- **Item Palette in Survival**: hold <kbd>R</kbd> + scroll to cycle the selected hotbar slot through item groups, using inventory swaps (no server-side installation required — works on vanilla servers)
- **Component-aware groups**: enchanted / renamed items can be registered as distinct entries
- **JSON config**: `config/PocketPalette-ItemGroups.json` (same format as LotTweaks)
- **In-game group registration**: line up items on your hotbar and run `/pocketpalette add 1` (or `add 2`)
- `/pocketpalette reload` to reload the config file

## Requirements

- Minecraft 1.21.8
- Fabric Loader + Fabric API
- Java 21+

## License

This mod is licensed under the [GNU General Public License v3.0](fabric/LICENSE).

## Development

```bash
cd fabric
./gradlew build      # Full build
./gradlew test       # Run tests
./gradlew runClient  # Launch dev client
```
