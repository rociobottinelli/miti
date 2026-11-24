package ar.edu.uade.miti.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R
import ar.edu.uade.miti.domain.model.EstadoDeSincronizacion
import ar.edu.uade.miti.presentation.hoy.comoFechaCorta
import ar.edu.uade.miti.presentation.hoy.comoHora
import ar.edu.uade.miti.presentation.theme.MitiTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Lo que se le dice a la persona sobre la sincronización, de más a menos grave (C1). Lo usan Hoy y
 * Compras. [ultimaSincronizacion] es null si este celular nunca sincronizó.
 */
sealed interface AvisoDeSync {
    data class SinConexion(val ultimaSincronizacion: LocalDateTime?) : AvisoDeSync

    /** Hay conexión, pero el último intento falló; se reintenta solo y se puede forzar. */
    data object NoSePudoSincronizar : AvisoDeSync

    /** Hay conexión pero hace rato que no se sincroniza: lo que se ve puede no incluir lo del otro. */
    data class Desactualizado(val ultimaSincronizacion: LocalDateTime?) : AvisoDeSync

    companion object {
        /** De más a menos grave: sin conexión, error al sincronizar, datos viejos. Si todo está bien, nada. */
        fun segun(sync: EstadoDeSincronizacion, zona: ZoneId): AvisoDeSync? {
            val ultima = sync.ultimaSincronizacion?.enZona(zona)
            return when {
                !sync.conConexion -> SinConexion(ultima)
                sync.reintentando -> NoSePudoSincronizar
                sync.desactualizado && !sync.sincronizando -> Desactualizado(ultima)
                else -> null
            }
        }

        private fun Instant.enZona(zona: ZoneId) = LocalDateTime.ofInstant(this, zona)
    }
}

/**
 * El aviso en pantalla (Figma 37:2589). Sin conexión no ofrece botón: la sincronización arranca sola
 * cuando vuelve la red. [otro] es el nombre de la otra persona, si hay.
 */
@Composable
fun AvisoDeSincronizacion(
    aviso: AvisoDeSync,
    hoy: LocalDate,
    otro: String?,
    alSincronizar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (aviso) {
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

/** "14:32" si fue hoy; si no, "20/11 14:32". */
fun cuando(momento: LocalDateTime, hoy: LocalDate): String =
    if (momento.toLocalDate() == hoy) momento.toLocalTime().comoHora() else "${momento.toLocalDate().comoFechaCorta()} ${momento.toLocalTime().comoHora()}"

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun AvisoDeSincronizacionPreview() {
    val hoy = LocalDate.of(2026, 9, 30)
    MitiTheme {
        Column(Modifier.padding(16.dp)) {
            AvisoDeSincronizacion(AvisoDeSync.SinConexion(hoy.atTime(14, 32)), hoy, "Tomás", {}, Modifier.padding(bottom = 8.dp))
            AvisoDeSincronizacion(AvisoDeSync.NoSePudoSincronizar, hoy, "Tomás", {}, Modifier.padding(bottom = 8.dp))
            AvisoDeSincronizacion(AvisoDeSync.Desactualizado(hoy.minusDays(1).atTime(21, 3)), hoy, "Tomás", {})
        }
    }
}
