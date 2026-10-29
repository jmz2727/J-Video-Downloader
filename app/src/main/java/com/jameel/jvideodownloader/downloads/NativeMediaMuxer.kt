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
            if(videoReader.sampleTrackIndex<0 || audioReader.sampleTrackIndex<0) throw MediaFailure("INCOMPLETE")
            // AAC priming/B-frames can have negative PTS. Only sampleTrackIndex signals EOF.
            val earliest=minOf(0L,videoReader.sampleTime,audioReader.sampleTime)
            if(earliest==Long.MIN_VALUE) throw MediaFailure("MUX_UNSUPPORTED")
            val offset=-earliest
            val samples=IntArray(2)
            val lastPts=LongArray(2) { -1L }
            // Shift both tracks equally, preserving relative synchronization.
            while(videoReader.sampleTrackIndex>=0 || audioReader.sampleTrackIndex>=0) {
                currentCoroutineContext().ensureActive()
                val useVideo=audioReader.sampleTrackIndex<0 || videoReader.sampleTrackIndex>=0 && videoReader.sampleTime<=audioReader.sampleTime
                val reader=if(useVideo) videoReader else audioReader
                buffer.clear()
                val count=reader.readSampleData(buffer,0)
                if(count<=0) throw MediaFailure("INCOMPLETE")
                if(count>buffer.capacity() || reader.sampleFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED != 0) throw MediaFailure("MUX_UNSUPPORTED")
                if(reader.sampleTime>Long.MAX_VALUE-offset) throw MediaFailure("MUX_UNSUPPORTED")
                val timestamp=reader.sampleTime+offset
                val slot=if(useVideo) 0 else 1
                info.set(0,count,timestamp,if(reader.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                buffer.limit(count)
                writer.writeSampleData(if(useVideo) vout else aout,buffer,info)
                samples[slot]++; lastPts[slot]=maxOf(lastPts[slot],timestamp); reader.advance()
            }
            if(samples.any { it==0 }) throw MediaFailure("INCOMPLETE")
            if(mime=="video/mp4") {
                for((slot,format) in listOf(vf,af).withIndex()) {
                    if(format.containsKey(MediaFormat.KEY_DURATION)) {
                        val duration=format.getLong(MediaFormat.KEY_DURATION)
                        if(duration>0 && duration<Long.MAX_VALUE-offset) {
                            val end=duration+offset
                            if(end>lastPts[slot]) {
                                info.set(0,0,end,MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                writer.writeSampleData(if(slot==0) vout else aout,ByteBuffer.allocate(0),info)
                            }
                        }
                    }
                }
            }
            writer.stop(); started=false
        } catch(e: Throwable) { output.delete(); throw e }
        finally {
            if(started) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }; videoReader.release(); audioReader.release()
        }
    }
}
