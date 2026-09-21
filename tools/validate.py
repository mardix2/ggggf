#!/usr/bin/env python3
"""Офлайн-проверка мода.

Настоящую сборку Gradle запустить можно только там, где доступны
maven.fabricmc.net и серверы Mojang. Этот скрипт проверяет всё остальное:
корректность JSON, наличие текстур и переводов и — главное — что списки
предметов, блоков, эффектов и заклинаний в Java и в ресурсах совпадают.

Запуск:  python3 tools/validate.py
"""

import json
import os
import re
import struct
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from content import (ANIMATED_BLOCK_TEXTURES, AUGMENTS, BLOCKS, EFFECTS, ITEMS,
                     ITEMS_3D, SCHOOLS, SPELLS, SPRITE_ITEMS)

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SRC = os.path.join(ROOT, "src/main/java/net/arcanum")
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/arcanum")
DATA = os.path.join(RES, "data")

problems = []


def fail(message):
    problems.append(message)


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def java_files():
    for dirpath, _dirs, files in os.walk(SRC):
        for name in sorted(files):
            if name.endswith(".java"):
                yield os.path.join(dirpath, name)


def expand(names):
    """Разворачивает динамические имена вида "rune_" + school."""
    out = set()
    for name in names:
        if name.endswith("_"):
            for school in SCHOOLS:
                out.add(name + school)
        else:
            out.add(name)
    return out


# ----------------------------------------------------------------------
#  Что зарегистрировано в Java
# ----------------------------------------------------------------------

def registered_items():
    text = read(os.path.join(SRC, "registry/ModItems.java"))
    names = expand(re.findall(r'register\(\s*"([a-z0-9_]+)"', text))
    # Руны-модификаторы регистрируются циклом по перечислению Augment.
    if "register(augment.itemName()" in text:
        names |= {"augment_" + name for name in augment_names()}
    return names


def augment_names():
    """Имена значений перечисления Augment — источник истины для предметов-рун."""
    text = read(os.path.join(SRC, "spell/Augment.java"))
    return set(re.findall(r'^\s+[A-Z_]+\("([a-z_]+)"', text, re.MULTILINE))


def registered_blocks():
    text = read(os.path.join(SRC, "registry/ModBlocks.java"))
    return expand(re.findall(r'register\(\s*"([a-z0-9_]+)"', text))


def registered_effects():
    text = read(os.path.join(SRC, "registry/ModEffects.java"))
    return set(re.findall(r'register\(\s*"([a-z0-9_]+)"\s*,\s*StatusEffectCategory', text))


def registered_spells():
    found = {}
    for path in java_files():
        for spell, school in re.findall(
                r'Spell\.builder\(\s*"([a-z0-9_]+)"\s*,\s*SpellSchool\.([A-Z]+)\s*\)', read(path)):
            if spell in found:
                fail(f"заклинание {spell} объявлено дважды")
            found[spell] = school.lower()
    return found


def translation_keys_used():
    """Все строковые литералы в Java, похожие на ключ перевода мода."""
    pattern = re.compile(r'"([a-z][a-z0-9_]*(?:\.[a-z0-9_]+)+)"')
    keys = set()
    for path in java_files():
        for match in pattern.findall(read(path)):
            # Имена файлов тоже выглядят как ключи, но переводить их не нужно.
            if "arcanum" in match and not match.endswith(".") and not match.endswith(".json"):
                keys.add(match)
    return keys


# ----------------------------------------------------------------------
#  Проверки
# ----------------------------------------------------------------------

def check_json_parses():
    count = 0
    for dirpath, _dirs, files in os.walk(RES):
        for name in files:
            if not name.endswith(".json"):
                continue
            path = os.path.join(dirpath, name)
            try:
                json.loads(read(path))
                count += 1
            except json.JSONDecodeError as error:
                fail(f"битый JSON: {os.path.relpath(path, ROOT)} — {error}")
    return count


