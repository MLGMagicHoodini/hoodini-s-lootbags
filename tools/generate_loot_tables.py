#!/usr/bin/env python3
"""
Hoodini's Lootbags - loot table generator
=========================================
Run this from anywhere:   python3 tools/generate_loot_tables.py
It rewrites the uncommon/rare/epic/legendary/mythic bag loot tables in
src/main/resources/data/hoodinislootbags/loot_table/bags/

Everything you may want to change is in the SETTINGS section below.
(The common bag is not touched - it has no gear.)
"""
import json, math, os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                   "src", "main", "resources", "data", "hoodinislootbags", "loot_table", "bags")

TIERS = ["uncommon", "rare", "epic", "legendary", "mythic"]

# =====================================================================
#                              SETTINGS
# =====================================================================

# Chance that an opened bag gives an enchanted piece of gear or a book
# (instead of a plain item like iron ingots or food).
ENCHANTED_SHARE = {"uncommon": 0.03, "rare": 0.05, "epic": 0.07, "legendary": 0.09, "mythic": 0.11}

# Of those enchanted drops, how many are enchanted BOOKS (the rest are gear).
BOOK_SHARE = 0.20

# Highest level an enchantment can roll in each bag, based on its vanilla maximum.
def level_cap(tier, vmax):
    if vmax == 1:                      # Mending, Infinity... levels above 1 do nothing
        return 1
    return {
        "uncommon":  min(vmax, 2),     # at most level II
        "rare":      min(vmax, 3),     # at most level III
        "epic":      vmax,             # vanilla maximum (Sharpness V, Unbreaking III...)
        "legendary": vmax + 3,         # a bit above vanilla (Sharpness VIII...)
        "mythic":    vmax * 2 + 5,     # far above vanilla (Sharpness XV...)
    }[tier]

# A rolled level is picked between ceil(cap * LOW_FRACTION) and the cap.
LOW_FRACTION = 0.5

# Which gear materials each bag can drop, and how likely each one is (relative numbers).
TOOL_MATERIALS = {   # swords, pickaxes, axes, shovels
    "uncommon":  {"wooden": 25, "stone": 40, "golden": 20, "iron": 15},
    "rare":      {"stone": 20, "golden": 20, "iron": 60},
    "epic":      {"golden": 20, "iron": 80},
    "legendary": {"iron": 75, "diamond": 25},
    "mythic":    {"iron": 15, "diamond": 65, "netherite": 20},
}
ARMOR_MATERIALS = {  # helmets, chestplates, leggings, boots
    "uncommon":  {"leather": 35, "chainmail": 25, "golden": 25, "iron": 15},
    "rare":      {"chainmail": 25, "golden": 20, "iron": 55},
    "epic":      {"chainmail": 10, "golden": 20, "iron": 70},
    "legendary": {"iron": 75, "diamond": 25},
    "mythic":    {"iron": 15, "diamond": 65, "netherite": 20},
}

# Gear types: (kind, relative weight, [(enchantment, chance to be on the item)])
# The FIRST enchantment of each item is always applied, so gear is never plain.
# Exclusive enchantments (Sharpness/Smite, Fortune/Silk Touch...) are deliberately not mixed.
GEAR = {
    "sword":      ("tool", 3, [("sharpness", 1.0), ("unbreaking", .6), ("looting", .5), ("fire_aspect", .4), ("sweeping_edge", .4), ("mending", .3)]),
    "pickaxe":    ("tool", 3, [("efficiency", 1.0), ("unbreaking", .6), ("fortune", .5), ("mending", .3)]),
    "axe":        ("tool", 2, [("efficiency", 1.0), ("sharpness", .5), ("unbreaking", .6), ("mending", .3)]),
    "shovel":     ("tool", 1, [("efficiency", 1.0), ("unbreaking", .6), ("fortune", .4), ("mending", .3)]),
    "helmet":     ("armor", 2, [("protection", 1.0), ("unbreaking", .6), ("respiration", .4), ("aqua_affinity", .4), ("thorns", .3), ("mending", .3)]),
    "chestplate": ("armor", 2, [("protection", 1.0), ("unbreaking", .6), ("thorns", .3), ("mending", .3)]),
    "leggings":   ("armor", 2, [("protection", 1.0), ("unbreaking", .6), ("thorns", .3), ("mending", .3)]),
    "boots":      ("armor", 2, [("protection", 1.0), ("unbreaking", .6), ("feather_falling", .5), ("depth_strider", .4), ("thorns", .3), ("mending", .3)]),
    "bow":        ("single", 2, [("power", 1.0), ("unbreaking", .6), ("punch", .4), ("flame", .4), ("infinity", .3), ("mending", .3)]),
    "crossbow":   ("single", 1, [("quick_charge", 1.0), ("unbreaking", .6), ("piercing", .5), ("mending", .3)]),
    "trident":    ("single", 1, [("impaling", 1.0), ("loyalty", .6), ("unbreaking", .6), ("mending", .3)]),
}

