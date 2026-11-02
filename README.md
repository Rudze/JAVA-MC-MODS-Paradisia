<div align="center">
  <img alt="Paradisia" src="./assets_readme/images/logo.png" width="300" />
</div>

<h2 align="center">
  Paradisia — Mods & Modpack
</h2>

---

<div align="center">

[![forthebadge](https://forthebadge.com/images/badges/built-with-love.svg)](https://forthebadge.com) &nbsp;
[![forthebadge](https://forthebadge.com/images/badges/made-with-java.svg)](https://forthebadge.com) &nbsp;

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge)](https://www.minecraft.net) &nbsp;
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1-E67E22?style=for-the-badge)](https://neoforged.net) &nbsp;
[![Cobblemon](https://img.shields.io/badge/Cobblemon-1.8.4-E86FA6?style=for-the-badge)](https://cobblemon.com) &nbsp;
[![Release](https://img.shields.io/github/v/release/Rudze/JAVA-MC-MODS-Paradisia?style=for-the-badge&color=E86FA6)](https://github.com/Rudze/JAVA-MC-MODS-Paradisia/releases/latest)

</div>

---

## À propos du projet

Ce repo regroupe **tous les mods maison du serveur Cobblemon Paradisia** et le **modpack client** distribué aux joueurs.

Paradisia est une île peuplée de Pokémon : les joueurs y arrivent accueillis par le **Professeur Chen**, choisissent leur starter, explorent un monde de farm et achètent un terrain en ville auprès du maire, en payant leurs impôts fonciers en pièces.

Le modpack est publié automatiquement à chaque nouvelle version, prêt à importer dans Prism Launcher : le jeu se lance directement sur le serveur, sans passer par le menu.

---

## Installer le modpack

1. Installer [Prism Launcher](https://prismlauncher.org) et y connecter son compte Minecraft
2. Télécharger `Paradisia-<version>.zip` dans la [dernière release](https://github.com/Rudze/JAVA-MC-MODS-Paradisia/releases/latest)
3. Dans Prism : **Ajouter une instance → Importer**, puis choisir le zip
4. Lancer l'instance : le jeu se connecte directement au serveur 🎮

---

## Les mods

| Mod | ID | Côté | Description |
|-----|----|------|-------------|
| AutoConnect | `nh_autoconnect` | Client | Pas de menu principal : le jeu rejoint directement le serveur, et le quitter ferme le jeu |
| Backpack | `nh_backpack` | Les deux | Sac à dos portable, porté dans l'emplacement dos d'Accessories |
| Cosmetics | `nh_cosmetics` | Les deux | Cosmétiques configurables (modèle + texture + emplacement), façon Nexo / HMCCosmetics |
| DeathPenalty | `nh_deathpenalty` | Les deux | Délai de réapparition et stats réduites après une mort |
| Furniture | `nh_furniture` | Les deux | Meubles décoratifs configurables (modèle, hitbox, sons), façon Nexo |
| LoadingScreen | `nh_loadingscreen` | Client | Écran de chargement noir avec le logo Paradisia |
| Locks | `locks` | Les deux | Serrures sur presque tous les blocs, crochetage, trousseau et clés maîtresses (portage de Locks Reforged) |
| Numismatic Overhaul | `numismaticoverhaul` | Les deux | Monnaie en pièces de bronze, d'argent et d'or (portage de Numismatic Overhaul) |
| Quests | `nh_quests` | Serveur | Professeur Chen (accueil, starter, Pokédex) et bureau des impôts fonciers |
| TreeFelling | `nh_treefelling` | Les deux | Couper une bûche abat tout l'arbre |
| WaystoneTP | `nh_waystonetp` | Les deux | Waystones A ↔ B pour voyager entre les points de téléportation |
| WorldBuild | `nh_worldbuild` | Les deux | Monde de build (la ville), claims à la pelle en or et impôt foncier hebdomadaire |

---

## Fonctionnalités

* ✅ **Professeur Chen** — dialogue d'accueil, choix du starter et remise du Pokédex
* ✅ **Claims** — le maire délimite les terrains à la pelle en or, `/claim <joueur>` les attribue
* ✅ **Impôts fonciers** — 1 pièce d'or par bloc et par semaine, payés à Chen dans un coffre
* ✅ **Monde de build protégé** — seuls les propriétaires construisent dans leur terrain
* ✅ **Connexion directe** — le modpack lance le joueur directement sur le serveur
* ✅ **Releases automatiques** — semantic-release construit et publie le modpack à chaque version

---

## Structure du projet

```
JAVA-MC-MODS-Paradisia/
├── mods/
│   └── MC-NewHorizon-Mod-*/      # Un projet Gradle NeoForge par mod
│
├── modpack/
│   ├── instance/                 # Instance Prism : configs, resource packs, shaders,
│   │                             # mods maison et mods absents de Modrinth
│   ├── modrinth-mods.json        # Mods téléchargés depuis Modrinth (URL + sha1)
│   └── build.sh                  # Construit dist/Paradisia-<version>.zip
│
├── assets_readme/                # Images du README
├── .github/workflows/release.yml # semantic-release sur chaque push sur main
├── .releaserc.json
└── CHANGELOG.md                  # Généré automatiquement
```

---

## Développement

### 1 — Cloner le repo

```bash
git clone git@github.com:Rudze/JAVA-MC-MODS-Paradisia.git
cd JAVA-MC-MODS-Paradisia
```

### 2 — Compiler un mod

Il faut un JDK 21.

```bash
cd mods/MC-NewHorizon-Mod-WorldBuild
./gradlew build   # → build/libs/*.jar
```

Le mod Quests compile contre WorldBuild et Numismatic Overhaul (à compiler avant), et contre les jars
Cobblemon / Kotlin placés dans son dossier `libs/` (non versionné).

### 3 — Construire le modpack en local

```bash
./modpack/build.sh 0.0.0-test   # → dist/Paradisia-0.0.0-test.zip
```

---

## Versions

Les versions sont calculées par [semantic-release](https://semantic-release.gitbook.io) à chaque push sur `main`,
à partir des messages de commit ([Conventional Commits](https://www.conventionalcommits.org/fr/)) :

| Commit | Effet | Exemple |
|--------|-------|---------|
| `fix: ...` | Version corrective | 0.1.0 → 0.1.1 |
| `feat: ...` | Nouvelle fonctionnalité | 0.1.0 → 0.2.0 |
| `feat!: ...` ou `BREAKING CHANGE:` | Version majeure | 0.1.0 → 1.0.0 |

Chaque release contient le zip du modpack, et le `CHANGELOG.md` est mis à jour automatiquement.

---

## Crédits

* **Locks** — portage NeoForge de *Locks Reforged*, lui-même basé sur le mod *Locks* de Melonslise (CC BY-NC 3.0)
* **Numismatic Overhaul** — portage du mod *Numismatic Overhaul* de glisco
* **Cobblemon** — par l'équipe Cobblemon

---

## Contact

Pour toute question : contact@rudydavid.fr

---

### Soutenir le projet

Laisse une ⭐ si le projet te plaît !
