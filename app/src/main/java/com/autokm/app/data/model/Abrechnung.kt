package com.autokm.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Eine abgeschlossene monatliche Abrechnung. [tarifChfProKm] wird als Schnappschuss gespeichert,
 * damit eine spätere Tarifänderung alte Abrechnungen nicht rückwirkend verändert.
 */
@Entity(tableName = "abrechnungen")
data class Abrechnung(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val monat: String, // Format "YYYY-MM"
    val erstelltAm: Long = System.currentTimeMillis(),
    val summeKm: Double,
    val summeTankkostenChf: Double,
    val saldoChf: Double,
    val tarifChfProKm: Double,
)
