package ar.edu.uade.miti.presentation.hoy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.components.AvisoErrorDeSync
import ar.edu.uade.miti.presentation.components.AvisoMiti
import ar.edu.uade.miti.presentation.components.BannerSinConexion
import ar.edu.uade.miti.presentation.components.TipoDeAviso
import ar.edu.uade.miti.presentation.theme.Espaciado
import ar.edu.uade.miti.presentation.theme.MitiTheme
import java.time.LocalDate
import java.time.LocalDateTime

/** El encabezado de Hoy y, debajo, el aviso de sincronización si hay alguno (C1, Figma 37:2589). */
@Composable
internal fun EncabezadoConAviso(encabezado: Encabezado, alAbrirAjustes: () -> Unit, alSincronizar: () -> Unit) {
    Column {
        EncabezadoDeHoy(encabezado, alAbrirAjustes)
        encabezado.aviso?.let {
            AvisoDeSincronizacion(
                aviso = it,
                hoy = encabezado.fecha,
                otro = encabezado.otro?.nombre,
                alSincronizar = alSincronizar,
                modifier = Modifier.padding(horizontal = Espaciado.margenPantalla).padding(bottom = Espaciado.entreCards),
            )
        }
    }
}

@Composable
private fun AvisoDeSincronizacion(
    aviso: AvisoDeSync,
    hoy: LocalDate,
    otro: String?,
    alSincronizar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (aviso) {
        // Sin conexión no hay nada que forzar: la sincronización arranca sola cuando vuelve la red.
        is AvisoDeSync.SinConexion -> BannerSinConexion(aviso.ultimaSincronizacion?.let { cuando(it, hoy) }, modifier)
        AvisoDeSync.NoSePudoSincronizar -> AvisoErrorDeSync(alReintentar = alSincronizar, modifier = modifier)
        is AvisoDeSync.Desactualizado -> AvisoMiti(
            texto = textoDesactualizado(otro, aviso.ultimaSincronizacion, hoy),
            icono = Icons.Outlined.History,
            tipo = TipoDeAviso.ADVERTENCIA,
            accion = stringResource(R.string.sync_actualizar),
            alTocarAccion = alSincronizar,
            modifier = modifier,
        )
    }
}

/** "Puede que no veas lo último de Tomás · última sincronización 14:32". */
@Composable
private fun textoDesactualizado(otro: String?, ultima: LocalDateTime?, hoy: LocalDate): String {
    val quien = otro?.let { stringResource(R.string.sync_desactualizado_de, it) } ?: stringResource(R.string.sync_desactualizado)
    return ultima?.let { stringResource(R.string.sync_con_hora, quien, cuando(it, hoy)) } ?: quien
}

/**
 * Pregunta antes de marcar una rutina cuando el celular no sabe qué hizo la otra persona (C1: "si la
 * información llega tarde, pueden repetir tareas"). Marcar igual sigue siendo posible: offline first.
 */
@Composable
internal fun DialogoConfirmarMarca(confirmacion: ConfirmacionDeMarca, hoy: LocalDate, alConfirmar: () -> Unit, alCancelar: () -> Unit) {
    val quien = confirmacion.otro ?: stringResource(R.string.sync_la_otra_persona)
    val desde = confirmacion.ultimaSincronizacion?.let { stringResource(R.string.sync_confirmar_desde, cuando(it, hoy)) }
        ?: stringResource(R.string.sync_confirmar_nunca)
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(stringResource(R.string.sync_confirmar_titulo, confirmacion.titulo)) },
        text = { Text(stringResource(R.string.sync_confirmar_texto, quien, desde)) },
        confirmButton = { TextButton(onClick = alConfirmar) { Text(stringResource(R.string.sync_confirmar_marcar)) } },
        dismissButton = { TextButton(onClick = alCancelar) { Text(stringResource(R.string.sync_confirmar_cancelar)) } },
    )
}

/** "14:32" si fue hoy; si no, "20/11 14:32". */
private fun cuando(momento: LocalDateTime, hoy: LocalDate): String =
    if (momento.toLocalDate() == hoy) momento.toLocalTime().comoHora() else "${momento.toLocalDate().comoFechaCorta()} ${momento.toLocalTime().comoHora()}"

private val hoyDeEjemplo = LocalDate.of(2026, 9, 30)
private val encabezadoDeEjemplo = Encabezado(hoyDeEjemplo, IntegranteEnPantalla("a", "Ana", 0), IntegranteEnPantalla("t", "Tomás", 1))

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun AvisosDeConexionPreview() {
    MitiTheme {
        Column {
            EncabezadoConAviso(encabezadoDeEjemplo.copy(aviso = AvisoDeSync.SinConexion(hoyDeEjemplo.atTime(14, 32))), {}, {})
            EncabezadoConAviso(encabezadoDeEjemplo.copy(aviso = AvisoDeSync.NoSePudoSincronizar), {}, {})
            EncabezadoConAviso(encabezadoDeEjemplo.copy(aviso = AvisoDeSync.Desactualizado(hoyDeEjemplo.minusDays(1).atTime(21, 3))), {}, {})
        }
    }
}

@Preview
@Composable
private fun DialogoConfirmarMarcaPreview() {
    MitiTheme {
        DialogoConfirmarMarca(
            ConfirmacionDeMarca("r", "Darle de comer a Pancho", "Tomás", hoyDeEjemplo.atTime(8, 15)),
            hoyDeEjemplo, {}, {},
        )
    }
}