def check_registry_matches_content():
    items = registered_items()
    if items != set(ITEMS):
        fail(f"предметы Java и content.py расходятся: "
             f"только в Java {sorted(items - set(ITEMS))}, "
             f"только в content {sorted(set(ITEMS) - items)}")

    blocks = registered_blocks()
    if blocks != set(BLOCKS):
        fail(f"блоки Java и content.py расходятся: "
             f"только в Java {sorted(blocks - set(BLOCKS))}, "
             f"только в content {sorted(set(BLOCKS) - blocks)}")

    effects = registered_effects()
    if effects != set(EFFECTS):
        fail(f"эффекты Java и content.py расходятся: "
             f"только в Java {sorted(effects - set(EFFECTS))}, "
             f"только в content {sorted(set(EFFECTS) - effects)}")

    spells = registered_spells()
    if set(spells) != set(SPELLS):
        fail(f"заклинания Java и content.py расходятся: "
             f"только в Java {sorted(set(spells) - set(SPELLS))}, "
             f"только в content {sorted(set(SPELLS) - set(spells))}")
    for spell, school in spells.items():
        if spell in SPELLS and SPELLS[spell][0] != school:
            fail(f"у заклинания {spell} школа в Java ({school}) "
                 f"не совпадает с content.py ({SPELLS[spell][0]})")


def check_assets():
    # Спрайт нужен не каждому предмету: у объёмных моделей его заменяет
    # геометрия, а текстуры проверяются через сами модели.
    for name in ITEMS:
        expect_file(f"models/item/{name}.json")
        expect_file(f"items/{name}.json")
    for name in SPRITE_ITEMS:
        expect_file(f"textures/item/{name}.png")
    expect_file("textures/item/palette.png")

    for name, (_ru, _en, kind) in BLOCKS.items():
        expect_file(f"blockstates/{name}.json")
        expect_file(f"models/item/{name}.json")
        expect_file(f"items/{name}.json")
        if kind == "lamp":
            expect_file(f"models/block/{name}_on.json")
            expect_file(f"models/block/{name}_off.json")
        else:
            expect_file(f"models/block/{name}.json")

    for name in EFFECTS:
        expect_file(f"textures/mob_effect/{name}.png")

    expect_file("equipment/arcane_robe.json")
    expect_file("textures/entity/equipment/humanoid/arcane_robe.png")
    expect_file("textures/entity/equipment/humanoid_leggings/arcane_robe.png")
    expect_file("icon.png")


def expect_file(relative):
    path = os.path.join(ASSETS, relative)
    if not os.path.exists(path):
        fail(f"нет файла assets/arcanum/{relative}")


def check_model_textures():
    """Каждая текстура, на которую ссылается модель, должна существовать."""
    for dirpath, _dirs, files in os.walk(os.path.join(ASSETS, "models")):
        for name in files:
            if not name.endswith(".json"):
                continue
            path = os.path.join(dirpath, name)
            model = json.loads(read(path))
            for key, value in (model.get("textures") or {}).items():
                if not isinstance(value, str) or value.startswith("#"):
                    continue
                if not value.startswith("arcanum:"):
                    continue
                texture = value.split(":", 1)[1]
                png = os.path.join(ASSETS, "textures", texture + ".png")
                if not os.path.exists(png):
                    fail(f"{os.path.relpath(path, ROOT)}: нет текстуры {value} (ключ {key})")


