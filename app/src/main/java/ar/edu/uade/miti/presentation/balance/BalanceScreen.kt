package ar.edu.uade.miti.presentation.balance

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ar.edu.uade.miti.presentation.components.PantallaEnConstruccion

@Composable
fun BalanceScreen(modifier: Modifier = Modifier) {
    PantallaEnConstruccion(
        titulo = "Balance",
        descripcion = "Quién le debe a quién este mes, gastos, cuotas y pagos (RF04).",
        modifier = modifier,
    )
}
