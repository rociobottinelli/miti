package ar.edu.uade.miti.presentation.compras

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ar.edu.uade.miti.presentation.components.PantallaEnConstruccion

@Composable
fun ComprasScreen(modifier: Modifier = Modifier) {
    PantallaEnConstruccion(
        titulo = "Compras",
        descripcion = "Lista compartida y carrito; al terminar se ofrece registrar el gasto (RF03).",
        modifier = modifier,
    )
}
