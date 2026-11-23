#!/usr/bin/env python3
"""Launcher + notification icons for the native recode, same family look as the
Flutter unggun icon (bonfire + version strip). Reuses tools/badge_icon.py art so
both stay in lockstep. Deterministic: Pillow 12.3.0, checked-in font.

Single-variant launcher icon (owner decision 2026-11-23): one 192px
(xxxhdpi-class) full-color rounded square at res/drawable/ic_launcher.png. No
density ladder, no adaptive-icon layers, no monochrome layer - launchers
downscale it themselves and themed-icon modes have nothing to flatten.

    pip install pillow==12.3.0
    python3 native/tools/make_icons.py 1.0.2
"""
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools"))

import badge_icon
from PIL import Image, ImageDraw

RES = os.path.join(ROOT, "native", "app", "src", "main", "res")
S = badge_icon.S


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
    img = badge_icon.unggun(version)
    out = os.path.join(RES, "drawable")
    os.makedirs(out, exist_ok=True)
    img.resize((192, 192), Image.LANCZOS).save(os.path.join(out, "ic_launcher.png"), optimize=True)
    write_notification()
    print("wrote native single-variant icons for", version)


if __name__ == "__main__":
    main()
