package com.jameel.jvideodownloader

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jameel.jvideodownloader.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class DatabaseTest {
    private lateinit var db: DownloadDatabase
    private lateinit var context: Context
    @Before fun open() { context = ApplicationProvider.getApplicationContext(); db = Room.inMemoryDatabaseBuilder(context, DownloadDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close() { db.close() }
    private fun row(id: String = "one", key: String = "hash") = DownloadRecord(id, "https://example.com/a.mp4", "Test", fileName="test.mp4", mime="video/mp4", activeKey=key)
    @Test fun stateAndProgressAreDurable() = runBlocking {
        val dao = db.downloads(); dao.insert(row()); assertEquals(1, dao.start("one"))
        dao.progress("one", 100, 200, 50)
        assertEquals(100L, dao.get("one")!!.bytes)
        assertEquals(1, dao.complete("one", "content://media/external/downloads/1", 200))
        assertEquals("COMPLETED", dao.observe().first().single().status)
        assertNull(dao.get("one")!!.activeKey)
    }
    @Test fun cancellationCannotBeResurrected() = runBlocking {
        val dao=db.downloads(); dao.insert(row()); dao.start("one"); dao.finish("one", "CANCELLED")
        assertEquals(0, dao.complete("one", "content://media/a", 10)); assertEquals(0,dao.start("one"))
        assertEquals("CANCELLED",dao.get("one")!!.status)
    }
    @Test fun duplicateActiveUrlsAreAtomic() = runBlocking {
        db.downloads().insert(row())
        try { db.downloads().insert(row("two")); fail("duplicate") } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        db.downloads().finish("one", "FAILED")
        db.downloads().insert(row("two")); assertEquals(2,db.downloads().observe().first().size)
    }
    @Test fun clearPreservesActiveWork() = runBlocking {
        db.downloads().insert(row()); db.downloads().insert(row("two","key2")); db.downloads().finish("two","FAILED")
        db.downloads().clear(); assertEquals(listOf("one"), db.downloads().observe().first().map { it.id })
    }
    @Test fun retryHasBoundedStateAndError() = runBlocking {
        val dao = db.downloads(); dao.insert(row()); dao.start("one"); dao.requeue("one", "NETWORK")
        assertEquals("QUEUED",dao.get("one")!!.status); assertEquals("NETWORK",dao.get("one")!!.error)
        dao.finish("one","FAILED","NETWORK"); assertEquals(1,dao.retry("one","hash")); assertNull(dao.get("one")!!.error)
    }
    @Test fun historySurvivesDatabaseReopen() = runBlocking {
        val name="persistence-test.db"; context.deleteDatabase(name)
        var disk=Room.databaseBuilder(context,DownloadDatabase::class.java,name).allowMainThreadQueries().build()
        try {
            disk.downloads().insert(row()); disk.close()
            disk=Room.databaseBuilder(context,DownloadDatabase::class.java,name).allowMainThreadQueries().build()
            assertEquals("Test", disk.downloads().get("one")!!.title)
        } finally { disk.close(); context.deleteDatabase(name) }
    }
}
