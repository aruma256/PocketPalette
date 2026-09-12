# PocketPalette

A **client-side only** Fabric mod that lets builders cycle through related items in the hotbar — **even in Survival mode**. No server-side installation required — works on vanilla servers.

Hold the Palette key (default: <kbd>R</kbd>) and scroll to swap the selected hotbar item for a related item (e.g. stone → granite → diorite → andesite). Only items you actually have in your inventory will show up as options.

While holding the key, changing the selected hotbar slot or inventory contents
automatically rebuilds the palette from the current inventory, keeping the current
group. This also covers item changes sent by the server. Palette swaps themselves
preserve the cycling order.

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
./gradlew runClientGameTest  # Launch an isolated client and test Survival palette swaps
```

CI runs on every branch push and pull request. The client game test creates a
temporary singleplayer world, holds R and scrolls in both directions, and checks
the resulting inventories on both the client and integrated server, including
stack counts and custom names. It also checks that normal hotbar scrolling resumes
after R is released. Test code is kept in `fabric/src/gametest` and is not included
in the published mod JAR.

The test also checks number-key selection while R is held, server-side item
removal/addition and name/count changes, and stable cycling of multiple stacks.

On Linux without a display, run `xvfb-run -a ./gradlew runClientGameTest` with
Xvfb and Mesa installed. CI uploads test reports, client logs and screenshots as
the `test-reports` artifact. The client test uses `fabric/build/run/clientGameTest`
instead of the normal development world's run directory.
