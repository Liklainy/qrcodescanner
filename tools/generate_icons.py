#!/usr/bin/env python3
"""Generate the QR Scanner launcher icon assets from one geometry definition.

Run from anywhere:  python3 tools/generate_icons.py   (no dependencies)

The launcher mark is described in the 108dp adaptive-icon viewport; the vector
drawables emit it directly and the store PNGs rasterise the same shapes. The
Quick Settings tile restates the mark in its own 24dp viewport, at proportions
that hold up when it is drawn one-ninth that size.
"""
import os, struct, zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app/src/main/res")

# --- geometry (108dp adaptive viewport) ------------------------------------
VP = 108.0
ORIGIN = 30.0          # mark spans 30..78 -> 48dp, inside the 66dp safe zone
MOD = 3.0              # one QR module
GRID = 16

def px(i):             # grid index -> dp
    return ORIGIN + i * MOD

# rounded rect -> list of shapes; a shape is (x, y, w, h, r)
def rr(cx, cy, w, h, r):
    return (cx, cy, w, h, r)

def finder(col, row):
    """7x7 module finder: 1-module ring, 1-module gap, 3x3 core."""
    x, y = px(col), px(row)
    outer = rr(x, y, 7 * MOD, 7 * MOD, 2.0 * MOD)
    hole = rr(x + MOD, y + MOD, 5 * MOD, 5 * MOD, 1.2 * MOD)
    core = rr(x + 2 * MOD, y + 2 * MOD, 3 * MOD, 3 * MOD, 0.8 * MOD)
    return [("ring", outer, hole), ("fill", core)]

def alignment(col, row):
    """5x5 alignment pattern: ring plus a single centre module."""
    x, y = px(col), px(row)
    outer = rr(x, y, 5 * MOD, 5 * MOD, 1.4 * MOD)
    hole = rr(x + MOD, y + MOD, 3 * MOD, 3 * MOD, 0.7 * MOD)
    core = rr(x + 2 * MOD, y + 2 * MOD, MOD, MOD, 0.35 * MOD)
    return [("ring", outer, hole), ("fill", core)]

def dot(col, row):
    return [("fill", rr(px(col), px(row), MOD, MOD, 0.35 * MOD))]

# data modules tracing the open corner, so the mark reads as a QR code
DOTS = [(15, 9), (15, 11), (15, 13), (15, 15), (9, 15), (11, 15), (13, 15)]

def mark():
    s = []
    s += finder(0, 0)          # top-left
    s += finder(9, 0)          # top-right
    s += finder(0, 9)          # bottom-left
    s += alignment(9, 9)       # bottom-right alignment pattern
    for c, r in DOTS:
        s += dot(c, r)
    return s

# --- vector drawable path data ---------------------------------------------
def n(v):
    return f"{v:g}" if abs(v - round(v)) > 1e-6 else f"{round(v):g}"

def rr_path(s):
    x, y, w, h, r = s
    return (f"M{n(x+r)},{n(y)}H{n(x+w-r)}A{n(r)},{n(r)} 0 0 1 {n(x+w)},{n(y+r)}"
            f"V{n(y+h-r)}A{n(r)},{n(r)} 0 0 1 {n(x+w-r)},{n(y+h)}"
            f"H{n(x+r)}A{n(r)},{n(r)} 0 0 1 {n(x)},{n(y+h-r)}"
            f"V{n(y+r)}A{n(r)},{n(r)} 0 0 1 {n(x+r)},{n(y)}z")

def path_data(shapes):
    out = []
    for sh in shapes:
        if sh[0] == "fill":
            out.append(rr_path(sh[1]))
        else:
            out.append(rr_path(sh[1]) + rr_path(sh[2]))
    return "".join(out)

def vector(size, body, viewport=VP):
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{n(size)}dp"\n'
            f'    android:height="{n(size)}dp"\n'
            f'    android:viewportWidth="{n(viewport)}"\n'
            f'    android:viewportHeight="{n(viewport)}">\n{body}</vector>\n')

def mark_path(color):
    return ('    <path\n'
            f'        android:fillColor="{color}"\n'
            '        android:fillType="evenOdd"\n'
            f'        android:pathData="{path_data(mark())}" />\n')

# --- colours ----------------------------------------------------------------
BG_TOP = (0x1E, 0x23, 0x2B)
BG_BOTTOM = (0x07, 0x09, 0x0C)
FG = (0xFF, 0xFF, 0xFF)

def hexc(c):
    return "#%02X%02X%02X" % c

# --- write vector drawables --------------------------------------------------
def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(text)
    print("wrote", os.path.relpath(path, ROOT))

background = ('<?xml version="1.0" encoding="utf-8"?>\n'
              '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
              '    xmlns:aapt="http://schemas.android.com/aapt"\n'
              '    android:width="108dp"\n'
              '    android:height="108dp"\n'
              '    android:viewportWidth="108"\n'
              '    android:viewportHeight="108">\n'
              '    <path android:pathData="M0,0h108v108h-108z">\n'
              '        <aapt:attr name="android:fillColor">\n'
              '            <gradient\n'
              '                android:type="linear"\n'
              '                android:startX="0" android:startY="0"\n'
              '                android:endX="108" android:endY="108"\n'
              f'                android:startColor="{hexc(BG_TOP)}"\n'
              f'                android:endColor="{hexc(BG_BOTTOM)}" />\n'
              '        </aapt:attr>\n'
              '    </path>\n'
              '</vector>\n')

write(os.path.join(RES, "drawable/ic_launcher_background.xml"), background)
write(os.path.join(RES, "drawable/ic_launcher_foreground.xml"), vector(108, mark_path(hexc(FG))))
# Themed icons: the system tints the monochrome layer, so the fill colour only
# needs to be opaque.
write(os.path.join(RES, "drawable/ic_launcher_monochrome.xml"), vector(108, mark_path("#FFFFFF")))

