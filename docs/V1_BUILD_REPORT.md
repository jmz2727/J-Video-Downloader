# J Video Downloader — Build and test report

Date: 2026-10-29 (Asia/Aden)

## Outcome

**BUILD STATUS: FAIL / BLOCKED — not an acceptance-complete application delivery.**

Source implementation, original icon, test sources and CI configuration are delivered. **No Android APK was generated.** No APK filename, download location, signature, package launch, Android build success or successful GitHub Actions run is claimed. There is no verified remote build path/run URL yet; the configured workflow is ready for its first run in a dedicated repository.

### Exact blockers

- Available environment: OpenJDK runtime 17.0.20, Python, shell and FFmpeg. The Java runtime contains `jdk.compiler`, so the shared production Java policy classes could actually be compiled/tested without installing tools.
- Android SDK, `sdkmanager`, `adb`, emulator, installed Gradle and a Kotlin/Android build toolchain were not found. The Gradle Wrapper JAR/scripts were fetched (small files), but the Gradle distribution, SDK, Maven dependencies and emulator were **not** downloaded silently.
- The user selected GitHub for the build. The connected GitHub repository inventory contained only an unrelated project. It was not modified. No dedicated repository was selected or made available. Available GitHub tools did not offer repository creation.
- Consequently no initial Android compilation smoke gate could run, and no Kotlin/resource/lint result is asserted. Source implementation proceeded while this build gate remained blocked. A private repository target is required to run CI, inspect real logs, correct build failures and produce an APK.

## Actual executed checks

| Check | Result | Evidence / limit |
|---|---|---|
| Production shared Java policy/signature compilation + assertions | **PASS: 87 assertions** | Java runtime compiler module compiled the exact `MediaPolicy.java` and `MediaSniffer.java` used by the app; host runner passed. Not Kotlin compilation. |
| XML/localization/resources/project/wrapper integrity | **PASS: 147 checks** | `tools/static-check.py`; English/Arabic parity, all referenced resources, required build files, no broad storage permission, backups disabled, RTL flag, wrapper class present. |
| Workflow YAML structural assertions | **PASS: 7 checks** | Parsed YAML; expected triggers, read-only permissions, tests, assembly, APK signature verification, named artifact, missing-artifact failure. No Actions execution. |
| POSIX wrapper shell syntax | **PASS** | `bash -n gradlew`; shell syntax only. |
| Wrapper JAR integrity / official checksum | **PASS** | Archive tested; SHA256 equals official Gradle 9.3.1 wrapper checksum: `b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13`. |
| Gradle distribution integrity configuration | **PASS (configuration only)** | Official 9.3.1 distribution SHA256 pinned in wrapper properties. Distribution has not been downloaded or executed. |
| Original MP4 test fixture | **PASS** | FFmpeg generated solid-color clip; ffprobe reports 1.000000 seconds, 1,629 bytes; host signature assertion passed. This does not prove an Android download. |
| Original application icon | **PASS (asset inspection)** | SVG/vector resources and 512×512 PNG exist; PNG opened and visually inspected. Adaptive foreground uses safe-zone scaling; launcher rendering on Android untested. |

See `docs/host-checks.txt` for actual host output and `docs/verification.json` for machine-readable results.

## Tests NOT executed

- Gradle `testDebugUnitTest`: JUnit policy tests, local HTTP MP4/stream/storage tests, Robolectric Room persistence/cancellation/duplicate tests, queue permit and settings persistence tests.
- Gradle `lintDebug` and `assembleDebug`; Kotlin compilation and generated Room schema.
- APK signing/package verification; installation, cold launch and device navigation.
- Compose navigation/paste/error/settings and Arabic configuration tests.
- Full UI → actual worker → storage download test; Wi-Fi handover; API 26/28 FileProvider; API 29+ MediaStore; SAF provider permissions; process-kill/reboot/foreground quotas; real RTL rendered layout and accessibility.

No automated test is reported as passed simply because its source was written. Included tests and workflow can still contain compilation/runtime issues that only the first real Android build exposes.

## Platform support matrix

