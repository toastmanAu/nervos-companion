package com.example.nervoscompanion.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NewsDao {
  @Query("SELECT * FROM news_items ORDER BY publishedAt DESC")
  suspend fun getAllNews(): List<NewsEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNews(news: List<NewsEntity>)

  @Query("DELETE FROM news_items")
  suspend fun deleteAllNews()
}

@Dao
interface EcosystemAppDao {
  @Query("SELECT * FROM ecosystem_apps")
  suspend fun getAllApps(): List<EcosystemAppEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertApps(apps: List<EcosystemAppEntity>)

  @Query("DELETE FROM ecosystem_apps")
  suspend fun deleteAllApps()
}

@Dao
interface ChainStatsDao {
  @Query("SELECT * FROM chain_stats WHERE id = 1 LIMIT 1")
  suspend fun getStats(): ChainStatsEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStats(stats: ChainStatsEntity)
}
