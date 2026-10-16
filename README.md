# Chronicle – das automatische Abenteuer-Tagebuch

Ein **reiner Client-Mod** für Fabric (Minecraft **1.21.8 bis 26.3**). Chronicle schreibt im Hintergrund mit, was du erlebst, und bringt dich per Kompass zu jedem Eintrag zurück. Der Server braucht nichts, der Mod funktioniert auch auf Vanilla-, Paper- und Spigot-Servern.

## Was wird erfasst?

| Kategorie | Wann |
|---|---|
| 🌿 Biome | erstes Betreten jedes Bioms (mit passendem Icon) |
| 🌀 Dimensionen | erster Besuch in Nether, End oder Mod-Dimensionen |
| 💀 Tode | Position, Todesnachricht, Level und Überlebenszeit |
| 💎 Funde | erstes Mal seltene Items im Inventar (Diamant, Netherit, Elytra …) |
| ⭐ Meilensteine | Level 10/20/30/50/100, unter Y=0, Grundgestein, über Y=256, 1k/5k/10k/25k Blöcke vom Start, Nether-Dach |
| 📝 Notizen | eigene Notizen an deiner aktuellen Position |

Außerdem:
- **Kompass im HUD**: Setze einen Eintrag als *Ziel*. Oben im Bild erscheinen Richtung, Entfernung und Höhenunterschied. Zwischen Oberwelt und Nether wird automatisch ×8 umgerechnet. Am Ziel verschwindet der Kompass von selbst.
- **Nether-Koordinaten** in den Details jedes Eintrags.
- **Anpinnen, Suchen, Filtern** und Koordinaten kopieren.
- Ein Tagebuch **pro Welt bzw. Server**, gespeichert in `.minecraft/chronicle/*.json`.

Tasten: **J** öffnet die Chronik. „Schnellnotiz“ ist frei belegbar (Steuerung → Chronicle). In der Modern-UI gibt es außerdem **Strg+F** (Suche) und **Strg+N** (neue Notiz).

## Zwei Oberflächen

Umschalten geht oben rechts in der Chronik oder in den Optionen.

- **Vanilla**: ausschließlich Minecraft-eigene Komponenten: `Button`, `EditBox`, `MultiLineEditBox`, `CycleButton`, `ObjectSelectionList`, `ConfirmScreen` und System-Toasts. Sieht aus wie ein Menü aus dem Spiel selbst.
- **Modern**: ausschließlich eigene Komponenten (`ui/modern/widget`): abgerundete Panels mit geglätteten Ecken, aufklappbare Karten, animierte Schalter, Dialoge, Textfelder mit Markierung und Zwischenablage, eingleitende HUD-Benachrichtigungen und eine Kompass-Pille mit drehendem Pfeil.

## Bauen

Voraussetzung: JDK 25 (baut auch die Java-21-Versionen).

```bash
./gradlew build
```

Die fertigen Jars landen in `build/libs/`:

| Jar | läuft auf |
|---|---|
| `chronicle-1.0.0+mc1.21.8.jar` | 1.21.8 |
| `chronicle-1.0.0+mc1.21.10.jar` | 1.21.9 – 1.21.10 |
| `chronicle-1.0.0+mc1.21.11.jar` | 1.21.11 |
| `chronicle-1.0.0+mc26.1.2.jar` | 26.1 – 26.1.2 |
| `chronicle-1.0.0+mc26.2.jar` | 26.2 |
| `chronicle-1.0.0+mc26.3.jar` | 26.3 |

Benötigt wird jeweils **Fabric Loader ≥ 0.19.5** und die passende **Fabric API**.

Zum Ausprobieren: `./gradlew :26.3:runClient` (bzw. eine andere Version).
Automatischer Screenshot-Test im echten Client: `./gradlew :26.3:runClientGameTest`. Die Bilder landen in `versions/26.3/build/run/clientGameTest/screenshots/`.

## Projektaufbau

```
src/main/java             gemeinsamer Code für alle Versionen
  journal/                Datenmodell, Erkennung (EventTracker), Speichern, Navigation
  hud/                    Kompass + Benachrichtigungen (Vanilla & Modern)
  ui/vanilla/             Vanilla-UI
  ui/modern/              Modern-UI + eigene Komponenten in widget/
src/compat/<facette>/     versionsabhängige Brücken (siehe unten)
versions/<mc>/            pro Minecraft-Version nur gradle.properties
src/gametest/             Screenshot-Test
```

Mojang hat zwischen 1.21.8 und 26.3 einiges umgebaut. Die Unterschiede stecken in kleinen Klassen unter `src/compat`, alles andere ist für alle Versionen derselbe Code:

| Ordner | Versionen | Warum |
|---|---|---|
| `gg` / `gge` | bis 1.21.11 / ab 26.1 | `GuiGraphics` → `GuiGraphicsExtractor`, `render` → `extractRenderState` |
| `input_legacy` / `input_events` | 1.21.8 / ab 1.21.9 | Maus/Tastatur als Event-Records |
| `list_content` | 1.21.9 – 1.21.11 | neue Listen-Zeilen-API |
| `v1_21_8` … `v26_2` | je Version | `ResourceLocation` → `Identifier`, Keybind-Kategorien, `KeyBindingHelper` → `KeyMappingHelper`, Screen-Verwaltung über `gui` (ab 26.2) |

Tastencodes kommen immer aus `InputConstants`. Ab 26.3 nutzt Minecraft SDL3 statt GLFW, die Werte sind dort andere.

Eine neue Minecraft-Version kommt so dazu: Ordner `versions/<mc>/gradle.properties` anlegen, in `settings.gradle` eintragen, passende `compat`-Ordner wählen.
