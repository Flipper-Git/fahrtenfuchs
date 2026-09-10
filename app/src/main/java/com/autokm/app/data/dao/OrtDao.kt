package com.autokm.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.autokm.app.data.model.Ort
import kotlinx.coroutines.flow.Flow

@Dao
interface OrtDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(orte: List<Ort>)

    @Query("SELECT COUNT(*) FROM orte")
    suspend fun anzahl(): Int

    @Query("SELECT COUNT(*) FROM orte")
    fun anzahlFlow(): Flow<Int>

    @Query(
        "SELECT * FROM orte WHERE name LIKE '%' || :suchtext || '%' " +
            "ORDER BY name LIMIT 50"
    )
    fun suche(suchtext: String): Flow<List<Ort>>

    @Query("SELECT * FROM orte WHERE id = :id")
    suspend fun findeNachId(id: Long): Ort?
}
