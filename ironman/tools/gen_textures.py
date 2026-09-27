#!/usr/bin/env python3
"""Генерация всех текстур мода: предметы, блоки, слои брони и иконка.

Pillow не нужен: картинки рисуются попиксельно (см. png.py).
Запуск:  python3 tools/gen_textures.py
"""

import os
import random
import zlib

from png import Canvas, hex_to_rgb, shade

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TEX = os.path.join(ROOT, "src/main/resources/assets/ironman/textures")


def rgb(value, alpha=255):
    return (*hex_to_rgb(value), alpha)


# ---------------------------------------------------------------------------
#  Палитры костюмов
# ---------------------------------------------------------------------------

SUITS = {
    # Mark I — серое железо с заклёпками и ржавчиной.
    "mark_i": {
        "o": rgb(0x2A2C30), "d": rgb(0x55585E), "b": rgb(0x8A8E94), "l": rgb(0xB8BCC2),
        "h": rgb(0xDADDE0), "g": rgb(0x6E7178), "G": rgb(0x9A9EA4), "k": rgb(0x7A5234),
        "e": rgb(0x1A1A1A), "r": rgb(0x9FE9F5), "R": rgb(0xE8FDFF), "c": rgb(0x4FA8C0),
        "t": rgb(0xFF9A3C),
    },
    # Mark III — классический красный с золотом.
    "mark_iii": {
        "o": rgb(0x3A0808), "d": rgb(0x7A1414), "b": rgb(0xB31E1E), "l": rgb(0xD83A30),
        "h": rgb(0xF06A58), "g": rgb(0xD9A83A), "G": rgb(0xF4D06A), "k": rgb(0x8A6420),
        "e": rgb(0xE8FDFF), "r": rgb(0x7FE6FF), "R": rgb(0xF2FFFF), "c": rgb(0x2FA6C8),
        "t": rgb(0x9FF4FF),
    },
    # Mark 85 — тёмно-красный, больше золота, нано-блики.
    "mark_85": {
        "o": rgb(0x2A0408), "d": rgb(0x5E0C14), "b": rgb(0x8E1422), "l": rgb(0xB82A36),
        "h": rgb(0xE05A62), "g": rgb(0xE8B444), "G": rgb(0xFFDC7A), "k": rgb(0x9A6A1E),
        "e": rgb(0xF2FFFF), "r": rgb(0x8AF0FF), "R": rgb(0xFFFFFF), "c": rgb(0x38B8DC),
        "t": rgb(0xB8F8FF),
    },
}


def check(art, size=16):
    assert len(art) == size, f"{len(art)} строк вместо {size}"
    for line in art:
        assert len(line) == size, f"строка «{line}» длиной {len(line)}"


def paint(art, colors, size=16):
    check(art, size)
    canvas = Canvas(size, size)
    for y, line in enumerate(art):
        for x, char in enumerate(line):
            if char != ".":
                canvas.set(x, y, colors[char])
    return canvas


# ---------------------------------------------------------------------------
#  Иконки частей костюма
# ---------------------------------------------------------------------------

HELMET = [
    "................",
    ".....oooooo.....",
    "...oohbbbbbboo..",
    "..ohbbbbbbbbbdo.",
    "..obggggggggbdo.",
    ".obgGGGGGGGGgdo.",
    ".obgGGGGGGGGgdo.",
    ".obgeeeGGeeegdo.",
    ".obgGGGGGGGGgdo.",
    ".obgGGGGGGGGgdo.",
    ".odgGGGGGGGGgdo.",
    "..odgGkkkkGgdo..",
    "..odgGGGGGGgdo..",
    "...odgggggdo....",
    "....oooooooo....",
    "................",
]

CHESTPLATE = [
    "................",
    "..ooo......ooo..",
    ".ohbbo....obbdo.",
    ".obbbboooobbbdo.",
    ".obbbbbbbbbbbdo.",
    "..obbbccccbbdo..",
    "..obbcrRRrcbdo..",
    "..obbcRRRRcbdo..",
    "..obbcrRRrcbdo..",
    "..obbbccccbbdo..",
    "..obgGgGgGggdo..",
    "..obgGgGgGggdo..",
    "..odggggggggdo..",
    "...odddddddddo..",
    "....ooooooooo...",
    "................",
]

