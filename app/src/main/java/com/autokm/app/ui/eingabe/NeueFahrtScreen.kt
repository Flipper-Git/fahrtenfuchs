package com.autokm.app.ui.eingabe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autokm.app.data.AppDatabase
import com.autokm.app.data.model.Ort
import com.autokm.app.routing.RoutingRepository
import com.autokm.app.routing.RoutingStatus

@Composable
fun NeueFahrtScreen(
    database: AppDatabase,
    routingRepository: RoutingRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: NeueFahrtViewModel = viewModel(
        factory = NeueFahrtViewModel.Factory(database, routingRepository),
    )
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "%.1f km".format(state.km),
            style = MaterialTheme.typography.displayMedium,
        )

        val modiReihenfolge = EingabeModus.entries
        TabRow(selectedTabIndex = modiReihenfolge.indexOf(state.modus)) {
            modiReihenfolge.forEach { modus ->
                Tab(
                    selected = state.modus == modus,
                    onClick = { viewModel.modusWechseln(modus) },
                    text = { Text(modusLabel(modus)) },
                )
            }
        }

        when (state.modus) {
            EingabeModus.DREHRAD -> DrehradEingabe(viewModel)
            EingabeModus.MANUELL -> ManuelleEingabe(state, viewModel)
            EingabeModus.ORTE -> OrteEingabe(state, viewModel)
            EingabeModus.SCHNELLWAHL -> SchnellwahlEingabe(viewModel)
        }

        TextButton(onClick = { viewModel.kmZuruecksetzen() }) {
            Text("km zurücksetzen")
        }

        OutlinedTextField(
            value = state.tankkostenText,
            onValueChange = viewModel::tankkostenGeaendert,
            label = { Text("Tankkosten (CHF)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.notiz,
            onValueChange = viewModel::notizGeaendert,
            label = { Text("Notiz (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { viewModel.fahrtSpeichern() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Fahrt eingeben")
        }

        state.gespeichertHinweis?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun modusLabel(modus: EingabeModus) = when (modus) {
    EingabeModus.DREHRAD -> "Drehrad"
    EingabeModus.MANUELL -> "Manuell"
    EingabeModus.ORTE -> "Orte"
    EingabeModus.SCHNELLWAHL -> "Schnellwahl"
}

@Composable
private fun DrehradEingabe(viewModel: NeueFahrtViewModel) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Drehrad(onDrehung = { delta -> viewModel.drehradGedreht(delta) })
    }
}

@Composable
private fun ManuelleEingabe(state: NeueFahrtUiState, viewModel: NeueFahrtViewModel) {
    OutlinedTextField(
        value = state.manuelleEingabeText,
        onValueChange = viewModel::manuelleEingabeGeaendert,
        label = { Text("Kilometer") },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SchnellwahlEingabe(viewModel: NeueFahrtViewModel) {
    val kacheln = listOf(200, 100, 50, 20, 10, 5, 2, 1)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(kacheln) { betrag ->
            AssistChip(
                onClick = { viewModel.schnellwahlAddieren(betrag) },
                label = { Text("+$betrag") },
            )
        }
    }
}

@Composable
private fun OrteEingabe(state: NeueFahrtUiState, viewModel: NeueFahrtViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VonNachAuswahl(state.vonOrt, state.nachOrt, onZuruecksetzen = { viewModel.orteZuruecksetzen() })

        val statusText = when (val status = state.routingStatus) {
            is RoutingStatus.NichtGeladen -> null
            is RoutingStatus.WirdHeruntergeladen -> "Lädt Routing-Daten: ${status.fortschrittProzent}%"
            is RoutingStatus.Bereit -> null
            is RoutingStatus.Fehler -> "Fehler: ${status.nachricht}"
        }
        statusText?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        OutlinedTextField(
            value = state.orteSucheText,
            onValueChange = viewModel::orteSucheGeaendert,
            label = { Text("Ort suchen") },
            modifier = Modifier.fillMaxWidth(),
        )

        val anzuzeigendeOrte = if (state.orteSucheText.isBlank()) state.letzteOrte else state.orteSucheErgebnisse
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
            items(anzuzeigendeOrte.take(10)) { ort ->
                Card(
                    onClick = { viewModel.ortAusgewaehlt(ort) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                ) {
                    Text(
                        text = ort.anzeigeName,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun VonNachAuswahl(vonOrt: Ort?, nachOrt: Ort?, onZuruecksetzen: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        AssistChip(onClick = {}, label = { Text(vonOrt?.anzeigeName ?: "Von-Ort wählen") })
        Text("→")
        AssistChip(onClick = {}, label = { Text(nachOrt?.anzeigeName ?: "Nach-Ort wählen") })
        if (vonOrt != null || nachOrt != null) {
            TextButton(onClick = onZuruecksetzen) { Text("×") }
        }
    }
}
