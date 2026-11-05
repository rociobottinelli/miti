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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentacion.balance.BalanceScreen
import ar.edu.uade.miti.presentacion.compras.ComprasScreen
import ar.edu.uade.miti.presentacion.hoy.HoyScreen
import ar.edu.uade.miti.presentacion.tema.MitiTheme

/** Destinos de la barra inferior (preentrega, sección 11). */
enum class Destino(@StringRes val etiqueta: Int, @DrawableRes val icono: Int) {
    HOY(R.string.destino_hoy, R.drawable.ic_hoy),
    COMPRAS(R.string.destino_compras, R.drawable.ic_compras),
    BALANCE(R.string.destino_balance, R.drawable.ic_balance),
}

// TODO: reemplazar el estado local por Navigation Compose cuando se sumen las dependencias.
@Composable
fun MitiApp() {
    var destinoActual by rememberSaveable { mutableStateOf(Destino.HOY) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destino.entries.forEach { destino ->
                    NavigationBarItem(
                        selected = destino == destinoActual,
                        onClick = { destinoActual = destino },
                        icon = { Icon(painterResource(destino.icono), contentDescription = null) },
                        label = { Text(stringResource(destino.etiqueta)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (destinoActual) {
            Destino.HOY -> HoyScreen(modifier)
            Destino.COMPRAS -> ComprasScreen(modifier)
            Destino.BALANCE -> BalanceScreen(modifier)
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
