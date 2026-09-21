#!/usr/bin/env python3
"""Генерация датапак-части мода: рецепты, добыча, теги, генерация мира,
обряды алтаря и достижения."""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from content import BLOCKS, ITEMS, SCHOOLS, SPELLS

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
DATA = os.path.join(ROOT, "src/main/resources/data")

NS = "arcanum"


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def mod(*parts):
    return os.path.join(DATA, NS, *parts)


def vanilla(*parts):
    return os.path.join(DATA, "minecraft", *parts)


def item(name):
    return name if ":" in name else f"{NS}:{name}"


# ----------------------------------------------------------------------
#  Верстак
# ----------------------------------------------------------------------

def shaped(name, pattern, key, result, count=1, category="misc"):
    write(mod("recipe", name + ".json"), {
        "type": "minecraft:crafting_shaped",
        "category": category,
        "pattern": pattern,
        "key": {k: item(v) for k, v in key.items()},
        "result": {"id": item(result), "count": count},
    })


def shapeless(name, ingredients, result, count=1, category="misc"):
    write(mod("recipe", name + ".json"), {
        "type": "minecraft:crafting_shapeless",
        "category": category,
        "ingredients": [item(i) for i in ingredients],
        "result": {"id": item(result), "count": count},
    })


def recipes():
    shapeless("arcane_dust", ["arcane_crystal"], "arcane_dust", 4)
    shaped("arcane_crystal_block",
           ["CCC", "CCC", "CCC"], {"C": "arcane_crystal"}, "arcane_crystal_block")
    shapeless("arcane_crystal_from_block", ["arcane_crystal_block"], "arcane_crystal", 9)

    shapeless("blank_scroll",
              ["minecraft:paper", "minecraft:paper", "minecraft:paper", "arcane_dust"],
              "blank_scroll", 3)

    shaped("apprentice_wand",
           ["  C", " S ", "S  "],
           {"C": "arcane_crystal", "S": "minecraft:stick"},
           "apprentice_wand")

    shaped("spellbook",
           [" C ", "CBC", " L "],
           {"C": "arcane_crystal", "B": "minecraft:book", "L": "minecraft:leather"},
           "spellbook")

    shaped("arcane_pedestal",
           ["SCS", " S ", "SSS"],
           {"S": "minecraft:stone_bricks", "C": "arcane_crystal"},
           "arcane_pedestal", 2)

    shaped("arcane_lamp",
           [" C ", "CGC", " C "],
           {"C": "arcane_crystal", "G": "minecraft:glowstone"},
           "arcane_lamp", 2)

    shaped("warded_stone",
           ["DOD", "OCO", "DOD"],
           {"D": "minecraft:deepslate_bricks", "O": "minecraft:obsidian", "C": "arcane_crystal"},
           "warded_stone", 4)

    shaped("teleport_anchor",
           ["CGC", "GEG", "CGC"],
           {"C": "arcane_crystal", "G": "minecraft:gold_ingot", "E": "minecraft:ender_pearl"},
           "teleport_anchor")

    shaped("infusion_altar",
           ["C C", "SGS", "SSS"],
           {"C": "arcane_crystal", "S": "minecraft:stone_bricks", "G": "minecraft:gold_ingot"},
           "infusion_altar")

    shapeless("mana_potion",
              ["minecraft:glass_bottle", "arcane_dust", "minecraft:lapis_lazuli"],
              "mana_potion")

    shapeless("dim_mana_crystal",
              ["arcane_crystal", "arcane_dust", "arcane_dust"],
              "dim_mana_crystal")

    for school in SCHOOLS:
        shaped("runestone_" + school,
               ["SSS", "SRS", "SSS"],
               {"S": "minecraft:stone", "R": "rune_" + school},
               "runestone_" + school, 8)


# ----------------------------------------------------------------------
#  Обряды алтаря
# ----------------------------------------------------------------------