def check_3d_models():
    """Структурная проверка объёмных моделей."""
    if not ITEMS_3D <= set(ITEMS):
        fail(f"в ITEMS_3D есть неизвестные предметы: {sorted(ITEMS_3D - set(ITEMS))}")

    valid_angles = {-45, -22.5, 0, 22.5, 45}
    for name in sorted(ITEMS_3D):
        path = os.path.join(ASSETS, "models", "item", name + ".json")
        if not os.path.exists(path):
            continue
        model = json.loads(read(path))
        where = f"models/item/{name}.json"

        elements = model.get("elements")
        if not elements:
            fail(f"{where}: объёмная модель без элементов")
            continue
        if "display" not in model:
            fail(f"{where}: не задано положение предмета (display)")

        declared = {"#" + key for key in model.get("textures", {})}
        for index, element in enumerate(elements):
            start, end = element["from"], element["to"]
            for axis in range(3):
                if not -16 <= start[axis] <= 32 or not -16 <= end[axis] <= 32:
                    fail(f"{where}: элемент {index} выходит за пределы -16..32")
                if start[axis] >= end[axis]:
                    fail(f"{where}: элемент {index} вывернут по оси {axis}")

            rotation = element.get("rotation")
            if rotation is not None:
                if rotation["angle"] not in valid_angles:
                    fail(f"{where}: элемент {index} повёрнут на "
                         f"{rotation['angle']}° — игра такой угол не примет")
                if rotation["axis"] not in ("x", "y", "z"):
                    fail(f"{where}: элемент {index} — неизвестная ось поворота")

            for side, face in element["faces"].items():
                if face["texture"] not in declared:
                    fail(f"{where}: элемент {index}, грань {side} ссылается на "
                         f"необъявленную текстуру {face['texture']}")
                if len(face.get("uv", [])) != 4:
                    fail(f"{where}: элемент {index}, грань {side} без корректного uv")


def check_animations():
    """Анимированные текстуры: кадры должны быть квадратными и описанными."""
    expected = set(ANIMATED_BLOCK_TEXTURES)
    found = set()
    for folder in ("block", "item"):
        directory = os.path.join(ASSETS, "textures", folder)
        if not os.path.isdir(directory):
            continue
        for name in sorted(os.listdir(directory)):
            if not name.endswith(".png.mcmeta"):
                continue
            base = name[:-len(".png.mcmeta")]
            found.add(base)
            png = os.path.join(directory, base + ".png")
            if not os.path.exists(png):
                fail(f"{name}: нет самой текстуры {base}.png")
                continue
            width, height = png_size(png)
            if height % width != 0 or height == width:
                fail(f"{base}.png: {width}x{height} — это не лента кадров")
            meta = json.loads(read(os.path.join(directory, name)))
            if "animation" not in meta:
                fail(f"{name}: нет блока animation")

    if found != expected:
        fail(f"список анимаций в content.py разошёлся с файлами: "
             f"лишние {sorted(found - expected)}, отсутствуют {sorted(expected - found)}")


def png_size(path):
    with open(path, "rb") as handle:
        header = handle.read(24)
    return struct.unpack(">II", header[16:24])


def check_orphan_textures():
    """Текстуры, на которые никто не ссылается, — мусор в сборке."""
    referenced = set()
    for dirpath, _dirs, files in os.walk(os.path.join(ASSETS, "models")):
        for name in files:
            if not name.endswith(".json"):
                continue
            model = json.loads(read(os.path.join(dirpath, name)))
            for value in (model.get("textures") or {}).values():
                if isinstance(value, str) and value.startswith("arcanum:"):
                    referenced.add(value.split(":", 1)[1])

    for folder in ("item", "block"):
        directory = os.path.join(ASSETS, "textures", folder)
        for name in sorted(os.listdir(directory)):
            if not name.endswith(".png"):
                continue
            key = f"{folder}/{name[:-4]}"
            if key not in referenced:
                fail(f"текстура {key}.png никем не используется")


