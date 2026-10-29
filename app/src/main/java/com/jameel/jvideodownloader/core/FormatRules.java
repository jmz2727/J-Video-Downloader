package com.jameel.jvideodownloader.core;
/** Codec/container combinations documented for Android MediaMuxer, not guessed qualities. */
public final class FormatRules {
    private FormatRules() {}
    public static boolean canMux(String video, String audio, String container, int sdk) {
        if(video==null || audio==null || container==null) return false;
        if(container.equals("mp4")) return (video.startsWith("avc1") || video.startsWith("hvc1") || video.startsWith("hev1") || sdk>=34 && video.startsWith("av01")) && audio.startsWith("mp4a");
        if(container.equals("webm")) return (video.startsWith("vp8") || video.startsWith("vp9") || video.startsWith("vp09")) && (audio.startsWith("vorbis") || sdk>=29 && audio.startsWith("opus"));
        return false;
    }
}
