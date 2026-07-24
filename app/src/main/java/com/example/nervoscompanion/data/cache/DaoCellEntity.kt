package com.example.nervoscompanion.data.cache

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_dao_cells", primaryKeys = ["address", "txHash", "index"])
data class CachedDaoCell(
  val address: String,
  val txHash: String,
  val index: Int,
  val status: String, // "deposited" | "withdrawing"
  val totalCapacity: String, // Stored as String for BigInteger compatibility
  val occupiedCapacity: String,
  val depositBlockNumber: Long,
  val withdrawBlockNumber: Long?,
  val blockNumber: Long,
  val accruedYield: String, // Stored as String for BigInteger compatibility
  val nextBoundary: Double,
  val remainingEpochs: Double,
  val remainingDays: Double,
  val progress: Double,
  val cycleIndex: Int,
  val isMatured: Boolean,
  val depositEpochFraction: Double,
  val withdrawEpochFraction: Double?,
  val lastUpdated: Long
)

@Dao
interface DaoCellDao {

  @Query("SELECT * FROM cached_dao_cells WHERE address = :address")
  fun getCellsForAddress(address: String): Flow<List<CachedDaoCell>>

  @Query("SELECT * FROM cached_dao_cells WHERE address = :address")
  suspend fun getCellsForAddressSync(address: String): List<CachedDaoCell>

  @Query("DELETE FROM cached_dao_cells WHERE address = :address")
  suspend fun deleteCellsForAddress(address: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCells(cells: List<CachedDaoCell>)

  @Transaction
  suspend fun updateCellsForAddress(address: String, cells: List<CachedDaoCell>) {
    deleteCellsForAddress(address)
    if (cells.isNotEmpty()) {
      insertCells(cells)
    }
  }
}