# Earliest bag in which something may appear (everything else is allowed from uncommon up).
MIN_TIER_ENCHANT = {"mending": "epic", "infinity": "epic"}
MIN_TIER_GEAR = {"trident": "mythic"}

# Enchantments that can show up on enchanted books (one enchantment per book).
BOOK_ENCHANTS = ["sharpness", "protection", "efficiency", "unbreaking", "fortune", "looting", "power",
                 "feather_falling", "thorns", "mending", "infinity", "respiration", "depth_strider",
                 "sweeping_edge", "fire_aspect", "flame", "punch", "quick_charge", "piercing"]

# Vanilla maximum level of every enchantment used above.
VANILLA_MAX = {"sharpness": 5, "looting": 3, "fire_aspect": 2, "sweeping_edge": 3, "unbreaking": 3, "mending": 1,
               "efficiency": 5, "fortune": 3, "protection": 4, "respiration": 3, "aqua_affinity": 1, "thorns": 3,
               "feather_falling": 4, "depth_strider": 3, "power": 5, "punch": 2, "flame": 1, "infinity": 1,
               "quick_charge": 3, "piercing": 4, "impaling": 5, "loyalty": 3}

# Plain (non-enchanted) drops: (item, weight, min count, max count)
PLAIN = {
    "uncommon": [("iron_ingot", 10, 2, 6), ("gold_ingot", 6, 1, 4), ("cooked_porkchop", 8, 4, 8), ("redstone", 8, 6, 14),
                 ("lapis_lazuli", 6, 4, 10), ("ender_pearl", 3, 1, 2), ("bone", 8, 4, 10)],
    "rare": [("diamond", 5, 1, 2), ("gold_block", 4, 1, 2), ("iron_block", 5, 1, 3), ("experience_bottle", 8, 3, 8),
             ("ender_pearl", 6, 2, 4), ("golden_apple", 3, 1, 1), ("emerald", 8, 2, 6)],
    "epic": [("diamond", 8, 2, 5), ("emerald_block", 4, 1, 3), ("golden_apple", 5, 1, 3), ("ancient_debris", 4, 1, 2),
             ("blaze_rod", 6, 2, 6), ("experience_bottle", 6, 8, 16), ("shulker_shell", 2, 1, 2)],
    "legendary": [("diamond_block", 5, 1, 3), ("netherite_scrap", 5, 1, 3), ("enchanted_golden_apple", 2, 1, 1),
                  ("totem_of_undying", 2, 1, 1), ("shulker_shell", 5, 2, 4), ("ancient_debris", 5, 2, 4),
                  ("experience_bottle", 5, 16, 32)],
    "mythic": [("netherite_ingot", 6, 1, 3), ("nether_star", 3, 1, 1), ("enchanted_golden_apple", 4, 1, 3),
               ("totem_of_undying", 3, 1, 2), ("beacon", 2, 1, 1), ("heart_of_the_sea", 3, 1, 1)],
}

# =====================================================================
#                       (the code below does the work)
# =====================================================================
K = 1000  # weight resolution

