#!/usr/bin/env python3
"""Офлайн-проверка ресурсов мода.

Проверяет то, что обычно ломается незаметно до запуска игры:
  * все JSON разбираются;
  * у каждого предмета и блока из Java есть модель, текстура и перевод;
  * каждый ключ перевода, упомянутый в Java, есть в обоих языках,
    а наборы ключей en_us и ru_ru совпадают;
  * рецепты, добыча, теги и достижения ссылаются на существующие предметы;
  * текстуры брони на месте и имеют размер 64x32.

Запуск:  python3 tools/validate.py
"""

import glob
import json
import os
import re
import struct
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SRC = os.path.join(ROOT, "src/main/java/net/ironman")
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/ironman")
NS = "ironman"

problems = []


def fail(message):
    problems.append(message)


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def java_sources():
    return {path: read(path) for path in glob.glob(SRC + "/**/*.java", recursive=True)}


def registered_names(sources):
    """Имена предметов и блоков, как их регистрирует Java-код."""
    items = set()
    blocks = set()
    for path, src in sources.items():
        if path.endswith("ModItems.java"):
            items.update(re.findall(r'register\("([a-z0-9_]+)"', src))
        if path.endswith("ModBlocks.java"):
            blocks.update(re.findall(r'register\("([a-z0-9_]+)"', src))
    # Части костюмов регистрируются в цикле: модель × слот.
    tiers = re.findall(r'^\s+(MARK_\w+)\("(\w+)"', read(os.path.join(SRC, "suit/SuitTier.java")), re.M)
    for _, tier_id in tiers:
        for part in ("helmet", "chestplate", "leggings", "boots"):
            items.add(f"{tier_id}_{part}")
    return items, blocks


def png_size(path):
    with open(path, "rb") as handle:
        head = handle.read(24)
    return struct.unpack(">II", head[16:24])


def check_json():
    parsed = {}
    for path in glob.glob(RES + "/**/*.json", recursive=True):
        try:
            with open(path, encoding="utf-8") as handle:
                parsed[path] = json.load(handle)
        except (ValueError, OSError) as error:
            fail(f"JSON не читается: {os.path.relpath(path, ROOT)}: {error}")
    return parsed


def check_assets(items, blocks):
    for item in sorted(items):
        if not os.path.exists(f"{ASSETS}/items/{item}.json"):
            fail(f"нет items/{item}.json")
        if not os.path.exists(f"{ASSETS}/models/item/{item}.json"):
            fail(f"нет models/item/{item}.json")
        if not os.path.exists(f"{ASSETS}/textures/item/{item}.png"):
            fail(f"нет textures/item/{item}.png")
        elif png_size(f"{ASSETS}/textures/item/{item}.png") != (16, 16):
            fail(f"textures/item/{item}.png не 16x16")
    for block in sorted(blocks):
        for path in (f"blockstates/{block}.json", f"models/block/{block}.json",
                     f"items/{block}.json", f"textures/block/{block}.png"):
            if not os.path.exists(f"{ASSETS}/{path}"):
                fail(f"нет {path}")
        if not os.path.exists(f"{RES}/data/{NS}/loot_table/blocks/{block}.json"):
            fail(f"нет таблицы добычи для {block}")

    for path in glob.glob(f"{ASSETS}/equipment/*.json"):
        suit = os.path.basename(path)[:-5]
        for layer in ("humanoid", "humanoid_leggings"):
            texture = f"{ASSETS}/textures/entity/equipment/{layer}/{suit}.png"
            if not os.path.exists(texture):
                fail(f"нет текстуры брони {layer}/{suit}.png")
            elif png_size(texture) != (64, 32):
                fail(f"текстура брони {layer}/{suit}.png не 64x32")

    # Модели ссылаются на существующие текстуры.
    for path in glob.glob(f"{ASSETS}/models/**/*.json", recursive=True):
        model = json.loads(read(path))
        for texture in model.get("textures", {}).values():
            if texture.startswith(NS + ":"):
                file = f"{ASSETS}/textures/{texture.split(':', 1)[1]}.png"
                if not os.path.exists(file):
                    fail(f"{os.path.relpath(path, ASSETS)} ссылается на отсутствующую {texture}")

    for asset in re.findall(r'asset\("(\w+)"\)', read(os.path.join(SRC, "registry/ModArmorMaterials.java"))):
        if not os.path.exists(f"{ASSETS}/equipment/{asset}.json"):
            fail(f"нет equipment/{asset}.json для материала брони")


