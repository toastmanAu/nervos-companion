package com.example.nervoscompanion.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_items")
data class NewsEntity(
  @PrimaryKey val id: String,
  val source: String,
  val title: String,
  val summary: String,
  val url: String,
  val publishedAt: Long,
  val tags: List<String>
)

@Entity(tableName = "ecosystem_apps")
data class EcosystemAppEntity(
  @PrimaryKey val name: String,
  val description: String,
  val websiteUrl: String,
  val twitterUrl: String?,
  val githubUrl: String?,
  val discordUrl: String?,
  val bannerGradientColors: List<Long>
)

@Entity(tableName = "chain_stats")
data class ChainStatsEntity(
  @PrimaryKey val id: Int = 1,
  val blockNumber: Long?,
  val epochNumber: Long?,
  val epochProgress: String?,
  val nodeVersion: String?,
  val ckbPrice: Double?,
  val ckbChange: Double?,
  val ckbMarketCap: Double?,
  val ckbVolume: Double?,
  val priceHistory: String? = null,
  val lastUpdated: Long
)
