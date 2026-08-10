package com.loe159.rekordbot.mobile.data.local.queue

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.loe159.rekordbot.mobile.domain.queue.QueueStatus

@Database(
    entities = [QueuedTrackEntity::class],
    version = 1,
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
            ).build().also { instance = it }
        }
    }
}

class QueueConverters {
    @TypeConverter
    fun queueStatusToString(status: QueueStatus): String = status.name

    @TypeConverter
    fun stringToQueueStatus(value: String): QueueStatus = QueueStatus.valueOf(value)
}
