#!/usr/bin/env python3
"""Объёмные модели предметов и атлас-палитра к ним.

Плоский спрайт игра сама «выдавливает» на толщину в один пиксель — это не
объём, а открытка. Здесь предметы собраны из настоящих коробок: у посоха
есть древко, обмотка и навершие, у зелья — донце, плечи, горло и пробка,
у книги — корешок, обложка и блок страниц.

Запуск:  python3 tools/gen_models3d.py
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import model3d as m
from content import SCHOOLS
from png import Canvas, hex_to_rgb, shade

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/arcanum")


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def save_model(name, elements, display, extra_textures=None):
    write(os.path.join(ASSETS, "models", "item", name + ".json"),
          m.model(elements, display, extra_textures))


# ----------------------------------------------------------------------
#  Атлас-палитра
# ----------------------------------------------------------------------

def palette_atlas():
    """Одна текстура 32x32 на все объёмные модели: по ячейке 4x4 на материал."""
    canvas = Canvas(m.ATLAS_SIZE, m.ATLAS_SIZE)
    for name, color in m.MATERIALS.items():
        col, row = m.CELLS[name]
        base = hex_to_rgb(color)
        x0, y0 = col * m.CELL_PIXELS, row * m.CELL_PIXELS
        canvas.rect(x0, y0, x0 + m.CELL_PIXELS - 1, y0 + m.CELL_PIXELS - 1, (*base, 255))
        # Внутренние 2x2 — то, что реально попадает на грань. Лёгкая
        # шахматка даёт зерно, чтобы большие плоскости не выглядели пластиком.
        canvas.set(x0 + 1, y0 + 1, shade(base, 1.06))
        canvas.set(x0 + 2, y0 + 2, shade(base, 1.06))
        canvas.set(x0 + 2, y0 + 1, shade(base, 0.93))
        canvas.set(x0 + 1, y0 + 2, shade(base, 0.93))
    canvas.save(os.path.join(ASSETS, "textures", "item", "palette.png"))


# ----------------------------------------------------------------------
#  Составные формы
# ----------------------------------------------------------------------

def crystal(base, light, dark, bottom=1.5, top=15.0, width=1.0):
    """Гранёный кристалл.

    Пять ярусов, а не три: с тремя силуэт получается кубическим и читается
    как «блок», а не как камень. Здесь ярусы расширяются к талии и снова
    сходятся к острию.
    """
    height = top - bottom
    # Доля высоты и полуширина каждого яруса.
    tiers = [(0.00, 0.12, 1.2, dark),
             (0.12, 0.34, 2.4, base),
             (0.34, 0.62, 3.2, base),
             (0.62, 0.84, 2.2, light),
             (0.84, 1.00, 1.0, light)]

    elements = []
    for (start, end, half, material) in tiers:
        inset = half * width
        elements.append(m.box(8 - inset, bottom + height * start, 8 - inset,
                              8 + inset, bottom + height * end, 8 + inset, material))
    return elements


def shaft(material, bottom, top, half=1.0):
    return m.box(8 - half, bottom, 8 - half, 8 + half, top, 8 + half, material)


def grip(material, bottom, top, half=1.5):
    return m.box(8 - half, bottom, 8 - half, 8 + half, top, 8 + half, material)


def band(material, y, thickness=0.5, half=1.4):
    return m.box(8 - half, y, 8 - half, 8 + half, y + thickness, 8 + half, material)


def prongs(material, bottom, top, tilt=22.5, reach=1.0):
    """Четыре «лепестка» навершия, расходящиеся в стороны."""
    out = []
    # Вдоль оси Z: наклон вокруг X. Вдоль оси X: наклон вокруг Z.
    out.append(m.box(7.25, bottom, 8 + reach, 8.75, top, 9 + reach, material,
                     rotation=m.spin([8, bottom, 8 + reach], "x", -tilt)))
    out.append(m.box(7.25, bottom, 7 - reach, 8.75, top, 8 - reach, material,
                     rotation=m.spin([8, bottom, 8 - reach], "x", tilt)))
    out.append(m.box(8 + reach, bottom, 7.25, 9 + reach, top, 8.75, material,
                     rotation=m.spin([8 + reach, bottom, 8], "z", tilt)))
    out.append(m.box(7 - reach, bottom, 7.25, 8 - reach, top, 8.75, material,
                     rotation=m.spin([8 - reach, bottom, 8], "z", -tilt)))
    return out


def octagon_ring(material, inner=5.5, outer=13.5, z0=6.5, z1=9.5, width=1.5):
    """Восьмиугольная оправа: четыре прямых сегмента и четыре под 45 градусов."""
    low = 16 - outer
    elements = [
        m.box(inner, outer - width, z0, 16 - inner, outer, z1, material),
        m.box(inner, low, z0, 16 - inner, low + width, z1, material),
        m.box(low, inner, z0, low + width, 16 - inner, z1, material),
        m.box(outer - width, inner, z0, outer, 16 - inner, z1, material),
    ]

    # Диагонали: короткие бруски, повёрнутые вокруг собственного центра.
    corners = [
        (11.0, 11.0, 45), (5.0, 11.0, -45),
        (11.0, 5.0, -45), (5.0, 5.0, 45),
    ]
    for (cx, cy, angle) in corners:
        elements.append(m.box(cx - 1.4, cy - width / 2, z0, cx + 1.4, cy + width / 2, z1,
                              material, rotation=m.spin([cx, cy, (z0 + z1) / 2], "z", angle)))
    return elements


def sprite_faces(sprite_region):
    """Лицевая и тыльная грани берут рисунок со спрайта (тыльная — зеркально)."""
    x0, y0, x1, y1 = sprite_region
    return {
        "south": ("#sprite", [x0, 16 - y1, x1, 16 - y0]),
        "north": ("#sprite", [x1, 16 - y1, x0, 16 - y0]),
    }


# ----------------------------------------------------------------------
#  Предметы
# ----------------------------------------------------------------------

def wands():
    save_model("apprentice_wand", [
        shaft("wood", 1.0, 11.0),
        grip("leather", 2.0, 5.0),
        band("wood_dark", 11.0, 0.8),
        *crystal("crystal", "crystal_light", "crystal_dark", bottom=11.8, top=15.0, width=0.65),
    ], m.DISPLAY_STAFF)

    save_model("adept_staff", [
        shaft("wood_dark", 0.5, 12.0),
        grip("leather", 2.5, 6.0),
        band("gold", 6.5),
        band("gold", 9.0),
        m.box(6.0, 12.0, 6.0, 10.0, 13.0, 10.0, "gold"),
        *crystal("soul", "moon_light", "soul_dark", bottom=13.0, top=16.0, width=0.75),
    ], m.DISPLAY_STAFF)

    save_model("archmage_staff", [
        shaft("wood_dark", 0.5, 11.0),
        grip("leather", 2.0, 6.0),
        band("gold", 6.5),
        band("gold", 9.5),
        *prongs("gold", 11.0, 15.0, tilt=22.5, reach=1.0),
        *crystal("amber", "moon_light", "gold_dark", bottom=11.5, top=15.5, width=0.7),
    ], m.DISPLAY_STAFF)

    save_model("eldritch_scepter", [
        shaft("obsidian", 0.5, 10.0),
        grip("robe", 2.0, 6.0),
        band("eldritch", 6.5, 0.6, 1.5),
        band("eldritch", 9.4, 0.6, 1.5),
        *prongs("obsidian", 10.0, 15.5, tilt=45, reach=1.2),
        # Камень «висит» над навершием — между ним и древком оставлен зазор.
        *crystal("eldritch", "crystal_light", "void", bottom=12.0, top=16.0, width=0.85),
    ], m.DISPLAY_STAFF)


def focuses():
    for school in SCHOOLS:
        base = "school_" + school
        light = base + "_light"
        save_model("focus_" + school, [
            *octagon_ring("gold"),
            m.box(6.5, 6.5, 6.75, 9.5, 9.5, 9.25, base),
            m.box(7.3, 7.3, 6.4, 8.7, 8.7, 9.6, light),
        ], m.DISPLAY_SMALL)


def runes():
    for school in SCHOOLS:
        # Лицевая грань — готовый спрайт с глифом: рисунок читается лучше,
        # чем любая геометрия такого размера.
        save_model("rune_" + school, [
            m.box(3.0, 2.0, 6.9, 13.0, 14.0, 9.1, "stone_dark",
                  overrides=sprite_faces((3, 2, 13, 14))),
            m.box(2.6, 1.6, 7.0, 13.4, 2.4, 9.0, "stone"),
            m.box(2.6, 13.6, 7.0, 13.4, 14.4, 9.0, "stone"),
        ], m.DISPLAY_FLAT,
            extra_textures={"sprite": "arcanum:item/rune_" + school})


def scrolls():
    def body(ribbon):
        elements = [
            m.box(3.0, 6.0, 6.0, 13.0, 10.0, 10.0, "parchment"),
            m.box(2.0, 5.4, 5.4, 4.0, 10.6, 10.6, "parchment_dark"),
            m.box(12.0, 5.4, 5.4, 14.0, 10.6, 10.6, "parchment_dark"),
        ]
        if ribbon is not None:
            elements.append(m.box(7.4, 5.8, 5.8, 8.6, 10.2, 10.2, ribbon))
        return elements

    save_model("blank_scroll", body(None), m.DISPLAY_FLAT)
    save_model("spell_scroll", body("crystal"), m.DISPLAY_FLAT)


def spellbook():
    # Камера смотрит вдоль -Z, поэтому к игроку обращена грань с БОЛЬШИМ z:
    # украшенная обложка должна лежать сверху стопки, а не под ней.
    save_model("spellbook", [
        m.box(2.5, 1.5, 3.0, 13.5, 14.5, 5.0, "robe"),
        m.box(3.5, 2.5, 5.0, 12.5, 13.5, 6.5, "parchment"),
        m.box(2.5, 1.5, 6.5, 13.5, 14.5, 8.5, "robe"),
        m.box(1.8, 1.5, 3.0, 2.5, 14.5, 8.5, "robe_light"),
        m.box(11.2, 11.8, 8.5, 13.2, 13.8, 8.8, "gold"),
        m.box(11.2, 2.2, 8.5, 13.2, 4.2, 8.8, "gold"),
        m.box(6.8, 6.8, 8.5, 9.2, 9.2, 9.5, "crystal"),
    ], m.DISPLAY_FLAT)


def potions():
    display = dict(m.DISPLAY_SMALL)
    display["gui"] = {"rotation": [12, -25, 0], "translation": [0, 0, 0],
                      "scale": [0.85, 0.85, 0.85]}

    def bottle(liquid):
        return [
            m.box(5.0, 0.5, 5.0, 11.0, 2.0, 11.0, "glass"),
            m.box(4.5, 2.0, 4.5, 11.5, 7.5, 11.5, liquid),
            m.box(5.5, 7.5, 5.5, 10.5, 9.0, 10.5, "glass"),
            m.box(6.5, 9.0, 6.5, 9.5, 12.0, 9.5, "glass"),
            m.box(6.0, 12.0, 6.0, 10.0, 13.8, 10.0, "cork"),
        ]

    save_model("mana_potion", bottle("potion"), display)
    save_model("greater_mana_potion", bottle("potion_great"), display)


def crystals():
    save_model("arcane_crystal",
               crystal("crystal", "crystal_light", "crystal_dark"), m.DISPLAY_SMALL)

    save_model("infused_crystal",
               crystal("crystal_light", "moon_light", "crystal")
               + [m.box(4.6, 6.0, 4.6, 11.4, 7.0, 11.4, "gold")],
               m.DISPLAY_SMALL)

    save_model("dim_mana_crystal",
               crystal("dim", "stone", "stone_dark", bottom=3.0, top=13.0, width=0.85),
               m.DISPLAY_SMALL)

    save_model("charged_mana_crystal",
               crystal("charged", "moon_light", "soul_dark")
               + [m.box(6.2, 7.5, 6.2, 9.8, 8.5, 9.8, "soul")],
               m.DISPLAY_SMALL)

    save_model("mana_gem",
               crystal("sigil", "moon_light", "crystal_dark", bottom=2.5, top=14.0)
               + [m.box(4.4, 6.5, 4.4, 11.6, 7.6, 11.6, "gold"),
                  m.box(4.8, 9.0, 4.8, 11.2, 9.8, 11.2, "gold_dark")],
               m.DISPLAY_SMALL)

    save_model("soul_shard", [
        *crystal("soul", "moon_light", "soul_dark", bottom=2.5, top=13.5, width=0.8),
        m.box(9.8, 1.5, 6.6, 12.4, 7.0, 9.4, "soul_dark"),
    ], m.DISPLAY_SMALL)

    # Лунная эссенция: три пересекающихся бруска дают почти шар.
    save_model("lunar_essence", [
        m.box(5.5, 5.5, 5.5, 10.5, 10.5, 10.5, "moon"),
        m.box(4.3, 6.6, 6.6, 11.7, 9.4, 9.4, "moon"),
        m.box(6.6, 4.3, 6.6, 9.4, 11.7, 9.4, "moon"),
        m.box(6.6, 6.6, 4.3, 9.4, 9.4, 11.7, "moon"),
        m.box(6.0, 9.6, 6.0, 7.6, 10.6, 7.6, "moon_light"),
    ], m.DISPLAY_SMALL)


def main():
    palette_atlas()
    wands()
    focuses()
    runes()
    scrolls()
    spellbook()
    potions()
    crystals()
    print("Объёмные модели готовы.")


if __name__ == "__main__":
    main()
