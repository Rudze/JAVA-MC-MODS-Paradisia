## [1.4.7](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/compare/v1.4.6...v1.4.7) (2026-08-19)

### Bug Fixes

* every recipe JSON used the pre-1.21 result schema, and config unload crashed the server ([879b351](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/commit/879b35198962ac0f3f551e4e733ddf61ebdebe0f)), closes [LocksModEvents#onConfigLoad](https://gitlab.com/horizoncorporation/LocksModEvents/issues/onConfigLoad)

## [1.4.6](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/compare/v1.4.5...v1.4.6) (2026-08-19)

### Bug Fixes

* recipe/loot table datapack folders still used the 1.20.1 plural names ([4351d20](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/commit/4351d200f4c2a603db5555b6e21e224ad6af1768)), closes [LocksForgeEvents#onLootTableLoad](https://gitlab.com/horizoncorporation/LocksForgeEvents/issues/onLootTableLoad)

## [1.4.5](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/compare/v1.4.4...v1.4.5) (2026-08-19)

### Bug Fixes

* locked-block lock render doubles the camera rotation, making it fly around ([689c675](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/commit/689c675130992e868117d7bda5787d7e0e0d501d)), closes [#getFrustum](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/issues/getFrustum)

## [1.4.4](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/compare/v1.4.3...v1.4.4) (2026-08-19)

### Bug Fixes

* villager/wanderer trade registration crashes on world join ([0071dad](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/commit/0071dad453bcf8efa04ed570b286ae55fb51bcc2))

## [1.4.3](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/compare/v1.4.2...v1.4.3) (2026-08-19)

### Bug Fixes

* three mixins target methods that no longer exist in 1.21.1, crashing bootstrap ([d2ec44b](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/commit/d2ec44bc24984d2c75677bc4608f35dbc704dce4)), closes [ChunkMap#playerLoadedChunk](https://gitlab.com/horizoncorporation/ChunkMap/issues/playerLoadedChunk) [LevelRenderer#renderLevel](https://gitlab.com/horizoncorporation/LevelRenderer/issues/renderLevel)

## [1.4.2](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/compare/v1.4.1...v1.4.2) (2026-08-19)

### Bug Fixes

* accessories dependency version range rejects the installed beta build ([0787f38](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/commit/0787f38b81059289900f92d089fc32cb32a41261))

## [1.4.1](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/compare/v1.4.0...v1.4.1) (2026-08-18)

### Bug Fixes

* port capability/enchantment/NBT/loot-injection systems to NeoForge 1.21.1 APIs ([47c59d8](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/commit/47c59d8ee8b060d9b0e25a2a5246e17ed6b5c78b)), closes [Player#openMenu](https://gitlab.com/horizoncorporation/Player/issues/openMenu) [LootTable#pools](https://gitlab.com/horizoncorporation/LootTable/issues/pools) [DataComponents#CUSTOM_DATA](https://gitlab.com/horizoncorporation/DataComponents/issues/CUSTOM_DATA) [BlockEntity#getCapability](https://gitlab.com/horizoncorporation/BlockEntity/issues/getCapability)
* port client rendering, screens, and remaining APIs — mod now compiles clean ([5e5d1f1](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/commit/5e5d1f14428fe033339b14d9a5d7c6fd8aef4f55)), closes [Tesselator#begin](https://gitlab.com/horizoncorporation/Tesselator/issues/begin) [ItemStack#getTooltipLines](https://gitlab.com/horizoncorporation/ItemStack/issues/getTooltipLines) [EnchantmentHelper#getEnchantments](https://gitlab.com/horizoncorporation/EnchantmentHelper/issues/getEnchantments) [MenuScreens#register](https://gitlab.com/horizoncorporation/MenuScreens/issues/register) [MinecraftServer#getLootData](https://gitlab.com/horizoncorporation/MinecraftServer/issues/getLootData)

## [1.4.0](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/compare/v1.3.0...v1.4.0) (2026-08-18)

### Features

* rewrite networking to CustomPacketPayload, port enchantments to data ([3097a3f](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/commit/3097a3f4ddfdcb535d0748a4f3f788805c1f67cc))

## [1.3.0](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/compare/v1.2.0...v1.3.0) (2026-08-18)

### Features

* port config system and sweep private ResourceLocation constructor ([2fdcabf](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/commit/2fdcabf5e6c00185bd9deab87c6e2242845473df))

## [1.2.0](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/compare/v1.1.0...v1.2.0) (2026-08-18)

### Features

* port item/block/menu/sound/feature/placement/recipe/creative-tab registration ([dd621e9](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/commit/dd621e951418acad1ec305d6a659274af19b5b2c))

## [1.1.0](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/compare/v1.0.0...v1.1.0) (2026-08-18)

### Features

* replace Forge capabilities with NeoForge data attachments (core) ([f499101](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/commit/f499101d3a20308609fdf353dfedef81bc45f81a))

## 1.0.0 (2026-08-18)

### ⚠ BREAKING CHANGES

* Project setup

### Features

* Project setup ([db5c051](https://gitlab.com/horizoncorporation/jeux/minecraft/paradisia/mod-lock/commit/db5c05126dbd0a80eed25eb5c67fb01fea26a1c2))
