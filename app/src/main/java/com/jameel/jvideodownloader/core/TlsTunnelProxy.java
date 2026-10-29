package com.jameel.jvideodownloader.core;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

/** Authenticated loopback CONNECT gate for the extractor. Only pinned public-IP TLS/443 tunnels. */
public final class TlsTunnelProxy implements AutoCloseable {
    private final ServerSocket listener;
    private final Thread acceptor;
    private final ThreadPoolExecutor sessions;
    private final Set<Socket> sockets=ConcurrentHashMap.newKeySet();
    private final String password;
    private final String expectedAuth;
    public interface Resolver { InetAddress[] resolve(String host) throws UnknownHostException; }
    private final javax.net.SocketFactory socketFactory;
    private final Resolver resolver;
    private volatile boolean closed;
    private final Semaphore connectionSlots=new Semaphore(3);
    public TlsTunnelProxy() throws IOException { this(javax.net.SocketFactory.getDefault(),InetAddress::getAllByName); }
    public TlsTunnelProxy(javax.net.SocketFactory socketFactory, Resolver resolver) throws IOException {
        this.socketFactory=socketFactory; this.resolver=resolver;
        byte[] secret=new byte[32]; new SecureRandom().nextBytes(secret);
        password=Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        expectedAuth="Basic "+Base64.getEncoder().encodeToString(("jvideo:"+password).getBytes(StandardCharsets.US_ASCII));
        listener=new ServerSocket(); listener.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0),4);
        sessions=new ThreadPoolExecutor(0,8,15,TimeUnit.SECONDS,new SynchronousQueue<>(),r -> { Thread t=new Thread(r,"extractor-tls-tunnel"); t.setDaemon(true); return t; });
        acceptor=new Thread(this::accept,"extractor-proxy-accept"); acceptor.setDaemon(true); acceptor.start();
    }
    public String url() { return "http://jvideo:"+password+"@127.0.0.1:"+listener.getLocalPort(); }
    public int port() { return listener.getLocalPort(); }
    private void accept() {
        while(!closed) {
            try {
                Socket socket=listener.accept();
                if(!connectionSlots.tryAcquire()) { socket.close(); continue; }
                sockets.add(socket);
                try { sessions.execute(() -> handle(socket)); }
                catch(RejectedExecutionException full) { sockets.remove(socket); socket.close(); connectionSlots.release(); }
            } catch(IOException ignored) { if(closed) return; }
        }
    }
    private void handle(Socket client) {
        Socket remote=null;
        try {
            client.setSoTimeout(15000);
            InputStream input=client.getInputStream();
            String header=header(input);
            String[] lines=header.split("\r\n"); String[] request=lines[0].split(" ");
            String auth="";
            for(String line:lines) if(line.regionMatches(true,0,"Proxy-Authorization:",0,20)) auth=line.substring(20).trim();
            if(!MessageDigest.isEqual(expectedAuth.getBytes(StandardCharsets.US_ASCII),auth.getBytes(StandardCharsets.US_ASCII))) { reply(client,407); return; }
            if(request.length!=3 || !request[0].equals("CONNECT") || !request[2].matches("HTTP/1\\.[01]")) { reply(client,403); return; }
            URI uri=MediaPolicy.validate("https://"+request[1]+"/",false);
            if(uri.getPort()!=443) { reply(client,403); return; }
            InetAddress[] addresses=resolver.resolve(uri.getHost());
            if(addresses.length==0) { reply(client,403); return; }
            for(InetAddress address:addresses) if(!MediaPolicy.publicAddress(address)) { reply(client,403); return; }
            // Connect by resolved IP, never re-resolve after the safety check.
            remote=socketFactory.createSocket(); sockets.add(remote);
            remote.connect(new InetSocketAddress(addresses[0],443),15000); remote.setSoTimeout(20000);
            OutputStream output=client.getOutputStream();
            output.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.US_ASCII)); output.flush();
            Socket upstream=remote;
            Future<?> upload=sessions.submit(() -> relay(client,upstream));
            relay(remote,client); upload.cancel(true);
        } catch(Exception ignored) { /* Do not log capability URLs, credentials or remote headers. */ }
        finally { closeSocket(client); if(remote!=null) closeSocket(remote); connectionSlots.release(); }
    }
    private static String header(InputStream input) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); int current, ending=0;
        while(bytes.size()<8192 && (current=input.read())!=-1) {
            bytes.write(current);
            ending=(ending << 8) | current;
            if(ending==0x0d0a0d0a) return new String(bytes.toByteArray(),StandardCharsets.US_ASCII);
        }
        throw new IOException("INVALID_PROXY_REQUEST");
    }
    private static void reply(Socket socket,int status) throws IOException {
        socket.getOutputStream().write(("HTTP/1.1 "+status+" Denied\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
    }
    private static void relay(Socket source,Socket target) {
        try { byte[] buffer=new byte[16384]; int n; while((n=source.getInputStream().read(buffer))!=-1) { target.getOutputStream().write(buffer,0,n); target.getOutputStream().flush(); } }
        catch(IOException ignored) { }
        finally { try { target.shutdownOutput(); } catch(IOException ignored) { } }
    }
    private void closeSocket(Socket socket) { sockets.remove(socket); try { socket.close(); } catch(IOException ignored) { } }
    @Override public void close() {
        closed=true; try { listener.close(); } catch(IOException ignored) { }
        for(Socket socket:sockets) closeSocket(socket);
        sessions.shutdownNow(); acceptor.interrupt();
    }
}
