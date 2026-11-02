# Adding cosmetics from Nexo + HMCCosmetics

nh_cosmetics is data-driven, the same way nh_furniture is: one small json per item under
`nh_cosmetics_config/` describes an item, and `CosmeticDefinitionLoader` registers it
automatically at startup - no Java code, no per-item class. Cosmetics are plain
[Accessories](https://github.com/wisp-forest/accessories) items (`docs.wispforest.io/accessories`)
rather than blocks - equipping them, the slot UI, and unequip-on-death rules are all Accessories'
own job. This mod only needs to say *which slot* an item goes in and draw its model.

## TL;DR

```bash
cd tools
javac -cp "<gson.jar>:<snakeyaml.jar>" NexoCosmeticConverter.java
java -cp ".:<gson.jar>:<snakeyaml.jar>" NexoCosmeticConverter \
    <path/to/nexo_items.yml> <path/to/hmccosmetics_cosmetics.yml> <path/to/pack> ../src/main/resources nh_cosmetics
```

Then check "what it doesn't do" below, build, and test in-game.

## Why two yml files

Nexo's own cosmetic yml only registers the item and its model - unlike furniture, there's no
`Mechanics` section, because *placement* isn't a Nexo concept for cosmetics. The actual "where does
this go on the player" data lives in the separate HMCCosmetics yml (`slot: HELMET/OFFHAND/BACKPACK/BALLOON`,
`dyeable: true/false`) - that plugin is what actually equips things in-game on the Bukkit side. The
converter reads **both**: HMCCosmetics for slot/name, Nexo (matched by the same top-level id) for
the model path.

Note the folder naming isn't fully consistent between packs exported at different times - the first
pack put its Nexo yml under `Nexo/items/`, the second under `Nexo/item/` (singular). Check the actual
path rather than assuming - and the same goes for the pack folder's `models`/`textures` directories,
see below.

## 1. The converter tool

`tools/NexoCosmeticConverter.java` needs:
- the Nexo item yml (e.g. `items/lanshan/lanshan_cos_summer_1.yml`)
- the matching HMCCosmetics yml (e.g. `cosmetics/lanshan_cos_summer_1.yml` - same base filename)
- the Nexo pack folder, containing `assets/minecraft/models/...` and `assets/minecraft/textures/...`
  - ...or singular `assets/minecraft/model/...` / `.../texture/...` - `lanshan_cos_sunmoon_wizard`
    used the singular form, unlike every pack before it; the tool auto-detects whichever exists, no
    flag needed.

For every id shared by both ymls, it writes:

| Output | Purpose |
|---|---|
| `assets/nh_cosmetics/models/item/<id>.json` | The item's Blockbench model, byte-for-byte (only texture references remapped). This is both the inventory icon **and** the in-world geometry. |
| `assets/nh_cosmetics/textures/item/*.png(.mcmeta)` | Every texture the model references, copied as-is. |
| `nh_cosmetics_config/<id>.json` | The config itself - see the schema below. |
| `assets/nh_cosmetics/lang/en_us.json` | Merged in. |
| `nh_cosmetics_config/_index.json` | Appended, deduplicated. |
| `data/accessories/tags/item/<slot>.json` | Makes the item equippable in that Accessories slot - see [Slots](#slots). |
| `data/minecraft/tags/item/dyeable.json` | Appended for any piece with a tinted face - same mechanic as nh_furniture's dyeing. |

The cosmetic's internal id is the **model filename** (last segment of the yml's `Pack.model`
path), matching nh_furniture's convention - *unless* that filename is purely numeric (e.g.
`lanshan_cos_sunmoon_wizard`'s models are named `1.json`..`6.json`, not descriptively), in which case
the tool falls back to the yml's own cosmetic key (`sun_wizard_hat`, etc.) instead. A bare numeral
would be a genuinely bad, collision-prone item id - meaningless in commands/tags, and any future pack
could just as easily export its own `1.json`.

### What it does NOT do

