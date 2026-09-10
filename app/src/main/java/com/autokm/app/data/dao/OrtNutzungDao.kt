package com.autokm.app.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.autokm.app.data.model.Ort
import kotlinx.coroutines.flow.Flow

@Dao
interface OrtNutzungDao {
    @Query("INSERT OR REPLACE INTO ort_nutzung (ortId, letzteNutzung) VALUES (:ortId, :zeitpunkt)")
    suspend fun merkeNutzung(ortId: Long, zeitpunkt: Long = System.currentTimeMillis())

    @Query(
        """
        SELECT orte.* FROM orte
        INNER JOIN ort_nutzung ON orte.id = ort_nutzung.ortId
        ORDER BY ort_nutzung.letzteNutzung DESC
        LIMIT 10
        """
    )
    fun letzteOrte(): Flow<List<Ort>>
}
