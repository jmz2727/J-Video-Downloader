package com.jameel.jvideodownloader.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Fixed argv, no shell. Bounded stdout/stderr, deadline and interruptible cancellation. */
public final class BoundedProcess {
    public static final class Output {
        public final int exit;
        public final String stdout;
        public Output(int exit, String stdout) { this.exit=exit; this.stdout=stdout; }
    }
    public Output execute(List<String> argv, Map<String,String> environment, File directory, long seconds, int outputLimit) throws Exception {
        ProcessBuilder builder=new ProcessBuilder(new ArrayList<>(argv)).directory(directory);
        builder.environment().putAll(environment);
        Process process=builder.start();
        ExecutorService readers=Executors.newFixedThreadPool(2, task -> { Thread t=new Thread(task,"bounded-extractor-io"); t.setDaemon(true); return t; });
        AtomicBoolean overflow=new AtomicBoolean(false);
        Future<byte[]> out=readers.submit(() -> collect(process.getInputStream(),outputLimit,process,overflow,true));
        Future<byte[]> err=readers.submit(() -> collect(process.getErrorStream(),65536,process,overflow,false));
        try {
            if(!process.waitFor(seconds,TimeUnit.SECONDS)) throw new IOException("EXTRACTOR_TIMEOUT");
            byte[] stdout=out.get(5,TimeUnit.SECONDS);
            err.get(5,TimeUnit.SECONDS); // drain, deliberately never expose URLs/tokens in stderr
            if(overflow.get()) throw new IOException("METADATA_TOO_LARGE");
            return new Output(process.exitValue(),new String(stdout,StandardCharsets.UTF_8));
        } finally {
            process.destroyForcibly();
            try { process.getInputStream().close(); } catch(IOException ignored) { }
            try { process.getErrorStream().close(); } catch(IOException ignored) { }
            try { process.getOutputStream().close(); } catch(IOException ignored) { }
            out.cancel(true); err.cancel(true); readers.shutdownNow();
        }
    }
    private static byte[] collect(InputStream stream,int limit,Process process,AtomicBoolean overflow,boolean retain) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(Math.min(limit,8192));
        byte[] buffer=new byte[8192]; int count,total=0;
        while((count=stream.read(buffer))!=-1) {
            total+=count;
            if(total>limit) { overflow.set(true); process.destroyForcibly(); break; }
            if(retain) bytes.write(buffer,0,count);
        }
        return bytes.toByteArray();
    }
}