LEGGINGS = [
    "................",
    "...oooooooooo...",
    "...okkkkkkkkko..",
    "...obbbbbbbbdo..",
    "...obbbooobbdo..",
    "...obbo...obdo..",
    "...ogGo...oGgo..",
    "...ogGo...oGgo..",
    "...obbo...obdo..",
    "...obbo...obdo..",
    "...obbo...obdo..",
    "...obbo...obdo..",
    "...obdo...obdo..",
    "...oooo...oooo..",
    "................",
    "................",
]

BOOTS = [
    "................",
    "................",
    "................",
    "................",
    "..oooo....oooo..",
    "..obdo....obdo..",
    "..obdo....obdo..",
    "..ogGo....oGgo..",
    "..obdo....obdo..",
    ".obbdo...obbdo..",
    "obbbdo..obbbdo..",
    "obbbbo..obbbbo..",
    "odddddo.oddddo..",
    "ottttto.ottttto.",
    "ooooooo.ooooooo.",
    "................",
]

# Mark I: вместо лицевой пластины — железная маска с узкой прорезью.
HELMET_MK1 = [
    "................",
    ".....oooooo.....",
    "...oolllllloo...",
    "..olbbbbbbbbdo..",
    "..obdbbbbbbddo..",
    ".obbbbbbbbbbbdo.",
    ".obeeeeeeeeeedo.",
    ".obbbbbbbbbbbdo.",
    ".obdbbbbbbbbddo.",
    ".obbbbbbbbbbbdo.",
    ".obbededededbdo.",
    "..obbbbbbbbbdo..",
    "..obdbbbbbbddo..",
    "...odbbbbbbdo...",
    "....oooooooo....",
    "................",
]

PART_ART = {
    "helmet": HELMET,
    "chestplate": CHESTPLATE,
    "leggings": LEGGINGS,
    "boots": BOOTS,
}


def suit_icons():
    for suit, colors in SUITS.items():
        for part, art in PART_ART.items():
            if suit == "mark_i" and part == "helmet":
                art = HELMET_MK1
            elif suit == "mark_85":
                # Нанокостюм узнаётся по золотым кромкам.
                art = [line.replace("d", "g").replace("h", "G") for line in art]
            paint(art, colors).save(os.path.join(TEX, "item", f"{suit}_{part}.png"))


# ---------------------------------------------------------------------------
#  Слои брони 64x32 (развёртка модели игрока)
# ---------------------------------------------------------------------------

class Layer:
    """Холст 64x32 с именованными гранями кубоидов модели игрока."""

    def __init__(self):
        self.canvas = Canvas(64, 32)

    def faces(self, u, v, w, h, d):
        """Грани кубоида: имя -> (x0, y0, ширина, высота)."""
        return {
            "top": (u + d, v, w, d),
            "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h),
            "back": (u + d + w + d, v + d, w, h),
        }

    def fill(self, face, color, rows=None):
        x0, y0, w, h = face
        for y in range(h):
            if rows is not None and y not in rows:
                continue
            for x in range(w):
                self.canvas.set(x0 + x, y0 + y, color)

    def px(self, face, x, y, color):
        x0, y0, w, h = face
        if 0 <= x < w and 0 <= y < h:
            self.canvas.set(x0 + x, y0 + y, color)

    def rect(self, face, x1, y1, x2, y2, color):
        for y in range(y1, y2 + 1):
            for x in range(x1, x2 + 1):
                self.px(face, x, y, color)

    def save(self, path):
        self.canvas.save(path)


