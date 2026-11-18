# Unggun

Unggun (Indonesian for bonfire) installs and updates Android apps straight from their release pages, such as GitHub releases, and notifies you when a new release is out.

It is a fork of [Obtainium](https://github.com/ImranR98/Obtainium) by Imran Remtulla and contributors, based on upstream version 1.7.4 (commit `35c3e7fea4f341a716eca4bb3fc625319ce965ab`). The code is licensed GPL-3.0, the same as upstream. See `NOTICE.md` for what changed.

- Package: `com.vanipy.unggun`, launcher name `Unggun`
- Current version: see `pubspec.yaml` (X.Y.Z, shown as a badge on the launcher icon)
- Experimental. Unsigned-by-Google, installed outside Play. Not affiliated with upstream; upstream issues are not the place for Unggun bugs.
- Upstream documentation applies to most features: https://wiki.obtainium.imranr.dev/

## Install

Download `unggun-vX.Y.Z.apk` and its `.sha256` from the latest GitHub release, compare the SHA-256, then open the APK. The release notes list the signing certificate SHA-256 to check.

Setup steps: [docs/SETUP.md](docs/SETUP.md).

## Differences from upstream

- Different package ID, so it installs next to Obtainium and does not update it.
- Own app name, bonfire icon and version badge.
- Self-updates track this repository.
- Same `obtainium://` link scheme as upstream, so tapping such a link on a phone with both apps asks which app to open.

## Build

    git clone https://github.com/irvan-putra/unggun
    cd unggun
    flutter pub get
    flutter build apk --flavor normal --debug

CI (`.github/workflows/ci.yml`) runs analysis and tests and builds a debug APK. The manual `release.yml` workflow builds the release APK, signs it with the repository's signing secrets (`UNGGUN_KEYSTORE_B64`, `UNGGUN_STORE_PASSWORD`, `UNGGUN_KEY_ALIAS`, `UNGGUN_KEY_PASSWORD`), verifies package and certificate, and publishes a GitHub pre-release.

Icons: `pip install pillow==12.3.0 && python3 tools/badge_icon.py X.Y.Z`.

## License

GPL-3.0, see `LICENSE.txt`. Original work copyright Imran Remtulla and Obtainium contributors.
