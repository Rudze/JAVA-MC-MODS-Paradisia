"""Generates nh_bounties' Bountiful pools and decrees, priced like the Numismatic villager shops.

Usage: python3 tools/gen_bounties.py src/main/resources [path/to/Cobblemon.jar]
(the jar defaults to libs/Cobblemon-*.jar, which the build already needs)

Worth unit = 1 bronze coin (Numismatic: 100 bronze = 1 silver, 100 silver = 1 gold).
An item the villagers buy is worth their price (SHOP). Anything else, Pokémon actions included,
is priced from the minutes it takes, at the median bronze-per-minute the shop items pay.
Every quest then pays QUEST_BONUS times that, since players only get a few a day.
"""
import functools, glob, json, os, sys, zipfile

# Bronze per unit the Numismatic villagers pay (MC-NewHorizon-Mod-Numismatic-Overhaul's
# villager_trades). Sweet berries (60 each from the butcher) are left out: they farm far too fast.
SHOP = {"minecraft:wheat": 40 / 15, "minecraft:potato": 40 / 25, "minecraft:carrot": 40 / 25,
        "minecraft:beetroot": 40 / 15, "minecraft:pumpkin": 10, "minecraft:melon_slice": 50 / 9,
        "minecraft:coal": 10, "minecraft:raw_iron": 200 / 7, "minecraft:raw_gold": 200 / 3,
        "minecraft:quartz": 25, "minecraft:diamond": 3000, "minecraft:emerald": 1000,
        "minecraft:clay_ball": 2, "minecraft:sand": 60 / 25, "minecraft:flint": 6,
        "minecraft:cod": 20, "minecraft:salmon": 22.5, "minecraft:pufferfish": 50}
QUEST_BONUS = 1.5
OUT = sys.argv[1]
# Cobblemon jar whose spawn pools list the wild species.
COBBLEMON_JAR = sys.argv[2] if len(sys.argv) > 2 else \
    sorted(glob.glob(os.path.join(os.path.dirname(__file__), "..", "libs", "Cobblemon-*.jar")))[-1]

# Spawn bucket -> (minutes to find and catch one, amount min, amount max, rarity).
BUCKETS = {"common": (3, 1, 3, None), "uncommon": (8, 1, 2, "UNCOMMON"),
           "rare": (20, 1, 1, "RARE"), "ultra-rare": (60, 1, 1, "EPIC")}
WILD_SPECIES_WEIGHT = 20

@functools.cache
def wild_species():
    """Species id -> its most common spawn bucket, from Cobblemon's world spawn pools."""
    order = list(BUCKETS)
    best = {}
    with zipfile.ZipFile(COBBLEMON_JAR) as jar:
        for name in jar.namelist():
            if not (name.startswith("data/cobblemon/spawn_pool_world/") and name.endswith(".json")):
                continue
            pool = json.loads(jar.read(name))
            if not pool.get("enabled", True) or pool.get("neededInstalledMods"):
                continue
            for spawn in pool.get("spawns", []):
                if spawn.get("type") == "pokemon" and spawn.get("bucket") in BUCKETS:
                    sp = spawn["pokemon"].split()[0]
                    best[sp] = min(best.get(sp, len(order)), order.index(spawn["bucket"]))
    return {sp: order[i] for sp, i in sorted(best.items())}

def w(bronze):
    return max(1, round(bronze * QUEST_BONUS))

ITEM_MINUTES = {}

def item(content, minutes, lo, hi, rarity=None):
    ITEM_MINUTES[content] = minutes
    e = {"type": "item", "content": content, "amount": {"min": lo, "max": hi},
         "unitWorth": lambda: w(SHOP[content] if content in SHOP else minutes * per_minute())}
    if rarity: e["rarity"] = rarity
    return e

def poke(kind, selector, minutes, lo, hi, rarity=None):
    e = {"type": "nh_bounties:" + kind, "content": selector, "amount": {"min": lo, "max": hi},
         "unitWorth": lambda: w(minutes * per_minute())}
    if rarity: e["rarity"] = rarity
    return e