def infusion(name, inputs, result, count=1, pedestals=0, mana=0, scroll_school=None):
    data = {
        "inputs": [{"item": item(i), "count": c} for (i, c) in inputs],
        "result": {"item": item(result), "count": count},
    }
    if pedestals:
        data["pedestals"] = pedestals
    if mana:
        data["mana"] = mana
    if scroll_school:
        data["scroll_school"] = scroll_school
    write(mod("infusion", name + ".json"), data)


RUNE_REAGENT = {
    "fire": ("minecraft:blaze_powder", 2),
    "frost": ("minecraft:packed_ice", 2),
    "storm": ("minecraft:copper_ingot", 2),
    "arcane": ("minecraft:amethyst_shard", 2),
    "nature": (f"{NS}:moonpetal", 2),
    "shadow": (f"{NS}:soul_shard", 2),
    "light": ("minecraft:glowstone_dust", 2),
}


def infusions():
    infusion("infused_crystal",
             [("arcane_crystal", 1), ("arcane_dust", 3), ("minecraft:lapis_lazuli", 1)],
             "infused_crystal", mana=30)

    infusion("lunar_essence",
             [("moonpetal", 2), ("arcane_dust", 1), ("minecraft:glowstone_dust", 1)],
             "lunar_essence", mana=25)

    infusion("soul_shard",
             [("arcane_crystal", 1), ("minecraft:soul_sand", 1), ("minecraft:bone", 2)],
             "soul_shard", count=2, mana=40)

    infusion("phoenix_feather",
             [("infused_crystal", 1), ("minecraft:blaze_rod", 1),
              ("minecraft:feather", 2), ("minecraft:magma_cream", 1)],
             "phoenix_feather", pedestals=2, mana=150)

    for school, (reagent, amount) in RUNE_REAGENT.items():
        infusion("rune_" + school,
                 [("infused_crystal", 1), (reagent, amount), ("arcane_dust", 1)],
                 "rune_" + school, mana=50)

        infusion("focus_" + school,
                 [("rune_" + school, 1), ("minecraft:gold_ingot", 1), ("infused_crystal", 1)],
                 "focus_" + school, pedestals=2, mana=100)

        # Запись свитка: школа задана, конкретное заклинание выбирает алтарь.
        infusion("scroll_" + school,
                 [("blank_scroll", 1), ("rune_" + school, 1), ("infused_crystal", 1)],
                 "spell_scroll", pedestals=1, mana=60, scroll_school=school)

    infusion("adept_staff",
             [("apprentice_wand", 1), ("infused_crystal", 2), ("minecraft:gold_ingot", 1)],
             "adept_staff", pedestals=2, mana=120)

    infusion("archmage_staff",
             [("adept_staff", 1), ("infused_crystal", 4),
              ("minecraft:diamond", 1), ("lunar_essence", 1)],
             "archmage_staff", pedestals=4, mana=260)

    infusion("eldritch_scepter",
             [("archmage_staff", 1), ("infused_crystal", 6),
              ("minecraft:nether_star", 1), ("soul_shard", 4)],
             "eldritch_scepter", pedestals=6, mana=600)

    infusion("charged_mana_crystal",
             [("dim_mana_crystal", 1), ("arcane_dust", 2)],
             "charged_mana_crystal", mana=120)

    infusion("mana_gem",
             [("infused_crystal", 4), ("minecraft:diamond", 1), ("lunar_essence", 1)],
             "mana_gem", pedestals=4, mana=350)

    infusion("greater_mana_potion",
             [("mana_potion", 2), ("infused_crystal", 1)],
             "greater_mana_potion", mana=60)

    infusion("mana_font",
             [("arcane_crystal_block", 1), ("infused_crystal", 4), ("lunar_essence", 1)],
             "mana_font", pedestals=4, mana=250)

    robes = [
        ("arcane_hood", "minecraft:leather_helmet", 2, 150),
        ("arcane_robe", "minecraft:leather_chestplate", 3, 200),
        ("arcane_leggings", "minecraft:leather_leggings", 3, 180),
        ("arcane_boots", "minecraft:leather_boots", 2, 150),
    ]
    for name, base, crystals, mana in robes:
        infusion(name,
                 [(base, 1), ("infused_crystal", crystals), ("lunar_essence", 1)],
                 name, pedestals=2, mana=mana)


