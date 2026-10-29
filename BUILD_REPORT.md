# J Video Downloader — v2 build and security report

Date: 2026-10-29, Asia/Aden. Version: 1.1.0-dev (code 2).

## Build status: BLOCKED / NOT ACCEPTANCE-COMPLETE

No Android build or APK was produced. No app launch, Android storage/muxing behavior or live platform download is claimed. The source is reviewable; the requested Snaptube-equivalent full support is **not established**.

The supplied GitHub repository `jmz2727/J-Video-Downloader` was checked twice: empty, public, and writable by the connected account. No files/settings/workflows were changed there. Earlier user instructions prohibited public publication. A private repository, or explicit authorization to publish source publicly, is required before uploading the source and running its workflow. Android SDK, Gradle distribution, Kotlin/Android dependencies and emulator are absent locally; no major local install was attempted.

## Changes implemented in source

1. Public YouTube/Instagram/X/Facebook URLs use a yt-dlp extraction path instead of blanket refusal. Quality/audio choices come only from actual compatible HTTPS formats. There is no login, cookie import, DRM or regional-restriction bypass.
2. Official engine 2026.10.26 pinned as a 3,073,072-byte asset, SHA256 `e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3`. Actual downloaded bytes match official release metadata; archive integrity and bundled EJS were checked. The engine CLI version and requested flags ran on host Python with exit 0. Android execution is untested.
3. Android bootstrap `youtubedl-android:library:0.18.1` provides Python/QuickJS. Its native Maven dependencies were **not downloaded or tested locally**. No FFmpeg/aria2c dependency or automatic runtime updater is added. Review the first resolved dependency graph/native binaries before release.
4. Bounded, interruptible process execution with separate fixed argv, no shell, 180s timeout, stdout 2 MiB, stderr 64 KiB; fixed extractor options ignore configs/plugins/caches and remote component downloads. Raw logs/stream tokens are not exposed.
5. Authenticated loopback TLS CONNECT gate: public DNS/IP checks, pinned public destination, no HTTP forwarding/port downgrade, bounded connections/threads/header/read timeouts. The embedded client is configured to use it; full real-device/native network enforcement has not been audited.
6. New files only in private internal app storage, atomic pending→final publication, narrow unexported FileProvider and explicit read grants on Open/Share. No default external Downloads/SAF/MediaStore. Files are removed by uninstall/clear-app-data. Signed media URLs remain in memory; the public original URL and selected format ID persist in Room v2, with migration from v1.
7. Existing streaming downloader validates each URL/redirect/TLS, copies 64 KiB chunks and uses actual byte progress. Compatible multipart streams remux with native Android MediaExtractor/MediaMuxer and a bounded 8 MiB sample buffer. MP4 AVC/HEVC+AAC, AV1 on API 34+; WebM VP8/VP9+Vorbis or Opus on API 29+. Only available compatible formats up to 2160p are offered; no universal 4K/MP3 promise.
8. Private-storage settings/Arabic-English messages, video/audio and quality selection, selected-format persistence, re-extraction for expired stream capabilities, ABI-split build/artifact handling and added tests/documentation.

## Executed evidence

| Check | Result | Scope |
|---|---|---|
| Production Java URL/platform/state/retry/container rules | PASS: 87 assertions | Actually compiled with available Java runtime's compiler module |
| Production TLS gate / bounded process / signed URL / codec policy | PASS: 28 assertions | Actual loopback/process tests, including refusal/timeout/overflow/interruption; no remote platform download |
| Project/resources/localizations/private storage/engine integrity | PASS: 154 checks total | Not Kotlin or Android compilation |
| GitHub workflow YAML structure | PASS: 7 assertions | Workflow not uploaded or run |
| Workflow build shell blocks | PASS: shell syntax | No Gradle task executed |
| Pinned official engine asset | PASS: digest/ZIP/EJS; host `--version` returns 2026.10.26 | Not Android Python/QuickJS initialization |
| Gradle Wrapper | Present, complete; unchanged verified 9.3.1 wrapper JAR and distribution checksum | Gradle distribution not installed/run |

Exact host stdout: `docs/host-checks.txt`; machine-readable state: `docs/verification.json`.

## Platform status

| Input | Implementation | Actual download verification |
|---|---|---|
| Direct HTTPS media | Implemented in source | NOT RUN on Android |
| YouTube public page | Experimental yt-dlp integration implemented | NOT RUN live |
| Instagram public page | Experimental yt-dlp integration implemented | NOT RUN live |
| X/Twitter public page | Experimental yt-dlp integration implemented | NOT RUN live |
| Facebook public page | Experimental yt-dlp integration implemented | NOT RUN live |
| Private/login/DRM/region-restricted | No bypass implemented | Unsupported where access is required |
| HLS/DASH/SABR-only, live, incompatible codec/mux | Filtered or clear error | Unsupported in this source version |

## Tests not executed / risks

- All Gradle JUnit/Robolectric tests, including new format parsing and private publication tests. Test sources can still have compile/runtime errors.
- Kotlin/resources/lint compilation, dependency/ABI resolution, Room schema/migration validation and native runtime packaging.
- APK creation, signing/package verification, install/launch, complete UI workflow, actual Arabic layout, background/reboot/cancellation behavior on Android.
- Live platform extraction/audio/quality/4K downloads; native MP4/WebM output integrity and A/V sync; actual bad-certificate rejection and extractor redirect behavior; native process RSS/16 KiB page compatibility.
- Network safety does not certify arbitrary media as nonmalicious. The native runtime is a significant trusted dependency; process/output/media buffers do not hard-cap its complete RAM consumption or prove sandbox isolation from the app's own UID.
- Compatible formats may be absent, anonymous access may fail, and upstream platform changes can break extraction. Failure is reported, not silently replaced with a lower quality.
- Staging can require about 2× file size, or 3× for multipart/remux, plus 16 MiB. Downloads restart from zero; three automatic retries; strict FIFO not guaranteed; Android execution quotas still apply.
- No stable release signing setup or public distribution. The GPLv3 bootstrap license and provenance references are included; distribution compliance for an unbuilt APK is not asserted.

## Deliverables / next action

Updated complete source, pinned engine asset, original icon, tests, CI, README, privacy/test plan and this report. **APK: none.** No GitHub mutation/publication took place.

Make the supplied repository private (recommended for the stated personal scope), or explicitly authorize public source publication. Then upload and execute the basic build, fix actual logs to a passing APK, verify/install it and test the requested platforms individually. See README for Windows/GitHub steps. Historical v1 report is retained in `docs/V1_BUILD_REPORT.md` and does not describe the current source.
