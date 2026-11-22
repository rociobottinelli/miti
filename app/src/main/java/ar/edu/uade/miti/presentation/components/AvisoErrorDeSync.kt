package ar.edu.uade.miti.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.theme.MitiTheme

/**
 * Aviso de que no se pudieron enviar o traer cambios. Distingue "cambio guardado en el celular,
 * sincronización pendiente" de "no se pudo guardar": lo cargado no se perdió (RNF09), por eso el
 * aviso no oculta la lista y ofrece reintentar en vez de bloquear la pantalla.
 */
@Composable
fun AvisoErrorDeSync(alReintentar: () -> Unit, modifier: Modifier = Modifier) {
    AvisoMiti(
        texto = stringResource(R.string.aviso_error_de_sync),
        icono = Icons.Outlined.SyncProblem,
        tipo = TipoDeAviso.ERROR,
        accion = stringResource(R.string.accion_reintentar),
        alTocarAccion = alReintentar,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun AvisoErrorDeSyncPreview() {
    MitiTheme {
        AvisoErrorDeSync(alReintentar = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1415)
@Composable
private fun AvisoErrorDeSyncOscuroPreview() {
    MitiTheme(darkTheme = true) {
        AvisoErrorDeSync(alReintentar = {}, modifier = Modifier.padding(16.dp))
    }
}
