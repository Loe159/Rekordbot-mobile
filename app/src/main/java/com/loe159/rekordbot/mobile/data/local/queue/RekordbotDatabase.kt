package com.loe159.rekordbot.mobile.data.local.queue

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.loe159.rekordbot.mobile.data.local.shazam.ShazamConverters
import com.loe159.rekordbot.mobile.data.local.shazam.ShazamInboxDao
import com.loe159.rekordbot.mobile.data.local.shazam.ShazamInboxEntity
import com.loe159.rekordbot.mobile.domain.queue.QueueStatus
import org.json.JSONArray

@Database(
    entities = [QueuedTrackEntity::class, ShazamInboxEntity::class],
    version = 4,
    exportSchema = false,
)
@TypeConverters(QueueConverters::class, ShazamConverters::class)
abstract class RekordbotDatabase : RoomDatabase() {
    abstract fun queuedTrackDao(): QueuedTrackDao
    abstract fun shazamInboxDao(): ShazamInboxDao

    companion object {
        @Volatile
        private var instance: RekordbotDatabase? = null

        fun getInstance(context: Context): RekordbotDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                RekordbotDatabase::class.java,
                "rekordbot.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
                .also { instance = it }
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE queued_tracks ADD COLUMN source TEXT")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shazam_inbox_tracks` (" +
                        "`spotifyTrackId` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                        "`artist` TEXT NOT NULL, `spotifyUrl` TEXT NOT NULL, " +
                        "`albumName` TEXT, `artworkUrl` TEXT, `isrc` TEXT, " +
                        "`playlistAddedAtEpochMillis` INTEGER, " +
                        "`playlistPosition` INTEGER NOT NULL, `decision` TEXT NOT NULL, " +
                        "`decisionUpdatedAt` INTEGER, `firstSeenAt` INTEGER NOT NULL, " +
                        "`lastSeenAt` INTEGER NOT NULL, PRIMARY KEY(`spotifyTrackId`))",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_shazam_inbox_tracks_decision` " +
                        "ON `shazam_inbox_tracks` (`decision`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "`index_shazam_inbox_tracks_playlistAddedAtEpochMillis_playlistPosition_" +
                        "firstSeenAt` ON `shazam_inbox_tracks` " +
                        "(`playlistAddedAtEpochMillis`, `playlistPosition`, `firstSeenAt`)",
                )
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
