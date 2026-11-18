#!/usr/bin/env python3
"""Launcher icon generator for unggun: bonfire picture plus the shared version badge.

Same convention as the other com.vanipy.* apps: numbers-only version on a
full-width strip across the bottom, picture inside the round-mask safe circle.
Deterministic and pinned (Pillow 12.3.0, DejaVu Sans Bold).
    pip install pillow==12.3.0
    python3 tools/badge_icon.py 0.1.0
"""
import hashlib
import math
import os
import re
import sys

import PIL
from PIL import Image, ImageChops, ImageDraw, ImageFont

PILLOW_PIN = "12.3.0"
FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_SHA256 = "d1c3ff99f1e1ce1827a33efd4dad81f40babda06bff9e43bd7591c86662a287b"
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
S = 768
STRIP_TOP = 0.76      # strip covers 76%..100% of the icon height
TEXT_SIZE = 0.115     # numeral size, fraction of icon size
SAFE_R = 0.485        # picture and text must stay inside this radius (fraction)

if PIL.__version__ != PILLOW_PIN:
    sys.exit("Pillow %s required, found %s" % (PILLOW_PIN, PIL.__version__))


def font(px):
    if not os.path.exists(FONT_PATH):
        sys.exit("missing font " + FONT_PATH)
    if hashlib.sha256(open(FONT_PATH, "rb").read()).hexdigest() != FONT_SHA256:
        sys.exit("unexpected font file " + FONT_PATH)
    return ImageFont.truetype(FONT_PATH, px)


def circle_mask(size, r=SAFE_R):
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).ellipse([size * (.5 - r), size * (.5 - r), size * (.5 + r), size * (.5 + r)], fill=255)
    return m


def assert_inside_circle(layer, what, size=S, r=SAFE_R):
    alpha = layer.getchannel("A").point(lambda v: 255 if v > 8 else 0)
    outside = ImageChops.multiply(alpha, ImageChops.invert(circle_mask(size, r)))
    box = outside.getbbox()
    if box:
        sys.exit("%s leaves the round-mask safe circle at %s" % (what, box))


def new_layer(size=S):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def compose(bg, art, version, strip, text, rounded=None, size=S, circle_check=True):
    """Background, then the picture layer, then the badge strip."""
    if circle_check:
        assert_inside_circle(art, "picture", size)
    base = Image.new("RGBA", (size, size), bg) if bg else Image.new("RGBA", (size, size), (0, 0, 0, 0))
    base.alpha_composite(art)
    d = ImageDraw.Draw(base)
    y0 = int(size * STRIP_TOP)
    d.rectangle([0, y0, size, size], fill=strip)
    tl, td = new_layer(size)
    f = font(int(size * TEXT_SIZE))
    td.text((size / 2, (y0 + size) / 2), version, font=f, fill=text, anchor="mm")
    if circle_check:
        assert_inside_circle(tl, "version text", size)
    base.alpha_composite(tl)
    if rounded:
        m = Image.new("L", (size, size), 0)
        ImageDraw.Draw(m).rounded_rectangle([0, 0, size, size], radius=rounded * size, fill=255)
        base.putalpha(ImageChops.multiply(base.getchannel("A"), m))
    return base


def parse_version(argv, fallback=None):
    v = argv[1] if len(argv) > 1 else fallback
    if not v or not re.fullmatch(r"\d+\.\d+\.\d+", v):
        sys.exit("version must be X.Y.Z")
    return v


def write_densities(img, res_dir, name="ic_launcher.png"):
    for dens, px in DENSITIES.items():
        out = os.path.join(res_dir, "mipmap-" + dens)
        os.makedirs(out, exist_ok=True)
        img.resize((px, px), Image.LANCZOS).save(os.path.join(out, name), optimize=True)



def bonfire_art(d, bg):
    """Two crossed logs and a three-layer flame (unggun = bonfire)."""
    LOG = (122, 78, 46, 255)
    LOG_END = (176, 120, 72, 255)
    F1 = (232, 74, 36, 255)
    F2 = (255, 146, 40, 255)
    F3 = (255, 218, 110, 255)

    def flame(cx, base, h, w, col):
        pts = []
        steps = 40
        for i in range(steps + 1):
            t = i / steps
            y = base - h * t
            half = w * (1 - t) ** 0.8 * (1 + 0.10 * math.sin(t * 5.0))
            pts.append((cx - half + (0.06 * w) * math.sin(t * 3.0), y))
        for i in range(steps, -1, -1):
            t = i / steps
            y = base - h * t
            half = w * (1 - t) ** 0.8 * (1 + 0.10 * math.sin(t * 5.0))
            pts.append((cx + half + (0.06 * w) * math.sin(t * 3.0), y))
        d.polygon(pts, fill=col)
        r = w * 1.0
        d.ellipse([cx - r, base - r * 0.9, cx + r, base + r * 0.9], fill=col)

    cx = S * .5
    flame(cx, S * .50, S * .30, S * .135, F1)
    flame(cx - S * .075, S * .50, S * .19, S * .075, F2)
    flame(cx + S * .06, S * .50, S * .22, S * .075, F2)
    flame(cx, S * .50, S * .17, S * .06, F3)
    lw = S * .035
    for sign in (-1, 1):
        x0, y0 = cx - sign * S * .22, S * .50 + S * .11
        x1, y1 = cx + sign * S * .22, S * .50 - S * .00
        steps = 80
        for i in range(steps + 1):
            t = i / steps
            x = x0 + (x1 - x0) * t
            y = y0 + (y1 - y0) * t + S * .0
            d.ellipse([x - lw, y - lw, x + lw, y + lw], fill=LOG)
        d.ellipse([x0 - lw, y0 - lw, x0 + lw, y0 + lw], fill=LOG_END)
        d.ellipse([x1 - lw, y1 - lw, x1 + lw, y1 + lw], fill=LOG_END)


