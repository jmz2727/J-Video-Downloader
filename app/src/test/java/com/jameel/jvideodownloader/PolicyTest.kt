package com.jameel.jvideodownloader
import com.jameel.jvideodownloader.core.MediaPolicy
import org.junit.Assert.*
import org.junit.Test
class PolicyTest {
    @Test fun validation() {
        assertEquals("media.example.com", MediaPolicy.validate("https://media.example.com/a.mp4", false).host)
        listOf("file:///tmp/a", "https://user:pass@example.com/a", "https://localhost/a", "http://example.com/a", "https://example.com/a?token=x").forEach { url -> assertThrows(IllegalArgumentException::class.java) { MediaPolicy.validate(url, false) } }
    }
    @Test fun platformIdentity() { assertEquals("X", MediaPolicy.platform("https://x.com/a")); assertEquals("Direct", MediaPolicy.platform("https://youtube.com.evil.com/a")) }
    @Test fun filename() { assertEquals("_a_b", MediaPolicy.sanitize("../a:b")); assertEquals("media", MediaPolicy.sanitize("...")) }
    @Test fun transitionAndRetry() { assertFalse(MediaPolicy.canTransition("CANCELLED", "COMPLETED")); assertFalse(MediaPolicy.retryHttp(403)); assertTrue(MediaPolicy.retryHttp(503)); assertFalse(MediaPolicy.mayRetry(3)) }
}
