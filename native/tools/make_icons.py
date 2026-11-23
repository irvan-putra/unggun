#!/usr/bin/env python3
"""Launcher + notification icons for the native recode, same family look as the
Flutter unggun icon (bonfire + version strip). Reuses tools/badge_icon.py art so
both stay in lockstep. Deterministic: Pillow 12.3.0, checked-in font.

    pip install pillow==12.3.0
    python3 native/tools/make_icons.py 1.0.0
"""
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools"))

import badge_icon
from PIL import Image, ImageChops, ImageDraw

RES = os.path.join(ROOT, "native", "app", "src", "main", "res")
BG = (38, 31, 44, 255)
STRIP = (20, 16, 24, 255)
S = badge_icon.S


def write_adaptive(version):
    """Same face layout as tools/badge_icon.py write_adaptive, native res dir."""
    T = 864
    face = 576
    inset = (T - face) // 2
    fg = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    art, d = badge_icon.new_layer()
    badge_icon.bonfire_art(d, None)
    picture = art.resize((face, face), Image.LANCZOS)
    fg.alpha_composite(picture, (inset, inset))
    strip_y = inset + int(face * badge_icon.STRIP_TOP)
    ImageDraw.Draw(fg).rectangle([0, strip_y, T, T], fill=STRIP)
    text = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    ImageDraw.Draw(text).text(
        (T / 2, inset + face * .88), version,
        font=badge_icon.font(int(face * badge_icon.TEXT_SIZE)), fill="white", anchor="mm")
    badge_icon.assert_inside_circle(text, "adaptive version", T, r=.305)
    badge_icon.assert_inside_circle(picture, "adaptive picture face", face)
    fg.alpha_composite(text)
    mono_alpha = Image.new("L", (T, T), 0)
    mono_alpha.paste(picture.getchannel("A"), (inset, inset))
    ImageDraw.Draw(mono_alpha).rectangle([0, strip_y, T, T], fill=255)
    mono_alpha = ImageChops.subtract(mono_alpha, text.getchannel("A"))
    mono = Image.new("RGBA", (T, T), "white")
    mono.putalpha(mono_alpha)
    out = os.path.join(RES, "drawable-xxxhdpi")
    os.makedirs(out, exist_ok=True)
    for name, img in (("ic_launcher_foreground.png", fg), ("ic_launcher_monochrome.png", mono)):
        img.resize((432, 432), Image.LANCZOS).save(os.path.join(out, name), optimize=True)


def write_notification():
    """White bonfire silhouette for the status bar."""
    sil = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sil)
    badge_icon.bonfire_art(sd, None)
    a = sil.getchannel("A").point(lambda v: 255 if v > 8 else 0)
    white = Image.new("RGBA", (S, S), (255, 255, 255, 255))
    white.putalpha(a)
    out = os.path.join(RES, "drawable")
    os.makedirs(out, exist_ok=True)
    white.crop((int(S*.2), int(S*.12), int(S*.8), int(S*.72))).resize((96, 96), Image.LANCZOS).save(
        os.path.join(out, "ic_notification.png"), optimize=True)


def main():
    version = badge_icon.parse_version(sys.argv)
    badge_icon.write_densities(badge_icon.unggun(version), RES)
    write_adaptive(version)
    write_notification()
    print("wrote native icons for", version)


if __name__ == "__main__":
    main()
