package ar.edu.uade.miti.presentation.hoy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ar.edu.uade.miti.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import ar.edu.uade.miti.presentation.components.AvisoDeSincronizacion
import ar.edu.uade.miti.presentation.components.AvisoMiti
import ar.edu.uade.miti.presentation.components.TipoDeAviso
import ar.edu.uade.miti.presentation.components.AvisoDeSync
import ar.edu.uade.miti.presentation.components.cuando
import ar.edu.uade.miti.presentation.theme.Espaciado
import ar.edu.uade.miti.presentation.theme.MitiTheme
import java.time.LocalDate

/**
 * El encabezado de Hoy y, debajo, el aviso de sincronización si hay alguno (C1, Figma 37:2589) y el de
 * permisos de avisos si falta alguno (preentrega, sección 11).
 */
@Composable
internal fun EncabezadoConAviso(
    encabezado: Encabezado,
    alAbrirAjustes: () -> Unit,
    alSincronizar: () -> Unit,
    alConfigurarAvisos: (AvisoDePermisos) -> Unit = {},
) {
    val margen = Modifier.padding(horizontal = Espaciado.margenPantalla).padding(bottom = Espaciado.entreCards)
    Column {
        EncabezadoDeHoy(encabezado, alAbrirAjustes)
        encabezado.aviso?.let {
            AvisoDeSincronizacion(
                aviso = it,
                hoy = encabezado.fecha,
                otro = encabezado.otro?.nombre,
                alSincronizar = alSincronizar,
                modifier = margen,
            )
        }
        encabezado.permisos?.let { falta ->
            AvisoMiti(
                texto = stringResource(
                    when (falta) {
                        AvisoDePermisos.SIN_NOTIFICACIONES -> R.string.hoy_permiso_notificaciones
                        AvisoDePermisos.SIN_ALARMAS_EXACTAS -> R.string.hoy_permiso_alarmas
                    },
                ),
                icono = Icons.Outlined.NotificationsOff,
                tipo = TipoDeAviso.ADVERTENCIA,
                accion = stringResource(R.string.hoy_permiso_configurar),
                alTocarAccion = { alConfigurarAvisos(falta) },
                modifier = margen,
            )
        }
    }
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
            EncabezadoConAviso(encabezadoDeEjemplo.copy(permisos = AvisoDePermisos.SIN_NOTIFICACIONES), {}, {})
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