# ----------------------------------------------------------------------
#  Добыча
# ----------------------------------------------------------------------

def self_drop(name):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": item(name)}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }


def ore_drop(name, drop, minimum, maximum):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{
                "type": "minecraft:item",
                "name": item(drop),
                "functions": [
                    {"function": "minecraft:set_count",
                     "count": {"type": "minecraft:uniform", "min": minimum, "max": maximum}},
                    {"function": "minecraft:apply_bonus",
                     "enchantment": "minecraft:fortune",
                     "formula": "minecraft:ore_drops"},
                    {"function": "minecraft:explosion_decay"},
                ],
            }],
        }],
    }


def loot_tables():
    special = {
        "arcane_crystal_ore": ore_drop("arcane_crystal_ore", "arcane_crystal", 2, 4),
        "deepslate_arcane_crystal_ore": ore_drop("deepslate_arcane_crystal_ore",
                                                 "arcane_crystal", 2, 4),
        "arcane_crystal_cluster": ore_drop("arcane_crystal_cluster", "arcane_crystal", 2, 3),
    }
    for name in BLOCKS:
        write(mod("loot_table", "blocks", name + ".json"),
              special.get(name, self_drop(name)))


# ----------------------------------------------------------------------
#  Теги
# ----------------------------------------------------------------------

def tag(values):
    return {"replace": False, "values": values}


def tags():
    write(mod("tags", "item", "robe_repair.json"),
          tag([item("infused_crystal")]))
    write(mod("tags", "item", "wands.json"),
          tag([item(n) for n in
               ("apprentice_wand", "adept_staff", "archmage_staff", "eldritch_scepter")]))
    write(mod("tags", "item", "focuses.json"),
          tag([item("focus_" + school) for school in SCHOOLS]))
    write(mod("tags", "item", "runes.json"),
          tag([item("rune_" + school) for school in SCHOOLS]))
    write(mod("tags", "block", "runestones.json"),
          tag([item("runestone_" + school) for school in SCHOOLS]))

    pickaxe = [item(name) for name, (_ru, _en, kind) in BLOCKS.items()
               if kind not in ("cross",)]
    write(vanilla("tags", "block", "mineable", "pickaxe.json"), tag(pickaxe))

    write(vanilla("tags", "block", "needs_stone_tool.json"),
          tag([item("arcane_crystal_ore"), item("deepslate_arcane_crystal_ore"),
               item("arcane_crystal_block"), item("arcane_crystal_cluster")]))
    write(vanilla("tags", "block", "needs_iron_tool.json"),
          tag([item("warded_stone"), item("infusion_altar"), item("mana_font"),
               item("teleport_anchor")]))


# ----------------------------------------------------------------------
#  Генерация мира
# ----------------------------------------------------------------------

def ore_targets():
    return [
        {"target": {"predicate_type": "minecraft:tag_match",
                    "tag": "minecraft:stone_ore_replaceables"},
         "state": {"Name": item("arcane_crystal_ore")}},
        {"target": {"predicate_type": "minecraft:tag_match",
                    "tag": "minecraft:deepslate_ore_replaceables"},
         "state": {"Name": item("deepslate_arcane_crystal_ore")}},
    ]


