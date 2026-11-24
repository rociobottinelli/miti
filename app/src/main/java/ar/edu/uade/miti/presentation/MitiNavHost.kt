package ar.edu.uade.miti.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ar.edu.uade.miti.R
import ar.edu.uade.miti.core.navigation.Ruta
import ar.edu.uade.miti.presentation.ajustes.AjustesScreen
import ar.edu.uade.miti.presentation.balance.BalanceScreen
import ar.edu.uade.miti.presentation.gastos.FormularioDeGastoScreen
import ar.edu.uade.miti.presentation.compras.ComprasScreen
import ar.edu.uade.miti.presentation.hoy.HoyScreen
import ar.edu.uade.miti.presentation.theme.MitiTheme

/** Destinos de la barra inferior (preentrega, sección 11). */
enum class Destino(val ruta: Ruta, @param:StringRes val etiqueta: Int, @param:DrawableRes val icono: Int) {
    HOY(Ruta.Hoy, R.string.destino_hoy, R.drawable.ic_hoy),
    COMPRAS(Ruta.Compras, R.string.destino_compras, R.drawable.ic_compras),
    BALANCE(Ruta.Balance, R.string.destino_balance, R.drawable.ic_balance),
}

@Composable
fun MitiNavHost() {
    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val destinoActual = entradaActual?.destination

    // La barra inferior solo en las tres pestañas: los formularios ocupan la pantalla entera.
    val enPestania = Destino.entries.any { destinoActual?.hasRoute(it.ruta::class) == true }

    Scaffold(
        bottomBar = {
            if (enPestania) NavigationBar {
                Destino.entries.forEach { destino ->
                    NavigationBarItem(
                        selected = destinoActual?.hasRoute(destino.ruta::class) == true,
                        onClick = {
                            irAPestania(navController, destino.ruta)
                        },
                        icon = { Icon(painterResource(destino.icono), contentDescription = null) },
                        label = { Text(stringResource(destino.etiqueta)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Ruta.Hoy,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Ruta.Hoy> {
                HoyScreen(
                    // La compra se agrega en su pestaña; el gasto abre su formulario (#14).
                    alAgregarCompra = { irAPestania(navController, Ruta.Compras) },
                    alAgregarGasto = { navController.navigate(Ruta.FormularioDeGasto()) },
                    alAbrirAjustes = { navController.navigate(Ruta.Ajustes) { launchSingleTop = true } },
                )
            }
            composable<Ruta.Compras> { ComprasScreen() }
            composable<Ruta.Balance> {
                BalanceScreen(
                    alRegistrarGasto = { navController.navigate(Ruta.FormularioDeGasto()) },
                    alEditarGasto = { id -> navController.navigate(Ruta.FormularioDeGasto(id)) },
                )
            }
            composable<Ruta.FormularioDeGasto> {
                FormularioDeGastoScreen(alCerrar = { navController.popBackStack() })
            }
            composable<Ruta.Ajustes> { AjustesScreen(alVolver = { navController.popBackStack() }) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MitiAppPreview() {
    MitiTheme {
        MitiNavHost()
    }
}

/** Cambiar de pestaña no apila pantallas y conserva el estado de cada una. */
private fun irAPestania(navController: NavHostController, ruta: Ruta) {
    navController.navigate(ruta) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
