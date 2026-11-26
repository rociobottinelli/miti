package ar.edu.uade.miti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ar.edu.uade.miti.data.sync.ProgramadorDeSync
import ar.edu.uade.miti.data.sync.SYNC_EN_PANTALLA
import ar.edu.uade.miti.data.sync.repetirCada
import ar.edu.uade.miti.presentation.DestinoRaiz
import ar.edu.uade.miti.presentation.RaizDeMiti
import ar.edu.uade.miti.presentation.RaizViewModel
import ar.edu.uade.miti.presentation.theme.MitiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var programadorDeSync: ProgramadorDeSync

    private val raiz: RaizViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash del sistema con el logo (SplashScreen API); tiene que ir antes de super.onCreate.
        // Se queda hasta saber dónde arranca la app (onboarding, cuenta, hogar o Hoy), sin pantallazos.
        installSplashScreen().setKeepOnScreenCondition { raiz.destino.value == DestinoRaiz.Cargando }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MitiTheme {
                RaizDeMiti(raiz)
            }
        }
        // Preentrega, sección 14: se sincroniza al abrir la app y cada vez que vuelve a primer plano. Además,
        // mientras está en pantalla, cada 15 s, para ver enseguida lo que cambia el otro celular (C1).
        // repeatOnLifecycle corta el ciclo en onStop y lo vuelve a arrancar en onStart (simetría, sin fugas).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repetirCada(SYNC_EN_PANTALLA) { programadorDeSync.sincronizarAhora() }
            }
        }
    }
}
