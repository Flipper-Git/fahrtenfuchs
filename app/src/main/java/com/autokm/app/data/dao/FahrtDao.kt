package com.autokm.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.autokm.app.data.model.Fahrt
import kotlinx.coroutines.flow.Flow

@Dao
interface FahrtDao {
    @Insert
    suspend fun insert(fahrt: Fahrt): Long

    @Update
    suspend fun update(fahrt: Fahrt)

    @Delete
    suspend fun delete(fahrt: Fahrt)

    @Query("SELECT * FROM fahrten WHERE abrechnungId IS NULL ORDER BY datum DESC")
    fun offeneFahrten(): Flow<List<Fahrt>>

    @Query("SELECT * FROM fahrten WHERE abrechnungId = :abrechnungId ORDER BY datum")
    fun fahrtenFuerAbrechnung(abrechnungId: Long): Flow<List<Fahrt>>

    @Query("UPDATE fahrten SET abrechnungId = :abrechnungId WHERE id IN (:fahrtIds)")
    suspend fun weiseAbrechnungZu(fahrtIds: List<Long>, abrechnungId: Long)

    @Query("SELECT COALESCE(SUM(km), 0) FROM fahrten WHERE abrechnungId IS NULL")
    fun offeneSummeKm(): Flow<Double>

    @Query("SELECT COALESCE(SUM(tankkostenChf), 0) FROM fahrten WHERE abrechnungId IS NULL")
    fun offeneSummeTankkosten(): Flow<Double>
}
