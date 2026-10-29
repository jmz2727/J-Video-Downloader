package com.jameel.jvideodownloader

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withPermit
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.jameel.jvideodownloader.downloads.DownloadQueue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], application=android.app.Application::class)
class QueueTest {
    @Test fun oneTransferAtATimeAndCancelDoesNotBlockQueue() = runBlocking {
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().build())
        val queue = DownloadQueue()
        val running=AtomicInteger(); val maximum=AtomicInteger(); val completed=AtomicInteger()
        val jobs=(1..5).map { launch {
            queue.permit.withPermit {
                maximum.updateAndGet { maxOf(it, running.incrementAndGet()) }
                try { delay(10); completed.incrementAndGet() } finally { running.decrementAndGet() }
            }
        } }
        jobs[2].cancel(); jobs.joinAll()
        assertEquals(1,maximum.get()); assertEquals(4,completed.get())
    }
}