| Platform/input | Implemented | Tested here | Final status in this version |
|---|---|---|---|
| Direct MP4/WebM/MOV | Analyzer, HTTP streaming worker, MIME/signature validation, storage and controls | Policy/MP4 signature only; Android end-to-end **not executed** | **Implemented, unverified Android functionality** |
| Direct supported audio | Original file streaming; MP3 passthrough, no conversion | Android tests not executed | **Implemented, unverified** |
| YouTube | Host detection, explicit refusal + official download/export/direct-owner-file guidance | Host identity assertions passed; no platform download attempted | **Unsupported for download** |
| Facebook | Host detection and explicit refusal/guidance | Host identity assertions passed | **Unsupported for download** |
| X / Twitter | Host detection and explicit refusal/guidance | Host identity assertions passed | **Unsupported for download** |
| Instagram | Host detection and explicit refusal/guidance | Host identity assertions passed | **Unsupported for download** |

No social platform is described as “partially supported” merely because its hostname can be recognized. No social extractor, auth/session import, private scraper, DRM bypass or platform API integration is enabled.

## Implemented source / pending validation

- **Implemented in source:** Kotlin Compose UI; dark/light/system; Arabic/English/RTL resources; paste/share/manual URL; direct-media metadata; original quality selection/preferences; video or audio source passthrough; WorkManager foreground downloads; bounded queue/concurrency; cancellation, retries, real byte progress/speed/ETA; Room history; search/sort/filter/clear; MediaStore/SAF/FileProvider; open/share/confirmed delete; settings/privacy; vector icon; test sources; CI/debug artifact verification pipeline; README and Windows commands.
- **Pending because build blocked:** compiling all above, generated Room schema validation, test execution, lint corrections, APK assembly/signature/package verification, installation/launch, real device behavior, full acceptance.
- **Intentionally absent:** social-platform downloading, resolution discovery not supplied by a direct source, fabricated thumbnails/durations, audio extraction/muxing, MP3 conversion, HLS/DASH, pause/range resume, backend, analytics, ads, public publication.

## Known limits / remaining work

1. A single direct URL exposes a single original file; multiple source resolutions and alternate audio tracks cannot be invented. A video may or may not include audio. Player codec support is external.
2. Interrupted downloads restart from zero. Up to three automatic retries use WorkManager exponential backoff. Socket cancellation can wait up to the 20-second read timeout. Android controls task start order; strict FIFO is not guaranteed.
3. Jobs retain their network/storage preferences from enqueue time. Wi-Fi-only also requires the OS unmetered constraint; metered Wi-Fi can remain queued. Sockets/DNS bind to the chosen Wi-Fi network to prevent cellular fallback; real-device handover tests remain pending.
4. Staging requires approximately twice the media size plus 16 MiB free internal space. MediaStore/SAF publication follows a temporary download. A crash in the very small interval between external destination creation and its URI being journaled can leave an orphaned pending file; automated recovery of that interval is not implemented.
5. Public media MIME headers are required. Generic binary MIME, local/private hosts, literal IPv6 URLs, unusual ports, token/signature URLs and credentials are refused. This can exclude otherwise downloadable owner-authorized links.
6. Android 8–9 default files are app-owned and removed on uninstall. MediaStore/SAF files may persist. Clearing history retains files and removes in-app management links. Test provider-specific open/delete/revoked permissions before production use.
7. Android foreground-service/battery/JobScheduler restrictions apply. No promise of uninterrupted work after force-stop or beyond data-sync quota. No quota bypass is implemented.
8. Debug APK signing in clean CI is not stable for production update distribution. A real developer contact, user-owned stable release signing key, device QA and policy review are required before any distribution. No public publication is authorized or performed.

## Deliverables

- Full isolated Android source and complete Gradle Wrapper including JAR.
- Original SVG / adaptive vector launcher / PNG icon.
- Unit, local integration and instrumentation test sources plus own MP4 fixture.
- `.github/workflows/android-build.yml` (structurally validated, **not run**).
- README, installation/PowerShell commands, test plan, privacy and references.
- This truthful build/test report and host evidence.
- **APK: absent; no generated file.**

## Required next action

Provide access to a **new dedicated private GitHub repository** for J Video Downloader. Then upload this project at its root, run the basic workflow, inspect/fix actual build logs until passing, download/verify the artifact and execute device acceptance. No changes should be made to the unrelated connected repository.
