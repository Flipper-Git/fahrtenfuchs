package com.autokm.app.data

import android.content.Context
import com.autokm.app.data.dao.OrtDao
import com.autokm.app.data.model.Ort
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Befüllt die Orte-Tabelle beim ersten Start aus der eingebetteten CSV
 * (amtliches Ortschaftenverzeichnis von swisstopo, aufbereitet auf
 * eine Zeile pro eindeutiger Ortschaft: Name,PLZ,Kanton,Lat,Lon).
 */
object OrtImporter {
    private const val ASSET_DATEI = "orte_schweiz.csv"

    suspend fun importiereFallsLeer(context: Context, ortDao: OrtDao) {
        if (ortDao.anzahl() > 0) return

        val orte = mutableListOf<Ort>()
        BufferedReader(InputStreamReader(context.assets.open(ASSET_DATEI), Charsets.UTF_8)).use { reader ->
            reader.forEachLine { zeile ->
                if (zeile.isBlank()) return@forEachLine
                val teile = zeile.split(",")
                if (teile.size != 5) return@forEachLine
                orte.add(
                    Ort(
                        name = teile[0],
                        plz = teile[1],
                        kanton = teile[2],
                        lat = teile[3].toDouble(),
                        lon = teile[4].toDouble(),
                    )
                )
            }
        }
        ortDao.insertAll(orte)
    }
}