def speckle(layer, face, colors, seed, density=0.18):
    """Лёгкий шум по граням, чтобы металл не был «пластиковым»."""
    rng = random.Random(seed)
    x0, y0, w, h = face
    for y in range(h):
        for x in range(w):
            pixel = layer.canvas.get(x0 + x, y0 + y)
            if pixel[3] == 0 or rng.random() > density:
                continue
            factor = rng.choice((0.9, 1.1))
            layer.canvas.set(x0 + x, y0 + y, shade(pixel, factor))


def head(layer, c, suit):
    f = layer.faces(0, 0, 8, 8, 8)
    for name in f:
        layer.fill(f[name], c["b"])
    layer.fill(f["top"], c["l"], rows=range(0, 3))
    front = f["front"]
    if suit == "mark_i":
        # Грубая маска: прорезь для глаз и решётка рта.
        layer.rect(front, 0, 0, 7, 7, c["b"])
        layer.rect(front, 1, 3, 6, 3, c["e"])
        for x in (1, 3, 5):
            layer.px(front, x, 6, c["e"])
        for x, y in ((0, 0), (7, 0), (0, 7), (7, 7), (0, 4), (7, 4)):
            layer.px(front, x, y, c["d"])
        for side in ("right", "left", "back"):
            for x, y in ((1, 1), (6, 1), (1, 6), (6, 6)):
                layer.px(f[side], x, y, c["d"])
    else:
        # Золотая лицевая пластина с горящими глазами.
        layer.rect(front, 1, 1, 6, 7, c["g"])
        layer.rect(front, 2, 2, 5, 6, c["G"])
        layer.rect(front, 1, 3, 2, 3, c["e"])
        layer.rect(front, 5, 3, 6, 3, c["e"])
        layer.rect(front, 3, 6, 4, 6, c["k"])
        layer.px(front, 1, 7, c["k"])
        layer.px(front, 6, 7, c["k"])
        # Боковые «уши» шлема.
        for side in ("right", "left"):
            layer.rect(f[side], 3, 3, 5, 5, c["d"])
            layer.px(f[side], 4, 4, c["g"])
        layer.rect(f["back"], 3, 0, 4, 7, c["d"])
    for name in ("right", "left", "back"):
        speckle(layer, f[name], c, zlib.crc32((suit + name).encode()))


def body(layer, c, suit):
    f = layer.faces(16, 16, 8, 12, 4)
    for name in f:
        layer.fill(f[name], c["b"])
    front = f["front"]
    # Грудные пластины со светлой кромкой.
    layer.rect(front, 0, 0, 7, 0, c["l"])
    # Дуговой реактор.
    if suit == "mark_i":
        layer.rect(front, 3, 3, 4, 4, c["c"])
        layer.px(front, 3, 3, c["r"])
        for x, y in ((1, 1), (6, 1), (1, 6), (6, 6), (1, 9), (6, 9)):
            layer.px(front, x, y, c["d"])
        layer.rect(front, 0, 7, 7, 7, c["d"])
        layer.px(front, 2, 10, c["k"])
        layer.px(front, 5, 5, c["k"])
    else:
        layer.rect(front, 2, 2, 5, 5, c["c"])
        layer.rect(front, 3, 2, 4, 5, c["r"])
        layer.rect(front, 2, 3, 5, 4, c["r"])
        layer.rect(front, 3, 3, 4, 4, c["R"])
        # Золотой пресс.
        layer.rect(front, 1, 7, 6, 10, c["g"])
        for y in (7, 9):
            layer.rect(front, 2, y, 5, y, c["G"])
        layer.rect(front, 3, 7, 4, 10, c["k"])
        if suit == "mark_85":
            # Золотые наплечные пластины и «крылья» вокруг реактора.
            layer.rect(front, 0, 0, 1, 1, c["g"])
            layer.rect(front, 6, 0, 7, 1, c["g"])
            layer.px(front, 1, 3, c["G"])
            layer.px(front, 6, 3, c["G"])
    layer.rect(front, 0, 11, 7, 11, c["d"])
    # Спина: хребет и два сопла.
    back = f["back"]
    layer.rect(back, 3, 0, 4, 11, c["d"])
    layer.rect(back, 1, 3, 2, 5, c["g"])
    layer.rect(back, 5, 3, 6, 5, c["g"])
    layer.px(back, 1, 5, c["t"])
    layer.px(back, 6, 5, c["t"])
    for name in ("right", "left", "back"):
        speckle(layer, f[name], c, zlib.crc32((suit + "body" + name).encode()))


