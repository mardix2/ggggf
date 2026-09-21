#!/usr/bin/env python3
"""Генерация моделей, blockstate-файлов, описаний снаряжения и переводов."""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from content import (BLOCKS, CAST_TYPES, EFFECTS, ITEMS, ITEMS_3D, REACTIONS,
                     SCHOOLS, SCHOOL_NAMES, SPELLS)

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/arcanum")

NS = "arcanum"


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def asset(*parts):
    return os.path.join(ASSETS, *parts)


# ----------------------------------------------------------------------
#  Модели предметов
# ----------------------------------------------------------------------

def item_models():
    """Плоские модели — только тем предметам, у кого нет объёмной.

    Сами объёмные модели пишет tools/gen_models3d.py, но определение
    предмета (`items/<name>.json`) одинаково для обоих случаев.
    """
    for name in ITEMS:
        if name not in ITEMS_3D:
            write(asset("models", "item", name + ".json"), {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"{NS}:item/{name}"},
            })
        write(asset("items", name + ".json"), {
            "model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"},
        })


def block_item_models():
    """Предметы-блоки: обычные ссылаются на модель блока, «кресты» — на спрайт."""
    flat = {"arcane_crystal_cluster", "moonpetal"}
    for name in BLOCKS:
        if name in flat:
            write(asset("models", "item", name + ".json"), {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"{NS}:block/{name}"},
            })
        else:
            write(asset("models", "item", name + ".json"),
                  {"parent": f"{NS}:block/{name}"})
        write(asset("items", name + ".json"), {
            "model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"},
        })


# ----------------------------------------------------------------------
#  Модели блоков
# ----------------------------------------------------------------------

def cuboid(x0, y0, z0, x1, y1, z1, top="#top", side="#side", bottom="#bottom"):
    """Один параллелепипед со стандартной раскладкой текстур."""
    return {
        "from": [x0, y0, z0],
        "to": [x1, y1, z1],
        "faces": {
            "down": {"texture": bottom, "cullface": "down"} if y0 == 0 else {"texture": bottom},
            "up": {"texture": top, "cullface": "up"} if y1 == 16 else {"texture": top},
            "north": {"texture": side},
            "south": {"texture": side},
            "west": {"texture": side},
            "east": {"texture": side},
        },
    }


MULTIPART = {
    "infusion_altar": [(0, 0, 0, 16, 4, 16), (3, 4, 3, 13, 11, 13), (1, 11, 1, 15, 16, 15)],
    "mana_font": [(2, 0, 2, 14, 3, 14), (4, 3, 4, 12, 10, 12), (1, 10, 1, 15, 14, 15)],
    "teleport_anchor": [(2, 0, 2, 14, 2, 14), (5, 2, 5, 11, 13, 11), (3, 13, 3, 13, 16, 13)],
    "arcane_pedestal": [(2, 0, 2, 14, 3, 14), (5, 3, 5, 11, 12, 11), (3, 12, 3, 13, 15, 13)],
}


def block_models():
    for name, (_ru, _en, kind) in BLOCKS.items():
        if kind == "cube":
            write(asset("models", "block", name + ".json"), {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": f"{NS}:block/{name}"},
            })
        elif kind == "cluster":
            write(asset("models", "block", name + ".json"), {
                "parent": "minecraft:block/cross",
                "render_type": "minecraft:cutout",
                "textures": {"cross": f"{NS}:block/{name}"},
            })
        elif kind == "cross":
            write(asset("models", "block", name + ".json"), {
                "parent": "minecraft:block/cross",
                "render_type": "minecraft:cutout",
                "textures": {"cross": f"{NS}:block/{name}"},
            })
        elif kind == "lamp":
            for state in ("on", "off"):
                write(asset("models", "block", f"{name}_{state}.json"), {
                    "parent": "minecraft:block/cube_all",
                    "textures": {"all": f"{NS}:block/{name}_{state}"},
                })
        else:
            parts = MULTIPART[name]
            textures = {
                "particle": f"{NS}:block/{name}_side",
                "top": f"{NS}:block/{name}_top",
                "side": f"{NS}:block/{name}_side",
                "bottom": f"{NS}:block/{name}_{'bottom' if name == 'infusion_altar' else 'side'}",
            }
            write(asset("models", "block", name + ".json"), {
                "parent": "minecraft:block/block",
                "render_type": "minecraft:cutout",
                "textures": textures,
                "elements": [cuboid(*part) for part in parts],
            })


