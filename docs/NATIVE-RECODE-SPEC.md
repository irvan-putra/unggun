# Recoding Unggun natively: specification (draft for decision)

Status: research document, 2026-11-23. This is a spec for a from-scratch native (Kotlin) recode of Unggun without Flutter. Nothing here is built. The owner reads this, then decides whether to build it. Requested by Irvan on 2026-11-23 after the Flutter-to-native effort discussion; the alternative middle path (a small Kotlin foreground-service glue inside the current Flutter app) is being shipped separately.

Labeling convention: **[verified]** means read directly from the source tree at commit `3141245a` (main, 2026-11-23). **[inferred]** means concluded from behavior, naming, or upstream Obtainium documentation, not re-verified line by line. Anything unlabeled in a list inherits the label of its section heading.

## 0. What Unggun is today

[verified] Unggun is a fork of Obtainium 1.7.4 (upstream commit `35c3e7f`, 2026-11-01) with its own identity: package `com.vanipy.unggun`, bonfire icon, self-update pointing at `irvan-putra/unggun`, version numbering restarted at 0.1.x. It tracks APK releases published elsewhere (GitHub releases and 28 other source types), checks for updates on a schedule, downloads APKs, and installs them (prompt-free where Android allows). Size: ~37,700 lines of Dart, 63 dependencies, 62 test files (501 tests), 35 locale files. Android only, minSdk 26.

## 1. Non-negotiable continuity constraints

1. **[verified] Same signing key.** The native app must be signed with the existing unggun key (certificate SHA-256 `1F7A762C99D357B076968C9BCEFEDF45FF9B65231C60AF2D43A5160783D5BF29`, held in GitHub Actions secrets with an encrypted offline backup). Same package name + same key is what lets the native build install over the Flutter build in place, keeping its role as installer of record for every app it has updated. A new key forces an uninstall, losing the app list and - worse - losing installer-of-record status for the apps Unggun maintains, which brings back one prompted install per app.
2. **[verified] Same package name** `com.vanipy.unggun`, so Android treats the recode as an update.
3. **Data migration.** [verified] App data lives in SharedPreferences (app list as JSON, settings as keys). [inferred] The realistic migration is the existing export/import: export JSON from the Flutter build, install the native build over it, import. A direct SharedPreferences read is technically possible (same file) but fragile across an engine swap; treat export/import as the supported path.
4. **[verified] Existing export format is the compatibility contract.** The native app must read exports produced by 0.1.x (app array with per-app `additionalSettings` maps, optional settings section; a plain-text URL list is also accepted as import input).

## 2. Screens and user flows

[verified] Eleven pages exist today (lib/pages): home (app list with update badges, categories, search, sort, mass-update), app detail (per-app settings, changelog, install/update/track-only actions), add app (URL entry, source detection, source-specific options, search for sources that support it), import/export, settings (global: check interval, background guards, installer mode, notifications, categories, source configs), logs, pinned-certificates editor, pin-set editor, update health, per-app additional options. Components (~4.7k lines) include the generated settings form system, category chips, and the app card.

Recode scope decision (his call): a native recode can ship the same eleven screens in Compose, or a deliberately smaller set (list, detail, add, import/export, settings). The generated-form settings system is the single largest UI component; [inferred] it exists because 29 sources each declare option schemas. A smaller source set (section 3) shrinks it directly.

## 3. Sources and their behaviors

[verified] 29 source implementations exist: GitHub, GitHubStars, GitLab, Codeberg, Sourcehut, F-Droid, FdroidRepo, IzzyOnDroid, HTML (generic), DirectApkLink, APKMirror, APKPure, Aptoide, Uptodown, APK4Free, CoolApk, Farsroid, Itch.io, Jenkins, LiteApks, NeutronCode, Rockmods, RuStore, SamsungGalaxyStore, SourceForge, TelegramApp, Tencent, VivoAppStore, HuaweiAppGallery.

[verified] The source contract (AppSource base class): host matching with optional subdomains; URL standardization; request headers per source config; APK URL extraction with ABI/asset filters, zip/tarball inclusion; version extraction (standard or regex override); release date and changelog extraction; prerelease/include-latest toggles; track-only enforcement flags; per-source request pacing via the host gate.

[verified] His tracked set is GitHub-only today (the 19 imported apps are all GitHub repos, prerelease tracking on). GitHub source specifics verified from code: API host configurable (proxy prefix), optional PAT for 5000/hour, x-ratelimit-remaining/reset honored, repo-rename detection via redirect probe, releases or releases/latest depending on prerelease setting, asset filtering with zips/tarballs, stars variant.

Scope decision (his call): the native recode needs GitHub + DirectApkLink + HTML to cover his usage; each additional source is its own parser to port and test. [inferred] Porting all 29 is the bulk of parser effort; porting 3-5 is days, not weeks.

## 4. Update checking

[verified] Foreground (manual refresh): apps sorted by last check time, checked with 4-way concurrency; per-source prefetch hook; per-app failure stamped so a failing app waits one full interval before retry (not every 15 minutes); repo renames queued for user confirmation; results saved in batches every 3 seconds to bound UI rebuilds.

