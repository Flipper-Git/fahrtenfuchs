package com.autokm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.autokm.app.data.AppDatabase
import com.autokm.app.data.settings.SettingsRepository
import com.autokm.app.ui.theme.AutoKmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.get(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)

        setContent {
            AutoKmTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    DatenschichtCheckScreen(
                        database = database,
                        settingsRepository = settingsRepository,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}

@Composable
fun DatenschichtCheckScreen(
    database: AppDatabase,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val offeneSummeKm by database.fahrtDao().offeneSummeKm().collectAsState(initial = 0.0)
    val tarif by settingsRepository.tarifChfProKm.collectAsState(
        initial = SettingsRepository.STANDARD_TARIF_CHF_PRO_KM,
    )
    val anzahlOrte by database.ortDao().anzahlFlow().collectAsState(initial = 0)

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
