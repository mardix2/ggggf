#!/usr/bin/env python3
"""Генерация JSON-ресурсов мода: модели, состояния блоков, снаряжение,
рецепты, таблицы добычи, теги, генерация мира, типы урона и достижения.

Переводы лежат рядом в lang/*.json и копируются как есть.
Запуск:  python3 tools/gen_data.py
"""

import json
import os
import shutil

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/ironman")
DATA = os.path.join(RES, "data")
NS = "ironman"

SUITS = ["mark_i", "mark_iii", "mark_85"]
PARTS = ["helmet", "chestplate", "leggings", "boots"]

MATERIALS = ["raw_palladium", "palladium_ingot", "gold_titanium_alloy", "starkium", "nanotech_core"]
COMPONENTS = ["repulsor_node", "jet_thruster", "micro_missile", "power_cell"]
REACTORS = ["arc_reactor_mk1", "arc_reactor_mk2", "starkium_arc_reactor"]
ARMOR = [f"{suit}_{part}" for suit in SUITS for part in PARTS]
ITEMS = MATERIALS + COMPONENTS + REACTORS + ARMOR
BLOCKS = ["palladium_ore", "deepslate_palladium_ore", "palladium_block"]

DAMAGE_TYPES = {
    "repulsor": 0.1,
    "unibeam": 0.1,
    "micro_missile": 0.1,
    "flamethrower": 0.1,
}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def ns(name):
    return f"{NS}:{name}"


# ---------------------------------------------------------------------------
#  Ассеты
# ---------------------------------------------------------------------------

def assets():
    for item in ITEMS:
        write(f"{ASSETS}/items/{item}.json",
              {"model": {"type": "minecraft:model", "model": ns(f"item/{item}")}})
        write(f"{ASSETS}/models/item/{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": ns(f"item/{item}")}})

    for block in BLOCKS:
        write(f"{ASSETS}/blockstates/{block}.json",
              {"variants": {"": {"model": ns(f"block/{block}")}}})
        write(f"{ASSETS}/models/block/{block}.json",
              {"parent": "minecraft:block/cube_all", "textures": {"all": ns(f"block/{block}")}})
        write(f"{ASSETS}/items/{block}.json",
              {"model": {"type": "minecraft:model", "model": ns(f"block/{block}")}})

    # Внешний вид костюмов: верхний слой (шлем, корпус, руки, ботинки)
    # и нижний (поножи). Текстуры — textures/entity/equipment/<слой>/<модель>.png.
    for suit in SUITS:
        write(f"{ASSETS}/equipment/{suit}.json", {
            "layers": {
                "humanoid": [{"texture": ns(suit)}],
                "humanoid_leggings": [{"texture": ns(suit)}],
            }
        })

    for lang in ("en_us", "ru_ru"):
        src = os.path.join(ROOT, "tools", "lang", f"{lang}.json")
        dst = os.path.join(ASSETS, "lang", f"{lang}.json")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(src, dst)


# ---------------------------------------------------------------------------
#  Рецепты
# ---------------------------------------------------------------------------

def shaped(name, pattern, key, result=None, count=1, category="misc"):
    write(f"{DATA}/{NS}/recipe/{name}.json", {
        "type": "minecraft:crafting_shaped",
        "category": category,
        "pattern": pattern,
        "key": key,
        "result": {"id": result or ns(name), "count": count},
    })


def shapeless(name, ingredients, result=None, count=1, category="misc"):
    write(f"{DATA}/{NS}/recipe/{name}.json", {
        "type": "minecraft:crafting_shapeless",
        "category": category,
        "ingredients": ingredients,
        "result": {"id": result or ns(name), "count": count},
    })


def cooking(name, kind, ingredient, result, experience, time):
    write(f"{DATA}/{NS}/recipe/{name}.json", {
        "type": f"minecraft:{kind}",
        "category": "misc",
        "ingredient": ingredient,
        "result": {"id": result},
        "experience": experience,
        "cookingtime": time,
    })


