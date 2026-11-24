package ar.edu.uade.miti.presentation.primeruso

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.components.AvisoMiti
import ar.edu.uade.miti.presentation.components.TipoDeAviso
import ar.edu.uade.miti.presentation.theme.MitiTheme
import kotlinx.coroutines.launch

/**
 * [alEmpezarACrear] avisa antes de crear, para que el arranque no salte a Hoy apenas el hogar existe y
 * se pueda mostrar el código. [alTerminar] se llama al tocar Continuar, al unirse o al recuperar el hogar.
 */
@Composable
fun TuHogarScreen(
    alEmpezarACrear: () -> Unit,
    alTerminar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TuHogarViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val nombrePorDefecto = stringResource(R.string.tu_hogar_nombre_por_defecto)
    val copiado = stringResource(R.string.tu_hogar_codigo_copiado)

    LaunchedEffect(estado) {
        if (estado is TuHogarUiState.Listo) alTerminar()
    }

    TuHogarContenido(
        estado = estado,
        alCambiarCodigo = viewModel::cambiarCodigo,
        alCrear = {
            alEmpezarACrear()
            viewModel.crear(nombrePorDefecto)
        },
        alUnirse = viewModel::unirse,
        alReintentar = viewModel::buscar,
        alCopiar = { codigo ->
            copiarAlPortapapeles(contexto, codigo)
            alcance.launch { snackbar.showSnackbar(copiado) }
        },
        alCompartir = { codigo -> compartir(contexto, codigo) },
        alContinuar = alTerminar,
        snackbar = snackbar,
        modifier = modifier,
    )
}

@Composable
fun TuHogarContenido(
    estado: TuHogarUiState,
    alCambiarCodigo: (String) -> Unit,
    alCrear: () -> Unit,
    alUnirse: () -> Unit,
    alReintentar: () -> Unit,
    alCopiar: (String) -> Unit,
    alCompartir: (String) -> Unit,
    alContinuar: () -> Unit,
    modifier: Modifier = Modifier,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.tu_hogar_titulo),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 18.dp)
                    .semantics { heading() },
            )
            when (estado) {
                TuHogarUiState.Buscando, TuHogarUiState.Listo -> Buscando()
                is TuHogarUiState.Creado -> Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    TarjetaCrear(codigo = estado.codigo, alCopiar = alCopiar, alCompartir = alCompartir)
                    BotonDeEnvio(stringResource(R.string.tu_hogar_continuar), enviando = false, alTocar = alContinuar, modifier = Modifier.padding(top = 16.dp))
                }
                is TuHogarUiState.Eligiendo -> Eligiendo(estado, alCambiarCodigo, alCrear, alUnirse, alReintentar)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Buscando() {
    Column(
        Modifier.fillMaxSize().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(R.string.tu_hogar_buscando),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun Eligiendo(
    estado: TuHogarUiState.Eligiendo,
    alCambiarCodigo: (String) -> Unit,
    alCrear: () -> Unit,
    alUnirse: () -> Unit,
    alReintentar: () -> Unit,
) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        estado.aviso?.let {
            AvisoMiti(
                texto = it.mensaje,
                icono = if (it.sinConexion) Icons.Outlined.WifiOff else Icons.Outlined.ErrorOutline,
                tipo = TipoDeAviso.ERROR,
                accion = stringResource(R.string.accion_reintentar),
                alTocarAccion = alReintentar,
            )
        }
        TarjetaDeHogar(
            icono = Icons.Outlined.Home,
            fondoIcono = MaterialTheme.colorScheme.primaryContainer,
            colorIcono = MaterialTheme.colorScheme.onPrimaryContainer,
            titulo = stringResource(R.string.tu_hogar_crear_titulo),
            subtitulo = stringResource(R.string.tu_hogar_crear_subtitulo),
        ) {
            BotonDeEnvio(
                stringResource(R.string.tu_hogar_crear_boton),
                enviando = estado.enCurso == AccionDeHogar.CREANDO,
                alTocar = alCrear,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        TarjetaDeHogar(
            icono = Icons.Outlined.GroupAdd,
            fondoIcono = MaterialTheme.colorScheme.secondaryContainer,
            colorIcono = MaterialTheme.colorScheme.onSecondaryContainer,
            titulo = stringResource(R.string.tu_hogar_unirme_titulo),
            subtitulo = stringResource(R.string.tu_hogar_unirme_subtitulo),
        ) {
            OutlinedTextField(
                value = estado.codigo,
                onValueChange = alCambiarCodigo,
                placeholder = { Text(stringResource(R.string.tu_hogar_codigo_placeholder)) },
                isError = estado.errorCodigo != null,
                supportingText = estado.errorCodigo?.let { { Text(it) } },
                singleLine = true,
                enabled = estado.enCurso == null,
                textStyle = MaterialTheme.typography.bodyLarge.copy(letterSpacing = 2.sp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { alUnirse() }),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            BotonDeEnvio(
                stringResource(R.string.tu_hogar_unirme_boton),
                enviando = estado.enCurso == AccionDeHogar.UNIENDOSE,
                alTocar = alUnirse,
            )
        }
    }
}

/** Tarjeta con ícono, título y subtítulo (Figma: surfaceContainerLow, borde y esquinas de 12 dp). */
@Composable
private fun TarjetaDeHogar(
    icono: ImageVector,
    fondoIcono: Color,
    colorIcono: Color,
    titulo: String,
    subtitulo: String,
    contenido: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).background(fondoIcono, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                    Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() })
                    Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            contenido()
        }
    }
}

