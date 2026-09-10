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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autokm.app.data.AppDatabase
import com.autokm.app.routing.RoutingRepository
import com.autokm.app.ui.eingabe.NeueFahrtScreen
import com.autokm.app.ui.theme.AutoKmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.get(applicationContext)
        val routingRepository = RoutingRepository(applicationContext)
        val letzterAbsturz = CrashLogger.letzterAbsturz(applicationContext)

        setContent {
            AutoKmTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Column(modifier = Modifier.padding(padding)) {
                        AbsturzHinweis(
                            letzterAbsturz = letzterAbsturz,
                            onLoeschen = { CrashLogger.loeschen(applicationContext) },
                        )
                        NeueFahrtScreen(
                            database = database,
                            routingRepository = routingRepository,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AbsturzHinweis(letzterAbsturz: String?, onLoeschen: () -> Unit) {
    var absturzText by remember { mutableStateOf(letzterAbsturz) }
    absturzText?.let { absturz ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Letzter Absturz:", style = MaterialTheme.typography.titleSmall)
            Text(text = absturz, style = MaterialTheme.typography.bodySmall)
            Button(onClick = {
                onLoeschen()
                absturzText = null
            }) {
                Text("Absturz-Log löschen")
            }
        }
    }
}
