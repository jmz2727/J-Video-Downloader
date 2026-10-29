package com.jameel.jvideodownloader.core;
import java.nio.charset.StandardCharsets;
/** Small signature check. Container detection is not a guarantee of codec compatibility. */
public final class MediaSniffer {
    private MediaSniffer() {}
    private static boolean at(byte[] b, int offset, String value) {
        byte[] signature = value.getBytes(StandardCharsets.US_ASCII);
        if (b.length < offset + signature.length) return false;
        for (int i=0;i<signature.length;i++) if(b[offset+i] != signature[i]) return false;
        return true;
    }
    public static boolean matches(byte[] b, String mime) {
        return switch(mime) {
            case "video/mp4", "audio/mp4" -> at(b,4,"ftyp");
            case "video/quicktime" -> at(b,4,"ftyp") || at(b,4,"moov") || at(b,4,"mdat");
            case "video/webm", "audio/webm" -> b.length>=4 && (b[0]&255)==0x1a && (b[1]&255)==0x45 && (b[2]&255)==0xdf && (b[3]&255)==0xa3;
            case "audio/mpeg" -> at(b,0,"ID3") || b.length>=2 && (b[0]&255)==255 && (b[1]&0xe0)==0xe0;
            case "audio/aac" -> b.length>=2 && (b[0]&255)==255 && (b[1]&0xf6)==0xf0;
            case "audio/ogg" -> at(b,0,"OggS");
            case "audio/flac" -> at(b,0,"fLaC");
            case "audio/wav", "audio/x-wav" -> at(b,0,"RIFF") && at(b,8,"WAVE");
            default -> false;
        };
    }
}
