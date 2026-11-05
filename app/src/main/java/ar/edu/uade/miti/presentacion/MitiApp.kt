package ar.edu.uade.miti.presentacion

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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentacion.balance.BalanceScreen
import ar.edu.uade.miti.presentacion.compras.ComprasScreen
import ar.edu.uade.miti.presentacion.hoy.HoyScreen
import ar.edu.uade.miti.presentacion.navegacion.Ruta
import ar.edu.uade.miti.presentacion.tema.MitiTheme

/** Destinos de la barra inferior (preentrega, sección 11). */
enum class Destino(val ruta: Ruta, @param:StringRes val etiqueta: Int, @param:DrawableRes val icono: Int) {
    HOY(Ruta.Hoy, R.string.destino_hoy, R.drawable.ic_hoy),
    COMPRAS(Ruta.Compras, R.string.destino_compras, R.drawable.ic_compras),
    BALANCE(Ruta.Balance, R.string.destino_balance, R.drawable.ic_balance),
}

@Composable
fun MitiApp() {
    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val destinoActual = entradaActual?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destino.entries.forEach { destino ->
                    NavigationBarItem(
                        selected = destinoActual?.hasRoute(destino.ruta::class) == true,
                        onClick = {
                            navController.navigate(destino.ruta) {
                                // Cambiar de pestaña no apila pantallas y conserva el estado de cada una.
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
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
            composable<Ruta.Hoy> { HoyScreen() }
            composable<Ruta.Compras> { ComprasScreen() }
            composable<Ruta.Balance> { BalanceScreen() }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MitiAppPreview() {
    MitiTheme {
        MitiApp()
    }
}
