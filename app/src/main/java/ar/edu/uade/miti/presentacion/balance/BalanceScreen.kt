package ar.edu.uade.miti.presentacion.balance

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ar.edu.uade.miti.presentacion.componentes.PantallaEnConstruccion

@Composable
fun BalanceScreen(modifier: Modifier = Modifier) {
    PantallaEnConstruccion(
        titulo = "Balance",
        descripcion = "Quién le debe a quién este mes, gastos, cuotas y pagos (RF04).",
        modifier = modifier,
    )
}
