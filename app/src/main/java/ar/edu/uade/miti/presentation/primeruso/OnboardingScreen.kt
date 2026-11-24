package ar.edu.uade.miti.presentation.primeruso

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.theme.MitiTheme
import kotlinx.coroutines.launch

/** Un paso del onboarding (Figma 37:566, 37:649 y 37:612). */
private data class PasoDeOnboarding(
    @param:DrawableRes val ilustracion: Int,
    @param:StringRes val titulo: Int,
    @param:StringRes val texto: Int,
)

private val pasos = listOf(
    PasoDeOnboarding(R.drawable.ilustracion_rutinas, R.string.onboarding_rutinas_titulo, R.string.onboarding_rutinas_texto),
    PasoDeOnboarding(R.drawable.ilustracion_tareas, R.string.onboarding_tareas_titulo, R.string.onboarding_tareas_texto),
    PasoDeOnboarding(R.drawable.ilustracion_balance, R.string.onboarding_balance_titulo, R.string.onboarding_balance_texto),
)

/**
 * Tres pasos que cuentan qué resuelve Miti. Se pasa deslizando o con "Siguiente"; "Omitir" y "Empezar"
 * terminan igual: el onboarding queda visto y no vuelve a aparecer (flujo 1 de la preentrega).
 */
@Composable
fun OnboardingScreen(alTerminar: () -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState(pageCount = { pasos.size })
    val alcance = rememberCoroutineScope()
    val esElUltimo = pager.currentPage == pasos.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = alTerminar) { Text(stringResource(R.string.onboarding_omitir)) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { pagina ->
            PaginaDeOnboarding(pasos[pagina])
        }
        IndicadorDePasos(actual = pager.currentPage, total = pasos.size)
        Button(
            onClick = {
                if (esElUltimo) alTerminar() else alcance.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 52.dp)
                .heightIn(min = 48.dp),
        ) {
            Text(stringResource(if (esElUltimo) R.string.onboarding_empezar else R.string.onboarding_siguiente))
        }
    }
}

@Composable
private fun PaginaDeOnboarding(paso: PasoDeOnboarding) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Decorativa: lo importante lo dicen el título y el texto.
            Image(painterResource(paso.ilustracion), contentDescription = null)
        }
        Column(Modifier.padding(start = 32.dp, end = 32.dp, bottom = 20.dp)) {
            Text(
                text = stringResource(paso.titulo),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(paso.texto),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** Puntos de abajo: el actual es una píldora más larga. TalkBack lo lee como "Paso 2 de 3". */
@Composable
private fun IndicadorDePasos(actual: Int, total: Int) {
    val descripcion = stringResource(R.string.onboarding_paso_de, actual + 1, total)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .clearAndSetSemantics { contentDescription = descripcion },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(total) { i ->
            val ancho by animateDpAsState(if (i == actual) 24.dp else 8.dp, label = "punto")
            Box(
                Modifier
                    .height(8.dp)
                    .width(ancho)
                    .background(
                        if (i == actual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    ),
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun OnboardingPreview() {
    MitiTheme { OnboardingScreen(alTerminar = {}) }
}

@Preview(showSystemUi = true)
@Composable
private fun OnboardingOscuroPreview() {
    MitiTheme(darkTheme = true) { OnboardingScreen(alTerminar = {}) }
}

@Preview(showSystemUi = true, fontScale = 2f)
@Composable
private fun OnboardingFuenteGrandePreview() {
    MitiTheme { OnboardingScreen(alTerminar = {}) }
}
