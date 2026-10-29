package com.jameel.jvideodownloader

import com.jameel.jvideodownloader.extractor.FormatCatalog
import com.jameel.jvideodownloader.domain.MediaFailure
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=android.app.Application::class)
class FormatCatalogTest {
    @Test fun offersOnlyActualSecureCompatibleFormats() {
        val json=JSONObject("""{"title":"Own test clip","duration":1,"formats":[
          {"format_id":"18","protocol":"https","ext":"mp4","height":360,"vcodec":"avc1.42001e","acodec":"mp4a.40.2","url":"https://cdn.example.com/360.mp4?sig=temporary","filesize":100},
          {"format_id":"137","protocol":"https","ext":"mp4","height":1080,"vcodec":"avc1.640028","acodec":"none","url":"https://cdn.example.com/video.mp4?sig=temporary","filesize":200},
          {"format_id":"140","protocol":"https","ext":"m4a","vcodec":"none","acodec":"mp4a.40.2","abr":128,"url":"https://cdn.example.com/audio.m4a?sig=temporary","filesize":50},
          {"format_id":"248","protocol":"https","ext":"webm","height":2160,"vcodec":"vp09.00.51.08","acodec":"none","url":"https://cdn.example.com/video.webm"},
          {"format_id":"251","protocol":"https","ext":"webm","vcodec":"none","acodec":"opus","abr":160,"url":"https://cdn.example.com/audio.webm"},
          {"format_id":"bad","protocol":"http","ext":"mp4","height":720,"vcodec":"avc1","acodec":"mp4a","url":"http://cdn.example.com/unsafe"},
          {"format_id":"drm","protocol":"https","has_drm":true,"ext":"mp4","height":1440,"vcodec":"avc1","acodec":"mp4a","url":"https://cdn.example.com/drm"}
        ]}""")
        val media=FormatCatalog.parse("https://youtube.com/watch?v=own-test",json)
        assertTrue(media.choices.any { it.id=="18" && it.size==100L })
        assertTrue(media.choices.any { it.id=="137+140" && it.parts.size==2 && it.size==250L })
        assertTrue(media.choices.any { it.id=="248+251" && it.mime=="video/webm" })
        assertTrue(media.choices.any { it.id=="140" && it.audioOnly })
        assertFalse(media.choices.any { it.id in listOf("bad","drm") })
        assertNull(media.selected())
        assertEquals("137+140",media.copy(selectedId="137+140").selected()!!.id)
    }
    @Test fun rejectsPrivateAndAdaptiveOnlyUrls() {
        for(source in listOf(
            """{"title":"test","formats":[{"format_id":"a","protocol":"https","ext":"mp4","vcodec":"avc1","acodec":"mp4a","height":720,"url":"https://127.0.0.1/private"}]}""",
            """{"title":"test","formats":[{"format_id":"a","protocol":"m3u8_native","ext":"mp4","vcodec":"avc1","acodec":"mp4a","height":720,"url":"https://cdn.example.com/list.m3u8"}]}"""
        )) {
            try { FormatCatalog.parse("https://x.com/own/status/1",JSONObject(source)); fail("must reject") }
            catch(e: MediaFailure) { assertEquals("NO_FORMATS",e.code) }
        }
    }
    @Test fun refusesLiveOrDrmMetadata() {
        for(key in listOf("is_live","has_drm")) {
            try { FormatCatalog.parse("https://youtube.com/watch?v=own-test",JSONObject().put(key,true)); fail("must reject") }
            catch(e: MediaFailure) { assertEquals("UNSUPPORTED_MEDIA",e.code) }
        }
    }
}
