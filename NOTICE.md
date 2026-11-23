# Notice

Unggun is a modified copy of Obtainium (https://github.com/ImranR98/Obtainium), GPL-3.0, based on upstream version 1.7.4, commit 35c3e7fea4f341a716eca4bb3fc625319ce965ab (2026-11-01). Git history of upstream is preserved in this repository.

Changes made in this fork:

- Application ID and namespace `dev.imranr.obtainium` changed to `com.vanipy.unggun`; Kotlin package and platform channel names follow.
- Launcher name, in-app English strings, icons, notification icon and TV banner changed to Unggun (bonfire).
- Version reset to 0.1.0.
- Self-update tracking points to https://github.com/irvan-putra/unggun.
- Upstream release, F-Droid metadata, Docker and agent-instruction files removed; new CI and release workflows added.
- Per-host request gate for source metadata requests (0.1.3): pauses a request class while the host reports a challenge or an exhausted rate limit, using the host's own reset hints.
- Rate-limited update checks queue an automatic recheck once the limit window resets, foreground refreshes included, and the gate spreads its remaining budget instead of spending it in one burst (0.1.6).
- Background update checks post an ongoing, non-swiping progress notification that moves per app checked and is canceled when the batch ends, after Mihon's library-update pattern; without foreground-service promotion, so the notification reports progress but does not extend the worker's execution window (0.1.7).
- Background update checks promote to a `dataSync` foreground service while they run (0.1.8), Mihon-style: the service owns the same progress notification (id 4, channel BG_UPDATE_CHECK), so the process is treated as user-visible work and a check is not killed mid-batch when the app is backgrounded or the screen turns off. When Android refuses the start (background-start restrictions on Android 12+), the check degrades silently to the 0.1.7 notification-only behavior. Shipped as a local Flutter plugin (`plugins/update_check_service`) so the MethodChannel is registered in the headless engine Workmanager spawns, not only in the UI engine.
- Other translation files still contain the word "Obtainium" in places.

The upstream community directory and wiki (apps.obtainium.imranr.dev, wiki.obtainium.imranr.dev) are third-party services run by the upstream maintainer; links to them remain in the app.
