package com.loe159.rekordbot.mobile.data.local.queue

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.loe159.rekordbot.mobile.domain.queue.QueueStatus
import org.json.JSONArray

@Database(
    entities = [QueuedTrackEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(QueueConverters::class)
abstract class RekordbotDatabase : RoomDatabase() {
    abstract fun queuedTrackDao(): QueuedTrackDao

    companion object {
        @Volatile
        private var instance: RekordbotDatabase? = null

        fun getInstance(context: Context): RekordbotDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                RekordbotDatabase::class.java,
                "rekordbot.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE queued_tracks ADD COLUMN energy INTEGER")
                database.execSQL(
                    "ALTER TABLE queued_tracks ADD COLUMN moods TEXT NOT NULL DEFAULT '[]'",
                )
                database.execSQL(
                    "ALTER TABLE queued_tracks ADD COLUMN situations TEXT NOT NULL DEFAULT '[]'",
                )
                database.execSQL(
                    "ALTER TABLE queued_tracks ADD COLUMN inspirationalDjs TEXT NOT NULL DEFAULT '[]'",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE queued_tracks ADD COLUMN isrc TEXT")
            }
        }
    }
}

class QueueConverters {
    @TypeConverter
    fun queueStatusToString(status: QueueStatus): String = status.name

    @TypeConverter
    fun stringToQueueStatus(value: String): QueueStatus = QueueStatus.valueOf(value)

    @TypeConverter
    fun stringListToJson(values: List<String>): String = JSONArray(values).toString()

    @TypeConverter
    fun jsonToStringList(value: String): List<String> {
        val json = JSONArray(value)
        return List(json.length()) { index -> json.getString(index) }
    }
}