def arm(layer, c, suit):
    f = layer.faces(40, 16, 4, 12, 4)
    for name in f:
        layer.fill(f[name], c["b"])
    for name in ("right", "front", "left", "back"):
        face = f[name]
        layer.rect(face, 0, 0, 3, 0, c["l"])
        if suit == "mark_i":
            layer.rect(face, 0, 4, 3, 4, c["d"])
            layer.px(face, 1, 2, c["d"])
            layer.px(face, 2, 8, c["d"])
        else:
            # Золотой наруч и красная перчатка.
            layer.rect(face, 0, 5, 3, 8, c["g"])
            layer.rect(face, 0, 6, 3, 6, c["G"])
            if suit == "mark_85":
                layer.rect(face, 0, 0, 3, 2, c["g"])
                layer.rect(face, 0, 1, 3, 1, c["G"])
        layer.rect(face, 0, 11, 3, 11, c["d"])
    # Ладонь: репульсор (у Mark I — сопло огнемёта).
    bottom = f["bottom"]
    layer.fill(bottom, c["d"])
    if suit == "mark_i":
        layer.rect(bottom, 1, 1, 2, 2, c["t"])
    else:
        layer.rect(bottom, 1, 1, 2, 2, c["r"])
        layer.px(bottom, 1, 1, c["R"])
    for name in ("right", "front", "left", "back"):
        speckle(layer, f[name], c, zlib.crc32((suit + "arm" + name).encode()))


def boots(layer, c, suit):
    """Ботинки: только нижние ряды ноги в первом слое."""
    f = layer.faces(0, 16, 4, 12, 4)
    for name in ("right", "front", "left", "back"):
        face = f[name]
        layer.fill(face, c["b"], rows=range(7, 12))
        layer.rect(face, 0, 7, 3, 7, c["g"] if suit != "mark_i" else c["l"])
        layer.rect(face, 0, 11, 3, 11, c["d"])
    # Подошва — сопло ускорителя.
    bottom = f["bottom"]
    layer.fill(bottom, c["d"])
    layer.rect(bottom, 1, 1, 2, 2, c["t"])


def leggings(layer, c, suit):
    """Второй слой: бёдра, колени, голени и пояс."""
    f = layer.faces(0, 16, 4, 12, 4)
    for name in ("top", "right", "front", "left", "back"):
        layer.fill(f[name], c["b"])
    for name in ("right", "front", "left", "back"):
        face = f[name]
        if suit == "mark_i":
            layer.rect(face, 0, 5, 3, 5, c["d"])
            layer.px(face, 1, 2, c["d"])
        else:
            layer.rect(face, 0, 4, 3, 6, c["g"])
            layer.rect(face, 1, 5, 2, 5, c["G"])
            if suit == "mark_85":
                layer.rect(face, 0, 0, 0, 11, c["g"])
        speckle(layer, face, c, zlib.crc32((suit + "leg" + name).encode()))
    # Пояс на корпусе: только нижняя часть граней.
    torso = layer.faces(16, 16, 8, 12, 4)
    for name in ("right", "front", "left", "back"):
        layer.fill(torso[name], c["b"], rows=range(8, 12))
        layer.fill(torso[name], c["d"], rows=range(8, 9))
    layer.rect(torso["front"], 3, 9, 4, 10, c["g"] if suit != "mark_i" else c["l"])


