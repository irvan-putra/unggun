# unggun

Unggun (Indonesian for bonfire) tracks the GitHub projects you have installed on your Android phone, tells you when a new release is out, and installs it through the stock Android installer with as little tapping as Android allows.

Unggun is a native Android app (Kotlin) that reimplements the GitHub-source semantics of [Obtainium](https://github.com/ImranR98/Obtainium) 1.7.4. This repository is forked from upstream; the original work is by Imran Remtulla and the Obtainium contributors, and the code remains GPL-3.0. See `NOTICE.md` for what the fork changed and `docs/NATIVE-RECODE-SPEC.md` for the native recode specification.

- Package: `com.vanipy.unggun`, launcher name `unggun`
- Current app: the native recode under `native/`, released as 1.0.x pre-releases
- The original Flutter fork is still in the tree (`lib/`, `android/`) but is superseded by the native app
- Experimental. Installed outside Play, not affiliated with upstream; please do not file Unggun bugs in upstream's tracker

## What it does

- Tracks GitHub repositories you add (paste one URL per line)
- Checks for new releases on demand, with a per-app prerelease toggle
- Notifies you when an update is available; per-app "track only" mode notifies without installing
- Installs updates through the stock PackageInstaller; on Android 12+ installs skip the confirm dialog once unggun is the installer of record, including unggun updating itself
- Optional delete-after-install to keep the APK cache clean
- Respects GitHub rate limits: shows the retry time and stops early instead of hammering the API

What it does not do (yet): background/scheduled update checks. Checks run when you tap. Untested means untested: silent-update behavior depends on the device's Android build and is verified on a Samsung A24, not across devices.

## Install

Download `unggun-vX.Y.Z.apk` from the [latest release](https://github.com/irvan-putra/unggun/releases) and check it against GitHub's SHA-256 digest for the asset (shown by `gh release view` / the API), then open the APK. The release notes list the signing certificate SHA-256 to check. New versions install in place over old ones.

## Build

    git clone https://github.com/irvan-putra/unggun
    cd unggun/native
    ./gradlew assembleDebug

CI (`native-ci.yml`) compiles, runs unit tests and an emulator smoke test. The manual `native-release.yml` workflow builds the release APK, signs it with the repository's signing secrets, verifies package, version and certificate, and publishes a GitHub pre-release (checksum: GitHub's asset digest).

Launcher icon: `pip install pillow==12.3.0 && python3 native/tools/make_icons.py X.Y.Z` (single 192px full-color PNG with the version strip; no adaptive or monochrome layers).

## License

GPL-3.0, see `LICENSE.txt`. Original work copyright Imran Remtulla and Obtainium contributors.
