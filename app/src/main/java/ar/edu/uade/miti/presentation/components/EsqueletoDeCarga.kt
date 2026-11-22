package ar.edu.uade.miti.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.theme.Espaciado
import ar.edu.uade.miti.presentation.theme.MitiTheme

/**
 * Siluetas de cards que se muestran mientras se cargan los datos por primera vez. Como Room es la
 * fuente de verdad, en el uso normal la lista aparece al instante (RNF04): esto solo se ve cuando
 * todavía no hay nada guardado en el celular. Evita el spinner infinito y anticipa la forma de la
 * pantalla. Para TalkBack es un solo elemento que dice "Cargando".
 *
 * @param cantidad cuántas siluetas dibujar.
 */
@Composable
fun EsqueletoDeCarga(modifier: Modifier = Modifier, cantidad: Int = 3) {
    val descripcion = stringResource(R.string.cargando)
    val transicion = rememberInfiniteTransition(label = "esqueleto")
    val opacidad by transicion.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
        label = "opacidad",
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(Espaciado.entreCards),
        modifier = modifier
            .fillMaxWidth()
            .alpha(opacidad)
            .clearAndSetSemantics { contentDescription = descripcion },
    ) {
        repeat(cantidad) { SiluetaDeCard() }
    }
}

@Composable
private fun SiluetaDeCard() {
    val relleno = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
            .padding(horizontal = Espaciado.margenPantalla),
    ) {
        Box(Modifier.size(28.dp).background(relleno, MaterialTheme.shapes.small))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = Espaciado.margenPantalla).fillMaxWidth(),
        ) {
            Box(Modifier.fillMaxWidth(0.6f).height(12.dp).background(relleno, MaterialTheme.shapes.extraSmall))
            Box(Modifier.fillMaxWidth(0.35f).height(10.dp).background(relleno, MaterialTheme.shapes.extraSmall))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EsqueletoDeCargaPreview() {
    MitiTheme {
        EsqueletoDeCarga(modifier = Modifier.padding(16.dp))
    }
}