def check_lang(sources, items, blocks):
    langs = {}
    for lang in ("en_us", "ru_ru"):
        path = f"{ASSETS}/lang/{lang}.json"
        langs[lang] = json.loads(read(path))
    en, ru = langs["en_us"], langs["ru_ru"]
    for key in sorted(set(en) ^ set(ru)):
        fail(f"ключ перевода есть только в одном языке: {key}")

    needed = set()
    for item in items:
        needed.add(f"item.{NS}.{item}")
    for block in blocks:
        needed.add(f"block.{NS}.{block}")
    for src in sources.values():
        for key in re.findall(r'"((?:message|hud|tooltip|command|itemgroup|key)\.ironman[\w.]*|'
                              r'key\.ironman\.\w+)"', src):
            needed.add(key)
        # Ключи, собранные из частей: "hud.ironman.x" ? ... : ... уже попали выше.
    needed.update(f"reactor.{NS}.{r}" for r in re.findall(
        r'^\s+[A-Z_]+\("(\w+)", \d', read(os.path.join(SRC, "suit/ReactorTier.java")), re.M))
    needed.add(f"key.category.{NS}.suit")
    needed.add(f"entity.{NS}.micro_missile")
    for path in glob.glob(f"{RES}/data/{NS}/damage_type/*.json"):
        message = json.loads(read(path))["message_id"]
        needed.add(f"death.attack.{message}")
        needed.add(f"death.attack.{message}.player")
    for path in glob.glob(f"{RES}/data/{NS}/advancement/*.json"):
        display = json.loads(read(path))["display"]
        needed.add(display["title"]["translate"])
        needed.add(display["description"]["translate"])

    for key in sorted(needed):
        for lang, table in langs.items():
            if key not in table:
                fail(f"{lang}: нет перевода {key}")


def check_data(items, blocks):
    known = {f"{NS}:{name}" for name in items | blocks}

    def ref_ok(ref):
        if ref.startswith("#") or not ref.startswith(NS + ":"):
            return True
        return ref in known

    def walk(node, where):
        if isinstance(node, dict):
            for key, value in node.items():
                if key in ("id", "name", "items", "base", "addition", "result", "ingredient", "icon") \
                        and isinstance(value, str) and ":" in value and "/" not in value:
                    if not ref_ok(value):
                        fail(f"{where}: неизвестный предмет {value}")
                walk(value, where)
        elif isinstance(node, list):
            for value in node:
                if isinstance(value, str) and value.startswith(NS + ":") and not ref_ok(value) \
                        and "/" not in value:
                    fail(f"{where}: неизвестный предмет {value}")
                walk(value, where)

    for folder in ("recipe", "loot_table", "advancement"):
        for path in glob.glob(f"{RES}/data/{NS}/{folder}/**/*.json", recursive=True):
            walk(json.loads(read(path)), os.path.relpath(path, RES))
            if folder == "recipe":
                recipe = json.loads(read(path))
                if recipe["type"] == "minecraft:crafting_shaped":
                    letters = set("".join(recipe["pattern"])) - {" "}
                    if letters != set(recipe["key"]):
                        fail(f"{os.path.relpath(path, RES)}: буквы шаблона {letters} "
                             f"не совпадают с ключами {set(recipe['key'])}")
    # Теги типов урона ссылаются на типы урона, а не на предметы — их не смотрим.
    for kind in ("item", "block"):
        for path in glob.glob(f"{RES}/data/*/tags/{kind}/**/*.json", recursive=True):
            walk(json.loads(read(path)), os.path.relpath(path, RES))

    for key in re.findall(r'id\("(\w+_placed)"\)', read(os.path.join(SRC, "world/ModWorldGen.java"))):
        if not os.path.exists(f"{RES}/data/{NS}/worldgen/placed_feature/{key}.json"):
            fail(f"нет placed_feature/{key}.json")
    for key in re.findall(r'key\("(\w+)"\)', read(os.path.join(SRC, "registry/ModDamageTypes.java"))):
        if not os.path.exists(f"{RES}/data/{NS}/damage_type/{key}.json"):
            fail(f"нет damage_type/{key}.json")
    for key in re.findall(r'TagKey<Item> \w+ = of\("(\w+)"\)', read(os.path.join(SRC, "registry/ModTags.java"))):
        if not os.path.exists(f"{RES}/data/{NS}/tags/item/{key}.json"):
            fail(f"нет tags/item/{key}.json")


def main():
    sources = java_sources()
    items, blocks = registered_names(sources)
    parsed = check_json()
    check_assets(items, blocks)
    check_lang(sources, items, blocks)
    check_data(items, blocks)

    if problems:
        print("\n".join(problems))
        print(f"\nнайдено проблем: {len(problems)}")
        sys.exit(1)
    print(f"OK: {len(parsed)} JSON-файлов, {len(items)} предметов, {len(blocks)} блоков — всё сходится")


if __name__ == "__main__":
    main()
