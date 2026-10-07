# dndClasses

A Minecraft **Forge 1.20.1** mod that brings D&D 5e-style classes, races, leveling and
features into the game, with the UI built entirely in HTML/CSS/JS through
[ApricityUI](https://github.com/Tower-of-Sighs/AUI) (AUI).

## Features

- **Character creation** (AUI screen): 12 classes, race/subrace, 27-point ability buy,
  subclass choice, starting equipment.
- **Character sheet** (AUI screen): class/subclass, level, XP progress and gained features.
- **XP & leveling**: XP from hostile kills, D&D XP curve (1–20), proficiency bonus.
- **Data-driven class features**: definitions in datapack JSON, applied through pluggable
  effect handlers (e.g. attribute modifiers, active resource features).
- **Spellcasting progression**: datapack tables per class/level (slots per ring 1–9 +
  known spells/cantrips).
- **Client capability sync**: `level / ability / spell` capabilities are serialized to the
  client so other mods can read them normally through `player.getCapability(...)`.

## Requirements

- Minecraft 1.20.1 + Forge 47.x
- [ApricityUI](https://www.curseforge.com/minecraft/mc-mods/apricityui) 1.2.0+ (client)
- [KubeJS](https://www.curseforge.com/minecraft/mc-mods/kubejs) (provided at runtime,
  optional on a server if unused)

## Building & running

```bash
./gradlew build       # build the mod jar
./gradlew runClient   # launch a dev client
```

## AUI pages

The screens live under `run/apricity/screens/` (dev runtime directory):

- `class_creation.html` / `class_creation.css` / `class_creation.js`
  (used both as the creation screen and the read-only character sheet).

## In-game commands (`/dnd`, operator)

| Command | Description |
| --- | --- |
| `/dnd info` | Show class, subclass, level, XP and proficiency |
| `/dnd xp add <n>` / `/dnd xp set <n>` | Grant / set experience |
| `/dnd level set <1-20>` | Set level |
| `/dnd features list` | List currently active features |
| `/dnd use <featureId>` | Trigger an active feature |
| `/dnd refresh` | Recompute features and spellcasting |

## Datapack content

- Features: `data/<namespace>/dndclasses/class_features/*.json`
- Spellcasting: `data/<namespace>/dndclasses/spellcasting/*.json`

External content must use a non-reserved namespace (not `dndclasses`, `minecraft`, `forge`).

## License

All Rights Reserved (see `gradle.properties`). ApricityUI's bundled Ore theme is distributed
under its own license (MPL-2.0).
