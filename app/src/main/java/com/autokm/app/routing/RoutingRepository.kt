package com.autokm.app.routing

import android.content.Context
import com.graphhopper.GHRequest
import com.graphhopper.config.CHProfile
import com.graphhopper.config.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

sealed interface RoutingStatus {
    data object NichtGeladen : RoutingStatus
    data class WirdHeruntergeladen(val fortschrittProzent: Int) : RoutingStatus
    data object Bereit : RoutingStatus
    data class Fehler(val nachricht: String) : RoutingStatus
}

/**
 * Lädt den vorbereiteten Schweizer Offline-Routing-Graphen (gebaut über
 * .github/workflows/build-routing-data.yml) beim ersten Bedarf herunter, entpackt ihn lokal
 * und stellt darüber die Distanzberechnung zwischen zwei Orten bereit.
 */
class RoutingRepository(private val context: Context) {
    private val graphParentDir = File(context.filesDir, "routing-graph")
    private val graphCacheDir = File(graphParentDir, "graph-cache")
    private var hopper: AndroidGraphHopper? = null

    fun istBereitLokal(): Boolean = File(graphCacheDir, "properties").exists()

    suspend fun sicherstellen(onFortschritt: (RoutingStatus) -> Unit) {
        withContext(Dispatchers.IO) {
            if (!istBereitLokal()) {
                onFortschritt(RoutingStatus.WirdHeruntergeladen(0))
                try {
                    herunterladenUndEntpacken(onFortschritt)
                } catch (e: Exception) {
                    onFortschritt(RoutingStatus.Fehler(e.message ?: "Download fehlgeschlagen"))
                    return@withContext
                }
            }
            try {
                ladeGraphFallsNoetig()
                onFortschritt(RoutingStatus.Bereit)
            } catch (e: Exception) {
                onFortschritt(RoutingStatus.Fehler(e.message ?: "Graph konnte nicht geladen werden"))
            }
        }
    }

    private fun herunterladenUndEntpacken(onFortschritt: (RoutingStatus) -> Unit) {
        graphParentDir.mkdirs()
        val zipDatei = File(context.cacheDir, "graph-cache-schweiz.zip")
        val connection = URL(GRAPH_DOWNLOAD_URL).openConnection()
        connection.connect()
        val gesamtBytes = connection.contentLengthLong
        var geleseneBytes = 0L

        connection.getInputStream().use { input ->
            FileOutputStream(zipDatei).use { output ->
                val buffer = ByteArray(64 * 1024)
                var letzterProzent = -1
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    output.write(buffer, 0, n)
                    geleseneBytes += n
                    if (gesamtBytes > 0) {
                        val prozent = ((geleseneBytes * 100) / gesamtBytes).toInt()
                        if (prozent != letzterProzent) {
                            letzterProzent = prozent
                            onFortschritt(RoutingStatus.WirdHeruntergeladen(prozent))
                        }
                    }
                }
            }
        }

        ZipInputStream(zipDatei.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val zielDatei = File(graphParentDir, entry.name)
                if (entry.isDirectory) {
                    zielDatei.mkdirs()
                } else {
                    zielDatei.parentFile?.mkdirs()
                    FileOutputStream(zielDatei).use { out -> zip.copyTo(out) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        zipDatei.delete()
    }

    private fun ladeGraphFallsNoetig() {
        if (hopper != null) return
        val neuerHopper = AndroidGraphHopper()
        neuerHopper.setGraphHopperLocation(graphCacheDir.absolutePath)
        neuerHopper.setEncodedValuesString("car_access, car_average_speed, road_access")
        neuerHopper.setProfiles(
            Profile(AndroidGraphHopper.CAR_PROFILE)
                .setWeighting("custom")
                .setCustomModel(AndroidGraphHopper.buildCarCustomModel())
        )
        neuerHopper.getCHPreparationHandler().setCHProfiles(CHProfile(AndroidGraphHopper.CAR_PROFILE))
        neuerHopper.load()
        hopper = neuerHopper
    }

    /** Liefert die Straßendistanz in km, oder null wenn keine Route gefunden wurde. */
    suspend fun distanzKm(vonLat: Double, vonLon: Double, nachLat: Double, nachLon: Double): Double? =
        withContext(Dispatchers.Default) {
            val aktuellerHopper = hopper ?: return@withContext null
            val request = GHRequest(vonLat, vonLon, nachLat, nachLon).setProfile(AndroidGraphHopper.CAR_PROFILE)
            val response = aktuellerHopper.route(request)
            if (response.hasErrors()) null else response.best.distance / 1000.0
        }

    companion object {
        private const val GRAPH_DOWNLOAD_URL =
            "https://github.com/Flipper-Git/fahrtenfuchs/releases/download/routing-data-ch/graph-cache-schweiz.zip"
    }
}
