package ar.edu.uade.miti.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.theme.MitiTheme

/**
 * Aviso de que la app está sin conexión y muestra los datos guardados en el celular
 * (preentrega, sección 14; corrección C1/C4: advertir que los datos pueden estar desactualizados).
 *
 * @param ultimaSincronizacion hora de la última sincronización ya formateada ("14:32"), o null si
 * este celular nunca sincronizó. Formatearla es tarea del ViewModel, no de la vista.
 */
@Composable
fun BannerSinConexion(ultimaSincronizacion: String?, modifier: Modifier = Modifier) {
    val texto = if (ultimaSincronizacion != null) {
        stringResource(R.string.aviso_sin_conexion_con_hora, ultimaSincronizacion)
    } else {
        stringResource(R.string.aviso_sin_conexion)
    }
    AvisoMiti(
        texto = texto,
        icono = Icons.Outlined.CloudOff,
        tipo = TipoDeAviso.INFORMATIVO,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun BannerSinConexionPreview() {
    MitiTheme {
        BannerSinConexion(ultimaSincronizacion = "14:32", modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun BannerSinConexionNuncaSincronizoPreview() {
    MitiTheme {
        BannerSinConexion(ultimaSincronizacion = null, modifier = Modifier.padding(16.dp))
    }
}