def armor_layers():
    out = os.path.join(TEX, "entity/equipment")
    os.makedirs(os.path.join(out, "humanoid"), exist_ok=True)
    os.makedirs(os.path.join(out, "humanoid_leggings"), exist_ok=True)
    for suit, c in SUITS.items():
        top = Layer()
        head(top, c, suit)
        body(top, c, suit)
        arm(top, c, suit)
        boots(top, c, suit)
        top.save(os.path.join(out, "humanoid", f"{suit}.png"))

        bottom = Layer()
        leggings(bottom, c, suit)
        bottom.save(os.path.join(out, "humanoid_leggings", f"{suit}.png"))


# ---------------------------------------------------------------------------
#  Материалы и компоненты
# ---------------------------------------------------------------------------

PALLADIUM = {
    "o": rgb(0x263038), "d": rgb(0x5A6874), "b": rgb(0x93A4B2), "l": rgb(0xC4D2DC),
    "h": rgb(0xEEF6FA),
}
ALLOY = {
    "o": rgb(0x4A3208), "d": rgb(0x9A7020), "b": rgb(0xD4A843), "l": rgb(0xECCB78),
    "h": rgb(0xFFF1C4), "r": rgb(0xB82A2A),
}
GLOW = {
    "o": rgb(0x14323C), "d": rgb(0x2F6C80), "c": rgb(0x2FA6C8), "r": rgb(0x7FE6FF),
    "R": rgb(0xF2FFFF), "m": rgb(0x5A6874), "M": rgb(0x93A4B2), "l": rgb(0xC4D2DC),
    "g": rgb(0xD4A843), "G": rgb(0xF4D06A), "t": rgb(0xFF9A3C), "y": rgb(0xFFE08A),
    "v": rgb(0xB77CFF), "V": rgb(0xE3CCFF), "k": rgb(0x1E2226), "e": rgb(0xB31E1E),
}

