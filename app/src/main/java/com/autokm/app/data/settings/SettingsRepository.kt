package com.autokm.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "einstellungen")

class SettingsRepository(private val context: Context) {
    val tarifChfProKm = context.dataStore.data.map { prefs ->
        prefs[TARIF_CHF_PRO_KM] ?: STANDARD_TARIF_CHF_PRO_KM
    }

    suspend fun setzeTarifChfProKm(tarif: Double) {
        context.dataStore.edit { prefs -> prefs[TARIF_CHF_PRO_KM] = tarif }
    }

    companion object {
        const val STANDARD_TARIF_CHF_PRO_KM = 0.30
        private val TARIF_CHF_PRO_KM = doublePreferencesKey("tarif_chf_pro_km")
    }
}
