package com.jameel.jvideodownloader.core;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Pure, shared policy. Used by analysis, transport and the queue. No Android dependency. */
public final class MediaPolicy {
    private MediaPolicy() {}
    public static URI validate(String text, boolean allowHttp) { return validateInternal(text,allowHttp,false); }
    public static URI validateMedia(String text) { return validateInternal(text,false,true); }
    private static URI validateInternal(String text, boolean allowHttp, boolean mediaCapability) {
        try {
            if (text == null || text.length() > 8192 || text.matches("(?s).*\\p{Cntrl}.*")) throw new Exception();
            URI u = new URI(text.trim());
            String scheme = u.getScheme();
            if (!("https".equalsIgnoreCase(scheme) || allowHttp && "http".equalsIgnoreCase(scheme))) throw new Exception();
            if (u.getHost() == null || u.getUserInfo() != null || u.getRawFragment() != null) throw new Exception();
            if (u.getPort() != -1 && u.getPort() != 80 && u.getPort() != 443) throw new Exception();
            if (!mediaCapability && u.getRawQuery() != null && java.util.regex.Pattern.compile("(?i)(^|&)(access_token|token|authorization|auth|session|sessionid|password|api_key|key|signature|sig|policy|x-amz-signature|x-goog-signature)=").matcher(java.net.URLDecoder.decode(u.getRawQuery(), "UTF-8")).find()) throw new Exception();
            String host = u.getHost().toLowerCase(Locale.ROOT);
            if (host.matches("[0-9.]+") && !publicAddress(InetAddress.getByName(host))) throw new Exception();
            if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local") || !host.contains(".")) throw new Exception();
            return u;
        } catch (Exception e) { throw new IllegalArgumentException("INVALID_URL"); }
    }
    public static String platform(String text) {
        try {
            String h = new URI(text.trim()).getHost();
            if (h == null) return "Direct";
            h = h.toLowerCase(Locale.ROOT);
            for (String[] p : new String[][]{{"youtube.com","YouTube"},{"youtu.be","YouTube"},{"youtube-nocookie.com","YouTube"},{"facebook.com","Facebook"},{"fb.watch","Facebook"},{"fb.com","Facebook"},{"x.com","X"},{"twitter.com","X"},{"t.co","X"},{"instagram.com","Instagram"}})
                if (h.equals(p[0]) || h.endsWith("." + p[0])) return p[1];
        } catch (Exception ignored) { }
        return "Direct";
    }
    public static boolean publicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        byte[] b = address.getAddress();
        int a = b[0] & 255;
        if (b.length == 4) {
            int c = b[1] & 255;
            return !(a == 0 || a == 10 || a == 127 || a >= 224 || a == 100 && c >= 64 && c <= 127 || a == 169 && c == 254 || a == 172 && c >= 16 && c <= 31 || a == 192 && (c == 168 || c == 0 || c == 2) || a == 198 && (c == 18 || c == 19 || c == 51) || a == 203 && c == 0);
        }
        // Only global unicast, excluding documentation and transition mechanisms.
        return (a & 0xe0) == 0x20 && !((b[0]&255)==0x20 && (b[1]&255)==0x01 && ((b[2]&255)==0x0d && (b[3]&255)==0xb8 || b[2]==0)) && !((b[0]&255)==0x20 && (b[1]&255)==0x02);
    }
    public static String sanitize(String name) {
        String n = name == null ? "" : name.replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|\\u202A-\\u202E\\u2066-\\u2069]", "_").replaceAll("^\\.+|[. ]+$", "").trim();
        if (n.isEmpty()) n = "media";
        if (n.length() > 100) n = n.substring(0, 100);
        return n;
    }
    public static String key(String url) {
        try {
            byte[] b = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder s = new StringBuilder();
            for (byte v : b) s.append(String.format(Locale.ROOT,"%02x", v));
            return s.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static boolean retryHttp(int code) { return code == 408 || code == 429 || code >= 500 && code <= 599; }
    public static boolean mayRetry(int attempt) { return attempt < 3; }
    public static boolean terminal(String status) { return Arrays.asList("COMPLETED","FAILED","CANCELLED").contains(status); }
    public static boolean canTransition(String from, String to) {
        if (from.equals(to)) return true;
        return switch (from) {
            case "QUEUED" -> Arrays.asList("RUNNING","CANCELLED","FAILED").contains(to);
            case "RUNNING" -> Arrays.asList("QUEUED","COMPLETED","CANCELLED","FAILED").contains(to);
            case "FAILED", "CANCELLED" -> to.equals("QUEUED");
            default -> false;
        };
    }
}