/** El hogar recién creado: el código grande para dictarlo, y los botones para copiarlo o mandarlo. */
@Composable
private fun TarjetaCrear(codigo: String, alCopiar: (String) -> Unit, alCompartir: (String) -> Unit) {
    val descripcionCodigo = stringResource(R.string.tu_hogar_codigo_descripcion, codigo.toCharArray().joinToString(" "))
    TarjetaDeHogar(
        icono = Icons.Outlined.Home,
        fondoIcono = MaterialTheme.colorScheme.primaryContainer,
        colorIcono = MaterialTheme.colorScheme.onPrimaryContainer,
        titulo = stringResource(R.string.tu_hogar_creado_titulo),
        subtitulo = stringResource(R.string.tu_hogar_crear_subtitulo),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(10.dp))
                .padding(16.dp)
                // TalkBack lo deletrea ("K 7 P 2 Q X") en vez de leerlo como una palabra.
                .semantics(mergeDescendants = true) { contentDescription = descripcionCodigo },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.tu_hogar_codigo_etiqueta).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                codigo,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 8.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { alCopiar(codigo) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.tu_hogar_copiar), Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = { alCompartir(codigo) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.tu_hogar_compartir), Modifier.padding(start = 8.dp))
            }
        }
    }
}

private fun copiarAlPortapapeles(contexto: Context, codigo: String) {
    contexto.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText(contexto.getString(R.string.tu_hogar_codigo_etiqueta), codigo))
}

private fun compartir(contexto: Context, codigo: String) {
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, contexto.getString(R.string.tu_hogar_mensaje_compartir, codigo))
    }
    contexto.startActivity(Intent.createChooser(envio, null))
}

@Composable
private fun VistaPrevia(estado: TuHogarUiState, oscuro: Boolean = false) {
    MitiTheme(darkTheme = oscuro) { TuHogarContenido(estado, {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(name = "Eligiendo", showSystemUi = true)
@Composable
private fun TuHogarPreview() = VistaPrevia(TuHogarUiState.Eligiendo())

@Preview(name = "Creado (37:780)", showSystemUi = true)
@Composable
private fun TuHogarCreadoPreview() = VistaPrevia(TuHogarUiState.Creado("K7P2QX"))

@Preview(name = "Creado oscuro", showSystemUi = true)
@Composable
private fun TuHogarCreadoOscuroPreview() = VistaPrevia(TuHogarUiState.Creado("K7P2QX"), oscuro = true)

@Preview(name = "Código inválido (37:879)", showSystemUi = true)
@Composable
private fun TuHogarErrorPreview() = VistaPrevia(TuHogarUiState.Eligiendo(codigo = "K7P2QZ", errorCodigo = "Código inválido"))

@Preview(name = "Sin conexión", showSystemUi = true)
@Composable
private fun TuHogarSinConexionPreview() = VistaPrevia(
    TuHogarUiState.Eligiendo(aviso = AvisoDeHogar("Necesitás conexión para crear el hogar o unirte a uno.", sinConexion = true)),
)

@Preview(name = "Buscando", showSystemUi = true)
@Composable
private fun TuHogarBuscandoPreview() = VistaPrevia(TuHogarUiState.Buscando)
