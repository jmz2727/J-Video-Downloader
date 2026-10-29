# v2 verification gates

## Actually executed

- 87 assertions on the exact production Java URL/platform/signature/state/retry policy.
- 28 assertions on the exact production TLS tunnel gate, signed-URL distinction, native mux codec/API policy and bounded process. Cases: no/incorrect proxy auth, plaintext proxy request, non-443 port, private/metadata destination, mixed public/private DNS, argument literal preservation without a shell, subprocess stdout overflow, timeout, interruption/cleanup.
- 154 total XML, localization, resource, Gradle-wrapper/project, private FileProvider, remote-cleartext, pinned engine hash/ZIP/EJS/config-policy checks.
- 7 workflow structural assertions; build-step shell syntax.
- Executed the pinned official Python engine's `--version` with the configured CLI switches on the available host Python; result 2026.10.26, exit 0, no platform request. NOT Android JNI/QuickJS execution.

## Automated sources included but NOT RUN

- Existing JUnit policy/HTTP copy/retry/cancellation/error tests; Room database lifecycle, duplicate constraint, guarded cancellation, persistence and history clearing; queue permit and preference persistence.
- `FormatCatalogTest`: actual returned-format selection, MP4/WebM pairing, audio-only, signed in-memory media URLs, insecure/DRM/private/HLS-only format rejection. These are synthetic metadata fixtures, not live platform tests.
- `PrivateStorageTest`: atomic internal publication, narrow content URI, file deletion/missing-file handling, cancellation without exposing a partial final file.
- Compose navigation, invalid link, clipboard, settings choice and Arabic configuration tests. No full quality-choice→native-extraction→download UI test or rendered RTL screenshot test has run.

## Run after private repository target is available

1. First run host checks → `testDebugUnitTest` → `lintDebug assembleDebug`. Read and fix actual logs. Do not disable security checks or claim success from YAML validity.
2. Verify all produced ABI APKs with `apksigner`, recorded SHA256 and package/min/target SDK inspection. Confirm native Python/QuickJS packaging/initialization on the actual supported ABI and on 4 KiB/16 KiB page-size devices. No FFmpeg-dependent path should be called.
3. Install, cold launch and navigate in Arabic/English; test restart/rotation, themes, large font, TalkBack, narrow screens and real Compose RTL. Settings and Room history must persist.
4. Use **permitted owner-provided public test URLs** for YouTube, Instagram, X and Facebook separately. Record date, URL/media identifier (avoid signed CDN tokens), engine version, APK hash, actual returned formats, chosen ID, output size/hash, player validation and failure logs redacted. Test audio and multiple resolutions only where they exist. Until then every platform remains experimental/unverified.
5. Validate MP4 AVC/HEVC+AAC, WebM VP9+Opus on API 29+, AV1 MP4 on API 34+. Compare the merged output's streams, duration and A/V sync. Verify unknown/multipart progress shows bytes, not a fabricated percent; single-part percent is based on real response Content-Length.
6. Verify all remote connections use verified TLS: bad certificate, downgrade redirect, redirect loop, private/metadata IP and DNS rebinding; test an extractor that follows an HTTP redirect and confirm the gate rejects it. The host tests cover the gate, not a complete native network audit.
7. Test live/private/login/geo/DRM/expired/SABR/HLS/DASH-only responses. There must be a clear failure with no access bypass, invented qualities, silently changed selected format or simulated download.
8. Inspect app-private directories/permissions, FileProvider exported=false, explicit grant behavior, no external default destination, no auth/signed URL database records or logs. Confirm cancellation never resurrects completion, pending-file cleanup after process death and no abandoned extraction proxy/process.
9. Test cache full, large files, bounded memory (including native process RSS), malformed containers, mux cancellation/oversized sample, missing/moved files, network interruption/retry exhaustion and Wi-Fi→cellular switch. Wi-Fi-only jobs bind both extraction tunnels and download sockets to the chosen network.
10. Validate Android background restrictions/notifications/force-stop/reboot. The app does not bypass JobScheduler/data-sync quotas and retries restart from zero.

A compatible source implementation is not proof of a working or secure application. The exact first compile/device failures must guide remaining fixes. There is no APK or live-platform success evidence in this delivery.
