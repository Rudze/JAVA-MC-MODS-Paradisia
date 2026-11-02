# Paradisia — mods et modpack

Mods NeoForge (Minecraft 1.21.1) du serveur Cobblemon **Paradisia**, et le modpack client.

## Installer le modpack

1. Télécharger `Paradisia-<version>.zip` dans la [dernière release](https://github.com/Rudze/JAVA-MC-MODS-Paradisia/releases/latest).
2. Dans [Prism Launcher](https://prismlauncher.org) : **Ajouter une instance → Importer**, puis choisir le zip.
3. Lancer l'instance : le jeu se connecte directement au serveur.

## Contenu du repo

| Dossier | Contenu |
|---|---|
| `mods/` | Le code source des mods du serveur (`nh_*`), un projet Gradle par mod |
| `modpack/instance/` | L'instance Prism : configs, packs de ressources, shaders et les jars qui ne sont pas sur Modrinth |
| `modpack/modrinth-mods.json` | Les mods téléchargés depuis Modrinth à la construction du zip (URL + sha1) |
| `modpack/build.sh` | Construit `dist/Paradisia-<version>.zip` |

## Versions

Les versions sont calculées par [semantic-release](https://semantic-release.gitbook.io) à chaque push sur `main`,
à partir des messages de commit ([Conventional Commits](https://www.conventionalcommits.org/fr/)) :

- `fix: ...` → version corrective (0.1.0 → 0.1.1)
- `feat: ...` → nouvelle fonctionnalité (0.1.0 → 0.2.0)
- `feat!: ...` ou `BREAKING CHANGE:` → version majeure

La release contient le zip du modpack et le `CHANGELOG.md` est mis à jour automatiquement.

## Compiler un mod

```sh
cd mods/MC-NewHorizon-Mod-WorldBuild
./gradlew build   # → build/libs/*.jar
```

Le mod Quests compile contre WorldBuild et Numismatic Overhaul (à compiler avant) et contre les jars
Cobblemon/Kotlin placés dans son dossier `libs/` (non versionné).
