package com.autokm.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Eine einzelne Fahrt. [vonOrtId]/[nachOrtId] sind unabhängig von [eingabemethode] immer optional:
 * bei ORTE liefern sie zugleich die berechnete Distanz, bei den übrigen Methoden sind sie reine
 * Notiz-Zusatzinfo. [abrechnungId] ist null, solange die Fahrt noch nicht abgerechnet wurde.
 */
@Entity(tableName = "fahrten")
data class Fahrt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val datum: Long, // Epoch-Tag (LocalDate.toEpochDay())
    val km: Double,
    val tankkostenChf: Double = 0.0,
    val eingabemethode: Eingabemethode,
    val vonOrtId: Long? = null,
    val nachOrtId: Long? = null,
    val notiz: String? = null,
    val abrechnungId: Long? = null,
    val erstelltAm: Long = System.currentTimeMillis(),
)
