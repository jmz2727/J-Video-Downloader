# J Video Downloader — build and test report

Date: 2026-10-29 UTC. Version: 1.1.0-dev (code 2).

## BUILD STATUS: PASS

[GitHub Actions run 40936286375](https://github.com/jmz2727/J-Video-Downloader/actions/runs/40936286375) completed successfully at 19:07:04 UTC for source commit `e4b7dc5ee5737f95e4dc8e43dc6dee39ebc10358`. Both `build` and `device-tests` passed. This supersedes the earlier blocked draft report. Public source changes were authorized by the user.

Real Debug APKs were assembled for arm64-v8a, armeabi-v7a and x86_64. CI checked each file existed and verified its signature using Android apksigner. Lint ran with abortOnError enabled. No security check or failing test was disabled to obtain success.

## APK delivery

- ARM64: [J-Video-Downloader-arm64-v8a-debug](https://github.com/jmz2727/J-Video-Downloader/actions/runs/40936286375/artifacts/12925690848).
- Inner file: `app-arm64-v8a-debug.apk`, with `app-arm64-v8a-debug.apk.sha256`.
- ZIP size: **35,618,181 bytes** (about 35.6 MB).
- GitHub artifact ID: **12925690848**; expires **2026-11-12T19:02:48Z**.
- **ZIP archive SHA256**: `bffed3ea92385d3bcfd0054d11dc9f1e5be972d0adfc19b8a16a8f7e2797d7af`.
- This digest is for the artifact ZIP, not the inner APK.
- Inner ARM64 APK: **36,487,761 bytes**, SHA256 `bc528d2c8322d808397c44e0f82f05f95b69e532c8848dba30da818d1b098518`. Recovered bytes matched the CI checksum sidecar.
- Package `com.jameel.jvideodownloader`, min SDK 26, compile/target 37. Debug signing only; no release keystore/public Release/store upload.
- Separate armeabi-v7a and x86_64 artifacts are on the same run. Download only the device's ABI.

The verified final APK is delivered through GitHub Actions. The local execution environment became unavailable during the original delivery, which blocked attaching a standalone copy to the chat; it did not block CI verification. During the execution-status review on 2026-10-31 (Asia/Aden), the existing final artifact was recovered and verified, and the exact inner APK was made available as a standalone chat download. Older local candidate APKs are not the final deliverable.

## Actual executed results

| Check | Result | Evidence/scope |
|---|---|---|
| Production Java URL/platform/state/retry/signature policy | PASS: 87 assertions | CI host-check stdout |
| TLS CONNECT gate, bounded process, signed URL and codec policy | PASS: 28 assertions | CI host-check stdout; actual local process/loopback cases |
| Resource/localization/wrapper/project/engine integrity | PASS: 166 total checks | 158 base + 8 engine/security checks in CI |
| JUnit/Robolectric | PASS: 28 tests across 7 classes | `testDebugUnitTest` succeeded; test-and-lint-reports artifact |
| Lint + Kotlin/resources/Room compilation | PASS | `lintDebug assembleDebug` succeeded |
| ABI APK signature verification | PASS: all 3 APKs | `apksigner verify --verbose` in build job |
| Android instrumentation | PASS: 6 tests, 0 skipped, 0 failed | API 35 x86_64 Pixel 2 emulator, device-test-reports artifact |
| Install and launch after tests | PASS | `installDebug` succeeded; `am start -W` returned `Status: ok`; screenshot collected |

Unit/integration coverage includes URL/platform rules, actual format catalog parsing using fixture metadata, HTTPS transport fixtures, MIME/signature mismatch, cancellation/interruption/retry, queue permits, private atomic publication/FileProvider, Room persistence/duplicate/cancellation/history operations and settings persistence.

The six Android tests exercised:
1. Navigation and malformed URL behavior.
2. Clipboard paste.
3. Arabic language selection, Arabic UI text and actual RTL configuration.
4. Pinned Android Python engine version, QuickJS execution and CA roots.
5. Real H264/AAC MP4 remux with both tracks and duration checks, private publication/deletion.
6. A full UI workflow against our owned public HTTPS MP4: analyze → actual quality choice → WorkManager transfer → exact SHA256 match → private file existence → activity recreation/history → confirmed delete → file and row removal. Optional completion notifications were disabled to test that valid setting.

Test fixture: `https://raw.githubusercontent.com/jmz2727/J-Video-Downloader/main/app/src/test/resources/fixture.mp4`. This tiny owned fixture is not a social-platform verification or a large-file benchmark. The native tests execute x86_64 binaries; ARM64 APK was built/signature-verified, not run on physical ARM64 hardware.

## Platform status

| Platform | Status | Evidence |
|---|---|---|
| Direct HTTPS MP4 | Implemented and tested | Full real Android UI/worker/private-file/hash/history/delete flow passed |
| YouTube | Partially supported / experimental, not live-tested | yt-dlp public-page path and real-format selection implemented; Android runtime tested, no successful live page extraction/download recorded |
| Instagram | Partially supported / experimental, not live-tested | Same implementation-level evidence; no live download recorded |
| X/Twitter | Partially supported / experimental, not live-tested | Same implementation-level evidence; no live download recorded |
| Facebook | Partially supported / experimental, not live-tested | Same implementation-level evidence; no live download recorded |
| Private/login/DRM/region restricted | Unsupported where access is required | No authentication/access restriction bypass |
| HLS/DASH/SABR-only, live, incompatible formats | Unsupported | Filtered or understandable error; no simulated download |

Full Snaptube parity and universal resolutions are not claimed.

## Implemented features and security

Compose Material 3 with Arabic/English/RTL, themes, four main screens; manual/clipboard/share input; real metadata/format selection; modular WorkManager queue/cancel/retry/progress; Room v2 history/search/sort/filter; open/share/confirmed deletion; preferences/privacy; original icon; CI and tests.

Files use private internal storage, narrow unexported FileProvider and explicit read grants, atomic publication, backup/transfer exclusions and no broad storage permission. HTTPS validation rejects unsafe schemes, local/metadata targets and unsafe redirects. The native process uses fixed argv without a shell, bounded output/timeouts and an authenticated public-TLS CONNECT gate. Signed CDN URLs stay in memory. No backend, advertising, tracking, FFmpeg, MP3 converter or hidden engine updater is added.

Engine: yt-dlp **2026.10.26**, **3,073,072 bytes**, pinned SHA256 `e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3`, EJS included. Android bootstrap `io.github.junkfood02.youtubedl-android:library:0.18.1`. SDK package `platforms;android-37.0`, build-tools 36.0.0, JDK 17, AGP 9.1.1, Gradle 9.3.1, Kotlin 2.2.10.

## Known limitations / not executed

- Live anonymous social-platform extraction, audio and multiple qualities need separate verification using permitted owner-provided content. Platforms can change or reject anonymous access.
- No MP3 conversion, DRM/authentication bypass, pause/resume, external storage location picker or universal 4K/transport support.
- Physical ARM64 devices, API 26 and other OS versions, 16 KiB page-size compatibility, prolonged background/reboot quotas, native RAM profiling, multi-gigabyte files, accessibility/large-font and rendered Arabic screenshot review were not executed. The existing English home screenshot was visually inspected during the status review; it is not evidence of the untested device/visual cases.
- The app bounds its own transfer/sample/process-output buffers; it does not hard-cap all Python/JNI memory or prove arbitrary files are harmless. A full native TLS/network/dependency audit is outstanding.
- Retries start over and are capped at three after the initial attempt. Staging requires about 2× single-file or 3× multipart size plus 16 MiB. Uninstall/clear-data deletes private downloads.
- No stable release signing is configured. Future debug CI runs may have a different signing certificate; uninstalling a conflicting build deletes app-private files, so export wanted files first.
- Six passing emulator tests establish their listed scope, not all original acceptance criteria or full platform support.

## Deliverables / remaining user action

Source, icon, Android UI/engine/storage/history/settings/localizations, tests, complete Wrapper, CI, privacy/provenance docs and three verified APK artifacts are in the supplied repository/run. Historical v1 report remains labeled in `docs/V1_BUILD_REPORT.md`.

Install the ABI-appropriate APK, then verify the desired permitted platform links on the actual phone. No repository credentials, local SDK installation or additional account authorization is needed for this delivered build.

## Execution-status review — 2026-10-31 (Asia/Aden)

There was **no suspended or failing APK-verification job**. Run 40936286375 is completed/success and every build/device job succeeded. The original missing standalone attachment was caused by the local tool returning **"No usable execution environment is available"** during delivery. GitHub's stored APK remained ready.

At review start, the latest repository operation was documentation commit `6994c27f3d16861903cbbd1c8f1a23c51d16e263` on 2026-10-29 at 22:10:42 Asia/Aden. It followed the successful CI completion at 22:07:04. The final app install/launch returned Status: ok and the screenshot was collected at 22:06:31.

Only the remaining delivery checks were performed:
- Retrieved the existing ARM64 artifact and existing test reports; **no Gradle build, CI dispatch, emulator run, dependency update or app-source change**.
- Verified exact archive size/SHA256, ZIP CRC, inner APK SHA256 against its sidecar, APK CRC, ARM64-only native libraries and the pinned embedded engine digest.
- Parsed the actual binary AndroidManifest: correct package/version, min SDK 26, target 37, debug=true, backup=false, FileProvider exported=false with URI grants.
- Parsed all seven JUnit XML suites: 28 tests, 0 failures/errors/skips. Parsed device HTML: 6 tests, 0 failures/skips.
- Parsed lint XML: **0 Error/Fatal, 39 Warning**. Categories: dependency/plugin version suggestions (23), obsolete SDK guard (1), unused resources (8), Kotlin extension suggestions (7). These are existing warnings, not newly introduced findings or a dependency-security audit.
- Inspected the existing English home screenshot and completed the standalone APK delivery.

Current status: **BUILD PASS / APK READY**. Social-platform live tests and physical ARM64 execution remain unverified as stated above. These documentation-only changes do not alter the successfully tested production source.
