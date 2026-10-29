import com.jameel.jvideodownloader.core.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class SecurityChecks {
    static int checks;
    static void check(boolean value) { checks++; if(!value) throw new AssertionError("Security check "+checks); }
    static List<String> child(String mode,String... extra) {
        List<String> args=new ArrayList<>(Arrays.asList(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),"SecurityChecks",mode));
        args.addAll(Arrays.asList(extra)); return args;
    }
    static String request(TlsTunnelProxy proxy,String first,String auth) throws Exception {
        try(Socket socket=new Socket("127.0.0.1",proxy.port())) {
            socket.setSoTimeout(2000);
            String text=first+"\r\n"+(auth==null?"":"Proxy-Authorization: "+auth+"\r\n")+"\r\n";
            socket.getOutputStream().write(text.getBytes(StandardCharsets.US_ASCII)); socket.getOutputStream().flush();
            ByteArrayOutputStream bytes=new ByteArrayOutputStream(); int n;
            while((n=socket.getInputStream().read())!=-1 && bytes.size()<1024) bytes.write(n);
            return bytes.toString("US-ASCII");
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length>0) {
            switch(args[0]) {
                case "ok": System.out.print(args.length>1?args[1]:"{\"title\":\"fixture\"}"); return;
                case "flood": for(int i=0;i<1024;i++) System.out.print("x".repeat(1024)); return;
                case "wait": Thread.sleep(10000); return;
            }
        }
        check(FormatRules.canMux("avc1.640028","mp4a.40.2","mp4",26));
        check(FormatRules.canMux("hvc1.1.6.L93","mp4a.40.2","mp4",26));
        check(!FormatRules.canMux("av01.0.08M","mp4a.40.2","mp4",33));
        check(FormatRules.canMux("av01.0.08M","mp4a.40.2","mp4",34));
        check(!FormatRules.canMux("vp09.00.51.08","opus","webm",28));
        check(FormatRules.canMux("vp09.00.51.08","opus","webm",29));
        check(FormatRules.canMux("vp9","vorbis","webm",26));
        check(!FormatRules.canMux("av01","opus","webm",37));
        check(!FormatRules.canMux("avc1","opus","mp4",37));
        check(MediaPolicy.validateMedia("https://cdn.example.com/file.mp4?sig=temporary").getScheme().equals("https"));
        try { MediaPolicy.validate("https://cdn.example.com/file.mp4?sig=temporary",false); throw new AssertionError(); } catch(IllegalArgumentException ok) { checks++; }
        for(String url:Arrays.asList("http://cdn.example.com/a","https://127.0.0.1/a?sig=x","https://a:secret@cdn.example.com/file")) {
            try { MediaPolicy.validateMedia(url); throw new AssertionError(url); } catch(IllegalArgumentException ok) { checks++; }
        }
        try(TlsTunnelProxy proxy=new TlsTunnelProxy()) {
            check(request(proxy,"CONNECT example.com:443 HTTP/1.1",null).startsWith("HTTP/1.1 407"));
            check(request(proxy,"CONNECT example.com:443 HTTP/1.1","Basic wrong").startsWith("HTTP/1.1 407"));
            String credentials=new URI(proxy.url()).getUserInfo();
            String auth="Basic "+Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.US_ASCII));
            check(request(proxy,"GET http://example.com/media HTTP/1.1",auth).startsWith("HTTP/1.1 403"));
            check(request(proxy,"CONNECT example.com:80 HTTP/1.1",auth).startsWith("HTTP/1.1 403"));
            check(!request(proxy,"CONNECT 127.0.0.1:443 HTTP/1.1",auth).contains("200 Connection"));
            check(!request(proxy,"CONNECT localhost:443 HTTP/1.1",auth).contains("200 Connection"));
            check(!request(proxy,"CONNECT 169.254.169.254:443 HTTP/1.1",auth).contains("200 Connection"));
        }
        // DNS rebinding/mixed answer policy checked before any remote socket is connected.
        try(TlsTunnelProxy proxy=new TlsTunnelProxy(javax.net.SocketFactory.getDefault(),host->new InetAddress[]{InetAddress.getByName("8.8.8.8"),InetAddress.getByName("127.0.0.1")})) {
            String auth="Basic "+Base64.getEncoder().encodeToString(new URI(proxy.url()).getUserInfo().getBytes(StandardCharsets.US_ASCII));
            check(request(proxy,"CONNECT fixture.example:443 HTTP/1.1",auth).startsWith("HTTP/1.1 403"));
        }
        BoundedProcess runner=new BoundedProcess(); File cwd=new File(".");
        BoundedProcess.Output output=runner.execute(child("ok"),Collections.emptyMap(),cwd,5,1024);
        check(output.exit==0 && output.stdout.equals("{\"title\":\"fixture\"}"));
        String literal="$(not-a-command); --exec malicious";
        check(runner.execute(child("ok",literal),Collections.emptyMap(),cwd,5,1024).stdout.equals(literal));
        try { runner.execute(child("flood"),Collections.emptyMap(),cwd,5,1024); throw new AssertionError("must cap output"); }
        catch(Exception expected) { checks++; }
        long start=System.nanoTime();
        try { runner.execute(child("wait"),Collections.emptyMap(),cwd,1,1024); throw new AssertionError("must timeout"); }
        catch(IOException expected) { check(expected.getMessage().equals("EXTRACTOR_TIMEOUT")); }
        check(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()-start)<4);
        ExecutorService pool=Executors.newSingleThreadExecutor();
        Future<?> task=pool.submit(()->{ try {runner.execute(child("wait"),Collections.emptyMap(),cwd,20,1024);} catch(Exception expected) {} });
        Thread.sleep(100); task.cancel(true); pool.shutdownNow(); check(pool.awaitTermination(3,TimeUnit.SECONDS));
        System.out.println("PASS: "+checks+" extractor process, TLS gate, signed-URL and native codec policy assertions");
    }
}
