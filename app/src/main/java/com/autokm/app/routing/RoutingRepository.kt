package com.autokm.app.routing

import android.content.Context
import com.graphhopper.GHRequest
import com.graphhopper.GraphHopper
import com.graphhopper.config.CHProfile
import com.graphhopper.config.Profile
import com.graphhopper.jackson.Jackson
import com.graphhopper.storage.DAType
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

    private val formatVersionDatei = File(graphParentDir, "format-version.txt")

    fun istBereitLokal(): Boolean =
        File(graphCacheDir, "properties").exists() &&
            formatVersionDatei.exists() &&
            formatVersionDatei.readText().trim() == GRAPH_FORMAT_VERSION.toString()

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
        graphParentDir.deleteRecursively()
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
        formatVersionDatei.writeText(GRAPH_FORMAT_VERSION.toString())
    }

    private fun ladeGraphFallsNoetig() {
        if (hopper != null) return
        val neuerHopper = AndroidGraphHopper()
        neuerHopper.setGraphHopperLocation(graphCacheDir.absolutePath)
        neuerHopper.setEncodedValuesString("car_access, car_average_speed, road_access")
        neuerHopper.setProfiles(bauePassendesCarProfile())
        neuerHopper.getCHPreparationHandler().setCHProfiles(CHProfile(AndroidGraphHopper.CAR_PROFILE))
        erzwingeMmapSpeicher(neuerHopper)
        neuerHopper.load()
        hopper = neuerHopper
    }

    /**
     * GraphHopper validiert beim Laden Profile.getVersion() gegen den im Graphen gespeicherten
     * Hash - der hängt auch von der Einfüge-Reihenfolge der Profil-Hints ab (LinkedHashMap).
     * new Profile(name) setzt intern schon ein leeres custom_model als ERSTEN Hint, ein späteres
     * putHint("custom_model_files", ...) landet dadurch immer an zweiter statt erster Stelle -
     * egal in welcher Reihenfolge man die Setter aufruft. Der Import baut das Profil aus YAML
     * über Jackson (privater No-Arg-Konstruktor, kein Default-Hint), deshalb hier genauso: erst
     * per Jackson nur mit custom_model_files deserialisieren, dann custom_model nachträglich
     * setzen. Mit .github/workflows/debug-profile-hash.yml gegen den echten Graphen verifiziert
     * (ergibt exakt den gespeicherten Hash 26199302).
     */
    private fun bauePassendesCarProfile(): Profile {
        val roh = """{"name":"${AndroidGraphHopper.CAR_PROFILE}","custom_model_files":["car.json"]}"""
        val profil = Jackson.newObjectMapper().readValue(roh, Profile::class.java)
        return profil.setCustomModel(AndroidGraphHopper.buildCarCustomModel())
    }

    /**
     * GraphHopper hat keinen öffentlichen Setter für DAType.MMAP (nur für RAM/RAM_STORE über
     * setStoreOnFlush). RAM_STORE nutzt eine VarHandle-Methode, die auf Android nicht existiert
     * (NoSuchMethodError in RAMDataAccess.<clinit>), MMAP dagegen nur Standard-ByteBuffer - daher
     * hier direkt das private Feld setzen. Der Graph wurde entsprechend auch mit
     * graph.dataaccess.default_type: MMAP gebaut, siehe routing-build/config.yml.
     */
    private fun erzwingeMmapSpeicher(hopper: GraphHopper) {
        val feld = GraphHopper::class.java.getDeclaredField("dataAccessDefaultType")
        feld.isAccessible = true
        feld.set(hopper, DAType.MMAP)
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

        /** Hochzählen, wenn sich das Graph-Format ändert (z.B. anderer dataaccess.default_type),
         * damit bereits heruntergeladene, inkompatible lokale Graphen automatisch neu geladen werden. */
        private const val GRAPH_FORMAT_VERSION = 2
    }
}
