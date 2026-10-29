# J Video Downloader — v2 secure extraction source

**Status: source changes implemented and host-tested; Android compilation, APK and real platform downloads NOT VERIFIED.** No APK is included. Repository: https://github.com/jmz2727/J-Video-Downloader. The user explicitly authorized public source publication on 2026-10-29. Android build status must be established by the actual Actions run.

## Changes

- Removed the blanket refusal at the repository layer for YouTube, Instagram, X/Twitter and Facebook. Those public page URLs now use yt-dlp metadata extraction. Private/login/DRM/geo restrictions are not bypassed.
- Pin the official yt-dlp **2026.10.26** zipimport engine in APK assets; verify its SHA256 before installation to app-private storage. The asset is 3,073,072 bytes and includes EJS. The Android Python/QuickJS bootstrap is `youtubedl-android:library:0.18.1`. The official engine can be updated in a reviewed app build, not silently at runtime.
- Show only actual HTTPS file formats returned by the extractor. Select Video + Audio or Audio only, then a format before downloading. No invented resolutions. Store the original public page and format ID; re-extract before a job to refresh expiring signed stream URLs. Signed stream URLs and limited User-Agent/Referer/Origin headers remain in memory only.
- Download file parts with the existing streaming OkHttp engine, rechecking redirects/DNS/TLS. Progress uses actual copied bytes. Multipart totals remain unknown until completion rather than reporting an estimated percentage as real progress.
- Remux compatible MP4/WebM tracks with Android `MediaExtractor`/`MediaMuxer`. MP4: AVC/HEVC + AAC (AV1 on API 34+); WebM: VP8/VP9 + Vorbis, or Opus on API 29+. These limits are device/OS dependent. No FFmpeg, aria2c or MP3 conversion dependency. HLS/DASH-only, SABR-only, live streams, incompatible codecs and DRM are not supported in this implementation.
- New media is saved **only inside `filesDir/media/`**. Cache, pending files, history and engine files are private. No default shared Downloads, MediaStore or SAF location. Removal or clear-app-data deletes these files. FileProvider is not exported and exposes only the media directory, with per-URI temporary read grants from explicit Open/Share actions.
- Remote cleartext is disabled in Android configuration and production URL calls. The native extractor is configured to use an authenticated loopback CONNECT tunnel, which allows TLS port 443 to resolved public IPs only, pins the resolved destination, limits connections/threads/headers/timeouts and refuses ordinary HTTP proxy requests. The HTTP loopback control connection itself never leaves the device. Python verifies server certificates using its bundled CA roots.
- The extraction process uses a fixed argv list without a shell, ignores external configs/plugins, disables automatic remote components and caching, limits stdout to 2 MiB/stderr to 64 KiB, times out after 180s and kills its process on cancellation. One extraction and one file transfer/remux run at a time. An 8 MiB sample buffer bounds remuxing; media is never read wholly into RAM. These are safeguards, not a proof that all native runtime memory is bounded or every media file is harmless.

## Support matrix

| Source | Current code path | Verified downloads |
|---|---|---|
| Direct HTTPS audio/video | Direct analyzer and streaming worker | Not run on Android |
| YouTube | yt-dlp extraction + actual compatible formats | Not tested live |
| Instagram | yt-dlp extraction + actual compatible formats | Not tested live |
| X/Twitter | yt-dlp extraction + actual compatible formats | Not tested live |
| Facebook | yt-dlp extraction + actual compatible formats | Not tested live |

Do not advertise full platform support or equivalence with Snaptube. Extractors can break, require access unavailable anonymously, or return only unsupported transport/container formats. This version reports a clear failure instead of bypassing restrictions, silently choosing another quality or simulating a download. Rights/permission remain the user's responsibility.

## Build with GitHub

1. The user authorized the supplied public repository. Keep APK distribution separate from source publication.
2. Upload the contents of `JVideoDownloader/` from the ZIP at repository root, preserving `.github/` and `gradle/wrapper/gradle-wrapper.jar`.
3. A source checkout first runs `python tools/prepare-engine.py` to fetch ~3 MiB from the pinned upstream release and verify SHA256. The source ZIP already includes this asset. Run **Actions → Android build**. Start without emulator tests. It runs the real host checks, JUnit/Robolectric tests, lint and assembly. Inspect actual failures before claiming success.
4. On a passing build, obtain **J-Video-Downloader-debug**. ABI splits generate separate `app-arm64-v8a-debug.apk`, `app-armeabi-v7a-debug.apk` and `app-x86_64-debug.apk` (exact filenames must be checked against actual outputs). Android Note10-class devices normally use arm64; verify the actual device ABI. CI verifies each generated APK's signature and writes its SHA256.
5. Only after the smoke build passes, enable optional device tests, then manually validate real permitted platform URLs, private file permissions, TLS refusal, native initialization and background behavior on a device.

The native bootstrap adds substantial APK/build dependency size. **Those Maven/native dependencies, Android SDK and Gradle distribution have not been downloaded locally.** No FFmpeg package was fetched. The small (~3 MiB) official engine asset is included so the extraction logic is pinned/current rather than relying on the bootstrap's older bundled extractor. Exact APK size is unknown until a build runs. ABI splits avoid downloading every architecture to the phone.

### Existing Windows Android tooling

Do you already have JDK 17 and Android SDK 37 available? If not, use the GitHub path instead of a large local install.

```powershell
Set-Location "C:\path\to\JVideoDownloader"
java -version
$env:ANDROID_HOME
Test-Path .\gradle\wrapper\gradle-wrapper.jar
python .\tools\prepare-engine.py
.\gradlew.bat testDebugUnitTest --stacktrace
.\gradlew.bat lintDebug assembleDebug --stacktrace
Get-ChildItem .\app\build\outputs\apk\debug\*.apk
Get-ChildItem .\app\build\outputs\apk\debug\*.apk | Get-FileHash -Algorithm SHA256
# Only after the above passes, with a connected device or available emulator:
.\gradlew.bat connectedDebugAndroidTest --stacktrace
```

AGP 9.1.1, Gradle 9.3.1, JDK 17, compile/target SDK 37, min SDK 26, built-in Kotlin/Compose compiler 2.2.10. The dependency versions and native packaging still need a real build to validate resolution and compatibility.

## Tests and limits

`./tools/host-check.sh` actually compiles/tests the production Java policy, tunnel gate and bounded process on the available Java runtime. It checks Android resources, private-provider scope, cleartext configuration, the official engine hash and EJS presence. It does not compile Kotlin, run JNI on Android, verify MediaMuxer output or download from a social platform. See [BUILD_REPORT.md](BUILD_REPORT.md).

Room v2 adds a persisted selected-format ID, with `MIGRATION_1_2`; no signed media URLs or authentication headers are stored. Legacy external destination/HTTP flags are disabled. There was no previously delivered working APK; if migrating an independently built v1, external file-provider URIs may no longer resolve because this version intentionally scopes the provider to private files. Review/export previous files before updating.

Single-file jobs need about 2× their size, multipart/remux jobs about 3× plus 16 MiB free space. Each media request has a 15s connect / 20s read / 30-minute call timeout. Retries restart from zero and are capped at three after the initial attempt. Android background execution quotas still apply. No APK, Play Store upload, GitHub release or public source publication is performed.

The bootstrap is GPLv3; its license is included in `docs/youtubedl-android-LICENSE.txt`. The yt-dlp zip includes its own component notices/licenses. Review corresponding-source and native dependency licensing before redistributing binaries. [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md) records upstream sources; this source delivery does not claim redistribution compliance for an unbuilt APK.
