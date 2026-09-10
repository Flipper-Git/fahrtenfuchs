package com.autokm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.autokm.app.data.AppDatabase
import com.autokm.app.data.settings.SettingsRepository
import com.autokm.app.routing.RoutingRepository
import com.autokm.app.routing.RoutingStatus
import com.autokm.app.ui.theme.AutoKmTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.get(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)
        val routingRepository = RoutingRepository(applicationContext)
        val letzterAbsturz = CrashLogger.letzterAbsturz(applicationContext)

        setContent {
            AutoKmTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    DatenschichtCheckScreen(
                        database = database,
                        settingsRepository = settingsRepository,
                        routingRepository = routingRepository,
                        letzterAbsturz = letzterAbsturz,
                        onAbsturzLoeschen = { CrashLogger.loeschen(applicationContext) },
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}

// Zürich HB <-> Bern HB, nur zum Testen der Routing-Integration (Sprint 2).
private const val TEST_VON_LAT = 47.3779
private const val TEST_VON_LON = 8.5403
private const val TEST_NACH_LAT = 46.9489
private const val TEST_NACH_LON = 7.4405

@Composable
fun DatenschichtCheckScreen(
    database: AppDatabase,
    settingsRepository: SettingsRepository,
    routingRepository: RoutingRepository,
    modifier: Modifier = Modifier,
    letzterAbsturz: String? = null,
    onAbsturzLoeschen: () -> Unit = {},
) {
    val offeneSummeKm by database.fahrtDao().offeneSummeKm().collectAsState(initial = 0.0)
    val tarif by settingsRepository.tarifChfProKm.collectAsState(
        initial = SettingsRepository.STANDARD_TARIF_CHF_PRO_KM,
    )
    val anzahlOrte by database.ortDao().anzahlFlow().collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    var routingStatus by remember { mutableStateOf<RoutingStatus>(RoutingStatus.NichtGeladen) }
    var testDistanzKm by remember { mutableStateOf<Double?>(null) }
    var absturzText by remember { mutableStateOf(letzterAbsturz) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        absturzText?.let { absturz ->
            Text(
                text = "Letzter Absturz:",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = absturz,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = {
                onAbsturzLoeschen()
                absturzText = null
            }) {
                Text("Absturz-Log löschen")
            }
        }

        Text(
            text = "Fahrtenfuchs",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Sprint 2 – Ortsdaten geladen.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Offene km: $offeneSummeKm · Tarif: $tarif CHF/km",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Orte in Datenbank: $anzahlOrte",
            style = MaterialTheme.typography.bodySmall,
        )

        Button(onClick = {
            scope.launch {
                try {
                    routingRepository.sicherstellen { status -> routingStatus = status }
                    testDistanzKm = routingRepository.distanzKm(
                        TEST_VON_LAT, TEST_VON_LON, TEST_NACH_LAT, TEST_NACH_LON,
                    )
                } catch (e: Exception) {
                    routingStatus = RoutingStatus.Fehler(e.toString())
                }
            }
        }) {
            Text("Testroute Zürich–Bern berechnen")
        }

        val statusText = when (val status = routingStatus) {
            is RoutingStatus.NichtGeladen -> "Routing-Graph noch nicht geladen"
            is RoutingStatus.WirdHeruntergeladen -> "Lädt Routing-Graph: ${status.fortschrittProzent}%"
            is RoutingStatus.Bereit -> "Routing-Graph bereit"
            is RoutingStatus.Fehler -> "Fehler: ${status.nachricht}"
        }
        Text(text = statusText, style = MaterialTheme.typography.bodySmall)
        testDistanzKm?.let {
            Text(text = "Testdistanz: %.1f km".format(it), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DatenschichtCheckPreview() {
    AutoKmTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Fahrtenfuchs", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Sprint 2 – Ortsdaten geladen.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