def recipes():
    ingot = ns("palladium_ingot")
    for source in ("raw_palladium", "palladium_ore", "deepslate_palladium_ore"):
        cooking(f"palladium_ingot_from_smelting_{source}", "smelting", ns(source), ingot, 0.7, 200)
        cooking(f"palladium_ingot_from_blasting_{source}", "blasting", ns(source), ingot, 0.7, 100)

    shaped("palladium_block", ["PPP", "PPP", "PPP"], {"P": ingot}, category="building")
    shapeless("palladium_ingot_from_block", [ns("palladium_block")], result=ingot, count=9)

    shaped("gold_titanium_alloy", ["GIG", "IPI", "GIG"],
           {"G": "minecraft:gold_ingot", "I": "minecraft:iron_ingot", "P": ingot}, count=4)
    shaped("starkium", ["APA", "PDP", "APA"],
           {"A": "minecraft:amethyst_shard", "P": ingot, "D": "minecraft:diamond"}, count=2)
    shaped("nanotech_core", ["RSR", "SNS", "RSR"],
           {"R": "minecraft:redstone", "S": ns("starkium"), "N": "minecraft:netherite_ingot"})

    shaped("repulsor_node", ["IGI", "GPG", "IGI"],
           {"I": "minecraft:iron_ingot", "G": "minecraft:glowstone_dust", "P": ingot}, count=2)
    shaped("jet_thruster", ["I I", "IFI", "IGI"],
           {"I": "minecraft:iron_ingot", "F": "minecraft:furnace", "G": "minecraft:gunpowder"}, count=2)
    shapeless("micro_missile", ["minecraft:iron_ingot", "minecraft:gunpowder", "minecraft:gunpowder", ingot],
              count=8, category="equipment")
    shaped("power_cell", ["NRN", "NPN", "NRN"],
           {"N": "minecraft:iron_nugget", "R": "minecraft:redstone", "P": ingot}, count=2)

    shaped("arc_reactor_mk1", ["ICI", "RPR", "ICI"],
           {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone", "P": ingot},
           category="equipment")
    shaped("arc_reactor_mk2", ["ARA", "RMR", "ARA"],
           {"A": ns("gold_titanium_alloy"), "R": "minecraft:redstone_block", "M": ns("arc_reactor_mk1")},
           category="equipment")
    shaped("starkium_arc_reactor", ["SDS", "DMD", "SDS"],
           {"S": ns("starkium"), "D": "minecraft:diamond", "M": ns("arc_reactor_mk2")},
           category="equipment")

    # Mark I — «собран в пещере из ящика обрезков»: железо, палладий, сопла.
    iron = "minecraft:iron_ingot"
    shaped("mark_i_helmet", ["IPI", "I I"], {"I": iron, "P": ingot}, category="equipment")
    shaped("mark_i_chestplate", ["I I", "IPI", "III"], {"I": iron, "P": ingot}, category="equipment")
    shaped("mark_i_leggings", ["III", "IPI", "I I"], {"I": iron, "P": ingot}, category="equipment")
    shaped("mark_i_boots", ["I I", "T T"], {"I": iron, "T": ns("jet_thruster")}, category="equipment")

    # Mark III — золото-титановый сплав, репульсоры, полноценные сопла.
    alloy = ns("gold_titanium_alloy")
    shaped("mark_iii_helmet", ["AAA", "AGA"], {"A": alloy, "G": "minecraft:glass_pane"}, category="equipment")
    shaped("mark_iii_chestplate", ["R R", "AMA", "AAA"],
           {"A": alloy, "R": ns("repulsor_node"), "M": ns("mark_i_chestplate")}, category="equipment")
    shaped("mark_iii_leggings", ["AAA", "A A", "A A"], {"A": alloy}, category="equipment")
    shaped("mark_iii_boots", ["A A", "T T"], {"A": alloy, "T": ns("jet_thruster")}, category="equipment")

    # Mark 85 — улучшение Mark III на кузнечном столе нанотехнологическим ядром.
    for part in PARTS:
        write(f"{DATA}/{NS}/recipe/mark_85_{part}_smithing.json", {
            "type": "minecraft:smithing_transform",
            "template": "minecraft:netherite_upgrade_smithing_template",
            "base": ns(f"mark_iii_{part}"),
            "addition": ns("nanotech_core"),
            "result": {"id": ns(f"mark_85_{part}")},
        })


# ---------------------------------------------------------------------------
#  Добыча, теги, генерация
# ---------------------------------------------------------------------------

def ore_loot(block):
    silk = {
        "condition": "minecraft:match_tool",
        "predicate": {
            "predicates": {
                "minecraft:enchantments": [
                    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}
                ]
            }
        },
    }
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{
                "type": "minecraft:alternatives",
                "children": [
                    {"type": "minecraft:item", "name": ns(block), "conditions": [silk]},
                    {
                        "type": "minecraft:item",
                        "name": ns("raw_palladium"),
                        "functions": [
                            {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                             "formula": "minecraft:ore_drops"},
                            {"function": "minecraft:explosion_decay"},
                        ],
                    },
                ],
            }],
        }],
    }