ITEMS = {
    "raw_palladium": ([
        "................",
        "................",
        "......ooo.......",
        "....oolllo......",
        "...olhlbbdo.....",
        "...olbbbbbdoo...",
        "..olbbdbbbbldo..",
        "..obbbbbblbbdo..",
        "..obdbbbbbbbdo..",
        "...obbblbbdddo..",
        "...odbbbbddoo...",
        "....oddddoo.....",
        ".....oooo.......",
        "................",
        "................",
        "................",
    ], PALLADIUM),
    "palladium_ingot": ([
        "................",
        "................",
        "................",
        "................",
        "......oooooooo..",
        ".....ohhlllllo..",
        "....olllllllbo..",
        "...olbbbbbbbdo..",
        "..oooooooooodo..",
        "..ohlllllllodo..",
        "..olbbbbbbbodo..",
        "..obbbbbbbbdo...",
        "..odddddddddo...",
        "..ooooooooooo...",
        "................",
        "................",
    ], PALLADIUM),
    "gold_titanium_alloy": ([
        "................",
        "................",
        "................",
        "................",
        "......oooooooo..",
        ".....ohhlllllo..",
        "....olllrrlllo..",
        "...olbbbbbbbdo..",
        "..oooooooooodo..",
        "..ohlllllllodo..",
        "..olbbrrbbbodo..",
        "..obbbbbbbbdo...",
        "..odddddddddo...",
        "..ooooooooooo...",
        "................",
        "................",
    ], ALLOY),
    "starkium": ([
        "................",
        ".......oo.......",
        "......oVVo......",
        "......oVvo......",
        ".....oVvRvo.....",
        ".....oVvrvo.....",
        "....oVvrRrvo....",
        "....oVvrrrvo....",
        "...oVvrRRRrvo...",
        "...oVvrrrrrvo...",
        "..oVvvrrRrrvvo..",
        "..oVvvvvvvvvvo..",
        ".ooooooooooooooo",
        "................",
        "................",
        "................",
    ], GLOW),
    "nanotech_core": ([
        "................",
        "................",
        ".....oooooo.....",
        "....okkkkkkko...",
        "...okkrkkkrkko..",
        "..okkkrkkkrkkko.",
        "..okrrrrRrrrrko.",
        "..okkkrRRRrkkko.",
        "..okkkrRRRrkkko.",
        "..okrrrrRrrrrko.",
        "..okkkrkkkrkkko.",
        "...okkrkkkrkko..",
        "....okkkkkkko...",
        ".....oooooo.....",
        "................",
        "................",
    ], GLOW),
    "repulsor_node": ([
        "................",
        "................",
        ".....oooooo.....",
        "....oMMMMMMo....",
        "...oMmmmmmmMo...",
        "..oMmccccccmMo..",
        "..oMmcrrrrcmMo..",
        "..oMmcrRRrcmMo..",
        "..oMmcrRRrcmMo..",
        "..oMmcrrrrcmMo..",
        "..oMmccccccmMo..",
        "...oMmmmmmmMo...",
        "....oMMMMMMo....",
        ".....oooooo.....",
        "................",
        "................",
    ], GLOW),
    "jet_thruster": ([
        "................",
        "......oooo......",
        ".....olMMmo.....",
        ".....oMMMmo.....",
        ".....oMMMmo.....",
        "....olMMMMmo....",
        "....oMggggmo....",
        "...olMMMMMMmo...",
        "...oMMMMMMMmo...",
        "..oommmmmmmmoo..",
        "..okkkkkkkkkko..",
        "...otyyyyyyto...",
        "....ottyytto....",
        ".....ottto......",
        "......oto.......",
        "................",
    ], GLOW),
    "micro_missile": ([
        "................",
        "............oo..",
        "...........oeeo.",
        "..........oMeo..",
        ".........oMlo...",
        "........oMlo....",
        ".......oMlo.....",
        "......oMlo......",
        ".....oMlo.......",
        "...ooMlo........",
        "..omMmo.........",
        "..ommo..........",
        ".otmo...........",
        "otyo............",
        "oto.............",
        "................",
    ], GLOW),
    "power_cell": ([
        "................",
        "......oooo......",
        ".....oMMMMo.....",
        "....ooooooooo...",
        "....olMMMMMmo...",
        "....oMccccMmo...",
        "....oMrrrrMmo...",
        "....oMrRRrMmo...",
        "....oMrRRrMmo...",
        "....oMrrrrMmo...",
        "....oMccccMmo...",
        "....oMccccMmo...",
        "....olMMMMMmo...",
        "....ooooooooo...",
        "................",
        "................",
    ], GLOW),
    "arc_reactor_mk1": ([
        "................",
        "....oooooooo....",
        "...oMmmmmmmmo...",
        "..oMmMMMMMMmmo..",
        ".oMmMccccccMmmo.",
        ".oMmcrrrrrrcMmo.",
        ".oMmcrRRRRrcMmo.",
        ".oMmcrRRRRrcMmo.",
        ".oMmcrRRRRrcMmo.",
        ".oMmcrRRRRrcMmo.",
        ".oMmcrrrrrrcMmo.",
        ".oMmMccccccMmmo.",
        "..oMmMMMMMMmmo..",
        "...oMmmmmmmmo...",
        "....oooooooo....",
        "................",
    ], GLOW),
    "arc_reactor_mk2": ([
        "................",
        ".....oooooo.....",
        "...ooMlllllMoo..",
        "..oMlccccccllMo.",
        "..olcrrcrrcrrlo.",
        ".oMccrRRRRrrclMo",
        ".olcrRRRRRRrcclo",
        ".olcrRRRRRRrcclo",
        ".olccRRRRRRrcclo",
        ".oMcrrRRRRrrclMo",
        "..olcrrcrrcrrlo.",
        "..oMlccccccllMo.",
        "...ooMlllllMoo..",
        ".....oooooo.....",
        "................",
        "................",
    ], GLOW),
    "starkium_arc_reactor": ([
        "................",
        ".....oooooo.....",
        "...ooMlllllMoo..",
        "..oMlccccccllMo.",
        "..olcrrrrrrrclo.",
        ".oMcrRRRRRRRrcMo",
        ".olcrrRRRRRrrclo",
        ".olcrrrRRRrrrclo",
        ".olcrrrrRrrrrclo",
        ".oMcrrrrrrrrrcMo",
        "..olcrrrrrrrclo.",
        "..oMlccccccllMo.",
        "...ooMlllllMoo..",
        ".....oooooo.....",
        "................",
        "................",
    ], GLOW),
}


