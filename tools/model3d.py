"""Конструктор объёмных моделей предметов.

Модель собирается из параллелепипедов, а цвет каждой грани берётся из общего
атласа-палитры `textures/item/palette.png`: одна маленькая текстура на все
модели. Так грани не растягиваются, UV считается тривиально, а перекрасить
любую деталь можно, поменяв один цвет в словаре MATERIALS.

Где плоская картинка выразительнее объёма (руны с глифами), грань может
ссылаться на обычный спрайт — для этого у `box` есть `overrides`.
"""

# --- палитра ----------------------------------------------------------

MATERIALS = {
    "wood_dark": 0x5A3F22,
    "wood": 0x8A6136,
    "wood_light": 0xA87A46,
    "gold": 0xE8C24A,
    "gold_dark": 0xB8912E,
    "iron": 0xC0C4CC,
    "iron_dark": 0x8A8E96,
    "stone": 0x8C8C96,
    "stone_dark": 0x5C5C66,
    "obsidian": 0x241B33,
    "robe": 0x3B2A5C,
    "robe_light": 0x54407E,
    "trim": 0xD8B44A,
    "parchment": 0xE4D4A8,
    "parchment_dark": 0xC2B083,
    "leather": 0x7A4B2A,
    "glass": 0xBFD8E8,
    "cork": 0x6B4A2A,
    "bone": 0xE8E2D0,
    "soul": 0x9FE3FF,
    "soul_dark": 0x4E8FA8,
    "crystal": 0xC77DFF,
    "crystal_light": 0xE0B0FF,
    "crystal_dark": 0x8A4FC4,
    "potion": 0x4FA8FF,
    "potion_great": 0x7FD8FF,
    "moon": 0xBFD9FF,
    "moon_light": 0xEAF2FF,
    "ember": 0xFF8A3D,
    "void": 0x1A1226,
    "dim": 0x6A6A80,
    "charged": 0x5BC8FF,
    "amber": 0xFFD24A,
    "eldritch": 0x9B30FF,
    "sigil": 0xFF6FD8,
}

SCHOOL_COLORS = {
    "fire": 0xFF7A2A,
    "frost": 0x7FD8FF,
    "storm": 0xE8E45C,
    "arcane": 0xC77DFF,
    "nature": 0x74D14C,
    "shadow": 0x6E4A8E,
    "light": 0xFFF3B0,
}

for _school, _color in SCHOOL_COLORS.items():
    MATERIALS["school_" + _school] = _color
    # Светлый вариант получается смешиванием с белым — отдельно его не задаём.
    MATERIALS["school_" + _school + "_light"] = (
        (min(255, ((_color >> 16) & 0xFF) + 60) << 16)
        | (min(255, ((_color >> 8) & 0xFF) + 60) << 8)
        | min(255, (_color & 0xFF) + 60))

# Раскладка атласа: сетка 8x8 из ячеек 4x4 пикселя в текстуре 32x32.
ATLAS_SIZE = 32
CELL_PIXELS = 4
GRID = ATLAS_SIZE // CELL_PIXELS