- **`firstperson-item` overrides.** Some HMCCosmetics entries (backpack-slot ones so far) declare a
  second model shown only to the wearer's own first-person camera (a repositioned/simplified view -
  the Nexo yml exports it as a separate item, usually suffixed `_thirdperson`, confusingly, since
  that's what HMCCosmetics itself calls the override field). Accessories only renders "hand"-slot
  items in first person by default (and balloons opt back out of even that - see
  [Balloons](#balloons)); `hat`/`back` don't have an equivalent hook wired up yet. The tool prints a
  note and ignores the override - the main model is used for
  every perspective, including the wearer's own camera if they ever look at it (mirror, screenshot, etc).
- **The menus yml.** HMCCosmetics' `menus/*.yml` is a Bukkit GUI layout - irrelevant here, since
  Accessories has its own inventory/slot screen.
- **The actual in-game look.** Equip each new piece and check it from third person (and, for
  hand-slot ones, first person). See [How rendering works](#how-rendering-works) if something looks off.

## 2. Config schema

```json
{
  "name": "Display Name",
  "slot": "hat",
  "color": [255, 255, 255]
}
```

For a balloon (HMCCosmetics `slot: BALLOON`), the converter instead writes `"slot": "hand"` plus a
`"balloon": true` marker - see [Balloons](#balloons).

`backScale`/`backOffset` (only relevant for `slot: "back"`) aren't shown above because they're
**not meant to be set per item** - the shared default is calibrated so a `back` cosmetic needs
zero config beyond `name`/`slot`/`color`, same drag-and-drop expectation as everything else in this
mod. Only add them to a specific item's config as an escape hatch if that piece's geometry is
unusual enough that the shared default visibly doesn't fit it:

```json
"backScale": 0.8,
"backOffset": [0.0, 0.30, 0.0]
```

| Field | From | Notes |
|---|---|---|
| `name` | HMCCosmetics `item.name`, falling back to Nexo `itemname` | Display name / translation string. |
| `slot` | HMCCosmetics `slot`, mapped - see below | Defaults to `"hat"` if omitted. |
| `color` | Nexo yml top-level `color: r, g, b` | Only meaningful for dyeable pieces - see [Dyeing](#dyeing--tinting). Defaults to white if omitted. |
| `backScale` / `backOffset` | not derived from either yml - calibrated once, in-game | See [How rendering works](#how-rendering-works). The shared default (`backScale: 0.8`, `backOffset: [0, 0.30, 0.0]`, set in `CosmeticDefinitionLoader`) was calibrated against `coral_wings` with *no* per-item override at all - that's the point: it should already fit new `back` pieces without any config changes. A Z of `-0.05` was tried briefly (chasing a visible gap behind `coral_wings` specifically) but pushed every `back` piece too far into the body once compared across the whole set (backpacks, wings, tails all included), so it's back to `0.0`. If a piece's own geometry is unusual enough that the shared default visibly doesn't fit it (an especially tall or offset model), override `backScale`/`backOffset` in just that item's config rather than touching the shared default, which would move every other `back` piece too. |

## Slots

HMCCosmetics slots map onto [Accessories](https://docs.wispforest.io/accessories/general/slot_types) slots:

| HMCCosmetics `slot` | Accessories slot | Notes |
|---|---|---|
| `HELMET` | `hat` (built in) | |
| `OFFHAND` | `hand` (built in) | Rendered on whichever arm the slot index resolves to (see below). |
| `BACKPACK` | `back` (built in) | |
| `BALLOON` | `hand` (built in) | Same pool as `OFFHAND` items, flagged `"balloon": true` - see [Balloons](#balloons). This mod adds no custom Accessories slots. |

A cosmetic becomes equippable in a slot purely by being listed in that slot's
`accessories:<slot>` item tag (e.g. `data/accessories/tags/item/hat.json`) - note the namespace is
always `accessories`, regardless of which mod's item is being added, because Accessories' own tag
validator always looks up `accessories:<slotName>`. The converter handles this automatically; if
hand-writing a config, add the id to that tag file yourself.

## Balloons

A balloon (HMCCosmetics `slot: BALLOON`) equips into the same `hand` Accessories slot as a held
cosmetic like a staff or trident - same pool of 2 slots, not a separate slot type (an earlier version
of this mod added a custom `balloon` slot; that's gone now, per the same "same slot as hand items"
request that prompted this design). What makes it a balloon instead of a held item is purely the
`"balloon": true` config flag the converter sets from `slot: BALLOON`.

Rendering it is completely different from `renderHand`, though: HMCCosmetics itself doesn't hand-draw
its balloons either - it equips a **third-party ModelEngine model onto a real leashed Pufferfish
entity** that floats above/behind the player, connected only by a real vanilla leash, and lets
vanilla's own leash rendering draw the rope. Spawning a synced companion entity per equipped accessory
is out of scope here (no ModelEngine, and a server-authoritative entity per balloon is a much bigger
feature than a cosmetics-config mod should need) - but the important part isn't the entity, it's the
*behaviour*: the balloon is not rigidly attached to the wearer, it hovers above them in world space
and lags behind with its own inertia, tethered only by a sagging rope.

A first version anchored the balloon on the player model's body bone, like `back` does - that was
wrong, and looked like the balloon was bolted on and rotating with the player's body, because a bone
anchor is by definition rigidly coupled to the wearer's rotation. A real leashed mob's position isn't
coupled to its holder's rotation at all, only loosely to its holder's *position* (leash tension pulls
it along, but it drifts/lags rather than snapping to a fixed relative offset). `renderBalloon` now
works entirely in world space instead of model-bone space to get that: once per game tick it eases a
tracked "balloon position" toward a rest point (`Vec3.lerp` with a fixed factor, the same "spring
toward a moving target" shape as leash tension), and stores a prev/current pair so rendering can
interpolate smoothly between ticks (`partialTicks`) instead of visibly stepping at 20Hz - mirroring
how vanilla interpolates any entity's own position for rendering. The balloon end answers only to
that eased world-space position, so turning around doesn't drag it along in lockstep - it has to
catch up, the way a balloon on a string actually would.

The rope's *anchor* end, and the rest point the balloon eases toward, are both derived from the
wearer's actual hand position, read back out of `model.translateToHand` (the same bone `renderHand`
itself anchors on) rather than a fixed shoulder/chest-height guess or a point centered above the
whole body - so the balloon reads as tied to *that hand* specifically (drifting horizontally with it
too), not floating disconnected above the head. It's converted out of the outer PoseStack's
camera-relative space into a true world position (by adding the camera's own world position) so it
can be combined with the balloon's independently-tracked world position. `RISE_HEIGHT` (world-space
blocks straight up from the hand) controls how far above the hand the balloon rests, and therefore how
long the visible rope is - a short one reads as "stuck to the hand", so this is deliberately generous.
The rope itself reuses vanilla's leash *rendering math* (`EntityRenderer#renderLeash`'s sagging
quad-strip, drawn with the same `RenderType.leash()`), fed the anchor-to-balloon world-space delta
instead of two real entities' positions.

The balloon item itself is rotated to face the same way the wearer's body does (vanilla's yaw
convention for entities - `Axis.YP.rotationDegrees(180 - bodyYaw)`, from
`LivingEntityRenderer#setupRotations`, read via the wearer's own `getPreciseBodyRotation`) instead of
being frozen facing one fixed compass direction while the wearer turns around it, which looked static
and unnatural. Unlike that vanilla method, there's deliberately **no** X/Y mirror scale applied on top
of the rotation: that mirror only makes sense inside the entity's own nested Y-down bone-local space
(see the "back" section above), which this fresh, plain Y-up world-space `PoseStack` was never part
of - a first attempt copied vanilla's `scale(-1, -1, 1)` anyway on the assumption it was part of the
same "always mirror entity-attached items" convention as `hat`, and it rendered the balloon upside
down.

Its rest position also respects collision, so it doesn't clip up into a cave ceiling or overhang - the
last step before storing the rest position is `clampToCeiling`, which casts (`Level#clip`, the same
API vanilla itself uses for block raytracing) from the hand anchor toward the unobstructed rest point
and, if something solid is hit, rests just short of the surface instead of passing through it. That
clamp is skipped once the balloon is more than `ESCAPE_DISTANCE` (3 blocks) away from where its
*unclamped* target currently is - otherwise a balloon left behind under a low ceiling the wearer just
walked out from under (or one that desynced from a teleport) would have to awkwardly crawl back along
terrain surfaces to catch up, instead of just floating straight back through whatever's now in the
way, the way a leashed mob's elastic snap-back would.

This is a first-pass calibration (rise height, lag/easing factor, item scale, escape distance), the
same way `back`'s offsets were: not yet fully confirmed in-game against a real balloon model, so
expect to retune the
constants in `CosmeticAccessoryRenderer#renderBalloon` after equipping one and taking a look.

## Dyeing / tinting

Identical mechanism to nh_furniture: a tintindex-0 face (Blockbench's "Tint" checkbox, matching
HMCCosmetics' `dyeable: true`) is auto-detected from the model, no config field needed to turn it
on. The item gets an `ItemColor` reading its `DYED_COLOR` data component (falling back to the
config's `color`) and is added to `minecraft:dyeable`, so combining it with a dye in a crafting
table works exactly like leather armor.

## How rendering works

`CosmeticAccessoryRenderer` doesn't use Accessories' generic `transformToModelPart` helper - the
first version did, and every model came out visibly warped, because Accessories' helper rotates
around a different axis than what these models are actually calibrated for and never mirrors.
Rendering is genuinely different per slot, confirmed in-game for each:

- **`hat`**: `model.head.translateAndRotate(matrices)`, then vanilla's own
  `CustomHeadLayer#translateToHead` sequence (`translate(0,-0.25,0)` → `rotate 180° Y` →
  `scale(0.625, -0.625, -0.625)`), then `renderStatic(..., ItemDisplayContext.HEAD, ...)`. This
  mirrors how HMCCosmetics actually displays these: it equips them into the player's *real*
  (spoofed) vanilla head slot and lets vanilla's own rendering
  (`net.minecraft.client.renderer.entity.layers.CustomHeadLayer`) do the work. The model's own
  baked `"head"` display transform is authored assuming vanilla's exact sequence runs first, which
  is why replicating it by hand (instead of Accessories' generic helper) fixed the deformation.
- **`hand`**: `model.translateToHand(arm, matrices)`, then vanilla's own
  `ItemInHandLayer#renderArmWithItem` sequence (`rotate -90° X` → `rotate 180° Y` → a small
  per-arm translate), then `renderStatic(..., THIRD_PERSON_RIGHT_HAND/LEFT_HAND, ...)`. An earlier
  version dropped that extra rotate/translate, assuming it was tuned only for vanilla's own
  generated-icon item models - that assumption was wrong. It was confirmed by comparing against
  holding the same item normally, not equipped as an accessory at all: that goes through
  unmodified vanilla rendering (the same `ItemInHandLayer` sequence, via `PlayerItemInHandLayer`)
  and looks correct, so replicating it exactly here is what actually fixes it. The deformation
  reported when this sequence was first tried had a different, unrelated cause - the undyed-tint
  alpha bug (see [Dyeing](#dyeing--tinting)) - which briefly made the *simplified* version look
  like the fix when it wasn't.
- **`back`**: a fundamentally different technique from the other three. Two things were tried and
  rejected first: anchoring on `model.head` (correct height, but visibly follows head-tilt, since a
  cloak/wings reaching 50+ units from its pivot amplifies any head pitch into a wild swing) and
  spawning a separate passenger `ArmorStand` wearing the item in *its own* head slot (positionally
  correct once mounted, but never re-synced its own yaw against the player turning, so it visually
  detached from the body during rotation). What actually works: the same technique the standalone
  Backpack module's own `BackpackAccessoryRenderer` already uses successfully - anchor on
  `model.body`, draw with `ItemDisplayContext.NONE` (skips the model's own baked transforms
  entirely, unlike `hat`), then apply an explicit `scale` → `translate(backOffsetX/Y/Z)` →
  `rotate 180° Z` using the definition's own `backScale`/`backOffset` config fields. Being on `NONE`
  rather than a specific baked context means each piece's own "head" transform data is simply
  unused for this slot - the config's scale/offset are the only positioning input, which is exactly
  why they need per-item tuning (see the schema table above).

## 3. Build and deploy

```bash
./gradlew build -x test -q
```

Copy `build/libs/nh_cosmetics-0.1.0.jar` over the old one in the instance's `mods/` folder - the
instance must already have `accessories-neoforge` and `owo-lib-neoforge` installed (this mod
depends on both; versions are pinned in `gradle.properties` to match what's already in the target
instance). Boot-test first:

```bash
./gradlew runClient --console=plain
```

then grep the log for `nh_cosmetics` warnings/errors - catches most yml-shape mismatches without
needing to open the game.