def worldgen():
    write(mod("worldgen", "configured_feature", "arcane_crystal_ore.json"), {
        "type": "minecraft:ore",
        "config": {"size": 5, "discard_chance_on_air_exposure": 0.0, "targets": ore_targets()},
    })
    write(mod("worldgen", "configured_feature", "arcane_crystal_ore_deep.json"), {
        "type": "minecraft:ore",
        "config": {"size": 8, "discard_chance_on_air_exposure": 0.0, "targets": ore_targets()},
    })

    write(mod("worldgen", "placed_feature", "arcane_crystal_ore_placed.json"), {
        "feature": item("arcane_crystal_ore"),
        "placement": [
            {"type": "minecraft:count", "count": 3},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range",
             "height": {"type": "minecraft:uniform",
                        "min_inclusive": {"absolute": -16},
                        "max_inclusive": {"absolute": 56}}},
            {"type": "minecraft:biome"},
        ],
    })
    write(mod("worldgen", "placed_feature", "arcane_crystal_ore_deep_placed.json"), {
        "feature": item("arcane_crystal_ore_deep"),
        "placement": [
            {"type": "minecraft:count", "count": 2},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range",
             "height": {"type": "minecraft:uniform",
                        "min_inclusive": {"absolute": -60},
                        "max_inclusive": {"absolute": -8}}},
            {"type": "minecraft:biome"},
        ],
    })

    write(mod("worldgen", "configured_feature", "moonpetal.json"), {
        "type": "minecraft:flower",
        "config": {
            "tries": 24,
            "xz_spread": 7,
            "y_spread": 3,
            "feature": {
                "feature": {
                    "type": "minecraft:simple_block",
                    "config": {"to_place": {"type": "minecraft:simple_state_provider",
                                            "state": {"Name": item("moonpetal")}}},
                },
                "placement": [{"type": "minecraft:block_predicate_filter",
                               "predicate": {"type": "minecraft:would_survive",
                                             "state": {"Name": item("moonpetal")}}}],
            },
        },
    })
    write(mod("worldgen", "placed_feature", "moonpetal_placed.json"), {
        "feature": item("moonpetal"),
        "placement": [
            {"type": "minecraft:rarity_filter", "chance": 14},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
            {"type": "minecraft:biome"},
        ],
    })


# ----------------------------------------------------------------------
#  Достижения
# ----------------------------------------------------------------------

def has_items(*names):
    return {
        "trigger": "minecraft:inventory_changed",
        "conditions": {"items": [{"items": item(name)} for name in names]},
    }


def advancement(name, parent, icon, frame, criteria, requirements=None, background=None):
    data = {
        "display": {
            "icon": {"id": item(icon)},
            "title": {"translate": f"advancements.arcanum.{name}.title"},
            "description": {"translate": f"advancements.arcanum.{name}.description"},
            "frame": frame,
            "show_toast": True,
            "announce_to_chat": True,
            "hidden": False,
        },
        "criteria": criteria,
    }
    if background:
        data["display"]["background"] = background
    if parent:
        data["parent"] = f"{NS}:{parent}"
    if requirements:
        data["requirements"] = requirements
    write(mod("advancement", name + ".json"), data)


def advancements():
    advancement("root", None, "arcane_crystal", "task",
                {"crystal": has_items("arcane_crystal")},
                background="arcanum:block/arcane_crystal_block")
    advancement("wand", "root", "apprentice_wand", "task",
                {"wand": has_items("apprentice_wand")})
    advancement("altar", "wand", "infusion_altar", "task",
                {"altar": has_items("infusion_altar")})
    advancement("scroll", "altar", "spell_scroll", "task",
                {"scroll": has_items("spell_scroll")})
    advancement("robe", "altar", "arcane_robe", "goal", {
        "hood": has_items("arcane_hood"),
        "robe": has_items("arcane_robe"),
        "leggings": has_items("arcane_leggings"),
        "boots": has_items("arcane_boots"),
    }, requirements=[["hood"], ["robe"], ["leggings"], ["boots"]])
    advancement("archmage", "altar", "eldritch_scepter", "challenge",
                {"scepter": has_items("eldritch_scepter")})


def main():
    recipes()
    infusions()
    loot_tables()
    tags()
    worldgen()
    advancements()
    print("Датапак готов.")


if __name__ == "__main__":
    main()
