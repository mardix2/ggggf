#!/usr/bin/env python3
"""Генерация всех текстур мода.

Скрипт лежит в репозитории специально: текстуры — производный артефакт,
и их всегда можно пересобрать одной командой, поменяв палитру или форму.

Запуск:  python3 tools/gen_textures.py
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from png import Canvas, palette, shade, hex_to_rgb
from shapes import diamond, disc, ring, line, frame, noise_fill, speckle, glyph

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ITEM_DIR = os.path.join(ROOT, "src/main/resources/assets/arcanum/textures/item")
BLOCK_DIR = os.path.join(ROOT, "src/main/resources/assets/arcanum/textures/block")
EFFECT_DIR = os.path.join(ROOT, "src/main/resources/assets/arcanum/textures/mob_effect")
EQUIP_DIR = os.path.join(ROOT, "src/main/resources/assets/arcanum/textures/entity/equipment")

SCHOOLS = {
    "fire": 0xFF7A2A,
    "frost": 0x7FD8FF,
    "storm": 0xE8E45C,
    "arcane": 0xC77DFF,
    "nature": 0x74D14C,
    "shadow": 0x6E4A8E,
    "light": 0xFFF3B0,
}

WOOD = palette(0x8A6136)
DARK_WOOD = palette(0x5A3F22)
GOLD = palette(0xE8C24A)
PARCHMENT = palette(0xE4D4A8)
STONE = 0x8C8C8C
DEEPSLATE = 0x4A4A50
ROBE = 0x3B2A5C
ROBE_TRIM = 0xD8B44A


def save(canvas, directory, name):
    os.makedirs(directory, exist_ok=True)
    canvas.save(os.path.join(directory, name + ".png"))


# ----------------------------------------------------------------------
#  Предметы
# ----------------------------------------------------------------------

def item_crystal(name, color, small=False):
    c = Canvas(16, 16)
    pal = palette(color)
    if small:
        diamond(c, 7.5, 8.5, 3.5, 5.0, pal)
    else:
        diamond(c, 7.5, 8.0, 5.0, 6.8, pal)
    save(c, ITEM_DIR, name)


def item_dust(name, color):
    c = Canvas(16, 16)
    pal = palette(color)
    heap = [
        (4, 11), (6, 12), (8, 11), (10, 12), (5, 9), (9, 9), (7, 8),
        (3, 12), (11, 10), (7, 10),
    ]
    speckle(c, heap, pal, seed=7)
    # Пара искр над кучкой — «пыль ещё светится».
    c.set(6, 5, pal["h"])
    c.set(10, 6, pal["4"])
    save(c, ITEM_DIR, name)


def item_shard(name, color):
    c = Canvas(16, 16)
    pal = palette(color)
    diamond(c, 6.0, 7.0, 3.0, 4.5, pal)
    diamond(c, 10.0, 10.0, 2.2, 3.4, pal)
    save(c, ITEM_DIR, name)


def item_rune(school):
    c = Canvas(16, 16)
    stone = palette(0x6E6E78)
    color = palette(SCHOOLS[school])
    frame(c, 3, 2, 12, 13, stone["2"], stone["o"])
    noise_fill(c, 4, 3, 11, 12, 0x6E6E78, seed=hash(school) & 0xFFFF, spread=0.12)
    glyph(c, school, 5, 4, color["h"])
    # Лёгкое свечение вокруг глифа.
    glyph(c, school, 5, 5, color["1"])
    glyph(c, school, 5, 4, color["h"])
    save(c, ITEM_DIR, "rune_" + school)


def item_focus(school):
    c = Canvas(16, 16)
    color = palette(SCHOOLS[school])
    ring(c, 8, 8, 6.5, 1.6, GOLD["3"])
    ring(c, 8, 8, 5.2, 0.8, GOLD["1"])
    diamond(c, 8, 8, 2.6, 3.4, color)
    c.set(6, 6, color["h"])
    save(c, ITEM_DIR, "focus_" + school)


def item_wand(name, length, gem_color, handle_pal, trim=None, gem_size=2.4):
    c = Canvas(16, 16)
    start = (3, 14)
    end = (14 - length // 3, 14 - length)
    line(c, start[0], start[1], end[0], end[1], handle_pal["2"], width=2)
    line(c, start[0], start[1] - 1, end[0], end[1] - 1, handle_pal["3"], width=1)
    if trim is not None:
        mid = ((start[0] + end[0]) // 2, (start[1] + end[1]) // 2)
        line(c, mid[0] - 1, mid[1] + 1, mid[0] + 2, mid[1] - 2, trim["3"], width=1)
    diamond(c, end[0] + 0.5, end[1] - 0.5, gem_size, gem_size + 1.0, palette(gem_color))
    save(c, ITEM_DIR, name)


def item_scroll(name, ribbon):
    c = Canvas(16, 16)
    frame(c, 3, 4, 12, 11, PARCHMENT["3"], PARCHMENT["1"])
    noise_fill(c, 4, 5, 11, 10, 0xE4D4A8, seed=11, spread=0.06, density=0.3)
    # Свёрнутые края.
    c.rect(2, 3, 3, 12, PARCHMENT["2"])
    c.rect(12, 3, 13, 12, PARCHMENT["2"])
    for y in range(3, 13):
        c.set(2, y, PARCHMENT["1"])
        c.set(13, y, PARCHMENT["1"])
    for y in range(6, 10):
        line(c, 5, y, 10, y, PARCHMENT["o"] if y % 2 == 0 else None)
    if ribbon is not None:
        c.rect(7, 2, 8, 13, palette(ribbon)["3"])
    save(c, ITEM_DIR, name)


def item_book(name, cover_color):
    c = Canvas(16, 16)
    cover = palette(cover_color)
    frame(c, 2, 2, 13, 13, cover["2"], cover["o"])
    c.rect(2, 2, 4, 13, cover["1"])
    c.rect(5, 3, 12, 12, PARCHMENT["3"])
    for y in range(4, 12, 2):
        line(c, 6, y, 11, y, PARCHMENT["1"])
    diamond(c, 3.5, 7.5, 1.4, 2.2, GOLD)
    save(c, ITEM_DIR, name)


def item_potion(name, liquid_color, sparkle=False):
    c = Canvas(16, 16)
    glass = palette(0xC8D8E8)
    liquid = palette(liquid_color)
    disc(c, 8, 10, 5.2, glass["2"], glass["o"])
    disc(c, 8, 10.5, 4.2, liquid["3"], liquid["1"])
    c.rect(6, 3, 9, 6, glass["2"])
    for y in range(3, 7):
        c.set(6, y, glass["o"])
        c.set(9, y, glass["o"])
    c.rect(6, 1, 9, 2, DARK_WOOD["3"])
    c.set(6, 8, glass["h"])
    c.set(7, 7, glass["h"])
    if sparkle:
        c.set(10, 8, liquid["h"])
        c.set(5, 12, liquid["h"])
        c.set(11, 12, liquid["4"])
    save(c, ITEM_DIR, name)


def item_feather():
    c = Canvas(16, 16)
    pal = palette(0xFF8A3D)
    tip = palette(0xFFE7A0)
    line(c, 3, 13, 12, 3, pal["1"], width=1)
    for i, (dx, dy) in enumerate([(1, 0), (2, 1), (1, 1), (2, 2), (1, 2)]):
        line(c, 4 + i, 12 - i, 4 + i + dx + 1, 12 - i - dy - 2, pal["3"])
        line(c, 4 + i - 1, 12 - i - 1, 4 + i - dx - 1, 12 - i + dy, pal["2"])
    line(c, 10, 5, 12, 3, tip["h"])
    save(c, ITEM_DIR, "phoenix_feather")


def item_essence():
    c = Canvas(16, 16)
    pal = palette(0xBFD9FF)
    disc(c, 8, 8, 5.0, pal["1"], pal["o"])
    disc(c, 8, 8, 3.2, pal["3"])
    disc(c, 6.5, 6.5, 1.4, pal["h"])
    ring(c, 8, 8, 6.4, 0.9, pal["2"])
    save(c, ITEM_DIR, "lunar_essence")


def item_gem():
    c = Canvas(16, 16)
    pal = palette(0xFF6FD8)
    diamond(c, 8, 8, 5.4, 5.4, pal)
    ring(c, 8, 8, 6.6, 0.9, GOLD["3"])
    save(c, ITEM_DIR, "mana_gem")


def item_armor(name, art_rows, color, trim_color):
    """Силуэт части мантии: art_rows — 16 строк по 16 символов."""
    c = Canvas(16, 16)
    pal = palette(color)
    trim = palette(trim_color)
    colors = {"o": pal["o"], "1": pal["1"], "2": pal["2"], "3": pal["3"],
              "4": pal["4"], "t": trim["3"], "T": trim["h"], ".": None, " ": None}
    assert len(art_rows) == 16, name
    for y, row in enumerate(art_rows):
        assert len(row) == 16, (name, y, len(row))
        for x, char in enumerate(row):
            if char not in (".", " "):
                c.set(x, y, colors[char])
    save(c, ITEM_DIR, name)


HOOD = [
    "................",
    ".....oooooo.....",
    "....o222222o....",
    "...o22333322o...",
    "...o23333332o...",
    "..o2333333332o..",
    "..o2333333332o..",
    "..o23oooooo32o..",
    "..o23o....o32o..",
    "..o23o....o32o..",
    "..o22o....o22o..",
    "..ott........tto",
    "..oTt........tTo",
    "...oo........oo.",
    "................",
    "................",
]

ROBE_ART = [
    "................",
    "...oo......oo...",
    "..o22o....o22o..",
    "..o233oooo332o..",
    "..o2333333332o..",
    ".o233333333332o.",
    ".o233ttttttt32o.",
    ".o233tTTTTt332o.",
    ".o2333ttttt332o.",
    ".o233333333332o.",
    "..o23333333332o.",
    "..o2333333332o..",
    "..o233333332o...",
    "..ott3333tto....",
    "..oToooooooTo...",
    "................",
]

LEGGINGS_ART = [
    "................",
    "..oooooooooooo..",
    "..o2ttttttt22o..",
    "..o2TTTTTTT22o..",
    "..o23333333332o.",
    "..o2333333332o..",
    "..o233333333o...",
    "..o233oo33332o..",
    "..o23o..o3332o..",
    "..o23o..o3332o..",
    "..o23o..o3332o..",
    "..o23o..o3332o..",
    "..o23o..o3332o..",
    "..ott3o..o3tto..",
    "..oo........oo..",
    "................",
]

BOOTS_ART = [
    "................",
    "................",
    "................",
    "................",
    "..oooo....oooo..",
    "..o22o....o22o..",
    "..o23o....o32o..",
    "..o23o....o32o..",
    "..o233o..o332o..",
    "..o2333oo3332o..",
    ".o23333333333o..",
    ".ott3333333tto..",
    ".oTTooooooooTo..",
    "..oooooooooooo..",
    "................",
    "................",
]


# ----------------------------------------------------------------------
#  Блоки
# ----------------------------------------------------------------------

def block_ore(name, base_hex, crystal_hex, seed):
    c = Canvas(16, 16)
    noise_fill(c, 0, 0, 15, 15, base_hex, seed=seed)
    pal = palette(crystal_hex)
    for (cx, cy, r) in [(4.5, 5.0, 2.4), (10.5, 9.0, 2.8), (6.0, 11.5, 1.8)]:
        diamond(c, cx, cy, r, r + 0.6, pal)
    save(c, BLOCK_DIR, name)


def block_crystal():
    c = Canvas(16, 16)
    pal = palette(0xC77DFF)
    noise_fill(c, 0, 0, 15, 15, 0x9A5BD6, seed=3, spread=0.12)
    for (x0, y0, x1, y1) in [(0, 0, 15, 15)]:
        line(c, x0, y0, x1, y1, pal["h"])
        line(c, x1, y0, x0, y1, pal["4"])
    for (cx, cy, r) in [(4, 4, 2.6), (12, 5, 2.2), (8, 11, 3.0)]:
        diamond(c, cx, cy, r, r, pal, facet=False)
    save(c, BLOCK_DIR, "arcane_crystal_block")


def block_cluster():
    c = Canvas(16, 16)
    pal = palette(0xC77DFF)
    diamond(c, 8, 8.5, 2.2, 6.5, pal)
    diamond(c, 4.5, 11.0, 1.6, 4.4, pal)
    diamond(c, 11.5, 10.5, 1.8, 4.8, pal)
    save(c, BLOCK_DIR, "arcane_crystal_cluster")


def block_stone(name, base_hex, seed, border=None):
    c = Canvas(16, 16)
    noise_fill(c, 0, 0, 15, 15, base_hex, seed=seed)
    if border is not None:
        pal = palette(border)
        for x in range(16):
            c.set(x, 0, pal["1"])
            c.set(x, 15, pal["o"])
        for y in range(16):
            c.set(0, y, pal["1"])
            c.set(15, y, pal["o"])
    save(c, BLOCK_DIR, name)


def block_runestone(school):
    c = Canvas(16, 16)
    noise_fill(c, 0, 0, 15, 15, 0x5C5C66, seed=hash(school) & 0xFFF)
    pal = palette(SCHOOLS[school])
    frame(c, 2, 2, 13, 13, None, pal["1"])
    glyph(c, school, 4, 4, pal["3"])
    glyph(c, school, 4, 4, pal["h"])
    save(c, BLOCK_DIR, "runestone_" + school)


def block_altar():
    top = Canvas(16, 16)
    noise_fill(top, 0, 0, 15, 15, 0x77737F, seed=21)
    pal = palette(0xC77DFF)
    ring(top, 8, 8, 6.0, 1.2, pal["1"])
    ring(top, 8, 8, 3.4, 1.0, pal["3"])
    glyph(top, "arcane", 5, 5, pal["h"])
    save(top, BLOCK_DIR, "infusion_altar_top")

    side = Canvas(16, 16)
    noise_fill(side, 0, 0, 15, 15, 0x6E6A76, seed=22)
    for y in (3, 11):
        line(side, 0, y, 15, y, palette(0x5A5762)["2"])
    glyph(side, "arcane", 5, 5, pal["2"])
    save(side, BLOCK_DIR, "infusion_altar_side")

    bottom = Canvas(16, 16)
    noise_fill(bottom, 0, 0, 15, 15, 0x5E5B66, seed=23)
    save(bottom, BLOCK_DIR, "infusion_altar_bottom")


def block_font():
    top = Canvas(16, 16)
    noise_fill(top, 0, 0, 15, 15, 0x3E4C72, seed=31)
    water = palette(0x5BA8FF)
    disc(top, 8, 8, 6.0, water["3"], water["1"])
    disc(top, 8, 8, 3.0, water["4"])
    disc(top, 6.5, 6.5, 1.2, water["h"])
    save(top, BLOCK_DIR, "mana_font_top")

    side = Canvas(16, 16)
    noise_fill(side, 0, 0, 15, 15, 0x3A4668, seed=32)
    line(side, 0, 4, 15, 4, water["1"])
    glyph(side, "frost", 5, 6, water["4"])
    save(side, BLOCK_DIR, "mana_font_side")


def block_anchor():
    top = Canvas(16, 16)
    noise_fill(top, 0, 0, 15, 15, 0x4A3A60, seed=41)
    pal = palette(0xC77DFF)
    ring(top, 8, 8, 5.6, 1.4, pal["3"])
    disc(top, 8, 8, 2.4, pal["h"])
    save(top, BLOCK_DIR, "teleport_anchor_top")

    side = Canvas(16, 16)
    noise_fill(side, 0, 0, 15, 15, 0x443556, seed=42)
    line(side, 7, 2, 7, 13, pal["3"], width=2)
    glyph(side, "arcane", 5, 5, pal["4"])
    save(side, BLOCK_DIR, "teleport_anchor_side")


def block_pedestal():
    top = Canvas(16, 16)
    noise_fill(top, 0, 0, 15, 15, 0x807C88, seed=51)
    ring(top, 8, 8, 5.0, 1.0, palette(0xC77DFF)["2"])
    save(top, BLOCK_DIR, "arcane_pedestal_top")

    side = Canvas(16, 16)
    noise_fill(side, 0, 0, 15, 15, 0x757180, seed=52)
    for y in (2, 13):
        line(side, 0, y, 15, y, palette(0x5A5762)["2"])
    save(side, BLOCK_DIR, "arcane_pedestal_side")


def block_lamp():
    for name, lit in (("arcane_lamp_on", True), ("arcane_lamp_off", False)):
        c = Canvas(16, 16)
        base = 0xB9A8D8 if lit else 0x554B66
        noise_fill(c, 0, 0, 15, 15, base, seed=61, spread=0.1)
        pal = palette(0xFFF3B0 if lit else 0x6A6072)
        ring(c, 8, 8, 5.4, 1.4, pal["3"])
        disc(c, 8, 8, 3.0, pal["h"] if lit else pal["1"])
        save(c, BLOCK_DIR, name)


def block_moonpetal():
    c = Canvas(16, 16)
    stem = palette(0x4C7A3A)
    petal = palette(0xD8C8FF)
    core = palette(0xFFF3B0)
    line(c, 8, 15, 8, 7, stem["3"])
    line(c, 8, 11, 5, 9, stem["2"])
    line(c, 8, 12, 11, 10, stem["2"])
    for (dx, dy) in [(0, -3), (-3, -1), (3, -1), (-2, 2), (2, 2)]:
        disc(c, 8 + dx, 6 + dy, 2.0, petal["3"], petal["1"])
    disc(c, 8, 6, 1.6, core["3"], core["1"])
    save(c, BLOCK_DIR, "moonpetal")


# ----------------------------------------------------------------------
#  Иконки эффектов, слои брони и значок мода
# ----------------------------------------------------------------------

EFFECTS = {
    "mana_surge": (0x55CCFF, "arcane"),
    "mana_burn": (0x8A2BE2, "shadow"),
    "arcane_shield": (0xB388FF, "arcane"),
    "chilled": (0x9FE3FF, "frost"),
    "combustion": (0xFF6A00, "fire"),
    "blessed": (0xFFF0A0, "light"),
    "arcane_sight": (0xD0A0FF, "storm"),
    "silence": (0x4A4A55, "shadow"),
}


def effect_icons():
    for name, (color, sigil) in EFFECTS.items():
        c = Canvas(18, 18)
        pal = palette(color)
        disc(c, 9, 9, 8.2, pal["1"], pal["o"])
        disc(c, 9, 9, 6.4, pal["3"])
        glyph(c, sigil, 5, 5, pal["h"])
        save(c, EFFECT_DIR, name)


def armor_layers():
    """Слои брони: сплошная ткань с золотой отделкой по краям областей."""
    for layer, folder in (("humanoid", "humanoid"), ("humanoid_leggings", "humanoid_leggings")):
        c = Canvas(64, 32)
        noise_fill(c, 0, 0, 63, 31, ROBE, seed=71, spread=0.10, density=0.4)
        trim = palette(ROBE_TRIM)
        # Отделка по вертикальным швам модели — на любой части выглядит уместно.
        for x in (0, 15, 16, 31, 32, 39, 40, 47, 48, 55, 56, 63):
            for y in range(32):
                c.set(x, y, trim["1"])
        for y in (0, 15, 16, 31):
            for x in range(64):
                c.set(x, y, trim["3"] if y in (0, 16) else trim["1"])
        save(c, os.path.join(EQUIP_DIR, folder), "arcane_robe")


def mod_icon():
    c = Canvas(128, 128)
    background = palette(0x1A1226)
    c.rect(0, 0, 127, 127, background["3"])
    ring(c, 64, 64, 58, 5, palette(0x6B4FA0)["3"])
    for index, (school, color) in enumerate(SCHOOLS.items()):
        import math
        angle = index * 2 * math.pi / len(SCHOOLS) - math.pi / 2
        x = 64 + math.cos(angle) * 44
        y = 64 + math.sin(angle) * 44
        diamond(c, x, y, 9, 12, palette(color))
    diamond(c, 64, 64, 18, 26, palette(0xC77DFF))
    save(c, os.path.join(ROOT, "src/main/resources/assets/arcanum"), "icon")


def main():
    # Материалы
    item_crystal("arcane_crystal", 0xC77DFF)
    item_crystal("infused_crystal", 0xE0B0FF)
    item_crystal("dim_mana_crystal", 0x6A6A80, small=True)
    item_crystal("charged_mana_crystal", 0x5BC8FF)
    item_dust("arcane_dust", 0xB98CFF)
    item_shard("soul_shard", 0x9FE3FF)
    item_feather()
    item_essence()
    item_gem()

    # Свитки и книга
    item_scroll("blank_scroll", None)
    item_scroll("spell_scroll", 0xC77DFF)
    item_book("spellbook", 0x4A2E6B)

    # Зелья
    item_potion("mana_potion", 0x4FA8FF)
    item_potion("greater_mana_potion", 0x7FD8FF, sparkle=True)

    # Руны и фокусы
    for school in SCHOOLS:
        item_rune(school)
        item_focus(school)

    # Посохи
    item_wand("apprentice_wand", 9, 0xC77DFF, WOOD, gem_size=2.0)
    item_wand("adept_staff", 11, 0x9FD8FF, DARK_WOOD, trim=GOLD, gem_size=2.4)
    item_wand("archmage_staff", 12, 0xFFD24A, DARK_WOOD, trim=GOLD, gem_size=2.8)
    item_wand("eldritch_scepter", 12, 0x9B30FF, palette(0x2A2233), trim=palette(0xB388FF), gem_size=3.2)

    # Мантия
    item_armor("arcane_hood", HOOD, ROBE, ROBE_TRIM)
    item_armor("arcane_robe", ROBE_ART, ROBE, ROBE_TRIM)
    item_armor("arcane_leggings", LEGGINGS_ART, ROBE, ROBE_TRIM)
    item_armor("arcane_boots", BOOTS_ART, ROBE, ROBE_TRIM)

    # Блоки
    block_ore("arcane_crystal_ore", STONE, 0xC77DFF, seed=101)
    block_ore("deepslate_arcane_crystal_ore", DEEPSLATE, 0xC77DFF, seed=102)
    block_crystal()
    block_cluster()
    block_stone("warded_stone", 0x3E3E48, seed=103, border=0x6B4FA0)
    block_altar()
    block_font()
    block_anchor()
    block_pedestal()
    block_lamp()
    block_moonpetal()
    for school in SCHOOLS:
        block_runestone(school)

    effect_icons()
    armor_layers()
    mod_icon()
    print("Текстуры готовы.")


if __name__ == "__main__":
    main()
