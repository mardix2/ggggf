#!/usr/bin/env python3
"""Схематичный предпросмотр объёмных моделей.

Это не рендерер Minecraft, а грубая проверка геометрии: ортографическая
проекция коробок с тем же поворотом, что задан в `display.gui`, и такой же
подсветкой граней, как в игре (верх ярче, бока темнее). Позволяет увидеть
силуэт и поймать вывернутые или уехавшие детали, не запуская игру.

Запуск:  python3 tools/preview_models.py [папка-назначения]
"""

import json
import math
import os
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import model3d as m
from content import ITEMS_3D
from png import Canvas, hex_to_rgb, shade

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/arcanum")

TILE = 56
COLUMNS = 6

#: Так Minecraft затемняет грани в зависимости от их направления.
FACE_SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}

#: Углы каждой грани в координатах модели (0..16).
FACE_CORNERS = {
    "north": lambda a, b: [(a[0], a[1], a[2]), (b[0], a[1], a[2]), (b[0], b[1], a[2]), (a[0], b[1], a[2])],
    "south": lambda a, b: [(a[0], a[1], b[2]), (b[0], a[1], b[2]), (b[0], b[1], b[2]), (a[0], b[1], b[2])],
    "west": lambda a, b: [(a[0], a[1], a[2]), (a[0], a[1], b[2]), (a[0], b[1], b[2]), (a[0], b[1], a[2])],
    "east": lambda a, b: [(b[0], a[1], a[2]), (b[0], a[1], b[2]), (b[0], b[1], b[2]), (b[0], b[1], a[2])],
    "down": lambda a, b: [(a[0], a[1], a[2]), (b[0], a[1], a[2]), (b[0], a[1], b[2]), (a[0], a[1], b[2])],
    "up": lambda a, b: [(a[0], b[1], a[2]), (b[0], b[1], a[2]), (b[0], b[1], b[2]), (a[0], b[1], b[2])],
}


# --- чтение собственных PNG -------------------------------------------

def read_png(path):
    """Читает PNG, записанный нашим же кодировщиком: RGBA, фильтр 0."""
    data = open(path, "rb").read()
    width, height = struct.unpack(">II", data[16:24])
    idat = b""
    offset = 8
    while offset < len(data):
        length = struct.unpack(">I", data[offset:offset + 4])[0]
        tag = data[offset + 4:offset + 8]
        if tag == b"IDAT":
            idat += data[offset + 8:offset + 8 + length]
        offset += 12 + length
    raw = zlib.decompress(idat)
    stride = width * 4 + 1
    rows = []
    for y in range(height):
        line = raw[y * stride + 1:(y + 1) * stride]
        rows.append([tuple(line[x * 4:x * 4 + 4]) for x in range(width)])
    return width, height, rows


