package fi.thl.koronahaavi.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyGroupTokenDao {

    @Query("SELECT * FROM key_group_token")
    suspend fun getAll(): List<KeyGroupToken>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(token: KeyGroupToken)

    @Query("SELECT * FROM key_group_token WHERE exposure_count > 0 OR day_count > 0")
    fun getExposureTokensFlow(): Flow<List<KeyGroupToken>>

    @Delete
    fun delete(vararg tokens: KeyGroupToken)

    @Query("DELETE FROM key_group_token")
    suspend fun deleteAll()
}