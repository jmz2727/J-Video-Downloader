# Dependencies and provenance

- Official yt-dlp engine 2026.10.26, immutable GitHub release: https://github.com/yt-dlp/yt-dlp/releases/tag/2026.10.26
- Asset URL: https://github.com/yt-dlp/yt-dlp/releases/download/2026.10.26/yt-dlp
- Release metadata reported SHA256: e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3; downloaded bytes matched. This is a pinned HTTPS release digest, not an independent audit or a claimed GPG signature verification.
- Asset is a Python zipimport archive, 3,073,072 bytes; it includes `yt_dlp_ejs`. The application uses its bundled scripts and disables remote-component downloads.
- Android bootstrap: io.github.junkfood02.youtubedl-android:library:0.18.1, source tag https://github.com/yausername/youtubedl-android/tree/0.18.1 — GPLv3 license included. Its Python/QuickJS prebuilts are not locally executed, aligned/ABI tested or independently reproduced in this environment.
- The bootstrap's versioned app-private installation paths were verified against its source; this coupling is pinned and must be rechecked when upgrading it. The application does not use its unbounded execute buffers or shell-based destroy-child-process helper.
- Commons IO 2.18.0, Commons Compress 1.27.1 and Jackson 2.18.3 are constrained above the wrapper's old transitive defaults. These pins require dependency resolution/security review during the first real build; no claim of zero vulnerabilities is made.
- No FFmpeg or aria2c component is included. No binary runtime auto-updater, remote plugin loading, cookie importer or backend is implemented.
- Android native muxing compatibility: https://developer.android.com/reference/android/media/MediaMuxer
- Current engine runtime/EJS requirements: https://github.com/yt-dlp/yt-dlp/wiki/EJS

Preserve upstream license notices. Private personal use does not itself require publishing this project. If distributing an APK, review GPL corresponding-source obligations and all bundled native/third-party component notices first. No release was built or distributed here.
