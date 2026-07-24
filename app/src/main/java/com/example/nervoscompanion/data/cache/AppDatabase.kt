package com.example.nervoscompanion.data.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
  entities = [NewsEntity::class, EcosystemAppEntity::class, ChainStatsEntity::class, CachedDaoCell::class],
  version = 5,
  exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

  abstract fun newsDao(): NewsDao
  abstract fun ecosystemAppDao(): EcosystemAppDao
  abstract fun chainStatsDao(): ChainStatsDao
  abstract fun daoCellDao(): DaoCellDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "nervos_companion_db"
        )
        .fallbackToDestructiveMigration()
        .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
