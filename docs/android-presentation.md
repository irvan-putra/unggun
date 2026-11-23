# Android launcher presentation

Prepared for the approved0.1.5/code6 test release. Publication requires final checks and signed workflow success.

The launcher label stays lowercase `unggun` and the bonfire artwork is unchanged. The version badge strip now reads0.1.5, regenerated with the pinned `tools/badge_icon.py X.Y.Z`; CI checks the resulting resources. Legacy, adaptive and themed icons come from the same generator, so the strip position and font match the Tuju family convention.

This release changes no app behavior. It refreshes the setup guide (`docs/SETUP.md`): a new section covers what to do when a tracked GitHub project is merged, renamed or deleted, and the import section now documents adding several apps at once from a pasted URL list.

An installable update needs a separately approved new version/code and same-signing-key release. No uninstall, data clearing, signing, rate-limit or background behavior change is part of this release.

The exact DejaVu font file is vendored with its license in `tools/fonts/` and its SHA-256 is checked. This prevents runner distribution font differences from changing the badge.
