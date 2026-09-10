package com.autokm.app.ui.eingabe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autokm.app.data.AppDatabase
import com.autokm.app.data.model.Eingabemethode
import com.autokm.app.data.model.Fahrt
import com.autokm.app.data.model.Ort
import com.autokm.app.routing.RoutingRepository
import com.autokm.app.routing.RoutingStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class EingabeModus { DREHRAD, MANUELL, ORTE, SCHNELLWAHL }

data class NeueFahrtUiState(
    val modus: EingabeModus = EingabeModus.DREHRAD,
    val km: Double = 0.0,
    val manuelleEingabeText: String = "",
    val tankkostenText: String = "",
    val notiz: String = "",
    val vonOrt: Ort? = null,
    val nachOrt: Ort? = null,
    val orteSucheText: String = "",
    val orteSucheErgebnisse: List<Ort> = emptyList(),
    val letzteOrte: List<Ort> = emptyList(),
    val routingStatus: RoutingStatus = RoutingStatus.NichtGeladen,
    val gespeichertHinweis: String? = null,
)

class NeueFahrtViewModel(
    private val database: AppDatabase,
    private val routingRepository: RoutingRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NeueFahrtUiState())
    val uiState: StateFlow<NeueFahrtUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            database.ortNutzungDao().letzteOrte().collectLatest { orte ->
                _uiState.value = _uiState.value.copy(letzteOrte = orte)
            }
        }
    }

    fun modusWechseln(modus: EingabeModus) {
        _uiState.value = _uiState.value.copy(modus = modus)
    }

    fun drehradGedreht(deltaKm: Double) {
        val neu = (_uiState.value.km + deltaKm).coerceAtLeast(0.0)
        _uiState.value = _uiState.value.copy(km = neu, modus = EingabeModus.DREHRAD)
    }

    fun manuelleEingabeGeaendert(text: String) {
        val bereinigt = text.replace(',', '.')
        val wert = bereinigt.toDoubleOrNull()
        _uiState.value = _uiState.value.copy(
            manuelleEingabeText = text,
            modus = EingabeModus.MANUELL,
            km = wert ?: _uiState.value.km,
        )
    }

    fun schnellwahlAddieren(betragKm: Int) {
        _uiState.value = _uiState.value.copy(
            km = _uiState.value.km + betragKm,
            modus = EingabeModus.SCHNELLWAHL,
        )
    }

    fun kmZuruecksetzen() {
        _uiState.value = _uiState.value.copy(km = 0.0, manuelleEingabeText = "")
    }

    fun tankkostenGeaendert(text: String) {
        _uiState.value = _uiState.value.copy(tankkostenText = text)
    }

    fun notizGeaendert(text: String) {
        _uiState.value = _uiState.value.copy(notiz = text)
    }

    fun orteSucheGeaendert(text: String) {
        _uiState.value = _uiState.value.copy(orteSucheText = text)
        viewModelScope.launch {
            database.ortDao().suche(text).collectLatest { treffer ->
                _uiState.value = _uiState.value.copy(orteSucheErgebnisse = treffer)
            }
        }
    }

    fun orteZuruecksetzen() {
        _uiState.value = _uiState.value.copy(vonOrt = null, nachOrt = null)
    }

    fun ortAusgewaehlt(ort: Ort) {
        viewModelScope.launch { database.ortNutzungDao().merkeNutzung(ort.id) }

        val aktuell = _uiState.value
        when {
            aktuell.vonOrt == null -> {
                _uiState.value = aktuell.copy(vonOrt = ort)
            }
            aktuell.nachOrt == null && ort.id != aktuell.vonOrt.id -> {
                _uiState.value = aktuell.copy(nachOrt = ort, modus = EingabeModus.ORTE)
                berechneDistanz(aktuell.vonOrt, ort)
            }
            else -> {
                _uiState.value = aktuell.copy(vonOrt = ort, nachOrt = null)
            }
        }
    }

    private fun berechneDistanz(von: Ort, nach: Ort) {
        viewModelScope.launch {
            routingRepository.sicherstellen { status ->
                _uiState.value = _uiState.value.copy(routingStatus = status)
            }
            val distanz = routingRepository.distanzKm(von.lat, von.lon, nach.lat, nach.lon)
            if (distanz != null) {
                _uiState.value = _uiState.value.copy(km = distanz)
            }
        }
    }

    fun fahrtSpeichern() {
        val aktuell = _uiState.value
        if (aktuell.km <= 0.0) return
        val tankkosten = aktuell.tankkostenText.replace(',', '.').toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            database.fahrtDao().insert(
                Fahrt(
                    datum = LocalDate.now().toEpochDay(),
                    km = aktuell.km,
                    tankkostenChf = tankkosten,
                    eingabemethode = when (aktuell.modus) {
                        EingabeModus.DREHRAD -> Eingabemethode.DREHRAD
                        EingabeModus.MANUELL -> Eingabemethode.MANUELL
                        EingabeModus.ORTE -> Eingabemethode.ORTE
                        EingabeModus.SCHNELLWAHL -> Eingabemethode.SCHNELLWAHL
                    },
                    vonOrtId = aktuell.vonOrt?.id,
                    nachOrtId = aktuell.nachOrt?.id,
                    notiz = aktuell.notiz.ifBlank { null },
                )
            )
            _uiState.value = NeueFahrtUiState(
                letzteOrte = aktuell.letzteOrte,
                gespeichertHinweis = "Fahrt gespeichert: %.1f km".format(aktuell.km),
            )
        }
    }

    fun hinweisGeleert() {
        _uiState.value = _uiState.value.copy(gespeichertHinweis = null)
    }

    class Factory(
        private val database: AppDatabase,
        private val routingRepository: RoutingRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NeueFahrtViewModel(database, routingRepository) as T
        }
    }
}