def average_color(path, uv):
    """Средний непрозрачный цвет прямоугольника uv (координаты 0..16)."""
    width, height, rows = read_png(path)
    x0, y0, x1, y1 = uv
    if x0 > x1:
        x0, x1 = x1, x0
    px0, px1 = int(min(x0, x1) / 16 * width), max(1, int(max(x0, x1) / 16 * width))
    py0, py1 = int(min(y0, y1) / 16 * height), max(1, int(max(y0, y1) / 16 * height))
    total = [0, 0, 0]
    count = 0
    for y in range(py0, min(py1, height)):
        for x in range(px0, min(px1, width)):
            pixel = rows[y][x]
            if pixel[3] > 0:
                total[0] += pixel[0]
                total[1] += pixel[1]
                total[2] += pixel[2]
                count += 1
    if count == 0:
        return (160, 160, 160)
    return (total[0] // count, total[1] // count, total[2] // count)


# --- геометрия --------------------------------------------------------

def rotate(point, degrees, axis):
    angle = math.radians(degrees)
    cos, sin = math.cos(angle), math.sin(angle)
    x, y, z = point
    if axis == "x":
        return (x, y * cos - z * sin, y * sin + z * cos)
    if axis == "y":
        return (x * cos + z * sin, y, -x * sin + z * cos)
    return (x * cos - y * sin, x * sin + y * cos, z)


def material_of(face, textures, model_dir):
    """Цвет грани: либо ячейка палитры, либо средний цвет спрайта."""
    texture = face["texture"].lstrip("#")
    uv = face["uv"]
    if texture == "palette":
        col = int(round((uv[0] - 0.5) / 2))
        row = int(round((uv[1] - 0.5) / 2))
        for name, cell in m.CELLS.items():
            if cell == (col, row):
                return hex_to_rgb(m.MATERIALS[name])
        return (200, 0, 200)

    reference = textures.get(texture)
    if reference is None:
        return (200, 0, 200)
    path = os.path.join(ASSETS, "textures", reference.split(":", 1)[1] + ".png")
    if not os.path.exists(path):
        return (200, 0, 200)
    return average_color(path, uv)


def project(model, size):
    """Собирает грани модели в плоские многоугольники экрана."""
    display = model.get("display", {}).get("gui", {})
    rx, ry, rz = display.get("rotation", [0, 0, 0])
    scale = display.get("scale", [1, 1, 1])[0]
    textures = model.get("textures", {})

    polygons = []
    for element in model["elements"]:
        start, end = element["from"], element["to"]
        spin = element.get("rotation")
        for side, corners_of in FACE_CORNERS.items():
            face = element["faces"].get(side)
            if face is None:
                continue
            color = material_of(face, textures, ASSETS)
            shaded = shade(color, FACE_SHADE[side])

            screen = []
            depth = 0.0
            for corner in corners_of(start, end):
                point = corner
                if spin is not None:
                    origin = spin["origin"]
                    shifted = tuple(point[i] - origin[i] for i in range(3))
                    shifted = rotate(shifted, spin["angle"], spin["axis"])
                    point = tuple(shifted[i] + origin[i] for i in range(3))

                # Координаты модели -> центрированные, затем поворот витрины.
                centered = tuple((point[i] - 8.0) / 16.0 for i in range(3))
                centered = rotate(centered, rz, "z")
                centered = rotate(centered, ry, "y")
                centered = rotate(centered, rx, "x")
                centered = tuple(v * scale for v in centered)

                screen.append(((centered[0] + 0.5) * size, (0.5 - centered[1]) * size))
                depth += centered[2]
            polygons.append((depth / 4.0, screen, shaded))

    polygons.sort(key=lambda item: item[0])
    return polygons


def inside(polygon, x, y):
    """Точка внутри выпуклого четырёхугольника (знак векторного произведения)."""
    sign = 0
    for index in range(len(polygon)):
        ax, ay = polygon[index]
        bx, by = polygon[(index + 1) % len(polygon)]
        cross = (bx - ax) * (y - ay) - (by - ay) * (x - ax)
        if abs(cross) < 1e-9:
            continue
        current = 1 if cross > 0 else -1
        if sign == 0:
            sign = current
        elif sign != current:
            return False
    return True


def render(model, size):
    canvas = Canvas(size, size)
    for _depth, polygon, color in project(model, size):
        min_x = max(0, int(min(p[0] for p in polygon)))
        max_x = min(size - 1, int(max(p[0] for p in polygon)) + 1)
        min_y = max(0, int(min(p[1] for p in polygon)))
        max_y = min(size - 1, int(max(p[1] for p in polygon)) + 1)
        for y in range(min_y, max_y + 1):
            for x in range(min_x, max_x + 1):
                if inside(polygon, x + 0.5, y + 0.5):
                    canvas.set(x, y, color)
    return canvas


def main():
    out_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "docs")
    os.makedirs(out_dir, exist_ok=True)

    names = sorted(ITEMS_3D)
    rows = (len(names) + COLUMNS - 1) // COLUMNS
    sheet = Canvas(COLUMNS * TILE, rows * TILE)
    sheet.rect(0, 0, sheet.width - 1, sheet.height - 1, (26, 18, 38, 255))

    for index, name in enumerate(names):
        path = os.path.join(ASSETS, "models", "item", name + ".json")
        model = json.loads(open(path, encoding="utf-8").read())
        tile = render(model, TILE)
        sheet.blit_over(tile, (index % COLUMNS) * TILE, (index // COLUMNS) * TILE)

    sheet.save(os.path.join(out_dir, "items_preview.png"))
    print(f"Предпросмотр: {len(names)} моделей -> {os.path.join(out_dir, 'items_preview.png')}")


if __name__ == "__main__":
    main()