adaptive = ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@drawable/ic_launcher_background" />\n'
            '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
            '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
            '</adaptive-icon>\n')
write(os.path.join(RES, "mipmap-anydpi/ic_launcher.xml"), adaptive)
write(os.path.join(RES, "mipmap-anydpi/ic_launcher_round.xml"), adaptive)

# --- Quick Settings tile ------------------------------------------------------
# The same mark, re-cut at its own optical size. A tile icon is drawn at 24dp and
# tinted flat, so the launcher's 16-module grid does not survive the rescale: it
# yields 1.25dp ring strokes and a 1.25dp dot inside the alignment pattern, which
# silt up into grey mush below xxhdpi. The tile keeps the design language --
# rounded finders on three corners, a lighter mark on the fourth -- but spends its
# 20dp of live area on fewer, heavier parts:
#   * finders with a thicker ring and a tighter ring-to-core clearance
#   * the alignment pattern, too fine to survive, replaced by the four data
#     modules it would have sat among -- a different species from the finders, so
#     it counterweights them instead of competing with them
# Everything is a multiple of 0.8dp: ring stroke and module gap are both 1.6, a
# module is 3.2, and the 2x2 block spans 8.0 against the finders' 9.0, so the
# negative space reads the same everywhere in the mark.
TILE_VP, TILE_PAD = 24.0, 2.0
T_FINDER = 9.0                                    # outer box of a finder
T_STROKE = 1.6                                    # ring thickness (1.25 rescaled)
T_GAP = 1.1                                       # ring-to-core clearance
T_CORE = T_FINDER - 2 * (T_STROKE + T_GAP)        # 3.6dp core
T_FAR = TILE_VP - TILE_PAD - T_FINDER             # 13dp: far row/column origin
T_MODULE = 3.2                                    # bottom-right data module
T_MGAP = 1.6                                      # gap between them == ring stroke

def tile_finder(x, y):
    outer = rr(x, y, T_FINDER, T_FINDER, 2.6)
    inner = T_FINDER - 2 * T_STROKE
    hole = rr(x + T_STROKE, y + T_STROKE, inner, inner, 1.4)
    core = rr(x + T_STROKE + T_GAP, y + T_STROKE + T_GAP, T_CORE, T_CORE, 1.0)
    return [("ring", outer, hole), ("fill", core)]

def tile_quad():
    """2x2 block of data modules, centred on the axes of the far finders."""
    centre = T_FAR + T_FINDER / 2.0
    off = (T_MODULE + T_MGAP) / 2.0
    return [("fill", rr(centre + sx * off - T_MODULE / 2,
                        centre + sy * off - T_MODULE / 2,
                        T_MODULE, T_MODULE, 0.95))
            for sy in (-1, 1) for sx in (-1, 1)]

def tile_mark():
    return (tile_finder(TILE_PAD, TILE_PAD)
            + tile_finder(T_FAR, TILE_PAD)
            + tile_finder(TILE_PAD, T_FAR)
            + tile_quad())

tile_body = ('    <path\n'
             '        android:fillColor="@android:color/white"\n'
             '        android:fillType="evenOdd"\n'
             f'        android:pathData="{path_data(tile_mark())}" />\n')
write(os.path.join(RES, "drawable/ic_tile.xml"), vector(24, tile_body, viewport=TILE_VP))

# --- PNG rasteriser (store icons) -------------------------------------------
def cover(shape, x, y):
    """Signed coverage test: is point (x, y) inside the rounded rect?"""
    rx, ry, w, h, r = shape
    px_, py_ = x - rx, y - ry
    if px_ < 0 or py_ < 0 or px_ > w or py_ > h:
        return False
    cx = min(max(px_, r), w - r)
    cy = min(max(py_, r), h - r)
    dx, dy = px_ - cx, py_ - cy
    return dx * dx + dy * dy <= r * r

def inside_mark(shapes, x, y):
    for sh in shapes:
        if sh[0] == "fill":
            if cover(sh[1], x, y):
                return True
        else:
            if cover(sh[1], x, y) and not cover(sh[2], x, y):
                return True
    return False

def write_png(path, size, crop=72.0, ss=4):
    """Rasterise the icon into `size` px, cropping the central `crop` dp of the
    108dp viewport -- the region an adaptive-icon mask actually shows."""
    shapes = mark()
    lo = (VP - crop) / 2.0
    scale = crop / size
    rows = bytearray()
    for py_ in range(size):
        rows.append(0)  # PNG filter type 0
        for px_ in range(size):
            r = g = b = 0.0
            for sy in range(ss):
                for sx in range(ss):
                    ux = lo + (px_ + (sx + 0.5) / ss) * scale
                    uy = lo + (py_ + (sy + 0.5) / ss) * scale
                    t = (ux + uy) / (2 * VP)          # diagonal gradient
                    bg = [BG_TOP[i] + (BG_BOTTOM[i] - BG_TOP[i]) * t for i in range(3)]
                    c = FG if inside_mark(shapes, ux, uy) else bg
                    r += c[0]; g += c[1]; b += c[2]
            k = ss * ss
            rows += bytes((int(r / k + 0.5), int(g / k + 0.5), int(b / k + 0.5)))

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data +
                struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 2, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(bytes(rows), 9))
           + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)
    print("wrote", os.path.relpath(path, ROOT), f"({size}x{size}, {len(png)} bytes)")

STORE = os.path.join(ROOT, "fastlane/metadata/huawei/images")
write_png(os.path.join(STORE, "icon_512.png"), 512)   # Play / general use
write_png(os.path.join(STORE, "icon_216.png"), 216)   # AppGallery app icon