def check_lang():
    ru = json.loads(read(os.path.join(ASSETS, "lang/ru_ru.json")))
    en = json.loads(read(os.path.join(ASSETS, "lang/en_us.json")))
    if set(ru) != set(en):
        fail(f"наборы ключей ru_ru и en_us расходятся: "
             f"{sorted(set(ru) ^ set(en))[:10]}")

    for name in ITEMS:
        require_key(ru, f"item.arcanum.{name}")
    for name in BLOCKS:
        require_key(ru, f"block.arcanum.{name}")
    for name in EFFECTS:
        require_key(ru, f"effect.arcanum.{name}")
    for spell in SPELLS:
        require_key(ru, f"spell.arcanum.{spell}")
        require_key(ru, f"spell.arcanum.{spell}.desc")
    for school in SCHOOLS:
        require_key(ru, f"school.arcanum.{school}")

    for key in sorted(translation_keys_used()):
        require_key(ru, key)

    for key, value in ru.items():
        if key in en and value.count("%s") != en[key].count("%s"):
            fail(f"разное число подстановок в переводах ключа {key}")


def require_key(lang, key):
    if key not in lang:
        fail(f"нет перевода для ключа {key}")


def check_data_items():
    """Ссылки на предметы в рецептах, обрядах, добыче и тегах должны существовать.

    Достижения и генерация мира сюда не входят: там идентификаторы обозначают
    не предметы, а другие достижения и фичи — их проверяют отдельные функции.
    """
    known = {f"arcanum:{name}" for name in ITEMS} | {f"arcanum:{name}" for name in BLOCKS}

    for folder in ("recipe", "infusion", os.path.join("loot_table", "blocks"),
                   os.path.join("tags", "item"), os.path.join("tags", "block")):
        directory = os.path.join(DATA, "arcanum", folder)
        if not os.path.isdir(directory):
            continue
        for name in sorted(os.listdir(directory)):
            if not name.endswith(".json"):
                continue
            path = os.path.join(directory, name)
            where = os.path.relpath(path, ROOT)
            blob = json.loads(read(path))
            for identifier in set(re.findall(r'"(arcanum:[a-z0-9_]+)"', json.dumps(blob))):
                if identifier not in known:
                    fail(f"{where}: неизвестный предмет {identifier}")

    # В ванильных тегах тоже встречаются наши блоки.
    for folder in (os.path.join("tags", "block"), os.path.join("tags", "block", "mineable")):
        directory = os.path.join(DATA, "minecraft", folder)
        if not os.path.isdir(directory):
            continue
        for name in sorted(os.listdir(directory)):
            if not name.endswith(".json"):
                continue
            path = os.path.join(directory, name)
            blob = json.loads(read(path))
            for identifier in set(re.findall(r'"(arcanum:[a-z0-9_]+)"', json.dumps(blob))):
                if identifier not in known:
                    fail(f"{os.path.relpath(path, ROOT)}: неизвестный блок {identifier}")


def check_infusion_recipes():
    directory = os.path.join(DATA, "arcanum", "infusion")
    seen = []
    for name in sorted(os.listdir(directory)):
        recipe = json.loads(read(os.path.join(directory, name)))
        inputs = recipe["inputs"]
        if not inputs:
            fail(f"обряд {name} без ингредиентов")
        signature = tuple(sorted((entry["item"], entry.get("count", 1)) for entry in inputs))
        if signature in seen:
            fail(f"обряд {name} повторяет набор ингредиентов другого обряда")
        seen.append(signature)
        school = recipe.get("scroll_school")
        if school is not None and school not in SCHOOLS:
            fail(f"обряд {name}: неизвестная школа {school}")


def check_worldgen_links():
    """Placed feature должен ссылаться на существующий configured feature."""
    configured = os.path.join(DATA, "arcanum", "worldgen", "configured_feature")
    placed = os.path.join(DATA, "arcanum", "worldgen", "placed_feature")
    available = {name[:-5] for name in os.listdir(configured)}
    for name in sorted(os.listdir(placed)):
        blob = json.loads(read(os.path.join(placed, name)))
        feature = blob["feature"].split(":", 1)[1]
        if feature not in available:
            fail(f"{name}: нет configured feature {feature}")

    # И наоборот: placed feature, объявленные в Java, должны существовать.
    text = read(os.path.join(SRC, "world/ModWorldGen.java"))
    for key in re.findall(r'Arcanum\.id\("([a-z0-9_]+)"\)', text):
        if not os.path.exists(os.path.join(placed, key + ".json")):
            fail(f"ModWorldGen ссылается на placed feature {key}, файла нет")