[verified] Background: WorkManager periodic task every 15 minutes (the OS minimum); the app decides per run whether each app is due (default interval 360 minutes). WiFi/charging guards can restrict checks or installs. Failed checks retry with escalating delay up to 4 attempts; rate-limit failures wait for the source's own reset hint rather than the cap. The task then notifies updates and (if enabled) silently installs what it can.

[verified] Rate limiting (0.1.3, 0.1.6): a per-host request gate bounds concurrency per host (default 2), pauses a request class when the host reports a challenge (cf-mitigated, 5 minutes) or an exhausted quota (x-ratelimit-remaining 0 until x-ratelimit-reset, Retry-After, or secondary-limit body), persists blocks across restarts via SharedPreferences, spreads the remaining budget across the window once <= 15 requests remain (capped at 10s between requests), and - since 0.1.6 - queues rate-limited apps on a one-off background recheck at the reset time, including after a manual refresh, with the message "queued, retrying automatically in N minutes".

## 5. Install and update flows

[verified] Installer modes: stock (PackageInstaller session, prompt-free when Android's installer-of-record/update-owner conditions hold), root (shell), Shizuku (with pretend-to-be-Galaxy-Store/Google-Play options), and an external-install handoff. Downloads show determinate progress notifications with a cancel action; APK selection honors preferred index, ABI and asset filters, version-extraction regex, and a minimum update age (supply-chain delay) that suppresses too-new releases. Track-only apps notify but never download. After a background check, silently installable updates install in order with Unggun itself last.

## 6. Background keep-alive requirement (the reason this spec exists)

[verified] 0.1.7 ships an ongoing, per-app progress notification on the check path (Mihon pattern minus foreground promotion - the Flutter workmanager plugin exposes no setForeground). Native design for the recode, [verified] after Mihon (`LibraryUpdateWorker`/`LibraryUpdateNotifier`, mihonapp/mihon): the update-check Worker calls `setForeground` with a `ForegroundInfo` typed `FOREGROUND_SERVICE_TYPE_DATA_SYNC`; one cached ongoing notification (`setOngoing(true)`, `setOnlyAlertOnce(true)`, cancel action broadcasting to a receiver that cancels the work) shows a determinate bar updated before and after each app, throttled to 500ms, canceled unconditionally in `finally`. In a native recode this is the default shape, not glue.

## 7. Data model

[verified] The App record (22 fields): id, url, author, name, installedVersion, latestVersion, apkUrls, otherAssetUrls, preferredApkIndex, additionalSettings (per-source/per-app option map), lastUpdateCheck, pinned, disabled, categories, releaseDate, changeLog, releaseUrl, overrideSource, allowIdChange, pendingRepoRenameUrl, apkSizeBytes, latestVersionChangedAt, consecutiveFailedChecks, isArchived, newerReleasesFilteredOut, updateHealthEnabled, updateHealthDismissedVersion. Settings live as flat SharedPreferences keys (check interval, guards, installer mode, notification prefs, category filters, export options, per-source config). [inferred] A native recode should keep the same JSON field names in export files (compatibility contract, section 1.4) and can store internally however it likes (Room or DataStore + JSON).

## 8. Notifications

[verified] Nine channels exist: UPDATES_AVAILABLE (max importance), APPS_UPDATED, APPS_POSSIBLY_UPDATED, BG_UPDATE_CHECK (min importance; ongoing progress since 0.1.7), BG_UPDATE_CHECK_ERROR, APP_DOWNLOADING (progress + cancel), FILE_DOWNLOADED, COMPLETE_INSTALL (max), APPS_REMOVED. The recode should reuse these channel IDs so user channel settings carry over. [inferred] Channel IDs are stored per app package, so same-package continuity applies here too.

## 9. Testing strategy for the recode

[verified] The current safety net is 501 tests: source parsers against fixtures, the host gate, rate-limit queue/retry, import/export round-trips, version/regex utilities, notification shapes, translation-shape checks, plus an emulator smoke (install, launch, URL-list import through the file picker). [inferred] The pragmatic port: parser fixtures and expected outputs transfer verbatim (they are data), so the Kotlin parsers can be pinned to identical behavior first; provider/logic tests rewrite as JUnit; the emulator smoke ports as an instrumentation test. Parsers-green-before-UI is the order that keeps the recode honest.

## 10. Open questions for the owner

1. Screen set: all eleven, or the smaller five-screen set (section 2)?
2. Source set: GitHub + DirectApkLink + HTML only, or which others?
3. Root and Shizuku installer modes: keep both, or stock-only for v1 of the native app? (His phone uses stock.)
4. Upstream: freeze at Obtainium 1.7.4 semantics, or keep watching upstream and hand-port selected fixes?
5. Locale set: English + Indonesian only, or convert all 35?

## 11. Effort sketch (unchanged from the earlier assessment, now against this spec)

[verified from the tree] Full port with tests: order of 3-6 weeks focused work with the reduced source/screen set, longer for all 29 sources. Signing/package continuity is satisfied at build time; the migration story for users (one person) is export-then-import. The middle-path foreground-service glue for the current Flutter app ships independently as 0.1.8 and does not block this decision.
