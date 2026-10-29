# J Video Downloader

**Debug build: PASS.** Version **1.1.0-dev**, package `com.jameel.jvideodownloader`, Android 8.0+ (API 26), target SDK 37. The [verified build and device-test run](https://github.com/jmz2727/J-Video-Downloader/actions/runs/40936286375) built and signature-verified three real APKs. All 28 JUnit/Robolectric tests and all 6 Android emulator tests passed.

## Install

1. Sign in to GitHub and [download the verified ARM64 artifact](https://github.com/jmz2727/J-Video-Downloader/actions/runs/40936286375/artifacts/12925690848) (ZIP, 35,618,181 bytes). It contains `app-arm64-v8a-debug.apk` and its SHA256 sidecar. Actions retains this artifact until **2026-11-12**.
2. Extract the ZIP and install that APK on an ARM64 Android device. Allow installation from the browser/file manager when Android asks. This is a debug build, not a release-signed store package.
3. Paste or share a permitted HTTPS media/page URL, tap Analyze, choose an actual available format, then Download. Open/share a completed file explicitly from Downloads. Confirm before deleting a file.

The same run includes separate `J-Video-Downloader-armeabi-v7a-debug` and `J-Video-Downloader-x86_64-debug` artifacts. Choose one matching the device; downloading all architectures is unnecessary. Re-run the workflow to regenerate expired artifacts. No public GitHub Release or store publication was made.

## Verified scope

| Source | Implementation | Verification |
|---|---|---|
| Direct HTTPS MP4 | Metadata/signature analysis and streaming download | **PASS on Android emulator:** real owned HTTPS fixture, quality choice, WorkManager download, exact file hash, history after activity recreation, confirmed deletion |
| YouTube | Public-page yt-dlp extraction and compatible returned formats | **Experimental / not live-tested** |
| Instagram | Public-page yt-dlp extraction and compatible returned formats | **Experimental / not live-tested** |
| X/Twitter | Public-page yt-dlp extraction and compatible returned formats | **Experimental / not live-tested** |
| Facebook | Public-page yt-dlp extraction and compatible returned formats | **Experimental / not live-tested** |

The bundled Python/QuickJS runtime and pinned engine version ran successfully on the x86_64 Android emulator. That does not verify a live extraction/download from any social platform. Full Snaptube equivalence is not established. No login, cookie import, DRM or regional restriction bypass is implemented.

## Features

- Kotlin, Jetpack Compose Material 3, MVVM and separated domain/data/UI/download/storage layers.
- Arabic/English with RTL; dark, light and system themes; Home, Downloads, History and Settings.
- Manual input, clipboard and Android Share Intent; actual metadata and available compatible quality/audio choices.
- WorkManager background queue, actual byte progress, cancellation, bounded retry/backoff, Wi-Fi preference and notifications.
- Room v2 persistent history, search/sort/filter, open/share, missing-file handling and confirmed deletion.
- App-private internal media storage; no broad storage permission or default shared Downloads destination.
- Original J/video-download launcher icon, privacy screen, no tracking SDK, ads or backend.

## Extraction, security and limits

The official **yt-dlp 2026.10.26** zipimport engine is pinned by SHA256 and includes EJS. Android Python/QuickJS comes from `io.github.junkfood02.youtubedl-android:library:0.18.1`. There is no automatic engine updater, remote plugin loading, FFmpeg/aria2c package or MP3 conversion dependency.

Only actual compatible HTTPS file formats are offered, up to 2160p when returned. MP4 supports AVC/HEVC + AAC, with AV1 on API 34+; WebM supports VP8/VP9 + Vorbis, or Opus on API 29+. Compatible multipart media uses Android MediaExtractor/MediaMuxer. HLS/DASH/SABR-only, live, DRM, inaccessible or incompatible media produces a clear error.

URL/redirect/DNS rules reject unsafe schemes, credentials, private/metadata destinations and TLS downgrades. The native extractor uses an authenticated loopback CONNECT gate restricted to public TLS destinations. Fixed argv uses no shell, with bounded output and a 180-second extraction timeout. TLS certificate verification remains enabled. These controls do not certify arbitrary remote content as harmless or establish a full native-network security audit.

Downloads stream in 64 KiB chunks; remuxing uses a bounded 8 MiB sample buffer. One file transfer/remux and one extraction run at a time. Total native Python/runtime RAM is not hard-capped. Single-file jobs need about 2× file size, multipart jobs about 3× plus 16 MiB; retries restart from zero, at most three retries. Android execution quotas apply. Pause/resume, MP3 conversion and external location selection are unavailable.

Files live in `filesDir/media/` and are removed by uninstall/clear-app-data. Atomic publication avoids exposing a partially downloaded final file. An unexported, narrowly scoped FileProvider grants temporary read access only through explicit Open/Share. Backup and device-transfer exclusions protect private files/history. Original public page URLs and selected format IDs persist; expiring signed CDN URLs and authorization headers do not.

## Build and tests

GitHub Actions performs host checks → unit/integration tests → lint/assembly → APK signature verification → separate ABI artifacts → API 35 x86_64 emulator tests on pushes. Reports and a post-test launch screenshot are retained as Actions artifacts.

For an existing JDK 17 / Android SDK installation:

```sh
git clone https://github.com/jmz2727/J-Video-Downloader.git
cd J-Video-Downloader
python3 tools/prepare-engine.py
chmod +x gradlew tools/host-check.sh
./tools/host-check.sh
./gradlew testDebugUnitTest lintDebug assembleDebug --stacktrace
# With an available device/emulator:
./gradlew connectedDebugAndroidTest --stacktrace
```

Windows uses `gradlew.bat`. Install SDK package `platforms;android-37.0` and build-tools 36.0.0. AGP 9.1.1, Gradle 9.3.1 and Kotlin/Compose compiler 2.2.10 are pinned. The complete Gradle Wrapper, including its JAR and distribution checksum, is committed. The ignored ~3 MiB engine asset is fetched explicitly and hash-checked by the preparation script and preBuild.

Tested source commit: [`e4b7dc5ee5737f95e4dc8e43dc6dee39ebc10358`](https://github.com/jmz2727/J-Video-Downloader/commit/e4b7dc5ee5737f95e4dc8e43dc6dee39ebc10358). Subsequent documentation-only changes do not alter that tested app. See [BUILD_REPORT.md](BUILD_REPORT.md), [test coverage and remaining device checks](docs/TEST_PLAN.md) and [machine-readable evidence](docs/verification.json).

The bootstrap is GPLv3. Preserve upstream notices and corresponding-source obligations when redistributing binaries. [Third-party provenance](docs/THIRD_PARTY_NOTICES.md) lists upstream sources; no independent dependency security audit is claimed.
