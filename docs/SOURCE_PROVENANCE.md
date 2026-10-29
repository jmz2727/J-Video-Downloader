# Source and build provenance

The canonical source is https://github.com/jmz2727/J-Video-Downloader.

Verified production/test source commit: `e4b7dc5ee5737f95e4dc8e43dc6dee39ebc10358`.
Git tree: `3f6addab64e0d4d4dcc3ed808a73edb343daaf1f`.
Successful run: https://github.com/jmz2727/J-Video-Downloader/actions/runs/40936286375.

The delivery documentation commit follows that source commit without changing app, tests, dependencies or workflows. Git commit/tree/blob hashes provide provenance for tracked source; the earlier draft ZIP's stale source-manifest.sha256 was removed.

The intentionally untracked engine asset must be prepared using `python3 tools/prepare-engine.py`. Both the preparation script and Gradle preBuild require SHA256 `e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3` for the 2026.10.26 release. The complete Wrapper JAR remains tracked.

Final artifacts and their archive digests are recorded by GitHub Actions and in verification.json. The inner APK checksum is in its artifact sidecar. Debug signing certificates are build-environment credentials, not a configured stable release key.