def items():
    for name, (art, colors) in ITEMS.items():
        paint(art, colors).save(os.path.join(TEX, "item", f"{name}.png"))


# ---------------------------------------------------------------------------
#  Блоки
# ---------------------------------------------------------------------------

def noise_base(base_hex, seed, spread=0.14):
    rng = random.Random(seed)
    canvas = Canvas(16, 16)
    base = hex_to_rgb(base_hex)
    for y in range(16):
        for x in range(16):
            canvas.set(x, y, shade(base, 1.0 + rng.uniform(-spread, spread)))
    return canvas


ORE_SPOTS = [(3, 3), (4, 3), (3, 4), (10, 2), (11, 2), (11, 3), (6, 8), (7, 8), (7, 9),
             (12, 10), (12, 11), (13, 11), (3, 12), (4, 12), (4, 13), (9, 13)]


def ore(base_hex, seed, path):
    canvas = noise_base(base_hex, seed)
    for i, (x, y) in enumerate(ORE_SPOTS):
        color = PALLADIUM["h"] if i % 3 == 0 else PALLADIUM["l"] if i % 3 == 1 else PALLADIUM["b"]
        canvas.set(x, y, color)
        # Тень под крупинкой руды.
        below = canvas.get(x, min(15, y + 1))
        if below not in (PALLADIUM["h"], PALLADIUM["l"], PALLADIUM["b"]):
            canvas.set(x, min(15, y + 1), shade(below, 0.7))
    canvas.save(path)


def palladium_block(path):
    canvas = noise_base(0x93A4B2, 7, spread=0.05)
    for i in range(16):
        canvas.set(i, 0, PALLADIUM["h"])
        canvas.set(0, i, PALLADIUM["l"])
        canvas.set(i, 15, PALLADIUM["d"])
        canvas.set(15, i, PALLADIUM["d"])
    # Крестовая насечка как у металлических блоков.
    for i in range(2, 14):
        canvas.set(i, 7, PALLADIUM["d"])
        canvas.set(i, 8, PALLADIUM["l"])
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        canvas.set(x, y, PALLADIUM["o"])
        canvas.set(x + 1, y, PALLADIUM["h"])
    canvas.save(path)


def blocks():
    out = os.path.join(TEX, "block")
    ore(0x7D7D7D, 11, os.path.join(out, "palladium_ore.png"))
    ore(0x4D4D52, 12, os.path.join(out, "deepslate_palladium_ore.png"))
    palladium_block(os.path.join(out, "palladium_block.png"))


# ---------------------------------------------------------------------------
#  Иконка мода: шлем Mark III, увеличенный в 8 раз
# ---------------------------------------------------------------------------

def icon():
    small = paint(HELMET, SUITS["mark_iii"])
    big = Canvas(128, 128)
    bg = rgb(0x101820)
    ring = rgb(0x2FA6C8)
    for y in range(128):
        for x in range(128):
            dx, dy = x - 63.5, y - 63.5
            dist = (dx * dx + dy * dy) ** 0.5
            if dist <= 62:
                big.set(x, y, ring if dist > 58 else bg)
    for y in range(16):
        for x in range(16):
            pixel = small.get(x, y)
            if pixel[3] == 0:
                continue
            for yy in range(8):
                for xx in range(8):
                    big.set(x * 7 + 8 + xx, y * 7 + 8 + yy, pixel)
    big.save(os.path.join(ROOT, "src/main/resources/assets/ironman/icon.png"))


def main():
    for sub in ("item", "block"):
        os.makedirs(os.path.join(TEX, sub), exist_ok=True)
    suit_icons()
    armor_layers()
    items()
    blocks()
    icon()
    print("текстуры готовы")


if __name__ == "__main__":
    main()
