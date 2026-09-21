"""Примитивы для пиксель-арта: всё рисуется кодом, без внешних редакторов."""

import math
import random

from png import Canvas, palette, shade, hex_to_rgb


def diamond(canvas, cx, cy, rx, ry, pal, facet=True):
    """Гранёный кристалл: контур, тело, блик по левой грани."""
    for y in range(canvas.height):
        for x in range(canvas.width):
            dx = (x - cx) / rx
            dy = (y - cy) / ry
            d = abs(dx) + abs(dy)
            if d > 1.05:
                continue
            if d > 0.82:
                canvas.set(x, y, pal["o"])
            elif facet and x - cx < -(y - cy) * 0.2 - 1:
                canvas.set(x, y, pal["4"])
            elif facet and abs((x - cx) + (y - cy)) < 1.2:
                canvas.set(x, y, pal["h"])
            else:
                canvas.set(x, y, pal["2"] if (x + y) % 5 == 0 else pal["3"])


def disc(canvas, cx, cy, radius, color, outline=None):
    for y in range(canvas.height):
        for x in range(canvas.width):
            d = math.hypot(x - cx, y - cy)
            if d <= radius - 0.9:
                canvas.set(x, y, color)
            elif d <= radius + 0.2 and outline is not None:
                canvas.set(x, y, outline)


def ring(canvas, cx, cy, radius, thickness, color):
    for y in range(canvas.height):
        for x in range(canvas.width):
            d = math.hypot(x - cx, y - cy)
            if radius - thickness <= d <= radius:
                canvas.set(x, y, color)


def line(canvas, x0, y0, x1, y1, color, width=1):
    """Отрезок по Брезенхэму с заданной толщиной."""
    steps = max(abs(x1 - x0), abs(y1 - y0), 1)
    for i in range(steps + 1):
        t = i / steps
        x = round(x0 + (x1 - x0) * t)
        y = round(y0 + (y1 - y0) * t)
        for ox in range(width):
            for oy in range(width):
                canvas.set(x + ox, y + oy, color)


def frame(canvas, x0, y0, x1, y1, fill, border):
    canvas.rect(x0, y0, x1, y1, fill)
    for x in range(x0, x1 + 1):
        canvas.set(x, y0, border)
        canvas.set(x, y1, border)
    for y in range(y0, y1 + 1):
        canvas.set(x0, y, border)
        canvas.set(x1, y, border)


def noise_fill(canvas, x0, y0, x1, y1, base_hex, seed, spread=0.18, density=0.55):
    """Каменная «крошка»: стабильный шум по фиксированному зерну."""
    rng = random.Random(seed)
    base = hex_to_rgb(base_hex)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if rng.random() < density:
                factor = 1.0 + rng.uniform(-spread, spread)
            else:
                factor = 1.0
            canvas.set(x, y, shade(base, factor))


def speckle(canvas, points, pal, seed):
    rng = random.Random(seed)
    for (x, y) in points:
        canvas.set(x, y, pal["3"])
        canvas.set(x + 1, y, pal["2"])
        canvas.set(x, y + 1, pal["4"] if rng.random() < 0.5 else pal["2"])
        canvas.set(x + 1, y + 1, pal["o"])


# Глифы школ: 7x7, «1» — линия руны.
GLYPHS = {
    "fire": [
        "...1...",
        "..111..",
        ".11.11.",
        "11...11",
        ".1...1.",
        "..1.1..",
        "...1...",
    ],
    "frost": [
        "1..1..1",
        ".1.1.1.",
        "..111..",
        "1111111",
        "..111..",
        ".1.1.1.",
        "1..1..1",
    ],
    "storm": [
        "....11.",
        "...11..",
        "..111..",
        ".11111.",
        "..111..",
        ".11....",
        "11.....",
    ],
    "arcane": [
        "...1...",
        ".11111.",
        "1.1.1.1",
        "1111111",
        "1.1.1.1",
        ".11111.",
        "...1...",
    ],
    "nature": [
        "...1...",
        "..111..",
        ".1.1.1.",
        "1..1..1",
        ".1.1.1.",
        "...1...",
        "...1...",
    ],
    "shadow": [
        ".11111.",
        "1.....1",
        "1.111.1",
        "1.1.1.1",
        "1.111.1",
        "1.....1",
        ".11111.",
    ],
    "light": [
        "...1...",
        "1..1..1",
        ".1.1.1.",
        "1111111",
        ".1.1.1.",
        "1..1..1",
        "...1...",
    ],
}


def glyph(canvas, name, x, y, color):
    for row, art in enumerate(GLYPHS[name]):
        for col, char in enumerate(art):
            if char == "1":
                canvas.set(x + col, y + row, color)
