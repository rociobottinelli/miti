package ar.edu.uade.miti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import ar.edu.uade.miti.data.sync.ProgramadorDeSync
import ar.edu.uade.miti.presentation.MitiNavHost
import ar.edu.uade.miti.presentation.theme.MitiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var programadorDeSync: ProgramadorDeSync

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MitiTheme {
                MitiNavHost()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Preentrega, sección 14: se sincroniza al abrir la app (y cada vez que vuelve a primer plano).
        lifecycleScope.launch { programadorDeSync.sincronizarAhora() }
    }
}