def uni(lo, hi):
    return {"type": "minecraft:uniform", "min": lo, "max": hi}

def level_range(tier, ench):
    cap = level_cap(tier, VANILLA_MAX[ench])
    lo = 1 if cap == 1 else max(1, math.ceil(cap * LOW_FRACTION))
    return lo, cap

def allowed(name, tier, table):
    return name not in table or TIERS.index(tier) >= TIERS.index(table[name])

def enchant_functions(tier, enchants):
    fns = []
    for name, chance in enchants:
        if not allowed(name, tier, MIN_TIER_ENCHANT):
            continue
        lo, cap = level_range(tier, name)
        fn = {"function": "minecraft:set_enchantments", "add": True,
              "enchantments": {"minecraft:" + name: cap if lo == cap else uni(lo, cap)}}
        if chance < 1.0:
            fn["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
        fns.append(fn)
    return fns

def build(tier):
    # plain items
    plain = []
    for name, w, lo, hi in PLAIN[tier]:
        plain.append({"type": "minecraft:item", "name": "minecraft:" + name, "weight": w * K,
                      "functions": [{"function": "minecraft:set_count", "count": uni(lo, hi)}]})
    plain_total = sum(e["weight"] for e in plain)

    # gear (relative weights first)
    gear = []
    for gtype, (kind, tw, enchants) in GEAR.items():
        if not allowed(gtype, tier, MIN_TIER_GEAR):
            continue
        fns = enchant_functions(tier, enchants)
        if kind == "single":
            gear.append((tw, "minecraft:" + gtype, fns))
        else:
            mats = (TOOL_MATERIALS if kind == "tool" else ARMOR_MATERIALS)[tier]
            msum = sum(mats.values())
            for mat, mw in mats.items():
                gear.append((tw * mw / msum, f"minecraft:{mat}_{gtype}", fns))
    gear_rel = sum(g[0] for g in gear)

    # books: one entry per (enchantment, level)
    books = []
    for ench in BOOK_ENCHANTS:
        if not allowed(ench, tier, MIN_TIER_ENCHANT):
            continue
        lo, cap = level_range(tier, ench)
        for lvl in range(lo, cap + 1):
            books.append({"type": "minecraft:item", "name": "minecraft:enchanted_book", "weight": 1,
                          "functions": [{"function": "minecraft:set_components", "components": {
                              "minecraft:stored_enchantments": {"levels": {"minecraft:" + ench: lvl}}}}]})
    # weight of one book entry so books are BOOK_SHARE of all enchanted drops
    # (every enchantment is equally likely to be the one on the book)
    enchanted_total = plain_total * ENCHANTED_SHARE[tier] / (1 - ENCHANTED_SHARE[tier])
    gear_total = enchanted_total * (1 - BOOK_SHARE)
    book_total = enchanted_total * BOOK_SHARE
    per_ench_levels = {}
    for b in books:
        e = list(b["functions"][0]["components"]["minecraft:stored_enchantments"]["levels"].keys())[0]
        per_ench_levels[e] = per_ench_levels.get(e, 0) + 1
    n_ench = len(per_ench_levels)

    entries = list(plain)
    for rel, name, fns in gear:
        entries.append({"type": "minecraft:item", "name": name,
                        "weight": max(1, round(rel / gear_rel * gear_total)), "functions": fns})
    for b in books:
        e = list(b["functions"][0]["components"]["minecraft:stored_enchantments"]["levels"].keys())[0]
        b["weight"] = max(1, round(book_total / n_ench / per_ench_levels[e]))
        entries.append(b)

    total = sum(e["weight"] for e in entries)
    ench_w = sum(e["weight"] for e in entries[len(plain):])
    print(f"{tier:10s} enchanted share {ench_w / total * 100:5.1f}%   entries: {len(entries)}")
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, tier + ".json"), "w") as f:
        json.dump({"type": "minecraft:gift", "pools": [{"rolls": 1, "entries": entries}]}, f, indent=2)
        f.write("\n")

if __name__ == "__main__":
    for t in TIERS:
        build(t)