def self_loot(block):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": ns(block)}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }


def loot_and_tags():
    for block in ("palladium_ore", "deepslate_palladium_ore"):
        write(f"{DATA}/{NS}/loot_table/blocks/{block}.json", ore_loot(block))
    write(f"{DATA}/{NS}/loot_table/blocks/palladium_block.json", self_loot("palladium_block"))

    all_blocks = [ns(b) for b in BLOCKS]
    write(f"{DATA}/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": all_blocks})
    write(f"{DATA}/minecraft/tags/block/needs_iron_tool.json", {"replace": False, "values": all_blocks})
    write(f"{DATA}/minecraft/tags/item/beacon_payment_items.json",
          {"replace": False, "values": [ns("palladium_ingot")]})
    write(f"{DATA}/minecraft/tags/block/beacon_base_blocks.json",
          {"replace": False, "values": [ns("palladium_block")]})

    write(f"{DATA}/c/tags/block/ores.json",
          {"replace": False, "values": [ns("palladium_ore"), ns("deepslate_palladium_ore")]})
    write(f"{DATA}/c/tags/item/ores.json",
          {"replace": False, "values": [ns("palladium_ore"), ns("deepslate_palladium_ore")]})
    write(f"{DATA}/c/tags/item/ingots.json", {"replace": False, "values": [ns("palladium_ingot")]})
    write(f"{DATA}/c/tags/item/raw_materials.json", {"replace": False, "values": [ns("raw_palladium")]})

    write(f"{DATA}/{NS}/tags/item/repairs_mark_i.json", {"values": ["minecraft:iron_ingot"]})
    write(f"{DATA}/{NS}/tags/item/repairs_mark_iii.json", {"values": [ns("gold_titanium_alloy")]})
    write(f"{DATA}/{NS}/tags/item/repairs_mark_85.json", {"values": [ns("nanotech_core"), ns("starkium")]})

    # Костюмы — это броня: их можно зачаровывать как броню.
    helmets = [ns(f"{s}_helmet") for s in SUITS]
    chests = [ns(f"{s}_chestplate") for s in SUITS]
    legs = [ns(f"{s}_leggings") for s in SUITS]
    feet = [ns(f"{s}_boots") for s in SUITS]
    write(f"{DATA}/minecraft/tags/item/head_armor.json", {"replace": False, "values": helmets})
    write(f"{DATA}/minecraft/tags/item/chest_armor.json", {"replace": False, "values": chests})
    write(f"{DATA}/minecraft/tags/item/leg_armor.json", {"replace": False, "values": legs})
    write(f"{DATA}/minecraft/tags/item/foot_armor.json", {"replace": False, "values": feet})


