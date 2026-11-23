# Setting up Unggun

Status of this guide: written from the source code and the upstream Obtainium wiki. The labels below come from `assets/translations/en.json`. The Import screen, imports of a whole list, and automatic updates have **not** been exercised by the maintainer, so treat those steps as untested and report differences.

## 1. Install

1. Download `unggun-vX.Y.Z.apk` and `unggun-vX.Y.Z.apk.sha256` from the [latest release](https://github.com/irvan-putra/unggun/releases).
2. Compare the SHA-256 of the APK with the `.sha256` file.
3. Open the APK. Android asks you to allow installs from the app you opened it with. Play Protect may warn about an unknown developer.
4. Unggun has its own package ID (`com.vanipy.unggun`), so it installs next to Obtainium. It does not read, replace or update Obtainium or its apps.

## 2. First run

Two notices appear in a row (a welcome notice and a note about Google's developer-verification plans). Tap Okay on each. Allow notifications if you want update alerts.

## 3. Add or import apps

- **One app:** Add app, then paste the release page URL.
- **Many apps:** Settings, Import/export, "Unggun import", and pick an Unggun or Obtainium export file. The file can be a bare JSON list of apps or an object with an `apps` list. See `test/fixtures/import-sample.json` for the shape.
- **Many apps without a file:** Settings, Import/export, "Import from URL list", and paste one release URL per line. Apps added this way use the default settings, so turn on "Include prereleases" per app for projects that only publish pre-releases.
- Importing overwrites entries with the same app ID inside Unggun and applies the settings in the file. Imported apps may show as "not installed" until they are installed or updated through Unggun (the app says this itself).

## 3a. When a tracked repo moves

If a tracked GitHub project is merged into another repository or deleted, its entry stops refreshing and shows an error. Unggun cannot follow that move by itself. Remove the entry, then add the app again with the current repository URL. The already-installed app is matched by its package ID, so nothing needs to be reinstalled. If you keep an old export file for re-setup, update the URLs in it first, or the same error comes back after the import.

## 4. Updates

- Notifications: "Check for updates on startup" and "Enable background updates" are in Settings. Whether background checks actually run depends on Android battery limits.
- Per-app setting "Exempt from background updates (if enabled)" keeps an app out of background updates. Some import files set it on every app, so turn it off per app if you want those apps updated in the background.
- Silent installs depend on the Android version, which app installed the target, and the installation method. Do not assume they work; check one app first.
- "Include prereleases" is a per-app setting. Unggun's own entry tracks this repository, and its releases are marked pre-release, so enable "Include prereleases" on the Unggun entry to see them.

## 5. Links

`obtainium://` links are shared with Obtainium. With both installed, Android asks which app opens them.

## 6. Export

Exports go to the folder you pick, or `Downloads/Unggun` when using the Downloads option. Exports can include secrets depending on the "include settings" choice, so keep them private.
