# Recoding Unggun natively: specification (revision 2, owner decisions applied)

Status: 2026-11-23. A spec for a from-scratch native (Kotlin) recode of Unggun without Flutter. Nothing here is built. Revision 2 applies the owner's review answers (draft at commit `ff253310`). Build starts only on his go.

Labels: **[verified]** read from the source tree at `3141245a`. **[inferred]** concluded from behavior or upstream docs. **[decision]** the owner's answer, quoted from his review on 2026-11-23.

## 0. Goal

[decision] "i care more about functionalities (easier apk updates other than me hunting them one by one)" and "i just want to keep updated, i am not married to it". The native app exists to do one job: track the GitHub projects he installs, tell him when a new release is out, and install it with as little tapping as Android allows. Everything else in Obtainium 1.7.4 is out unless it serves that job.

## 1. Scope, as decided

| Topic | Decision | Source |
|---|---|---|
| Sources | GitHub only | "Github only" |
| Installer | Stock Android installer only (no root, no Shizuku) | "stock only" |
| Upstream | Frozen at Obtainium 1.7.4 semantics; no watching upstream | "let's just freeze" |
| Languages | English only | "english only" |
| Screens | Only what is needed: app list and logs. Add/import/settings can be sheets or sections of one screen | "just screens and logs we needed", "could be just one screen" |
| Migration | None. Fresh install, set up again | "no migration needed", "i am okay with uninstall then install & setup again" |
| Export format and data model | No compatibility contract. Free to design | "i don't care about compability" |
| Notification channels | Rebuild fresh | "it's okay if we rebuild the notifications" |
| Rate-limit machinery | Reinvent as needed, no port of 0.1.3/0.1.6 | "we can reinvent them as we need" |
| Install flows | Drop the optional ones (see 5) | "seems complicated, maybe we don't need all of them" |
| Behavior parity | Not required | "no need for exact same behaviors" |
| Background checks | Not required for v1; add after | "if automatic is hard for first version, then let's develop it later" |
| Mihon-style foreground service | Optional, comes with background checking | "maybe ... not married to it" |

## 2. Package name and signing key (changed from draft)

The draft called these non-negotiable. With no migration needed, they are now a recommendation, not a constraint.

Recommended default: keep package `com.vanipy.unggun` and the existing unggun signing key (certificate SHA-256 `1F7A762C99D357B076968C9BCEFEDF45FF9B65231C60AF2D43A5160783D5BF29`). It costs nothing and keeps the in-place update path open, so he is not forced to uninstall. If he prefers a clean start with a new package name, that also works, but then both apps can sit side by side until he removes the old one.

One thing to know before uninstalling the Flutter build: Android remembers which app installed each APK. Apps Unggun installed can update silently later only while the installer of record is still installed. After a fresh native install, the first update of each tracked app may ask for one tap, then the native app becomes the installer of record. [inferred] from Android's update-owner rules, to be confirmed on his phone.

## 3. What the app does (v1)

1. **Track.** Add a GitHub repo by URL or `owner/repo`. Also accept a plain-text list of URLs (he has used this to bring in 19 apps; keep it as the bulk-add path). No export/import of app records is required, so re-adding is the setup step.
2. **Check (manual).** A refresh action checks all tracked apps and marks the ones with a newer release. Uses the GitHub releases API. Honors prerelease on/off per app. Optional personal access token to raise the limit from 60 to 5000 requests per hour.
3. **Install.** Download the chosen APK asset with a progress notification, then install through the stock PackageInstaller. Per-app APK choice: pick by ABI and an optional name filter, remember the choice. Track-only per app (notify, never install).
4. **Logs.** A readable log screen of checks, downloads, installs and errors. This is how he sees why something failed.
5. **Notify.** A small set of new channels: updates available, install result, errors, and downloads (progress). Exact set is the builder's choice.

[inferred] Mass "update all", pinning, categories, search and sort are not in v1. Add only if he misses them.

## 4. Update checking

v1: manual refresh, with up to 4 apps in parallel and a failure for one app never blocks the others. Rate limits: read `x-ratelimit-remaining` and `x-ratelimit-reset` and `Retry-After` from GitHub, stop requests until reset, and say so in the log and the UI ("limit hit, retry after HH:MM"). Persist the block so a restart does not hammer the API. This is the small version of what 0.1.3 and 0.1.6 do; the queued automatic retry and request pacing come with background checking.

v1.1 (after v1 is in his hands): background checking. A WorkManager task checks apps that are due (default every 6 hours) and, when running, promotes itself to a foreground service of type `dataSync` with one ongoing progress notification (the Mihon pattern, `setForeground`, throttled updates, canceled in `finally`). That is what keeps Android from killing the check halfway, and it is the reason this recode was proposed. It is also where silent installs of ready updates in order, Unggun last, are added. [verified] The Flutter app has this as 0.1.8 already.

## 5. Install flows (cut down)

Kept: stock PackageInstaller session; determinate download notification with cancel; ABI/name filter for choosing the APK; track-only.
Dropped for v1: root and Shizuku installer modes, external-install handoff, minimum update age (supply-chain delay), preferred-index and version-regex overrides, zip/tarball extraction. [inferred] Add back individually if he hits a real need. The minimum-update-age filter is the one worth reconsidering later because it is a safety feature.

## 6. Data

A single local store (Room, or DataStore plus JSON). Per app: repo, display name, installed version, latest version, chosen asset, prerelease flag, track-only flag, last check time, failure count, release notes and URL. Settings: GitHub token (stored with Android Keystore protection, never logged), check interval (v1.1), notification toggles. No requirement to match the old 22-field record or old export JSON. A simple export of his own list as text (one URL per line) is enough for backup, and it is the same format the bulk-add accepts.

## 7. Testing

Parsers first, UI second still holds in spirit, but it is now small: the GitHub release parser, asset filter and version comparison are pinned with fixtures taken from real GitHub responses (his 19 apps are a ready source of them) and JUnit tests. Rate-limit handling gets unit tests with fake headers. Then an emulator smoke test in CI, as today: install, launch, bulk-add through the file picker, check against a recorded fixture, install a test APK. What no CI test can prove: silent-update behavior and background survival on his Samsung. Those need his phone and are called out as unverified until he confirms.

## 8. Upstream

Frozen at Obtainium 1.7.4 semantics for the GitHub source. No tracking of upstream. If GitHub changes its API in a way that breaks checks, that is a normal bug fix.

## 9. Effort (revised)

Earlier text said "3-6 weeks focused work". That figure was a human-engineer-equivalent number for 29 sources, eleven screens and 35 locales. It was a bad fit for how this work is actually done and it was not labeled as such. With the scope above it does not apply.

Estimate for the agent build, stated as a range because it is not measured:

- v1 (GitHub parser, rate-limit handling, stock installer, one list screen plus add and logs, notifications, CI with emulator smoke, signed prerelease APK): about 1 to 2 days of working time, assuming no surprises from the stock installer.
- v1.1 (background checks with foreground service, silent installs in order): another day or so, mostly because it can only be proven on his phone.

The time that does not shrink is his: installing builds, trying them, and telling me what feels wrong. Each device round trip adds hours of waiting, not minutes of work. The main risk is not code volume. It is Android's installer rules (silent updates need the right installer-of-record conditions) and background limits on Samsung, neither of which an emulator reproduces faithfully.

## 10. Open questions

None blocking. Two defaults assumed, easy to flip:

1. Keep package name and signing key (section 2), yes unless he wants a clean break.
2. Background checking waits for v1.1 (section 4), per his answer.
