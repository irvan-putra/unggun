# unggun native (Kotlin)

A from-scratch native recode of unggun, built to docs/NATIVE-RECODE-SPEC.md
(revision 2). It lives under `native/` and shares nothing with the Flutter app in
the repo root except the package id (`com.vanipy.unggun`), the signing key and
the icon family.

Scope (v1): GitHub source only, stock Android installer only, English only,
manual update checks, app list + logs screens, add/import/settings as sheets.
No migration from the Flutter app: add repos again (bulk paste, one URL per
line). Background checking and silent installs are v1.1.

## Layout

- `app/` single Android module, Kotlin, no Compose, minSdk 26 / target 34.
- `app/src/main/java/com/vanipy/unggun/logic/` pure-JVM release parser, version
  compare and APK asset picker. Pinned by unit tests with recorded GitHub
  fixtures in `app/src/test/resources/fixtures/`.
- `net/` GitHub client (rate-limit headers honored and persisted), parallel
  checker (4 at a time), download with progress notification and cancel.
- `install/` stock PackageInstaller session. Requests USER_ACTION_NOT_REQUIRED
  only when unggun is already the installer of record (Android 12+); otherwise
  the stock confirm UI shows.
- `data/` JSON-file app store, ring-buffer log store, Keystore-encrypted GitHub
  token.
- `tools/make_icons.py` regenerates launcher/notification icons with the same
  art as the Flutter app (`tools/badge_icon.py`), version strip included.

## Build

    ./gradlew -p native :app:assembleDebug      # from the repo root
    ./gradlew -p native :app:testDebugUnitTest  # JVM tests, no emulator

Signed release: `.github/workflows/native-release.yml` (manual) signs with the
unggun key from repo secrets, verifies the certificate, package and version, and
publishes a pre-release with a sha256 sidecar.

## What v1 cannot prove

Silent updates and background survival depend on Android's installer-of-record
rules and Samsung's background limits. An emulator does not reproduce either;
they stay unverified until confirmed on the owner's phone.