def unggun(version):
    BG = (38, 31, 44, 255)
    STRIP = (20, 16, 24, 255)
    WHITE = (255, 255, 255, 255)
    art, d = new_layer()
    bonfire_art(d, BG)
    return compose(BG, art, version, STRIP, WHITE, rounded=.2)


def write_extra(root, base):
    from PIL import Image as I
    plain = I.new("RGBA", (S, S), (0, 0, 0, 0))
    pd = ImageDraw.Draw(plain)
    bonfire_art(pd, None)
    bg = I.new("RGBA", (S, S), (38, 31, 44, 255))
    bg.alpha_composite(plain)
    m = I.new("L", (S, S), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, S, S], radius=.2 * S, fill=255)
    bg.putalpha(m)
    bg.resize((512, 512), I.LANCZOS).save(os.path.join(root, "assets", "graphics", "icon.png"), optimize=True)
    bg.resize((128, 128), I.LANCZOS).save(os.path.join(root, "assets", "graphics", "icon_small.png"), optimize=True)
    # notification glyph: white silhouette on transparent
    sil = I.new("RGBA", (S, S), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sil)
    bonfire_art(sd, None)
    a = sil.getchannel("A").point(lambda v: 255 if v > 8 else 0)
    white = I.new("RGBA", (S, S), (255, 255, 255, 255))
    white.putalpha(a)
    white.crop((int(S*.2), int(S*.12), int(S*.8), int(S*.72))).resize((96, 96), I.LANCZOS).save(
        os.path.join(root, "android", "app", "src", "main", "res", "drawable", "ic_notification.png"), optimize=True)
    # TV banner
    ban = I.new("RGBA", (320, 180), (38, 31, 44, 255))
    ic = bg.resize((120, 120), I.LANCZOS)
    ban.alpha_composite(ic, (30, 30))
    bd = ImageDraw.Draw(ban)
    bd.text((170, 90), "Unggun", font=font(34), fill=(255, 255, 255, 255), anchor="lm")
    ban.save(os.path.join(root, "android", "app", "src", "main", "res", "drawable-xhdpi", "tv_banner.png"), optimize=True)


def write_adaptive(root, version):
    """Adaptive icon (API 26+): flat background colour, bonfire + version foreground, and a
    monochrome layer for themed icons. Foreground content stays inside the 66dp safe zone."""
    res = os.path.join(root, "android", "app", "src", "main", "res")
    art, d = new_layer()
    bonfire_art(d, None)
    # crop the art to its bounding box, then fit it into the safe zone with the version below
    box = art.getbbox()
    pic = art.crop(box)
    T = 864  # 2x of 432 for quality, downsized after
    k = (T * 0.37) / max(pic.size)
    pic = pic.resize((int(pic.width * k), int(pic.height * k)), Image.LANCZOS)
    fg = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    fg.alpha_composite(pic, ((T - pic.width) // 2, int(T * 0.56) - pic.height))
    txt = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    ImageDraw.Draw(txt).text((T / 2, T * 0.65), version, font=font(int(T * 0.075)), fill=(255, 255, 255, 255), anchor="mm")
    fg.alpha_composite(txt)
    assert_inside_circle(fg, "adaptive foreground", T, r=0.305)
    mono = Image.new("RGBA", (T, T), (255, 255, 255, 255))
    mono.putalpha(fg.getchannel("A").point(lambda v: 255 if v > 8 else 0))
    for name, img in (("ic_launcher_foreground.png", fg), ("ic_launcher_monochrome.png", mono)):
        out = os.path.join(res, "drawable-xxxhdpi")
        os.makedirs(out, exist_ok=True)
        img.resize((432, 432), Image.LANCZOS).save(os.path.join(out, name), optimize=True)
    xml = os.path.join(res, "mipmap-anydpi-v26")
    os.makedirs(xml, exist_ok=True)
    body = ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '  <background android:drawable="@color/ic_launcher_background"/>\n'
            '  <foreground android:drawable="@drawable/ic_launcher_foreground"/>\n'
            '  <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n'
            '</adaptive-icon>\n')
    for n in ("ic_launcher.xml", "ic_launcher_round.xml"):
        open(os.path.join(xml, n), "w").write(body)


def main():
    version = parse_version(sys.argv)
    root = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    res = os.path.join(root, "android", "app", "src", "main", "res")
    write_densities(unggun(version), res)
    write_extra(root, version)
    write_adaptive(root, version)
    print("wrote icons for", version)


if __name__ == "__main__":
    main()
