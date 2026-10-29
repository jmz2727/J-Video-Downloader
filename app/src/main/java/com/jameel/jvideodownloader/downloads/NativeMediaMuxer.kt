package com.jameel.jvideodownloader.downloads

import android.media.*
import com.jameel.jvideodownloader.domain.MediaFailure
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.nio.ByteBuffer

/** Lossless MP4/WebM remux for OS-supported codecs. No decoder, conversion, native FFmpeg or whole-file buffering. */
class NativeMediaMuxer {
    suspend fun combine(video: File, audio: File, output: File, mime: String) {
        val videoReader=MediaExtractor(); val audioReader=MediaExtractor()
        var muxer: MediaMuxer?=null
        var started=false
        try {
            videoReader.setDataSource(video.absolutePath); audioReader.setDataSource(audio.absolutePath)
            if(videoReader.drmInitData != null || audioReader.drmInitData != null) throw MediaFailure("RESTRICTED")
            fun track(reader: MediaExtractor, prefix: String): Int = (0 until reader.trackCount).firstOrNull { reader.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith(prefix)==true } ?: throw MediaFailure("MUX_UNSUPPORTED")
            val vi=track(videoReader,"video/"); val ai=track(audioReader,"audio/")
            val vf=videoReader.getTrackFormat(vi); val af=audioReader.getTrackFormat(ai)
            val vm=vf.getString(MediaFormat.KEY_MIME); val am=af.getString(MediaFormat.KEY_MIME)
            val compatible = if(mime=="video/mp4") (vm in listOf("video/avc","video/hevc") || android.os.Build.VERSION.SDK_INT>=34 && vm=="video/av01") && am=="audio/mp4a-latm"
                else mime=="video/webm" && vm in listOf("video/x-vnd.on2.vp8","video/x-vnd.on2.vp9") && (am=="audio/vorbis" || android.os.Build.VERSION.SDK_INT>=29 && am=="audio/opus")
            if(!compatible) throw MediaFailure("MUX_UNSUPPORTED")
            videoReader.selectTrack(vi); audioReader.selectTrack(ai)
            val writer=MediaMuxer(output.absolutePath, if(mime=="video/mp4") MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 else MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM); muxer=writer
            val vout=writer.addTrack(vf); val aout=writer.addTrack(af)
            if(mime=="video/mp4" && vf.containsKey("rotation-degrees")) writer.setOrientationHint(vf.getInteger("rotation-degrees"))
            writer.start(); started=true
            val buffer=ByteBuffer.allocateDirect(8 * 1024 * 1024)
            val info=MediaCodec.BufferInfo()
            // Interleave by presentation time; media bytes stay on disk except one bounded sample.
            while(videoReader.sampleTime>=0 || audioReader.sampleTime>=0) {
                currentCoroutineContext().ensureActive()
                val useVideo=audioReader.sampleTime<0 || videoReader.sampleTime>=0 && videoReader.sampleTime<=audioReader.sampleTime
                val reader=if(useVideo) videoReader else audioReader
                buffer.clear()
                val count=reader.readSampleData(buffer,0)
                if(count<0) break
                if(count>buffer.capacity() || reader.sampleFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED != 0) throw MediaFailure("MUX_UNSUPPORTED")
                info.set(0,count,reader.sampleTime,if(reader.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                writer.writeSampleData(if(useVideo) vout else aout,buffer,info); reader.advance()
            }
            writer.stop(); started=false
        } catch(e: Throwable) { output.delete(); throw e }
        finally {
            if(started) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }; videoReader.release(); audioReader.release()
        }
    }
}
