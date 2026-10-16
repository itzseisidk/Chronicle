# Chronicle

Chronicle is a journal that writes itself. While you play, it keeps track of the places you find, the things you pick up and the times you die, and it can point you back to any of them with a compass on your HUD.

It's client-side only. Nothing has to be installed on the server, so it works on vanilla, Paper and Spigot servers and in singleplayer.

## What gets logged

| | |
|---|---|
| **Biomes** | the first time you enter each biome |
| **Dimensions** | your first trip to the Nether, the End or a modded dimension |
| **Deaths** | where you died, the death message, your level and how long you survived |
| **Finds** | the first time a rare item lands in your inventory (diamonds, netherite, elytra, ...) |
| **Milestones** | level 10/20/30/50/100, going below Y=0, reaching bedrock, building above Y=256, 1k/5k/10k/25k blocks from spawn, the Nether roof |
| **Notes** | your own notes, saved at your current position |

Every entry stores the coordinates, the dimension and the time. Anything you already had when you first joined a world is skipped, so you don't get spammed with 30 entries on your first tick.

## Find your way back

Set any entry as your **target** and a small compass shows up at the top of the screen with the direction, the distance and how far up or down it is. When you're in the Nether and the target is in the Overworld (or the other way round), the coordinates are converted with the factor 8 automatically. Once you get there, the target clears itself.

Entries also show the matching Nether/Overworld coordinates, so building portals gets a bit easier.

## Two styles

You can switch between them at any time, right in the journal or in the settings.

- **Vanilla** uses only Minecraft's own buttons, lists and text fields, plus the normal toasts. It looks like a menu that came with the game.
- **Modern** is a custom UI with rounded panels, cards that expand on click, a filter sidebar, animated notifications and a compass pill with a rotating arrow.

## Controls

- **J** opens the journal
- **Quick note** opens a new note directly. It has no key by default, set one under Controls → Chronicle
- In the Modern UI: **Ctrl+F** to search, **Ctrl+N** for a new note

In the journal you can pin entries, search, filter by category and copy coordinates to the clipboard.

## Settings

Notifications, the HUD compass and UI sounds can each be turned off, and every category can be excluded from tracking.

## Good to know

- Each world and each server gets its own journal, stored in `.minecraft/chronicle/`.
- Languages: English and German.
- Requires **Fabric API**.
- Supports Minecraft **1.21.8 to 26.3**. Make sure you download the file that matches your version.