def check_advancements():
    """Достижения: родитель существует, иконка — наш предмет, тексты переведены."""
    directory = os.path.join(DATA, "arcanum", "advancement")
    names = {name[:-5] for name in os.listdir(directory)}
    lang = json.loads(read(os.path.join(ASSETS, "lang/ru_ru.json")))
    known = {f"arcanum:{name}" for name in ITEMS} | {f"arcanum:{name}" for name in BLOCKS}

    for name in sorted(names):
        blob = json.loads(read(os.path.join(directory, name + ".json")))
        parent = blob.get("parent")
        if parent is not None and parent.split(":", 1)[1] not in names:
            fail(f"достижение {name}: нет родителя {parent}")

        icon = blob["display"]["icon"]["id"]
        if icon not in known:
            fail(f"достижение {name}: иконка {icon} не существует")

        for key in (blob["display"]["title"]["translate"],
                    blob["display"]["description"]["translate"]):
            require_key(lang, key)

        for requirement in blob.get("requirements", []):
            for criterion in requirement:
                if criterion not in blob["criteria"]:
                    fail(f"достижение {name}: в requirements указано "
                         f"неизвестное условие {criterion}")


def check_augments():
    """Руны-модификаторы: перечисление, предметы, обряды и переводы должны совпадать."""
    names = augment_names()
    if names != set(AUGMENTS):
        fail(f"перечисление Augment и content.py расходятся: "
             f"только в Java {sorted(names - set(AUGMENTS))}, "
             f"только в content {sorted(set(AUGMENTS) - names)}")

    directory = os.path.join(DATA, "arcanum", "infusion")
    for name in sorted(names):
        for prefix in ("augment_", "augment_install_"):
            path = os.path.join(directory, prefix + name + ".json")
            if not os.path.exists(path):
                fail(f"нет обряда {prefix}{name}.json")

    # Обряд без результата обязан объяснять, откуда он его возьмёт.
    for name in sorted(os.listdir(directory)):
        recipe = json.loads(read(os.path.join(directory, name)))
        if "result" not in recipe and not recipe.get("requires_wand"):
            fail(f"обряд {name}: нет ни result, ни requires_wand")
        if recipe.get("add_augment") and recipe["add_augment"] not in names:
            fail(f"обряд {name}: неизвестная руна {recipe['add_augment']}")


def check_loot_tables():
    directory = os.path.join(DATA, "arcanum", "loot_table", "blocks")
    for name in BLOCKS:
        if not os.path.exists(os.path.join(directory, name + ".json")):
            fail(f"нет таблицы добычи для блока {name}")


def main():
    parsed = check_json_parses()
    check_registry_matches_content()
    check_assets()
    check_model_textures()
    check_3d_models()
    check_animations()
    check_orphan_textures()
    check_lang()
    check_data_items()
    check_infusion_recipes()
    check_worldgen_links()
    check_loot_tables()
    check_advancements()
    check_augments()

    print(f"Проверено JSON-файлов: {parsed}")
    print(f"Предметов: {len(ITEMS)} (объёмных моделей: {len(ITEMS_3D)}), "
          f"блоков: {len(BLOCKS)}, заклинаний: {len(SPELLS)}, "
          f"эффектов: {len(EFFECTS)}, анимаций: {len(ANIMATED_BLOCK_TEXTURES)}")
    if problems:
        print(f"\nНайдено проблем: {len(problems)}")
        for problem in problems:
            print("  - " + problem)
        return 1
    print("Все проверки пройдены.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
