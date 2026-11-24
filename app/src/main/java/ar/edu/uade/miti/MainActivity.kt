package ar.edu.uade.miti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import ar.edu.uade.miti.data.sync.ProgramadorDeSync
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
    }

    override fun onStart() {
        super.onStart()
        // Preentrega, sección 14: se sincroniza al abrir la app (y cada vez que vuelve a primer plano).
        lifecycleScope.launch { programadorDeSync.sincronizarAhora() }
    }
}
