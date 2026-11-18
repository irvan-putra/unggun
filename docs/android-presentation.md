# Android launcher presentation correction

Prepared for the approved0.1.4/code5 test release. Publication requires final checks and signed workflow success.

The launcher label is lowercase `unggun`. The bonfire artwork is unchanged. Legacy icons already used the family bottom strip, but the Android26+ adaptive foreground floated its version text without that strip. The generator now maps the same visible face into the centre72dp of Android's108dp adaptive layer. The visible strip starts at76%, the numbers sit at88%, and the font is DejaVu Sans Bold at11.5% of the visible face, matching the Tuju family generator. Themed icons have cut-out numerals so the strip stays readable. The TV banner name is lowercase too. Export folder naming is unchanged to avoid moving user data.

Regenerate using the existing pinned `tools/badge_icon.py X.Y.Z`; CI checks the resulting resources. Legacy, adaptive circle/rounded-square and themed masks were compared with the actual Tuju resources. These renders are resource previews, not a phone launcher screenshot.

An installable update needs a separately approved new version/code and same-signing-key release. The current0.1.3 APK is not changed by a source commit. No uninstall, data clearing, signing, rate-limit or background behavior change is part of this correction.

The exact DejaVu font file is vendored with its license in `tools/fonts/` and its SHA-256 is checked. This prevents runner distribution font differences from changing the badge.