def blockstates():
    for name, (_ru, _en, kind) in BLOCKS.items():
        if kind == "lamp":
            write(asset("blockstates", name + ".json"), {
                "variants": {
                    "lit=true": {"model": f"{NS}:block/{name}_on"},
                    "lit=false": {"model": f"{NS}:block/{name}_off"},
                }
            })
        elif kind == "cluster":
            # Друза растёт на любой поверхности: поворачиваем модель по грани.
            write(asset("blockstates", name + ".json"), {
                "variants": {
                    "facing=up": {"model": f"{NS}:block/{name}"},
                    "facing=down": {"model": f"{NS}:block/{name}", "x": 180},
                    "facing=north": {"model": f"{NS}:block/{name}", "x": 90},
                    "facing=south": {"model": f"{NS}:block/{name}", "x": 90, "y": 180},
                    "facing=east": {"model": f"{NS}:block/{name}", "x": 90, "y": 90},
                    "facing=west": {"model": f"{NS}:block/{name}", "x": 90, "y": 270},
                }
            })
        else:
            write(asset("blockstates", name + ".json"), {
                "variants": {"": {"model": f"{NS}:block/{name}"}}
            })


def equipment():
    write(asset("equipment", "arcane_robe.json"), {
        "layers": {
            "humanoid": [{"texture": f"{NS}:arcane_robe"}],
            "humanoid_leggings": [{"texture": f"{NS}:arcane_robe"}],
        }
    })


# ----------------------------------------------------------------------
#  Переводы
# ----------------------------------------------------------------------

