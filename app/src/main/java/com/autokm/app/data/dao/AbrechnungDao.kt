package com.autokm.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.autokm.app.data.model.Abrechnung
import kotlinx.coroutines.flow.Flow

@Dao
interface AbrechnungDao {
    @Insert
    suspend fun insert(abrechnung: Abrechnung): Long

    @Query("SELECT * FROM abrechnungen ORDER BY monat DESC")
    fun alle(): Flow<List<Abrechnung>>

    @Query("SELECT * FROM abrechnungen WHERE id = :id")
    suspend fun findeNachId(id: Long): Abrechnung?
}