CELLS = {}
for _index, _name in enumerate(MATERIALS):
    if _index >= GRID * GRID:
        raise ValueError("палитра не помещается в атлас")
    CELLS[_name] = (_index % GRID, _index // GRID)


def uv_of(material):
    """UV ячейки палитры.

    Координаты UV всегда в диапазоне 0..16 независимо от размера текстуры,
    поэтому ячейка 4x4 пикселя занимает 2 единицы. Отступ в полединицы
    (= один пиксель) не даёт мип-мапам смешивать соседние цвета.
    """
    col, row = CELLS[material]
    return [col * 2 + 0.5, row * 2 + 0.5, col * 2 + 1.5, row * 2 + 1.5]


# --- элементы ---------------------------------------------------------

FACES = ("north", "south", "east", "west", "up", "down")


def _round(value):
    return round(value + 0.0, 3)


def box(x0, y0, z0, x1, y1, z1, material, rotation=None, overrides=None, shade=True):
    """Один параллелепипед. `overrides` — {грань: (текстура, uv)}."""
    for value in (x0, y0, z0, x1, y1, z1):
        if not -16 <= value <= 32:
            raise ValueError(f"координата {value} вне допустимого диапазона -16..32")

    uv = uv_of(material)
    faces = {face: {"uv": list(uv), "texture": "#palette"} for face in FACES}
    if overrides:
        for face, (texture, face_uv) in overrides.items():
            faces[face] = {"uv": list(face_uv), "texture": texture}

    element = {
        "from": [_round(x0), _round(y0), _round(z0)],
        "to": [_round(x1), _round(y1), _round(z1)],
        "faces": faces,
    }
    if rotation is not None:
        element["rotation"] = rotation
    if not shade:
        element["shade"] = False
    return element


def spin(origin, axis, angle, rescale=False):
    """Поворот элемента. Игра принимает только -45, -22.5, 0, 22.5 и 45 градусов."""
    if angle not in (-45, -22.5, 0, 22.5, 45):
        raise ValueError(f"недопустимый угол поворота: {angle}")
    rotation = {"origin": [_round(v) for v in origin], "axis": axis, "angle": angle}
    if rescale:
        rotation["rescale"] = True
    return rotation


# --- положение предмета в руке и в интерфейсе -------------------------

def _transform(rotation, translation, scale):
    return {"rotation": list(rotation), "translation": list(translation), "scale": list(scale)}


#: Длинные предметы: держатся как оружие, в интерфейсе — по диагонали.
DISPLAY_STAFF = {
    "thirdperson_righthand": _transform([0, -90, 55], [0, 4, 0.5], [0.85, 0.85, 0.85]),
    "thirdperson_lefthand": _transform([0, 90, -55], [0, 4, 0.5], [0.85, 0.85, 0.85]),
    "firstperson_righthand": _transform([0, -90, 25], [1.13, 3.2, 1.13], [0.68, 0.68, 0.68]),
    "firstperson_lefthand": _transform([0, 90, -25], [1.13, 3.2, 1.13], [0.68, 0.68, 0.68]),
    "gui": _transform([15, -40, 10], [0, 0, 0], [0.8, 0.8, 0.8]),
    "ground": _transform([0, 0, 0], [0, 2, 0], [0.5, 0.5, 0.5]),
    "head": _transform([0, 180, 0], [0, 13, 7], [1, 1, 1]),
    "fixed": _transform([0, 180, 0], [0, 0, 0], [1, 1, 1]),
}

#: Мелкие объёмные предметы: кристаллы, фокусы, осколки.
DISPLAY_SMALL = {
    "thirdperson_righthand": _transform([0, 0, 0], [0, 3, 1], [0.55, 0.55, 0.55]),
    "thirdperson_lefthand": _transform([0, 0, 0], [0, 3, 1], [0.55, 0.55, 0.55]),
    "firstperson_righthand": _transform([0, -90, 25], [1.13, 3.2, 1.13], [0.68, 0.68, 0.68]),
    "firstperson_lefthand": _transform([0, 90, -25], [1.13, 3.2, 1.13], [0.68, 0.68, 0.68]),
    "gui": _transform([30, 45, 0], [0, 0, 0], [0.75, 0.75, 0.75]),
    "ground": _transform([0, 0, 0], [0, 2, 0], [0.5, 0.5, 0.5]),
    "head": _transform([0, 180, 0], [0, 13, 7], [1, 1, 1]),
    "fixed": _transform([0, 180, 0], [0, 0, 0], [1, 1, 1]),
}

#: Плоские предметы с рисунком на лицевой грани: руны, свитки, книга.
DISPLAY_FLAT = {
    "thirdperson_righthand": _transform([0, 0, 0], [0, 3, 1], [0.55, 0.55, 0.55]),
    "thirdperson_lefthand": _transform([0, 0, 0], [0, 3, 1], [0.55, 0.55, 0.55]),
    "firstperson_righthand": _transform([0, -90, 25], [1.13, 3.2, 1.13], [0.68, 0.68, 0.68]),
    "firstperson_lefthand": _transform([0, 90, -25], [1.13, 3.2, 1.13], [0.68, 0.68, 0.68]),
    "gui": _transform([12, -22, 0], [0, 0, 0], [0.92, 0.92, 0.92]),
    "ground": _transform([0, 0, 0], [0, 2, 0], [0.5, 0.5, 0.5]),
    "head": _transform([0, 180, 0], [0, 13, 7], [1, 1, 1]),
    "fixed": _transform([0, 180, 0], [0, 0, 0], [1, 1, 1]),
}


def model(elements, display, extra_textures=None):
    """Готовая модель предмета: палитра + элементы + положение."""
    textures = {"palette": "arcanum:item/palette", "particle": "arcanum:item/palette"}
    if extra_textures:
        textures.update(extra_textures)
    return {
        "textures": textures,
        # "side" даёт объёмную подсветку граней в интерфейсе;
        # у плоских предметов ваниль ставит "front", нам это не нужно.
        "gui_light": "side",
        "elements": elements,
        "display": display,
    }
