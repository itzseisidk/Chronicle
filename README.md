# Chronicle

A **client-side** Fabric mod for Minecraft **1.21.8 to 26.3**. Chronicle keeps a journal of what happens while you play and can point you back to any entry with a compass. Nothing is needed on the server, so it also works on vanilla, Paper and Spigot servers.

![Modern UI](screenshot-modern.png)

## What gets logged

| Category | When |
|---|---|
| Biomes | first time you enter each biome (with a fitting icon) |
| Dimensions | first visit to the Nether, the End or a modded dimension |
| Deaths | position, death message, level and how long you survived |
| Finds | first time a rare item is in your inventory (diamond, netherite, elytra, ...) |
| Milestones | level 10/20/30/50/100, below Y=0, bedrock, above Y=256, 1k/5k/10k/25k blocks from spawn, Nether roof |
| Notes | your own notes at your current position |

Also:
- **HUD compass**: set an entry as your *target* and the direction, distance and height difference show up at the top of the screen. Overworld and Nether coordinates are converted (×8) automatically. The compass goes away once you reach the target.
- **Nether coordinates** in the details of every entry.
- **Pin, search, filter** and copy coordinates.
- One journal **per world or server**, saved in `.minecraft/chronicle/*.json`.

Keys: **J** opens the journal. "Quick note" has no key by default (Controls → Chronicle). The Modern UI also has **Ctrl+F** (search) and **Ctrl+N** (new note).

## Two UIs

Switch in the top right of the journal or in the settings.

- **Vanilla**: only Minecraft's own components: `Button`, `EditBox`, `MultiLineEditBox`, `CycleButton`, `ObjectSelectionList`, `ConfirmScreen` and system toasts. Looks like a menu from the game itself.
- **Modern**: only custom components (`ui/modern/widget`): rounded panels with smooth corners, expandable cards, animated toggles, dialogs, text fields with selection and clipboard, sliding HUD notifications and a compass pill with a rotating arrow.

![Vanilla UI](screenshot-vanilla.png)