def worldgen():
    write(f"{DATA}/{NS}/worldgen/configured_feature/palladium_ore.json", {
        "type": "minecraft:ore",
        "config": {
            "size": 6,
            "discard_chance_on_air_exposure": 0.0,
            "targets": [
                {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
                 "state": {"Name": ns("palladium_ore")}},
                {"target": {"predicate_type": "minecraft:tag_match",
                            "tag": "minecraft:deepslate_ore_replaceables"},
                 "state": {"Name": ns("deepslate_palladium_ore")}},
            ],
        },
    })

    def placed(name, count, height):
        write(f"{DATA}/{NS}/worldgen/placed_feature/{name}.json", {
            "feature": ns("palladium_ore"),
            "placement": [
                {"type": "minecraft:count", "count": count},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": height},
                {"type": "minecraft:biome"},
            ],
        })

    placed("palladium_ore_placed", 6, {
        "type": "minecraft:uniform",
        "min_inclusive": {"absolute": -16},
        "max_inclusive": {"absolute": 56},
    })
    placed("palladium_ore_deep_placed", 4, {
        "type": "minecraft:trapezoid",
        "min_inclusive": {"above_bottom": 0},
        "max_inclusive": {"absolute": 0},
    })


def damage_types():
    for name, exhaustion in DAMAGE_TYPES.items():
        write(f"{DATA}/{NS}/damage_type/{name}.json", {
            "message_id": f"{NS}.{name}",
            "scaling": "when_caused_by_living_non_player",
            "exhaustion": exhaustion,
        })
    write(f"{DATA}/minecraft/tags/damage_type/is_explosion.json",
          {"replace": False, "values": [ns("micro_missile")]})
    write(f"{DATA}/minecraft/tags/damage_type/is_projectile.json",
          {"replace": False, "values": [ns("micro_missile")]})
    write(f"{DATA}/minecraft/tags/damage_type/is_fire.json",
          {"replace": False, "values": [ns("flamethrower")]})


# ---------------------------------------------------------------------------
#  Достижения
# ---------------------------------------------------------------------------

def has_items(*items):
    return {"trigger": "minecraft:inventory_changed",
            "conditions": {"items": [{"items": item} for item in items]}}


def advancement(name, icon, criteria, parent=None, frame="task", background=None, requirements=None):
    display = {
        "icon": {"id": icon},
        "title": {"translate": f"advancements.{NS}.{name}.title"},
        "description": {"translate": f"advancements.{NS}.{name}.description"},
        "frame": frame,
        "show_toast": True,
        "announce_to_chat": True,
        "hidden": False,
    }
    if background:
        display["background"] = background
    data = {"display": display, "criteria": criteria}
    if parent:
        data["parent"] = ns(parent)
    if requirements:
        data["requirements"] = requirements
    write(f"{DATA}/{NS}/advancement/{name}.json", data)


def advancements():
    advancement("root", ns("raw_palladium"), {"palladium": has_items(ns("raw_palladium"))},
                background=ns("block/palladium_block"))
    advancement("arc_reactor", ns("arc_reactor_mk1"), {"reactor": has_items(ns("arc_reactor_mk1"))},
                parent="root")
    advancement("mark_i", ns("mark_i_helmet"),
                {part: has_items(ns(f"mark_i_{part}")) for part in PARTS},
                parent="arc_reactor", requirements=[[part] for part in PARTS])
    advancement("mark_iii", ns("mark_iii_helmet"),
                {part: has_items(ns(f"mark_iii_{part}")) for part in PARTS},
                parent="mark_i", frame="goal", requirements=[[part] for part in PARTS])
    advancement("starkium", ns("starkium_arc_reactor"), {"reactor": has_items(ns("starkium_arc_reactor"))},
                parent="mark_iii", frame="goal")
    advancement("mark_85", ns("mark_85_chestplate"),
                {part: has_items(ns(f"mark_85_{part}")) for part in PARTS},
                parent="starkium", frame="challenge", requirements=[[part] for part in PARTS])


def main():
    assets()
    recipes()
    loot_and_tags()
    worldgen()
    damage_types()
    advancements()
    print("данные готовы")


if __name__ == "__main__":
    main()
