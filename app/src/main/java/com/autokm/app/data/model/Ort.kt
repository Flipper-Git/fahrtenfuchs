package com.autokm.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Eine Schweizer Ortschaft aus dem amtlichen PLZ-Verzeichnis.
 * Eindeutigkeit für die Auswahl entsteht durch die Kombination
 * von Name, PLZ und Kanton (z.B. "Buchs (9471, SG)" vs. "Buchs (5033, AG)").
 */
@Entity(tableName = "orte")
data class Ort(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val plz: String,
    val kanton: String,
    val lat: Double,
    val lon: Double,
) {
    val anzeigeName: String
        get() = "$name ($plz, $kanton)"
}
