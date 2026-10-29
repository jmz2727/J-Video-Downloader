# Test coverage and remaining verification

Verified run: https://github.com/jmz2727/J-Video-Downloader/actions/runs/40936286375
Tested source: e4b7dc5ee5737f95e4dc8e43dc6dee39ebc10358 (2026-10-29).

## Actually executed: PASS

- Host production Java policy: 87 assertions.
- Host TLS gate/process/signed URL/codec policy: 28 assertions, including auth rejection, plaintext/non-443 refusal, private/mixed DNS destinations, literal argv, timeout/overflow/interruption.
- Project/resource/engine integrity: 166 total checks.
- JUnit/Robolectric: 28 tests across PolicyTest (4), FormatCatalogTest (3), TransportIntegrationTest (11), PrivateStorageTest (2), DatabaseTest (6), QueueTest (1), SettingsTest (1).
- Lint, Kotlin/resources/Room compilation and all three ABI Debug APKs; actual apksigner verification.
- Android API 35 x86_64 emulator: 6 tests, zero skipped/failures. Navigation, malformed URL, clipboard, Arabic UI/RTL, Android Python/QuickJS/CA initialization, actual MP4 H264/AAC mux with duration/both-track checks, private storage, and complete real HTTPS UI/worker/hash/history/confirmed-delete workflow.
- Post-test APK installation and cold activity start returned Status: ok; screenshot/logcat collected.

Reports: test-and-lint-reports and device-test-reports artifacts on the run. The screenshot was collected but no manual visual inspection of that final screenshot was completed.

The live HTTPS test uses only our small owned MP4 fixture. Format catalog tests use metadata fixtures. Neither establishes a successful YouTube, Instagram, X or Facebook download.

## Remaining checks

1. **Physical phone/ABI:** Install ARM64 APK on the actual phone, cold launch, navigate Arabic/English and themes. Test API 26, other Android versions and 16 KiB page-size devices separately.
2. **Each social platform separately:** Use permitted owner-provided public content. Record date, engine/APK hash, public media identifier, returned formats, chosen format ID, actual file size/hash and playback. Redact signed CDN tokens. Verify audio and multiple qualities only when actually returned. All four remain experimental until then.
3. **Media range:** Test compatible HEVC, WebM VP9/Opus, AV1 on API 34+, A/V sync by playback, unknown-length and multipart byte progress. Do not infer universal codec support from the passing H264/AAC remux test.
4. **Network security:** Complete native-engine redirect/downgrade/bad-certificate/private-DNS testing, including all subprocess network paths. Host gate tests are not a complete native networking audit.
5. **Restrictions:** Live/private/login/geo/DRM/expired/SABR/HLS/DASH-only inputs must produce clear errors without fabricated qualities or access bypass.
6. **Persistence/background:** Process death/reboot/force-stop, notification permissions, foreground-service/data-sync quotas, Wi-Fi to cellular changes and cancellation races. Unit tests cover guarded DB transitions; emulator activity recreation is not process-death coverage.
7. **Storage/performance:** Large files, full disk, low RAM/native RSS, malformed/oversized samples, staging cleanup, interruption/retry exhaustion and secure explicit FileProvider grants on real devices.
8. **UI accessibility:** TalkBack, large text, small screens, rendered RTL visual review and more navigation/share-intent combinations.
9. **Distribution:** Stable release signing, native/dependency security/provenance and GPL corresponding-source review before broader binary redistribution.

Do not mark an unexecuted check passed or claim full Snaptube parity. Existing passing tests and security checks must remain enabled.
