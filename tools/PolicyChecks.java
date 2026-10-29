import com.jameel.jvideodownloader.core.MediaPolicy;
import com.jameel.jvideodownloader.core.MediaSniffer;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/** Host-executable tests of the exact policy classes shipped in the app. */
public class PolicyChecks {
    private static int checks;
    static void check(boolean value) { checks++; if (!value) throw new AssertionError("Check " + checks); }
    static void invalid(String value, boolean http) { try { MediaPolicy.validate(value, http); throw new AssertionError(value); } catch(IllegalArgumentException expected) { checks++; } }
    public static void main(String[] args) throws Exception {
        check(MediaPolicy.validate("https://media.example.com/video.mp4?a=b",false).getHost().equals("media.example.com"));
        check(MediaPolicy.validate("http://media.example.com/video.mp4",true).getScheme().equals("http"));
        for(String s : new String[]{"", "not a url", "file:///etc/passwd", "javascript:alert(1)", "content://media/1", "ftp://example.com/file", "https://user:pass@example.com/a", "https://example.com:8443/a", "https://localhost/a", "https://server.local/a", "https://example.com/a#fragment", "https://example.com/a\nInjected", "https:///a", "https://example.com/a?access_token=secret", "https://example.com/a?%74oken=secret", "https://127.0.0.1/a", "https://10.0.0.1/a", "https://169.254.169.254/a", "https://127.1/a"}) invalid(s,false);
        invalid("http://example.com/file.mp4",false);
        for(String[] row : new String[][]{{"https://youtu.be/x","YouTube"},{"https://www.youtube.com/watch?v=x","YouTube"},{"https://m.facebook.com/v","Facebook"},{"https://fb.watch/a","Facebook"},{"https://x.com/a/status/1","X"},{"https://twitter.com/a/status/1","X"},{"https://instagram.com/reel/a","Instagram"},{"https://youtube.com.evil.com/a","Direct"},{"https://notyoutube.com/a","Direct"},{"https://cdn.example.com/video.mp4","Direct"}}) check(MediaPolicy.platform(row[0]).equals(row[1]));
        for(String ip : List.of("127.0.0.1","10.1.2.3","172.16.1.1","192.168.1.1","169.254.169.254","100.64.0.1","0.0.0.0","224.0.0.1","198.18.0.1","192.0.2.1","::1","fc00::1","fe80::1","2001:db8::1","2002::1")) check(!MediaPolicy.publicAddress(InetAddress.getByName(ip)));
        for(String ip : List.of("8.8.8.8","1.1.1.1","2606:4700:4700::1111")) check(MediaPolicy.publicAddress(InetAddress.getByName(ip)));
        check(MediaPolicy.sanitize("../test:bad?.mp4").equals("_test_bad_.mp4"));
        check(MediaPolicy.sanitize("\u202Eevil\u2066name").equals("_evil_name"));
        check(MediaPolicy.sanitize("...").equals("media"));
        check(MediaPolicy.sanitize(null).equals("media"));
        check(MediaPolicy.sanitize("a".repeat(150)).length()==100);
        check(MediaPolicy.sanitize("اختبار فيديو").equals("اختبار فيديو"));
        check(MediaPolicy.key("https://example.com/a").equals(MediaPolicy.key("https://example.com/a")));
        check(!MediaPolicy.key("https://example.com/a").equals(MediaPolicy.key("https://example.com/b")));
        for(int code : List.of(408,429,500,503,599)) check(MediaPolicy.retryHttp(code));
        for(int code : List.of(200,301,400,401,403,404,410)) check(!MediaPolicy.retryHttp(code));
        check(MediaPolicy.mayRetry(0)); check(MediaPolicy.mayRetry(2)); check(!MediaPolicy.mayRetry(3));
        check(MediaPolicy.canTransition("QUEUED","RUNNING")); check(MediaPolicy.canTransition("RUNNING","COMPLETED"));
        check(MediaPolicy.canTransition("RUNNING","QUEUED")); check(MediaPolicy.canTransition("FAILED","QUEUED"));
        check(!MediaPolicy.canTransition("COMPLETED","RUNNING")); check(!MediaPolicy.canTransition("CANCELLED","COMPLETED"));
        check(MediaPolicy.terminal("COMPLETED")); check(!MediaPolicy.terminal("RUNNING"));
        byte[] fixture=Files.readAllBytes(Path.of("app/src/test/resources/fixture.mp4"));
        check(MediaSniffer.matches(fixture,"video/mp4"));
        check(!MediaSniffer.matches("<html>error</html>".getBytes(),"video/mp4"));
        check(!MediaSniffer.matches(new byte[0],"video/mp4"));
        check(MediaSniffer.matches("ID3test".getBytes(),"audio/mpeg"));
        check(MediaSniffer.matches("OggStest".getBytes(),"audio/ogg"));
        check(!MediaSniffer.matches("ID3test".getBytes(),"video/webm"));
        System.out.println("PASS: " + checks + " policy/signature assertions; production Java classes compiled with JDK " + System.getProperty("java.version"));
    }
}
