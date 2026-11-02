#!/usr/bin/env bash
# Builds the Prism Launcher instance zip players import: the files under instance/ plus every
# mod listed in modrinth-mods.json, downloaded and checked against its sha1. Mods that aren't on
# Modrinth live in instance/minecraft/mods directly.
#
# Usage: modpack/build.sh <version>   ->   dist/Paradisia-<version>.zip
set -euo pipefail

version="${1:?usage: build.sh <version>}"
here="$(cd "$(dirname "$0")" && pwd)"
root="$(dirname "$here")"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

pack="$work/Paradisia"
cp -R "$here/instance" "$pack"
sed -i.bak "s/^name=.*/name=Paradisia $version/" "$pack/instance.cfg" && rm "$pack/instance.cfg.bak"

python3 -c '
import json, sys
for mod in json.load(open(sys.argv[1])):
    print(mod["file"], mod["url"], mod["sha1"], sep="\t")
' "$here/modrinth-mods.json" | while IFS=$'\t' read -r file url sha1; do
    curl -fsSL -A "Rudze/JAVA-MC-MODS-Paradisia" -o "$pack/minecraft/mods/$file" "$url"
    actual="$(shasum -a 1 "$pack/minecraft/mods/$file" | cut -d' ' -f1)"
    [ "$actual" = "$sha1" ] || { echo "sha1 mismatch for $file" >&2; exit 1; }
    echo "downloaded $file"
done

mkdir -p "$root/dist"
rm -f "$root/dist/Paradisia-$version.zip"
(cd "$work" && zip -qr "$root/dist/Paradisia-$version.zip" Paradisia -x '*.DS_Store')
echo "built dist/Paradisia-$version.zip"
