#!/usr/bin/env python3
"""Генерация текстур мода.

Скрипт лежит в репозитории специально: текстуры — производный артефакт,
и их всегда можно пересобрать одной командой, поменяв палитру или форму.

Предметам с объёмными моделями (tools/gen_models3d.py) плоский спрайт не
нужен, поэтому здесь рисуются только те, что перечислены в
`content.SPRITE_ITEMS`. Магические блоки получают пульсирующую анимацию:
несколько кадров в столбик плюс файл `.png.mcmeta`.

Запуск:  python3 tools/gen_textures.py
"""

import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from content import ANIMATED_BLOCK_TEXTURES, SPRITE_ITEMS
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

#: Кадров в цикле пульсации и сколько тиков держится кадр.
FRAMES = 4
FRAMETIME = 8


def save(canvas, directory, name):
    os.makedirs(directory, exist_ok=True)
    canvas.save(os.path.join(directory, name + ".png"))


def brighten(color, factor):
    """Меняет яркость цвета 0xRRGGBB и возвращает снова 0xRRGGBB."""
    r, g, b, _a = shade(hex_to_rgb(color), factor)
    return (r << 16) | (g << 8) | b


def animate(directory, name, frame_fn):
    """Склеивает кадры в столбик и кладёт рядом описание анимации.

    Кадры берутся по синусу, поэтому последний плавно переходит в первый —
    с `interpolate` это даёт ровное «дыхание» без рывка на стыке.
    """
    os.makedirs(directory, exist_ok=True)
    sheet = Canvas(16, 16 * FRAMES)
    for index in range(FRAMES):
        glow = 0.5 + 0.5 * math.sin(2 * math.pi * index / FRAMES)
        sheet.blit(frame_fn(glow), 0, index * 16)
    sheet.save(os.path.join(directory, name + ".png"))

    meta = {"animation": {"frametime": FRAMETIME, "interpolate": True}}
    with open(os.path.join(directory, name + ".png.mcmeta"), "w", encoding="utf-8") as handle:
        json.dump(meta, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


# ----------------------------------------------------------------------
#  Предметы (только те, у кого нет объёмной модели)
# ----------------------------------------------------------------------

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


def item_rune(school):
    """Спрайт руны: он же служит лицевой гранью объёмной таблички."""
    c = Canvas(16, 16)
    stone = palette(0x6E6E78)
    color = palette(SCHOOLS[school])
    frame(c, 3, 2, 12, 13, stone["2"], stone["o"])
    noise_fill(c, 4, 3, 11, 12, 0x6E6E78, seed=hash(school) & 0xFFFF, spread=0.12)
    glyph(c, school, 5, 5, color["1"])
    glyph(c, school, 5, 4, color["h"])
    save(c, ITEM_DIR, "rune_" + school)


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
#  Блоки: статичные
# ----------------------------------------------------------------------

def block_ore(name, base_hex, crystal_hex, seed):
    c = Canvas(16, 16)
    noise_fill(c, 0, 0, 15, 15, base_hex, seed=seed)
    pal = palette(crystal_hex)
    for (cx, cy, r) in [(4.5, 5.0, 2.4), (10.5, 9.0, 2.8), (6.0, 11.5, 1.8)]:
        diamond(c, cx, cy, r, r + 0.6, pal)
    save(c, BLOCK_DIR, name)


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


def block_font_side():
    side = Canvas(16, 16)
    noise_fill(side, 0, 0, 15, 15, 0x3A4668, seed=32)
    water = palette(0x5BA8FF)
    line(side, 0, 4, 15, 4, water["1"])
    glyph(side, "frost", 5, 6, water["4"])
    save(side, BLOCK_DIR, "mana_font_side")


def block_anchor_side():
    side = Canvas(16, 16)
    noise_fill(side, 0, 0, 15, 15, 0x443556, seed=42)
    pal = palette(0xC77DFF)
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


def block_lamp_off():
    c = Canvas(16, 16)
    noise_fill(c, 0, 0, 15, 15, 0x554B66, seed=61, spread=0.1)
    pal = palette(0x6A6072)
    ring(c, 8, 8, 5.4, 1.4, pal["3"])
    disc(c, 8, 8, 3.0, pal["1"])
    save(c, BLOCK_DIR, "arcane_lamp_off")


# ----------------------------------------------------------------------
#  Блоки: кадры пульсации
# ----------------------------------------------------------------------

def frame_crystal_block(glow):
    c = Canvas(16, 16)
    pal = palette(brighten(0xC77DFF, 0.85 + 0.45 * glow))
    noise_fill(c, 0, 0, 15, 15, 0x9A5BD6, seed=3, spread=0.12)
    line(c, 0, 0, 15, 15, pal["h"])
    line(c, 15, 0, 0, 15, pal["4"])
    for (cx, cy, r) in [(4, 4, 2.6), (12, 5, 2.2), (8, 11, 3.0)]:
        diamond(c, cx, cy, r, r, pal, facet=False)
    return c


def frame_cluster(glow):
    c = Canvas(16, 16)
    pal = palette(brighten(0xC77DFF, 0.8 + 0.5 * glow))
    diamond(c, 8, 8.5, 2.2, 6.5, pal)
    diamond(c, 4.5, 11.0, 1.6, 4.4, pal)
    diamond(c, 11.5, 10.5, 1.8, 4.8, pal)
    return c


def frame_font_top(glow):
    c = Canvas(16, 16)
    noise_fill(c, 0, 0, 15, 15, 0x3E4C72, seed=31)
    water = palette(brighten(0x5BA8FF, 0.8 + 0.5 * glow))
    disc(c, 8, 8, 6.0, water["3"], water["1"])
    disc(c, 8, 8, 3.0 + glow * 0.8, water["4"])
    disc(c, 6.5, 6.5, 1.2, water["h"])
    return c


def frame_anchor_top(glow):
    c = Canvas(16, 16)
    noise_fill(c, 0, 0, 15, 15, 0x4A3A60, seed=41)
    pal = palette(brighten(0xC77DFF, 0.75 + 0.55 * glow))
    ring(c, 8, 8, 5.6, 1.4, pal["3"])
    disc(c, 8, 8, 2.0 + glow * 0.8, pal["h"])
    return c


def frame_lamp_on(glow):
    c = Canvas(16, 16)
    noise_fill(c, 0, 0, 15, 15, brighten(0xB9A8D8, 0.9 + 0.2 * glow), seed=61, spread=0.1)
    pal = palette(brighten(0xFFF3B0, 0.8 + 0.4 * glow))
    ring(c, 8, 8, 5.4, 1.4, pal["3"])
    disc(c, 8, 8, 3.0, pal["h"])
    return c


def frame_moonpetal(glow):
    c = Canvas(16, 16)
    stem = palette(0x4C7A3A)
    petal = palette(0xD8C8FF)
    core = palette(brighten(0xFFF3B0, 0.75 + 0.5 * glow))
    line(c, 8, 15, 8, 7, stem["3"])
    line(c, 8, 11, 5, 9, stem["2"])
    line(c, 8, 12, 11, 10, stem["2"])
    for (dx, dy) in [(0, -3), (-3, -1), (3, -1), (-2, 2), (2, 2)]:
        disc(c, 8 + dx, 6 + dy, 2.0, petal["3"], petal["1"])
    disc(c, 8, 6, 1.6, core["3"], core["1"])
    return c


def frame_runestone(school, glow):
    c = Canvas(16, 16)
    noise_fill(c, 0, 0, 15, 15, 0x5C5C66, seed=hash(school) & 0xFFF)
    pal = palette(brighten(SCHOOLS[school], 0.7 + 0.6 * glow))
    frame(c, 2, 2, 13, 13, None, pal["1"])
    glyph(c, school, 4, 4, pal["3"])
    glyph(c, school, 4, 4, pal["h"])
    return c


# ----------------------------------------------------------------------
#  Иконки эффектов, слои брони и значок мода
# ----------------------------------------------------------------------

EFFECT_SIGILS = {
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
    for name, (color, sigil) in EFFECT_SIGILS.items():
        c = Canvas(18, 18)
        pal = palette(color)
        disc(c, 9, 9, 8.2, pal["1"], pal["o"])
        disc(c, 9, 9, 6.4, pal["3"])
        glyph(c, sigil, 5, 5, pal["h"])
        save(c, EFFECT_DIR, name)


def armor_layers():
    """Слои брони: сплошная ткань с золотой отделкой по краям областей."""
    for folder in ("humanoid", "humanoid_leggings"):
        c = Canvas(64, 32)
        noise_fill(c, 0, 0, 63, 31, ROBE, seed=71, spread=0.10, density=0.4)
        trim = palette(ROBE_TRIM)
        for x in (0, 15, 16, 31, 32, 39, 40, 47, 48, 55, 56, 63):
            for y in range(32):
                c.set(x, y, trim["1"])
        for y in (0, 15, 16, 31):
            for x in range(64):
                c.set(x, y, trim["3"] if y in (0, 16) else trim["1"])
        save(c, os.path.join(EQUIP_DIR, folder), "arcane_robe")


def mod_icon():
    c = Canvas(128, 128)
    c.rect(0, 0, 127, 127, palette(0x1A1226)["3"])
    ring(c, 64, 64, 58, 5, palette(0x6B4FA0)["3"])
    for index, (_school, color) in enumerate(SCHOOLS.items()):
        angle = index * 2 * math.pi / len(SCHOOLS) - math.pi / 2
        x = 64 + math.cos(angle) * 44
        y = 64 + math.sin(angle) * 44
        diamond(c, x, y, 9, 12, palette(color))
    diamond(c, 64, 64, 18, 26, palette(0xC77DFF))
    save(c, os.path.join(ROOT, "src/main/resources/assets/arcanum"), "icon")


def main():
    item_dust("arcane_dust", 0xB98CFF)
    item_feather()
    for school in SCHOOLS:
        item_rune(school)

    item_armor("arcane_hood", HOOD, ROBE, ROBE_TRIM)
    item_armor("arcane_robe", ROBE_ART, ROBE, ROBE_TRIM)
    item_armor("arcane_leggings", LEGGINGS_ART, ROBE, ROBE_TRIM)
    item_armor("arcane_boots", BOOTS_ART, ROBE, ROBE_TRIM)

    block_ore("arcane_crystal_ore", STONE, 0xC77DFF, seed=101)
    block_ore("deepslate_arcane_crystal_ore", DEEPSLATE, 0xC77DFF, seed=102)
    block_stone("warded_stone", 0x3E3E48, seed=103, border=0x6B4FA0)
    block_altar()
    block_font_side()
    block_anchor_side()
    block_pedestal()
    block_lamp_off()

    animate(BLOCK_DIR, "arcane_crystal_block", frame_crystal_block)
    animate(BLOCK_DIR, "arcane_crystal_cluster", frame_cluster)
    animate(BLOCK_DIR, "mana_font_top", frame_font_top)
    animate(BLOCK_DIR, "teleport_anchor_top", frame_anchor_top)
    animate(BLOCK_DIR, "arcane_lamp_on", frame_lamp_on)
    animate(BLOCK_DIR, "moonpetal", frame_moonpetal)
    for school in SCHOOLS:
        animate(BLOCK_DIR, "runestone_" + school,
                lambda glow, s=school: frame_runestone(s, glow))

    effect_icons()
    armor_layers()
    mod_icon()

    # Проверка, что список в content.py не разошёлся с тем, что здесь рисуется.
    drawn = {name[:-4] for name in os.listdir(ITEM_DIR) if name.endswith(".png")}
    expected = set(SPRITE_ITEMS) | {"palette"}
    if drawn - expected:
        print("ВНИМАНИЕ: лишние спрайты предметов:", sorted(drawn - expected))
    if expected - drawn - {"palette"}:
        print("ВНИМАНИЕ: не нарисованы спрайты:", sorted(expected - drawn - {"palette"}))

    print(f"Текстуры готовы (анимированных: {len(ANIMATED_BLOCK_TEXTURES)}).")


if __name__ == "__main__":
    main()