@functools.cache
def per_minute():
    """Median bronze per minute of the shop items, using the minutes estimated for each quest."""
    rates = sorted(SHOP[c] / ITEM_MINUTES[c] for c in SHOP)
    return rates[len(rates) // 2]

def key(*parts):
    return "_".join(p.replace("cobblemon:", "").replace("minecraft:", "").replace("species:", "").replace("type:", "").replace(":", "_") for p in parts)

pools = {}

# --- Pokémon : Cobblemon actions, on every board -----------------------------------------------
d = {}
d["catch_any"] = poke("catch", "any", 2.0, 2, 8)
for t in ["normal", "water", "grass", "bug", "flying", "poison"]:
    d[key("catch", t)] = poke("catch", "type:" + t, 3.5, 2, 5)
for t in ["fire", "electric", "rock", "ground", "fighting", "psychic", "fairy"]:
    d[key("catch", t)] = poke("catch", "type:" + t, 6, 1, 4, "UNCOMMON")
for t in ["ice", "ghost", "dark", "steel"]:
    d[key("catch", t)] = poke("catch", "type:" + t, 10, 1, 3, "RARE")
d["catch_dragon"] = poke("catch", "type:dragon", 25, 1, 2, "EPIC")
# One capture quest per species that spawns in the wild, priced by its commonest spawn
# bucket; their weights add up to WILD_SPECIES_WEIGHT entries so they don't crowd out the rest.
for sp, bucket in wild_species().items():
    minutes, lo, hi, rar = BUCKETS[bucket]
    e = poke("catch", "species:" + sp, minutes, lo, hi, rar)
    e["weightMult"] = round(WILD_SPECIES_WEIGHT / len(wild_species()), 4)
    d[key("catch", sp)] = e
d["catch_shiny"] = poke("catch", "shiny", 180, 1, 1, "LEGENDARY")
d["defeat_any"] = poke("defeat", "any", 0.6, 5, 25)
for t in ["normal", "water", "grass", "bug", "flying"]:
    d[key("defeat", t)] = poke("defeat", "type:" + t, 1.2, 3, 12)
d["defeat_dragon"] = poke("defeat", "type:dragon", 8, 1, 3, "EPIC")
d["evolve_any"] = poke("evolve", "any", 8, 1, 3, "UNCOMMON")
d["level_up_any"] = poke("level_up", "any", 1.0, 5, 20)
d["heal_any"] = poke("heal", "any", 0.5, 2, 6)
# Shared with the other themes' decrees, where it should take about a third of the quests.
pools["nh_pokemon_objs"] = {"weightMult": 0.2, "content": d}

# --- Récolte : farming, berries, apricorns -----------------------------------------------------
r = {}
for c, minutes, lo, hi in [("minecraft:wheat", 0.06, 16, 64), ("minecraft:carrot", 0.055, 16, 64),
                           ("minecraft:potato", 0.055, 16, 64), ("minecraft:beetroot", 0.07, 16, 48),
                           ("minecraft:sugar_cane", 0.055, 16, 64), ("minecraft:pumpkin", 0.15, 4, 24),
                           ("minecraft:melon_slice", 0.02, 32, 128), ("minecraft:cocoa_beans", 0.09, 8, 32),
                           ("minecraft:sweet_berries", 0.04, 16, 64), ("minecraft:cactus", 0.07, 8, 32),
                           ("minecraft:bamboo", 0.015, 32, 128), ("minecraft:kelp", 0.025, 32, 96)]:
    r[key(c)] = item(c, minutes, lo, hi)
for b in ["oran", "pecha", "cheri", "chesto", "rawst", "aspear", "leppa"]:
    r[key(b, "berry")] = item("cobblemon:%s_berry" % b, 0.25, 4, 16)
r["sitrus_berry"] = item("cobblemon:sitrus_berry", 0.5, 2, 8, "UNCOMMON")
for c in ["red", "blue", "yellow", "green", "pink", "black", "white"]:
    r[key(c, "apricorn")] = item("cobblemon:%s_apricorn" % c, 0.35, 3, 12)
r["medicinal_leek"] = item("cobblemon:medicinal_leek", 0.4, 2, 8, "UNCOMMON")
r["energy_root"] = item("cobblemon:energy_root", 0.6, 1, 6, "UNCOMMON")
r["vivichoke"] = item("cobblemon:vivichoke", 0.6, 1, 6, "UNCOMMON")
r["revival_herb"] = item("cobblemon:revival_herb", 1.0, 1, 4, "RARE")
pools["nh_recolte_objs"] = r

# --- Mineur ------------------------------------------------------------------------------------
m = {}
for c, minutes, lo, hi, rar in [
    ("minecraft:cobblestone", 0.005, 64, 256, None), ("minecraft:cobbled_deepslate", 0.008, 64, 192, None),
    ("minecraft:sand", 0.006, 64, 192, None), ("minecraft:gravel", 0.007, 64, 192, None),
    ("minecraft:clay_ball", 0.04, 16, 64, None), ("minecraft:flint", 0.05, 8, 32, None),
    ("minecraft:coal", 0.15, 8, 48, None), ("minecraft:raw_copper", 0.12, 8, 48, None),
    ("minecraft:raw_iron", 0.28, 4, 32, None), ("minecraft:redstone", 0.07, 16, 64, None),
    ("minecraft:lapis_lazuli", 0.13, 8, 48, None), ("minecraft:amethyst_shard", 0.07, 8, 32, None),
    ("minecraft:raw_gold", 0.55, 2, 16, "UNCOMMON"), ("minecraft:obsidian", 0.25, 4, 16, "UNCOMMON"),
    ("minecraft:quartz", 0.09, 16, 64, "UNCOMMON"), ("minecraft:diamond", 3.0, 1, 4, "RARE"),
    ("minecraft:emerald", 4.0, 1, 3, "RARE")]:
    m[key(c)] = item(c, minutes, lo, hi, rar)
for s in ["fire", "water", "thunder", "leaf", "moon", "sun", "shiny", "dusk", "dawn", "ice"]:
    m[key(s, "stone")] = item("cobblemon:%s_stone" % s, 2.5, 1, 2, "UNCOMMON")
pools["nh_mineur_objs"] = m

# --- Nature : wood and fishing -----------------------------------------------------------------
n = {}
# nh_treefelling drops a whole tree at once.
for l in ["oak", "birch", "spruce", "jungle", "acacia", "dark_oak", "cherry", "mangrove"]:
    n[key(l, "log")] = item("minecraft:%s_log" % l, 0.02, 16, 64)
for c, minutes, lo, hi, rar in [("minecraft:cod", 0.4, 2, 12, None), ("minecraft:salmon", 0.45, 2, 12, None),
                                ("minecraft:tropical_fish", 1.0, 1, 4, "UNCOMMON"),
                                ("minecraft:pufferfish", 1.0, 1, 4, "UNCOMMON")]:
    n[key(c)] = item(c, minutes, lo, hi, rar)
pools["nh_nature_objs"] = n

# --- Rewards -----------------------------------------------------------------------------------
for pool in pools.values():
    for e in pool.get("content", pool).values():
        e["unitWorth"] = e["unitWorth"]()

def coins(top, currency=False):
    """Coin rewards; Bountiful warns when the top two are worth more than top / 0.9."""
    c = {"bronze": {"type": "item", "content": "numismaticoverhaul:bronze_coin", "unitWorth": 1, "amount": {"min": 1, "max": 99}}}
    budget = top / 0.9
    silver = min(99, max(1, int((budget - 99) // 100)))
    c["argent"] = {"type": "item", "content": "numismaticoverhaul:silver_coin", "unitWorth": 100, "amount": {"min": 1, "max": silver}}
    gold = int((budget - silver * 100) // 10000)
    if gold:
        c["or"] = {"type": "item", "content": "numismaticoverhaul:gold_coin", "unitWorth": 10000, "amount": {"min": 1, "max": min(64, gold)}}
    body = {"content": c}
    if currency:
        body["currency"] = True
    return body

def top_worth(pool_names):
    return max(e["amount"]["max"] * e["unitWorth"] for n in pool_names for e in pools[n].get("content", pools[n]).values())

themes = {
    "nh_dresseur": ("Dresseur", [], []),
    "nh_recolte": ("Récolte", ["nh_recolte_objs"], ["farmer"]),
    "nh_mineur": ("Mineur", ["nh_mineur_objs"], ["toolsmith", "armorer", "weaponsmith"]),
    "nh_nature": ("Nature", ["nh_nature_objs"], ["fisherman"]),
}
decrees = {}
for name, (label, objs, profs) in themes.items():
    objs = objs + ["nh_pokemon_objs"]
    pools[name + "_rews"] = coins(top_worth(objs))
    decrees[name] = (label, objs, profs, [name + "_rews"])
# Reverse matching on: Bountiful picks the objectives first, then tops the rewards up from this
# pool. (Off, it picks rewards first and fills the objectives from it: coins for coins.)
pools["nh_monnaie_rews"] = coins(10 ** 9, currency=True)

base = os.path.join(OUT, "data", "nh_bounties")
os.makedirs(os.path.join(base, "bounty_pools", "paradisia"), exist_ok=True)
os.makedirs(os.path.join(base, "bounty_decrees", "paradisia"), exist_ok=True)
for name, pool in pools.items():
    body = pool if "content" in pool else {"content": pool}
    json.dump(body, open(os.path.join(base, "bounty_pools", "paradisia", name + ".json"), "w"), indent=2, ensure_ascii=False)
for name, (label, objs, profs, rews) in decrees.items():
    json.dump({"name": label, "linkedProfessions": profs, "objectives": objs, "rewards": rews},
              open(os.path.join(base, "bounty_decrees", "paradisia", name + ".json"), "w"), indent=2, ensure_ascii=False)

print("bronze par minute :", round(per_minute(), 1))
for name, pool in pools.items():
    content = pool.get("content", pool)
    print(name, len(content))
