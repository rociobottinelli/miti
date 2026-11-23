package ar.edu.uade.miti.presentation.primeruso

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.miti.R
import ar.edu.uade.miti.presentation.components.AvisoMiti
import ar.edu.uade.miti.presentation.components.TipoDeAviso
import ar.edu.uade.miti.presentation.theme.MitiTheme

@Composable
fun CrearCuentaScreen(
    alVolver: () -> Unit,
    alIrAIngresar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CrearCuentaViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    CrearCuentaContenido(
        estado = estado,
        alCambiarNombre = viewModel::cambiarNombre,
        alCambiarEmail = viewModel::cambiarEmail,
        alCambiarContrasenia = viewModel::cambiarContrasenia,
        alCrear = viewModel::crear,
        alVolver = alVolver,
        alIrAIngresar = alIrAIngresar,
        modifier = modifier,
    )
}

/** Figma 37:683. Sin estado propio: lo que se escribe vive en el ViewModel. */
@Composable
fun CrearCuentaContenido(
    estado: CuentaUiState,
    alCambiarNombre: (String) -> Unit,
    alCambiarEmail: (String) -> Unit,
    alCambiarContrasenia: (String) -> Unit,
    alCrear: () -> Unit,
    alVolver: () -> Unit,
    alIrAIngresar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habilitado = !estado.enviando
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        EncabezadoDeCuenta(stringResource(R.string.cuenta_crear_titulo), alVolver = alVolver)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (estado.envio as? EnvioDeCuenta.Fallo)?.let { fallo ->
                AvisoMiti(
                    texto = fallo.mensaje,
                    icono = if (fallo.sinConexion) Icons.Outlined.WifiOff else Icons.Outlined.ErrorOutline,
                    tipo = TipoDeAviso.ERROR,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            CampoDeCuenta(estado.nombre, alCambiarNombre, stringResource(R.string.cuenta_nombre), estado.errorNombre, habilitado = habilitado)
            CampoDeCuenta(
                estado.email, alCambiarEmail, stringResource(R.string.cuenta_email), estado.errorEmail,
                habilitado = habilitado, tipoDeTeclado = KeyboardType.Email,
            )
            CampoDeContrasenia(estado.contrasenia, alCambiarContrasenia, estado.errorContrasenia, alConfirmar = alCrear, habilitado = habilitado)
            BotonDeEnvio(stringResource(R.string.cuenta_crear_titulo), enviando = estado.enviando, alTocar = alCrear)
            TextButton(onClick = alIrAIngresar, enabled = habilitado, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp)) {
                Text(stringResource(R.string.cuenta_ya_tengo))
            }
        }
    }
}

/** Botón principal que muestra un indicador mientras espera al servidor y no se puede tocar dos veces. */
@Composable
internal fun BotonDeEnvio(texto: String, enviando: Boolean, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = alTocar,
        enabled = !enviando,
        modifier = modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp),
    ) {
        if (enviando) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(texto)
        }
    }
}

@Composable
private fun VistaPrevia(estado: CuentaUiState, oscuro: Boolean = false) {
    MitiTheme(darkTheme = oscuro) { CrearCuentaContenido(estado, {}, {}, {}, {}, {}, {}) }
}

@Preview(showSystemUi = true)
@Composable
private fun CrearCuentaPreview() = VistaPrevia(CuentaUiState())

@Preview(showSystemUi = true)
@Composable
private fun CrearCuentaOscuroPreview() = VistaPrevia(CuentaUiState(nombre = "Ana", email = "ana@mail.com"), oscuro = true)

@Preview(name = "Errores en los campos", showSystemUi = true)
@Composable
private fun CrearCuentaErroresPreview() = VistaPrevia(
    CuentaUiState(email = "ana@", errorNombre = "Ingresá tu nombre", errorEmail = "El email no tiene un formato válido"),
)

@Preview(name = "Ya existe la cuenta", showSystemUi = true)
@Composable
private fun CrearCuentaFalloPreview() = VistaPrevia(
    CuentaUiState(
        nombre = "Ana", email = "ana@mail.com", contrasenia = "secreto",
        envio = EnvioDeCuenta.Fallo("Ya existe una cuenta con ese email. Probá ingresar.", sinConexion = false),
    ),
)

@Preview(name = "Enviando", showSystemUi = true)
@Composable
private fun CrearCuentaEnviandoPreview() = VistaPrevia(
    CuentaUiState(nombre = "Ana", email = "ana@mail.com", contrasenia = "secreto", envio = EnvioDeCuenta.Enviando),
)
