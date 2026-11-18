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
| `bindAddress` | `127.0.0.1` | `127.0.0.1` : l'API n'est joignable que par le reverse proxy HTTPS du VPS. `0.0.0.0` : joignable directement depuis Internet, en HTTP |
| `port` | `8091` | Port HTTP |
| `token` | vide | Vide : l'API est publique. Rempli : la clé devient obligatoire |
| `allowedOrigin` | `*` | En-tête CORS `Access-Control-Allow-Origin`, pour que le site (GitHub Pages) puisse appeler l'API depuis le navigateur |

Après une modification, redémarre le serveur.

## Accès

L'API est **publique** : les données ne sont pas sensibles, et le site l'appelle directement depuis le navigateur.

Si un `token` est défini, chaque route sauf `/api/health` exige la clé, sous l'une de ces deux formes : `Authorization: Bearer <token>` ou `X-Api-Key: <token>`. Sans elle, l'API répond `401`.

### HTTPS

Le site est servi en HTTPS par GitHub Pages, et un navigateur bloque les appels HTTP faits depuis une page HTTPS. L'API doit donc être derrière un reverse proxy HTTPS. Avec **Caddy**, qui obtient et renouvelle le certificat tout seul, le fichier `/etc/caddy/Caddyfile` contient :

```
vps-12df6529.vps.ovh.ca {
    reverse_proxy 127.0.0.1:8091
}
```

Les ports 80 et 443 du VPS doivent être ouverts. L'API est ensuite joignable à l'adresse `https://vps-12df6529.vps.ovh.ca/api/...`.

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