def lang(index):
    """index=0 — русский, index=1 — английский."""
    out = {}

    out["itemgroup.arcanum.general"] = ["Arcanum Mysticum", "Arcanum Mysticum"][index]

    for name, names in ITEMS.items():
        out[f"item.arcanum.{name}"] = names[index]
    for name, values in BLOCKS.items():
        out[f"block.arcanum.{name}"] = values[index]
    for name, names in EFFECTS.items():
        out[f"effect.arcanum.{name}"] = names[index]
    for school, names in SCHOOL_NAMES.items():
        out[f"school.arcanum.{school}"] = names[index]
    for cast, names in CAST_TYPES.items():
        out[f"casttype.arcanum.{cast}"] = names[index]
    for reaction, names in REACTIONS.items():
        out[f"reaction.arcanum.{reaction}"] = names[index]

    for spell, (_school, ru_name, ru_desc, en_name, en_desc) in SPELLS.items():
        out[f"spell.arcanum.{spell}"] = ru_name if index == 0 else en_name
        out[f"spell.arcanum.{spell}.desc"] = ru_desc if index == 0 else en_desc

    entities = {
        "spell_projectile": ("Магический снаряд", "Spell Projectile"),
        "summoned_wolf": ("Призванный волк", "Summoned Wolf"),
        "shade": ("Тень", "Shade"),
        "guardian": ("Страж", "Guardian"),
    }
    for name, names in entities.items():
        out[f"entity.arcanum.{name}"] = names[index]

    keys = {
        "key.categories.arcanum": ("Arcanum Mysticum", "Arcanum Mysticum"),
        "key.categories.arcanum.main": ("Arcanum Mysticum", "Arcanum Mysticum"),
        "key.arcanum.spellbook": ("Книга заклинаний", "Spellbook"),
        "key.arcanum.next_spell": ("Следующее заклинание", "Next spell"),
        "key.arcanum.prev_spell": ("Предыдущее заклинание", "Previous spell"),
        "key.arcanum.spell_wheel": ("Колесо заклинаний (удерживать)",
                                    "Spell wheel (hold)"),
    }
    for key, names in keys.items():
        out[key] = names[index]

    hud = {
        "hud.arcanum.no_spell": ("Заклинание не выбрано", "No spell selected"),
        "hud.arcanum.cooldown": ("Перезарядка: %s c", "Cooldown: %ss"),
        "hud.arcanum.cost": ("Стоимость: %s маны", "Cost: %s mana"),
    }
    for key, names in hud.items():
        out[key] = names[index]

    screen = {
        "screen.arcanum.spellbook": ("Книга заклинаний", "Spellbook"),
        "screen.arcanum.mana": ("Мана: %s / %s", "Mana: %s / %s"),
        "screen.arcanum.unknown_spell": ("??? не изучено", "??? not learned"),
        "screen.arcanum.not_learned": ("Заклинание не изучено — нужен свиток",
                                       "Not learned yet — find a scroll"),
        "screen.arcanum.scroll_hint": ("Колесо мыши — прокрутка", "Scroll to see more"),
        "screen.arcanum.stat.school": ("Школа", "School"),
        "screen.arcanum.stat.type": ("Вид", "Type"),
        "screen.arcanum.stat.tier": ("Ступень посоха", "Wand tier"),
        "screen.arcanum.stat.cost": ("Мана", "Mana"),
        "screen.arcanum.stat.cooldown": ("Перезарядка", "Cooldown"),
        "screen.arcanum.stat.mastery": ("Мастерство", "Mastery"),
        "screen.arcanum.stat.progress": ("До следующего", "To next level"),
        "screen.arcanum.search_hint": ("поиск…", "search…"),
        "screen.arcanum.favorite_hint": ("ПКМ — в колесо", "Right click: add to wheel"),
        "screen.arcanum.wheel": ("Колесо заклинаний", "Spell Wheel"),
        "screen.arcanum.wheel_hint": ("Ведите мышь к заклинанию",
                                      "Move the mouse toward a spell"),
        "screen.arcanum.wheel_empty": ("Колесо пусто — отметьте заклинания в книге",
                                       "Wheel is empty — mark spells in the book"),
    }
    for key, names in screen.items():
        out[key] = names[index]

    tooltip = {
        "tooltip.arcanum.tier": ("Ступень посоха: %s", "Wand tier: %s"),
        "tooltip.arcanum.max_mana": ("Максимум маны: %s", "Max mana: %s"),
        "tooltip.arcanum.regen": ("Восстановление: +%s маны/с", "Regeneration: +%s mana/s"),
        "tooltip.arcanum.cost": ("Стоимость заклинаний: %s", "Spell cost: %s"),
        "tooltip.arcanum.cooldown": ("Перезарядка: %s", "Cooldown: %s"),
        "tooltip.arcanum.power": ("Сила заклинаний: %s", "Spell power: %s"),
        "tooltip.arcanum.focus_school": ("Усиливает: %s", "Empowers: %s"),
        "tooltip.arcanum.offhand": ("Носится во второй руке", "Hold in your off hand"),
        "tooltip.arcanum.blank_scroll": ("Пустой свиток", "Nothing is written here"),
        "tooltip.arcanum.scroll_cost": ("Разовое применение: %s маны",
                                        "One-time cast: %s mana"),
        "tooltip.arcanum.scroll_learn": ("Присесть + ПКМ — изучить навсегда",
                                         "Sneak + right click to learn it"),
        "tooltip.arcanum.stored": ("Запас маны: %s / %s", "Stored mana: %s / %s"),
        "tooltip.arcanum.restores": ("Восстанавливает %s маны", "Restores %s mana"),
        "tooltip.arcanum.bound": ("Впечатанные заклинания:", "Bound spells:"),
        "tooltip.arcanum.reach": ("Дальность: %s", "Range: %s"),
        "tooltip.arcanum.extra_shots": ("Снарядов: %s", "Extra projectiles: %s"),
        "tooltip.arcanum.echo": ("Шанс повтора: %s %%", "Repeat chance: %s%%"),
        "tooltip.arcanum.augment_install": ("Вплавляется в посох на алтаре",
                                            "Infuse into a wand at the altar"),
        "tooltip.arcanum.slots": ("Руны: %s / %s", "Runes: %s / %s"),
    }
    for key, names in tooltip.items():
        out[key] = names[index]

    messages = {
        "message.arcanum.mana_status": ("Мана: %s / %s", "Mana: %s / %s"),
        "message.arcanum.anchor_bound": ("Якорь установлен: %s, %s, %s",
                                         "Anchor bound at %s, %s, %s"),
        "message.arcanum.no_anchor": ("Якорь перемещения не установлен",
                                      "No teleport anchor bound"),
        "message.arcanum.anchor_other_world": ("Якорь находится в другом мире",
                                               "Your anchor is in another dimension"),
        "message.arcanum.no_spells_known": ("Вы не знаете ни одного заклинания",
                                            "You know no spells yet"),
        "message.arcanum.spell_selected": ("Выбрано: %s", "Selected: %s"),
        "message.arcanum.spell_learned": ("Изучено заклинание: %s", "Spell learned: %s"),
        "message.arcanum.mastery_up": ("Мастерство выросло: %s %s",
                                       "Mastery increased: %s %s"),
        "message.arcanum.already_known": ("Вы уже знаете %s", "You already know %s"),
        "message.arcanum.scroll_blank": ("В свитке ничего не записано",
                                         "This scroll is blank"),
        "message.arcanum.attunement_max": ("Разум больше не выдержит",
                                           "Your mind can hold no more"),
        "message.arcanum.attunement_up": ("Максимум маны теперь %s", "Max mana is now %s"),
        "message.arcanum.growth": ("Растений пробуждено: %s", "Plants awakened: %s"),
        "message.arcanum.dispelled": ("Развеяно призванных: %s", "Summons dispelled: %s"),
        "message.arcanum.no_infusions": ("Обряды алтаря не загружены",
                                         "No infusion recipes are loaded"),
        "message.arcanum.infusion_requires": ("  [постаментов: %s, маны: %s]",
                                              "  [pedestals: %s, mana: %s]"),
        "message.arcanum.learned_all": ("Изучены все заклинания (%s)",
                                        "All %s spells learned"),
        "message.arcanum.unknown_spell": ("Неизвестное заклинание: %s", "Unknown spell: %s"),
        "message.arcanum.cast.no_spell": ("Сначала выберите заклинание",
                                          "Select a spell first"),
        "message.arcanum.cast.not_learned": ("Вы не знаете %s", "You do not know %s"),
        "message.arcanum.cast.tier_too_low": ("Нужен посох ступени %s",
                                              "Requires a tier %s wand"),
        "message.arcanum.cast.cooldown": ("Ещё не готово: %s c", "Not ready: %ss"),
        "message.arcanum.cast.no_mana": ("Нужно %s маны, у вас %s",
                                         "Needs %s mana, you have %s"),
        "message.arcanum.cast.silenced": ("Вы под печатью безмолвия",
                                          "You are silenced"),
        "message.arcanum.cast.fizzled": ("Заклинанию не на что подействовать",
                                         "Nothing for the spell to affect"),
        "message.arcanum.altar.busy": ("Обряд уже идёт", "A ritual is already running"),
        "message.arcanum.altar.empty": ("Бросьте ингредиенты рядом с алтарём",
                                        "Drop the ingredients beside the altar"),
        "message.arcanum.altar.no_recipe": ("Подходящего рецепта нет (постаментов: %s)",
                                            "No matching recipe (pedestals: %s)"),
        "message.arcanum.altar.no_mana": ("Для обряда нужно %s маны",
                                          "The ritual needs %s mana"),
        "message.arcanum.altar.interrupted": ("Обряд сорвался", "The ritual collapsed"),
        "message.arcanum.altar.no_slots": ("В посохе нет свободных слотов под руны",
                                           "This wand has no free rune slots"),
        "message.arcanum.no_ore": ("Руды поблизости нет", "No ore nearby"),
        "message.arcanum.ore_found": ("Найдено жил: %s", "Ore veins found: %s"),
        "message.arcanum.grimoire_spent": ("Вы знаете всё, что помнила книга",
                                           "You already know all the book remembered"),
    }
    for key, names in messages.items():
        out[key] = names[index]

    advancements = {
        "advancements.arcanum.root.title": ("Arcanum Mysticum", "Arcanum Mysticum"),
        "advancements.arcanum.root.description": (
            "Найдите аркановый кристалл и загляните за грань обыденного",
            "Find an arcane crystal and look past the ordinary"),
        "advancements.arcanum.wand.title": ("Первый жезл", "First Wand"),
        "advancements.arcanum.wand.description": ("Создайте жезл ученика",
                                                  "Craft an apprentice wand"),
        "advancements.arcanum.altar.title": ("Обряд превращения", "The Infusion"),
        "advancements.arcanum.altar.description": ("Постройте алтарь превращения",
                                                   "Build an infusion altar"),
        "advancements.arcanum.scroll.title": ("Слово силы", "Word of Power"),
        "advancements.arcanum.scroll.description": ("Изучите заклинание со свитка",
                                                    "Learn a spell from a scroll"),
        "advancements.arcanum.robe.title": ("Облачение мага", "Vestments"),
        "advancements.arcanum.robe.description": ("Наденьте полный комплект мантии чародея",
                                                  "Wear the full arcane robe set"),
        "advancements.arcanum.archmage.title": ("Архимаг", "Archmage"),
        "advancements.arcanum.archmage.description": ("Создайте скипетр запредельного",
                                                      "Craft the eldritch scepter"),
        "advancements.arcanum.tower.title": ("Башня на горизонте", "Tower on the Horizon"),
        "advancements.arcanum.tower.description": ("Найдите башню павшего мага и заберите гримуар",
                                                   "Find the fallen mage's tower and take the grimoire"),
        "advancements.arcanum.augment.title": ("Тонкая настройка", "Fine Tuning"),
        "advancements.arcanum.augment.description": ("Создайте руну-модификатор для посоха",
                                                     "Craft a rune that modifies your wand"),
    }
    for key, names in advancements.items():
        out[key] = names[index]

    return out


def lang_files():
    write(asset("lang", "ru_ru.json"), lang(0))
    write(asset("lang", "en_us.json"), lang(1))


def main():
    item_models()
    block_item_models()
    block_models()
    blockstates()
    equipment()
    lang_files()
    print("Ресурсы готовы.")


if __name__ == "__main__":
    main()
