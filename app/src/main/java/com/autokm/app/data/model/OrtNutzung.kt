package com.autokm.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Merkt sich, wann ein Ort zuletzt bei der Distanzberechnung ausgewählt wurde. */
@Entity(tableName = "ort_nutzung")
data class OrtNutzung(
    @PrimaryKey val ortId: Long,
    val letzteNutzung: Long,
)
