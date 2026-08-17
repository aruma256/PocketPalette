# PocketPalette

A **client-side only** Fabric mod that lets builders cycle through related items in the hotbar — **even in Survival mode**. No server-side installation required — works on vanilla servers.

Hold the Palette key (default: <kbd>R</kbd>) and scroll to swap the selected hotbar item for a related item (e.g. stone → granite → diorite → andesite). Only items you actually have in your inventory will show up as options.

This mod is a Survival-focused spin-off of the Palette feature from [LotTweaks](https://github.com/aruma256/LotTweaks).

## Features

- **Item Palette in Survival**: hold <kbd>R</kbd> + scroll to cycle the selected hotbar slot through item groups
- **Component-aware groups**: enchanted / renamed items can be registered as distinct entries
- **JSON config**: `config/PocketPalette-ItemGroups.json` (same format as LotTweaks)
- **In-game palette editor**: hover an item in any container screen and press the Palette key (<kbd>R</kbd>) to edit its item group
- `/pocketpalette reload` to reload the config file

## Requirements

- Minecraft 1.21.8
- Fabric Loader + Fabric API
- Java 21+

## License

This mod is licensed under the [GNU General Public License v3.0](LICENSE).

## Development

```bash
cd fabric
./gradlew build      # Full build
./gradlew test       # Run tests
./gradlew runClient  # Launch dev client
```
