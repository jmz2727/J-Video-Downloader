package com.jameel.jvideodownloader.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "downloads", indices = [Index(value = ["activeKey"], unique = true)])
data class DownloadRecord(
    @PrimaryKey val id: String,
    val originalUrl: String,
    val title: String,
    val platform: String = "Direct",
    val fileName: String,
    val mime: String,
    val fileUri: String? = null,
    val status: String = "QUEUED",
    val total: Long = -1,
    val bytes: Long = 0,
    val speed: Long = 0,
    val created: Long = System.currentTimeMillis(),
    val quality: String = "Original",
    val error: String? = null,
    val activeKey: String? = null,
    val location: String? = null,
    val wifiOnly: Boolean = false,
    val allowHttp: Boolean = false,
    val stagingUri: String? = null,
    @ColumnInfo(defaultValue = "NULL") val selectionId: String? = null
)
@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY created DESC") fun observe(): Flow<List<DownloadRecord>>
    @Query("SELECT * FROM downloads WHERE id=:id") suspend fun get(id: String): DownloadRecord?
    @Insert suspend fun insert(row: DownloadRecord)
    @Query("UPDATE downloads SET status='RUNNING', bytes=0, speed=0, error=NULL WHERE id=:id AND status IN ('QUEUED','RUNNING')") suspend fun start(id: String): Int
    @Query("UPDATE downloads SET bytes=:bytes, total=:total, speed=:speed WHERE id=:id AND status='RUNNING'") suspend fun progress(id: String, bytes: Long, total: Long, speed: Long)
    @Query("UPDATE downloads SET stagingUri=:uri WHERE id=:id AND status='RUNNING'") suspend fun staging(id: String, uri: String?): Int
    @Query("UPDATE downloads SET status=:status, error=:error, activeKey=NULL, speed=0 WHERE id=:id AND status IN ('QUEUED','RUNNING')") suspend fun finish(id: String, status: String, error: String? = null): Int
    @Query("UPDATE downloads SET status='COMPLETED', fileUri=:uri, bytes=:bytes, total=:bytes, speed=0, activeKey=NULL, stagingUri=NULL WHERE id=:id AND status='RUNNING'") suspend fun complete(id: String, uri: String, bytes: Long): Int
    @Query("UPDATE downloads SET status='QUEUED', error=:error, speed=0 WHERE id=:id AND status='RUNNING'") suspend fun requeue(id: String, error: String)
    @Query("UPDATE downloads SET status='QUEUED', activeKey=:key, error=NULL, bytes=0, speed=0 WHERE id=:id AND status IN ('FAILED','CANCELLED')") suspend fun retry(id: String, key: String): Int
    @Query("DELETE FROM downloads WHERE id=:id AND status IN ('COMPLETED','FAILED','CANCELLED')") suspend fun remove(id: String)
    @Query("DELETE FROM downloads WHERE status IN ('COMPLETED','FAILED','CANCELLED')") suspend fun clear()
}
@Database(entities = [DownloadRecord::class], version = 2, exportSchema = true)
abstract class DownloadDatabase : RoomDatabase() { abstract fun downloads(): DownloadDao }

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloads ADD COLUMN selectionId TEXT DEFAULT NULL")
        // Older optional HTTP / external destination settings never apply to v2 jobs.
        db.execSQL("UPDATE downloads SET allowHttp=0, location=NULL")
    }
}
