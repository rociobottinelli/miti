package ar.edu.uade.miti.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ar.edu.uade.miti.core.navigation.Ruta
import ar.edu.uade.miti.presentation.primeruso.CrearCuentaScreen
import ar.edu.uade.miti.presentation.primeruso.IngresarScreen
import ar.edu.uade.miti.presentation.primeruso.OnboardingScreen
import ar.edu.uade.miti.presentation.primeruso.PermisoDeNotificacionesScreen
import ar.edu.uade.miti.presentation.primeruso.TuHogarScreen

/**
 * Raíz de la app: el primer uso y, cuando está todo listo, la app con la barra inferior. Cada pantalla
 * es un destino de navegación, así sus ViewModels se descartan al salir (un formulario no queda con lo
 * de la sesión anterior si se cierra sesión y se vuelve a entrar).
 */
@Composable
fun RaizDeMiti(raiz: RaizViewModel) {
    val destino by raiz.destino.collectAsStateWithLifecycle()
    // El splash sigue en pantalla mientras tanto (MainActivity).
    if (destino == DestinoRaiz.Cargando) return

    val nav = rememberNavController()
    val inicial = remember { rutaDe(destino) }

    // Cuando cambia el destino (ingresó, creó el hogar, cerró sesión) se reemplaza todo lo anterior: no
    // tiene sentido volver con "atrás" a Ingresar después de entrar.
    LaunchedEffect(destino) { irA(nav, destino) }

    NavHost(navController = nav, startDestination = inicial) {
        composable<Ruta.Onboarding> { OnboardingScreen(alTerminar = raiz::terminarOnboarding) }
        composable<Ruta.Ingresar> { IngresarScreen(alCrearCuenta = { nav.navigate(Ruta.CrearCuenta) { launchSingleTop = true } }) }
        composable<Ruta.CrearCuenta> {
            val aIngresar = { if (!nav.popBackStack(Ruta.Ingresar, inclusive = false)) nav.navigate(Ruta.Ingresar) { popUpTo(0) } }
            CrearCuentaScreen(alVolver = aIngresar, alIrAIngresar = aIngresar)
        }
        composable<Ruta.TuHogar> {
            TuHogarScreen(alEmpezarACrear = raiz::empezarACrearHogar, alTerminar = raiz::terminarTuHogar)
        }
        composable<Ruta.PermisoDeNotificaciones> { PermisoDeNotificacionesScreen(alTerminar = raiz::resolverPermiso) }
        composable<Ruta.Principal> { MitiNavHost() }
    }
}

private fun rutaDe(destino: DestinoRaiz): Ruta = when (destino) {
    DestinoRaiz.Cargando, DestinoRaiz.Onboarding -> Ruta.Onboarding
    is DestinoRaiz.Cuenta -> Ruta.Ingresar
    DestinoRaiz.TuHogar -> Ruta.TuHogar
    DestinoRaiz.Permiso -> Ruta.PermisoDeNotificaciones
    DestinoRaiz.App -> Ruta.Principal
}

private fun irA(nav: NavHostController, destino: DestinoRaiz) {
    val ruta = rutaDe(destino)
    val actual = nav.currentDestination
    // Crear cuenta es parte del destino "Cuenta": no hay que sacar a la persona de ahí.
    val yaEsta = actual?.hasRoute(ruta::class) == true ||
        (destino is DestinoRaiz.Cuenta && actual?.hasRoute(Ruta.CrearCuenta::class) == true)
    if (!yaEsta) {
        nav.navigate(ruta) { popUpTo(0) { inclusive = true } }
    }
    if (destino is DestinoRaiz.Cuenta && destino.empezarCreando && actual?.hasRoute(Ruta.CrearCuenta::class) != true) {
        nav.navigate(Ruta.CrearCuenta) { launchSingleTop = true }
    }
}
