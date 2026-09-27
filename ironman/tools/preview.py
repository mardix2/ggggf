#!/usr/bin/env python3
"""Картинка-превью для README: все иконки предметов и три костюма спереди.

Запуск:  python3 tools/preview.py docs/preview.png
"""
import os, sys, zlib, struct
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
from png import Canvas
def read_png(path):
    data = open(path,'rb').read()
    pos = 8; idat=b''; w=h=0
    while pos < len(data):
        ln = struct.unpack('>I', data[pos:pos+4])[0]; tag=data[pos+4:pos+8]; body=data[pos+8:pos+8+ln]; pos += 12+ln
        if tag==b'IHDR': w,h = struct.unpack('>II', body[:8])
        elif tag==b'IDAT': idat += body
    raw = zlib.decompress(idat); c = Canvas(w,h); stride = w*4+1
    for y in range(h):
        row = raw[y*stride+1:(y+1)*stride]
        for x in range(w): c.set(x,y,tuple(row[x*4:x*4+4]))
    return c
T='src/main/resources/assets/ironman/textures/'
items = sorted(os.listdir(T+'item'))
S=6
sheet = Canvas(16*S*8+8*4, 16*S*((len(items)+7)//8) + 4*((len(items)+7)//8) + 230)
bg=(60,64,70,255)
for y in range(sheet.height):
    for x in range(sheet.width): sheet.set(x,y,bg)
def blit(src, sx, sy, w, h, dx, dy, scale, over=True):
    for y in range(h):
        for x in range(w):
            p = src.get(sx+x, sy+y)
            if p[3]==0: continue
            for yy in range(scale):
                for xx in range(scale): sheet.set(dx+x*scale+xx, dy+y*scale+yy, p)
for i,name in enumerate(items):
    c = read_png(T+'item/'+name)
    blit(c, 0, 0, 16, 16, (i % 8) * (16 * S + 4), (i // 8) * (16 * S + 4), S)
base_y = ((len(items)+7)//8)*(16*S+4)
# front views
F=5
for k,suit in enumerate(['mark_i','mark_iii','mark_85']):
    top = read_png(T+'entity/equipment/humanoid/'+suit+'.png')
    low = read_png(T+'entity/equipment/humanoid_leggings/'+suit+'.png')
    ox = k*(18*F+30)+10; oy = base_y+10
    blit(top, 8,8,8,8, ox+4*F, oy, F)            # head front
    blit(top, 20,20,8,12, ox+4*F, oy+8*F, F)     # body front
    blit(low, 20,20,8,12, ox+4*F, oy+8*F, F)     # belt
    blit(top, 44,20,4,12, ox, oy+8*F, F)         # right arm
    blit(top, 44,20,4,12, ox+12*F, oy+8*F, F)    # left arm
    blit(low, 4,20,4,12, ox+4*F, oy+20*F, F)     # legs
    blit(low, 4,20,4,12, ox+8*F, oy+20*F, F)
    blit(top, 4,20,4,12, ox+4*F, oy+20*F, F)     # boots
    blit(top, 4,20,4,12, ox+8*F, oy+20*F, F)
    # raw layer for reference
    blit(top,0,0,64,32, 600, oy + k*70, 2)
    blit(low,0,0,64,32, 740, oy + k*70, 2)
sheet.save(sys.argv[1])
