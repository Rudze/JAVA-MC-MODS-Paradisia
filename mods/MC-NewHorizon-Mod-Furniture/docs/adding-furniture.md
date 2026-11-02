# Adding furniture from a Nexo yml

nh_furniture is data-driven: one small json per item under `nh_furniture_config/` describes a
block, and `FurnitureDefinitionLoader` registers it automatically at startup - no Java code, no
per-item class. Adding a new Nexo furniture pack means turning its yml + exported models/textures
into that json, plus a handful of generated asset files around it. A tool does essentially all of
this for you.

## TL;DR

```bash
cd tools
javac -cp "<gson.jar>:<snakeyaml.jar>" NexoFurnitureConverter.java
java -cp ".:<gson.jar>:<snakeyaml.jar>" NexoFurnitureConverter \
    <path/to/items.yml> <path/to/pack> ../src/main/resources nh_furniture
```

Then check the "what it doesn't do" list below, build, and test in-game.

---

## 1. The converter tool

`tools/NexoFurnitureConverter.java` takes two inputs straight from a Nexo (or Oraxen - Nexo is a
continuation of Oraxen and their yml schemas are close enough that the same tool handles both) pack
export:
- the item yml (e.g. `items/lanshan/lanshan_summer.yml`)
- the pack folder next to it, containing the exported models/textures

Gson and SnakeYAML are already in Gradle's cache - find their jar paths under
`~/.gradle/caches/modules-2/files-2.1/`. Run it with the command above.

Two variations of both inputs have shown up across packs so far, and the tool auto-detects both -
no flag needed, just point it at whatever the pack actually contains:
- **Pack folder layout**: newer exports wrap models/textures in the usual resource-pack
  `assets/minecraft/models/...` / `assets/minecraft/textures/...` path; older Oraxen exports (e.g.
  the Autumn Field pack) put `models/...` / `textures/...` directly at the pack root, no wrapper.
- **Hitbox yml shape**: newer ymls nest `barriers`/`interactions` under a `hitbox:` key, each offset
  written as an `"x,y,z"` string. Older Oraxen ymls (same pack) put `barriers`/`interactions` directly
  under `furniture:` with no `hitbox` wrapper, each offset as its own `{x: .., y: .., z: ..}` map
  instead of a string. Lights have the same split: newer ymls use `lights: {toggleable, lights: [...]}`
  (supports offset cells and a toggle); older ones just have a plain `light: <level>` scalar under
  `furniture:` (main cell only, no toggle, no offset cells) - if a pack only needs that, it's used as
  the main block's light level directly.
- **Seats**: whichever of `seats` or `seat` shows up (older Oraxen packs use the singular), it's
  still unsupported either way - see below.

If a genuinely new pack shows a third shape for either of these, the fix belongs in
`NexoFurnitureConverter.java`'s detection, not in hand-editing the generated config afterwards -
keep the "drop the files in and convert" promise intact for the next pack too.

For every item in the yml, it writes:

| Output | Purpose |
|---|---|
| `assets/nh_furniture/models/item/<id>.json` | The item's Blockbench model, byte-for-byte (only texture references remapped). This is both the inventory icon **and** the in-world geometry - see [How rendering works](#how-rendering-works). |
| `assets/nh_furniture/textures/block/*.png(.mcmeta)` | Every texture the model references, copied as-is. |
| `assets/nh_furniture/models/block/<id>.json` | A tiny placeholder cube. Never actually drawn (see below) - it only exists so Minecraft's model loader has something valid to bake. |
| `assets/nh_furniture/blockstates/<id>.json` | Variants covering whichever state properties the piece needs - see [Rotation](#rotation). |
| `nh_furniture_config/<id>.json` | The config itself - see [schema reference](#2-config-schema-reference). |
| `data/nh_furniture/loot_table/blocks/<id>.json` | Drop-self loot table. |
| `assets/nh_furniture/lang/en_us.json` | Merged in, doesn't overwrite other entries. |
| `nh_furniture_config/_index.json` | Appended, deduplicated. |
| `data/minecraft/tags/item/dyeable.json` | Appended for any piece with a tinted face - see [Dyeing](#dyeing--tinting). |

Reruns are safe - everything merges/deduplicates rather than overwriting.

**Naming**: the furniture's internal id is the **model filename** (last segment of the yml's
`Pack.model` path), not the yml's top-level key. A few Nexo ids don't match their model filename
(yml key `furniture_anchor` → model `anchor` → our id `anchor`) - the tool follows the filename,
so don't rename an id afterwards without renaming its model/texture files too.

### What it does NOT do

Check these by hand after running it:

- **Seats.** Nexo's Seat Mechanic (`seats`/`seat`: an `x,y,z` offset - see
  [Nexo's docs](https://docs.nexomc.com/mechanics/furniture-mechanic/seat-mechanic)) isn't
  implemented here. The tool just warns and skips it; the piece places fine, just isn't sittable.
- **Lights.** The plain `light: <level>` scalar (the Autumn Field lanterns' shape - see above) *is*
  auto-detected now and needs no manual touch-up. The richer `lights: {toggleable, lights: [...]}`
  shape (a right-click-toggleable light, or one on a lit cell offset from the main block) hasn't
  actually been seen in a real yml yet - the parsing for it exists but is unconfirmed, since
  `desert_lantern.json`/`desert_brazier.json` were hand-written, not tool-derived, before any pack
  used that shape. To add one of those by hand: `"light": <0-15>` on the config's `main` object (or
  on a `parts` entry, for a lit cell offset from the main block), plus `"toggleableLight": true` if it
  should be right-click-toggleable. `desert_lantern.json`/`desert_brazier.json` are worked
  examples - a solid cell, then a further non-solid lit cell.
- **Precise interaction hitboxes.** Nexo's `hitbox.interactions` can describe multiple exact
  regions; nh_furniture only has one shape - a full-block, non-solid box. Fine for almost
  everything, worth an eyeball check on odd shapes.
- **The actual in-game look.** Build, deploy, and place each new piece on every face it allows.
  If something looks wrong, it's almost always the yml data landing in the wrong bucket (see the
  `main`/`parts` derivation below), not a rendering bug.

---

## 2. Config schema reference

```json
{
  "name": "Display Name",
  "place": {"floor": true, "wall": false, "roof": false},
  "main": {"solid": true},
  "parts": [
    {"offset": [0, 1, 0], "solid": true, "light": 0}
  ],
  "light": 0,
  "toggleableLight": false,
  "sounds": {"place": "minecraft:block.metal.place", "break": "minecraft:block.metal.break"},
  "scale": 0.5,
  "color": [255, 255, 255]
}
```

`color` defaults to white - safe to omit unless you need the non-default behavior described below.

Nexo's yml has a `Mechanics.furniture.rotatable` flag; it's no longer read at all. Every piece always
faces the player on placement now (chest-style), regardless of what that flag says - see
[Rotation](#rotation). A handful of older configs still have a `"rotatable": ...` key sitting in
their json from before this change; it's harmless and ignored, no need to strip it out.

| Field | From Nexo yml | Notes |
|---|---|---|
| `name` | `itemname` | Display name / translation string. |
| `place.floor/wall/roof` | `Mechanics.furniture.limited_placing.*` | Which faces the block can be placed against. |
| `main.solid` / `main.box` | derived from `hitbox` | See below. |
| `parts[]` | `hitbox.barriers` entries other than `0,0,0` | Extra cells, rotated with the piece. |
| `light` / `toggleableLight` | `Mechanics.furniture.light` (plain scalar) or `.lights.*` | See "Lights" above - the richer offset/toggle shape is still add-by-hand if a pack ever needs it. |
| `sounds.place/break` | `Mechanics.furniture.block_sounds.*`, prefixed `minecraft:` | Falls back to wood sounds if unresolvable. |
| `scale` | `Mechanics.furniture.properties.scale` (first component) | Copied as-is - see [How rendering works](#how-rendering-works) for why nothing else is computed here. Defaults to `0.5` if the yml omits `properties.scale` entirely (every pack has actually set it explicitly to `0.5` so far, no exceptions - the Autumn Field pack was the first to omit it, which used to fall back to `1.0` and rendered every piece 2x too big). |
| `color` | top-level yml `color: r, g, b` | Only meaningful for dyeable pieces - see [Dyeing](#dyeing--tinting). |

**Deriving `main`/`parts` from Nexo's `hitbox`:**
- `hitbox.barriers` present → `main.solid = true`. Any offset other than `0,0,0` (which *is* the
  main block) becomes a `parts` entry.
- Only `hitbox.interactions` present → `main.solid = false`, full-block `box`. Renders and can be
  broken, just doesn't block movement.
- Neither present → default `main.solid = true`.

---

## Rotation

Every piece always places facing the player, chest-style - no exceptions, regardless of what Nexo's
yml `Mechanics.furniture.rotatable` flag says (that flag used to make player-facing placement only
the *default*, with non-rotatable pieces instead placing facing a fixed north unless the player was
sneaking; every piece now always faces the player, so the flag is no longer read - see
`FurnitureBlock#getStateForPlacement`). Two properties make player-facing placement work:

- **`facing`** - nearest of the 4 cardinal directions. Always present. Used to rotate
  `parts` offsets onto real grid positions (an offset like `[0,0,1]` has to land on an actual
  block), and as the exact orientation for wall-mounted pieces (a wall piece can only face
  straight away from the one wall it's flush against - a diagonal wouldn't mean anything there).
- **`rotation_8`** - nearest of 8 (cardinals + diagonals), present whenever floor or ceiling
  placement is allowed. Purely visual: the renderer uses it for the item's yaw instead of
  `facing`, so a player looking south-east places the piece facing south-east, not just south.
  It's a separate property because an 8-way angle can't always land `parts` offsets on the grid
  (there's no "half a block" position for a companion cell), so `facing` still handles that job
  at its coarser, grid-safe granularity.

---

## Crafting recipes

Nexo's own yml has no crafting-recipe concept at all (these packs are meant to be sold/unlocked
through the Bukkit plugin's own economy, not crafted), so the converter doesn't generate any - every
piece is creative-tab/loot-table only until a recipe is added by hand, under
`data/nh_furniture/recipe/<id>.json` (+ a matching `data/nh_furniture/advancement/recipes/<id>.json`
to unlock it in the recipe book - copy `beach_chair.json`'s pair as a template, it's the standard
vanilla "has any recipe ingredient + has the recipe" pattern, e.g. `stick.json`'s own advancement).

The beach and camping collections' recipes (added together, all 42 pieces at once) follow one
convention worth keeping for future collections: plain `minecraft:crafting_shapeless`, one wood type
per collection (`oak_planks` for beach, `spruce_planks` for camping) tying the group together, and
otherwise whatever vanilla ingredient is the closest real-world material/theme match for that specific
piece (`iron_ingot` for anchor/cooler/grill, `#minecraft:wool` for anything cloth, `sand` for the sand
castles, food items for the food-plate pieces, etc.) - there's no yml data to derive these from, so
they're a judgment call, not something to try to auto-generate from the pack export.

## Dyeing / tinting

Nexo marks a dyeable face by checking Blockbench's "Tint" box on it, which exports as
`"tintindex": 0` in the model json (Blockbench steps in
[Nexo's Items docs](https://docs.nexomc.com/configuration/items)). nh_furniture detects this
automatically - no config field turns it on - and for any such piece:

- registers an `ItemColor` (`ClientSetup.registerItemColors`) reading the item's `DYED_COLOR` data
  component - the same one leather armor uses - falling back to the config's `color` (or white)
  when undyed.
- adds the item to `data/minecraft/tags/item/dyeable.json`, merged into vanilla's own
  `minecraft:dyeable` tag. That's the *only* thing needed for vanilla's existing armor-dye
  crafting recipe to accept it: item + any dye(s) in a crafting table, exactly like dyeing
  leather armor. No custom recipe.
- the dye color is stored on the block entity when placed (`FurnitureBlock#setPlacedBy` reads
  the placed stack's `DYED_COLOR`) and persists/syncs like any other block entity data - a dyed
  piece keeps its color after being placed, saved, and reloaded.

Hand-writing a config for a piece you know has a tinted face: add `"color": [255, 255, 255]` (or
whatever the yml said) and its id to `data/minecraft/tags/item/dyeable.json` yourself.

---

## How rendering works

The block model is never actually drawn (`RenderShape.INVISIBLE` - the placeholder the tool
writes only exists to satisfy the model loader). Instead, `FurnitureBlockEntityRenderer` draws the
**item model** directly, via `ItemDisplayContext.FIXED` - the same context vanilla item frames
use. That makes Minecraft apply the model's own baked `display.fixed` transform
(translation/rotation/scale, baked in by whatever exported it from Blockbench) automatically, so
there's nothing left to guess: no bounding-box math, no reading the model's scale by hand.
nh_furniture only adds what Nexo adds on top of that: the yml's `properties.scale`, and
rotation/positioning for the block face and direction the player placed it on.

This is why the item model must stay byte-for-byte what the artist authored (beyond the texture
namespace remap): any hand-edit to its geometry or `display` block changes what's drawn in-world,
not just the inventory icon.

---

## 3. Build and deploy

```bash
./gradlew build -x test -q
```

Copy `build/libs/nh_furniture-0.1.0.jar` over the old one in the instance's `mods/` folder. Before
trusting a batch of new items, boot-test first:

```bash
./gradlew runClient --console=plain
```

then grep the log for `nh_furniture` warnings/errors (missing model, unknown blockstate property,
etc.) - that catches most yml-shape mismatches without needing to open the game.
