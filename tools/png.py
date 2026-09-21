"""Минимальный кодировщик PNG без сторонних зависимостей.

Pillow в окружении нет, а для 16x16 спрайтов он и не нужен: PNG — это
всего лишь несколько chunk-ов и zlib-поток со строками пикселей.
"""

import struct
import zlib


def write_png(path, width, height, pixels):
    """pixels — список строк, каждая строка — список кортежей (r, g, b, a)."""
    raw = bytearray()
    for row in pixels:
        raw.append(0)  # тип фильтра «None» для строки
        for (r, g, b, a) in row:
            raw += bytes((r, g, b, a))

    def chunk(tag, data):
        out = struct.pack(">I", len(data)) + tag + data
        return out + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", header)
           + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
           + chunk(b"IEND", b""))
    with open(path, "wb") as handle:
        handle.write(png)


class Canvas:
    """Простой холст RGBA с координатами (x, y) от левого верхнего угла."""

    def __init__(self, width, height):
        self.width = width
        self.height = height
        self.pixels = [[(0, 0, 0, 0)] * width for _ in range(height)]

    def set(self, x, y, color):
        if 0 <= x < self.width and 0 <= y < self.height and color is not None:
            self.pixels[y][x] = color

    def get(self, x, y):
        return self.pixels[y][x]

    def rect(self, x0, y0, x1, y1, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, color)

    def save(self, path):
        write_png(path, self.width, self.height, self.pixels)


def hex_to_rgb(value):
    return ((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF)


def shade(color, factor):
    """factor < 1 затемняет, > 1 осветляет (с плавным подходом к белому)."""
    r, g, b = color[:3]
    if factor <= 1.0:
        return (int(r * factor), int(g * factor), int(b * factor), 255)
    t = min(1.0, factor - 1.0)
    return (int(r + (255 - r) * t), int(g + (255 - g) * t), int(b + (255 - b) * t), 255)


def palette(base_hex):
    """Пять оттенков одного цвета: контур, тень, база, свет, блик."""
    base = hex_to_rgb(base_hex)
    return {
        "o": shade(base, 0.30),
        "1": shade(base, 0.55),
        "2": shade(base, 0.80),
        "3": (*base, 255),
        "4": shade(base, 1.25),
        "h": shade(base, 1.60),
        ".": None,
    }


def draw_art(canvas, art, colors, offset_x=0, offset_y=0):
    """Рисует ASCII-картинку: символ -> цвет из словаря colors."""
    for y, line in enumerate(art):
        for x, char in enumerate(line):
            if char == "." or char == " ":
                continue
            canvas.set(x + offset_x, y + offset_y, colors.get(char))
