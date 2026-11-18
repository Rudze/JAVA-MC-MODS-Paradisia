# NewHorizon: Web API (`nh_api`)

Mod **serveur uniquement**. Il ouvre une API HTTP en lecture seule qui renvoie du JSON, pour le site / dashboard Paradisia :
- les joueurs qui se sont déjà connectés ;
- leur argent (Numismatic Overhaul) ;
- leurs Pokémon : l'équipe et le PC (Cobblemon).

Les clients n'ont pas besoin du mod.

## Configuration

Fichier `config/nh_api.toml` sur le serveur :

| Clé | Défaut | Rôle |
|---|---|---|
| `enabled` | `true` | Démarre l'API avec le serveur |
| `bindAddress` | `0.0.0.0` | `0.0.0.0` : joignable depuis Internet, ce qui est nécessaire pour que GitHub Actions récupère les données du site. `127.0.0.1` : joignable seulement depuis la machine |
| `port` | `8091` | Port HTTP |
| `token` | vide | Vide : l'API est publique. Rempli : la clé devient obligatoire |
| `mapDirectory` | `squaremap/web` | Dossier web du mod squaremap, servi zippé sur `/api/map.zip` |
| `allowedOrigin` | `*` | En-tête CORS `Access-Control-Allow-Origin`, pour les sites qui appellent l'API depuis le navigateur |

Après une modification, redémarre le serveur.

## Accès

L'API est **publique** : les données ne sont pas sensibles.

Le site [WEB-MC-Paradisia](https://github.com/Rudze/WEB-MC-Paradisia) ne l'appelle pas depuis le navigateur. Une tâche GitHub Actions télécharge les données toutes les 5 minutes et les publie avec le site sur GitHub Pages. Il n'y a donc **pas besoin de HTTPS ni de reverse proxy** : l'API doit seulement être joignable depuis Internet, sur le port `8091`.

Si un `token` est défini, chaque route sauf `/api/health` exige la clé, sous l'une de ces deux formes : `Authorization: Bearer <token>` ou `X-Api-Key: <token>`. Sans elle, l'API répond `401`.

## Routes

Toutes les routes sont en `GET` et renvoient du JSON. `<joueur>` accepte un UUID (avec ou sans tirets) ou un pseudo.

### `GET /api/health` (sans clé)

```json
{"status":"ok","minecraftVersion":"1.21.1","onlinePlayers":0,"maxPlayers":10,"motd":"A Minecraft Server"}
```

### `GET /api/players`

Tous les joueurs qui ont un fichier de sauvegarde, c'est-à-dire tous ceux qui se sont déjà connectés.

```json
{"count":3,"players":[
  {"uuid":"a5ff35d0-…","name":"Rudzee","online":false,"lastSeen":1794864212684,"playTimeSeconds":301103,"deaths":7}
]}
```

- `lastSeen` : timestamp en millisecondes. C'est l'heure actuelle si le joueur est en ligne, sinon la date de sa dernière sauvegarde.
- `name` peut être `null` si le serveur ne connaît plus le pseudo.

### `GET /api/players/<joueur>`

Même chose que ci-dessus, avec en plus l'argent et le nombre de Pokémon :

```json
{"uuid":"…","name":"Rudzee","online":false,"lastSeen":…,"playTimeSeconds":301103,"deaths":7,
 "economy":{"total":404,"gold":0,"silver":4,"bronze":4},
 "pokemonCounts":{"party":1,"pc":0}}
```

- `economy.total` est le contenu de la bourse, compté en pièces de bronze (1 argent = 100 bronze, 1 or = 10 000 bronze).
- Les pièces que le joueur porte dans son inventaire ne sont pas comptées.

La réponse contient aussi trois blocs. Les valeurs sont en direct si le joueur est en ligne, sinon ce sont celles de sa dernière sauvegarde.

```json
"status":{"health":14.0,"maxHealth":20.0,"food":12,"saturation":0.0,"xpLevel":7,
          "dimension":"nh_worldbuild:world_build","gameMode":"survival","armor":9,
          "armorItems":{"head":null,"chest":null,
                        "legs":{"id":"minecraft:diamond_leggings","name":"Diamond Leggings"},
                        "feet":{"id":"minecraft:diamond_boots","name":"Diamond Boots"}}},
"stats":{"playTimeSeconds":301103,"timeSinceDeathSeconds":1939,"deaths":7,"mobKills":699,"playerKills":0,
         "damageDealt":10604,"damageTaken":1643,"distanceKm":512.49,"jumps":68711,"fishCaught":0,
         "villagerTrades":0,"timesSlept":65,"sessions":150,"blocksMined":50852,"itemsCrafted":38044,"itemsUsed":83160},
"pokedex":{"seen":3,"caught":1,"total":905}
```

- `health`, `food` et `armor` sont en demi-cœurs, demi-cuisses et demi-plastrons, comme dans le jeu (20 = barre pleine).
- `damageDealt` et `damageTaken` sont en points de vie.

### `GET /api/players/<joueur>/pokemon`

```json
{"uuid":"…","name":"Rudzee","pokemon":{
  "party":[ <pokemon>, … ],
  "pc":{"count":0,"boxes":[{"index":0,"name":null,"pokemon":[ <pokemon>, … ]}, …]}
}}
```

Chaque `<pokemon>` ressemble à ceci :

```json
{"uuid":"…","species":"Eevee","speciesId":"cobblemon:eevee","dexNumber":133,"form":"Normal",
 "nickname":null,"level":10,"experience":1026,"shiny":false,"gender":"male","nature":"mild",
 "ability":"runaway","friendship":49,"ball":"cobblemon:poke_ball","heldItem":null,"originalTrainer":"Rudzee",
 "health":{"current":32,"max":32,"fainted":false},
 "ivs":{"hp":13,"atk":11,"def":5,"spa":23,"spd":30,"spe":9},
 "evs":{"hp":0,"atk":0,"def":0,"spa":0,"spd":0,"spe":1},
 "moves":[{"name":"quickattack","pp":29,"maxPp":30}],
 "aspects":["male"],"slot":0}
```

`slot` donne la place du Pokémon dans l'équipe (0 à 5) ou dans la boîte du PC (0 à 29).

### `GET /api/map.zip`

Renvoie le dossier web du mod [squaremap](https://github.com/jpenilla/squaremap), zippé : la page de la carte et ses images. Le site le télécharge et le publie dans son dossier `map/`.

Sur le serveur, squaremap (`squaremap-neoforge-mc1.21.1-1.3.2.jar`, uniquement côté serveur) est réglé dans `squaremap/config.yml` :
- seul le monde `nh_worldbuild:world_build` est activé (`world-settings.default.map.enabled: false`) ;
- son serveur web interne est coupé (`internal-webserver.enabled: false`) ;
- le rendu est limité à 2 threads, et à 1 thread pour les mises à jour en arrière-plan.

Pour redessiner toute la carte, tape dans la console : `squaremap fullrender nh_worldbuild:world_build`.

## Erreurs

| Code | Cas |
|---|---|
| `401` | Clé absente ou fausse, seulement si un `token` est défini |
| `404` | Joueur jamais connecté, ou route inconnue |
| `405` | Méthode autre que `GET` |
| `500` | Erreur interne (le détail est dans les logs du serveur) |

Le corps de la réponse est toujours de la forme `{"error":"…"}`.

## Build

Le build a besoin de `libs/` avec Cobblemon et `kotlin-stdlib`, en `compileOnly`. Ces jars ne sont pas inclus dans le repo : copie-les depuis le dossier `libs/` du mod Bounties.

```
sh ./gradlew build
```
