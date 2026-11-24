package ar.edu.uade.miti.presentation.primeruso

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
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
fun IngresarScreen(
    alCrearCuenta: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IngresarViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    IngresarContenido(
        estado = estado,
        alCambiarEmail = viewModel::cambiarEmail,
        alCambiarContrasenia = viewModel::cambiarContrasenia,
        alIngresar = viewModel::ingresar,
        alCrearCuenta = alCrearCuenta,
        modifier = modifier,
    )
}

@Composable
fun IngresarContenido(
    estado: CuentaUiState,
    alCambiarEmail: (String) -> Unit,
    alCambiarContrasenia: (String) -> Unit,
    alIngresar: () -> Unit,
    alCrearCuenta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habilitado = !estado.enviando
    val fallo = estado.envio as? EnvioDeCuenta.Fallo
    // Como en el Figma: si el servidor rechaza los datos, se marcan los dos campos y el motivo va debajo
    // del email. Sin conexión, en cambio, el problema no son los datos: va un aviso arriba.
    val rechazo = fallo?.takeUnless { it.sinConexion }?.mensaje
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        EncabezadoDeCuenta(stringResource(R.string.ingresar_titulo))
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (fallo?.sinConexion == true) {
                AvisoMiti(
                    texto = stringResource(R.string.ingresar_sin_conexion),
                    icono = Icons.Outlined.WifiOff,
                    tipo = TipoDeAviso.ERROR,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            CampoDeCuenta(
                estado.email, alCambiarEmail, stringResource(R.string.cuenta_email),
                error = estado.errorEmail ?: rechazo,
                habilitado = habilitado, tipoDeTeclado = KeyboardType.Email,
            )
            CampoDeContrasenia(
                estado.contrasenia, alCambiarContrasenia, estado.errorContrasenia, alConfirmar = alIngresar,
                habilitado = habilitado, marcarError = estado.errorContrasenia != null || rechazo != null,
            )
            BotonDeEnvio(stringResource(R.string.ingresar_titulo), enviando = estado.enviando, alTocar = alIngresar)
            TextButton(onClick = alCrearCuenta, enabled = habilitado, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp)) {
                Text(stringResource(R.string.ingresar_crear_cuenta))
            }
        }
    }
}

@Composable
private fun VistaPrevia(estado: CuentaUiState, oscuro: Boolean = false) {
    MitiTheme(darkTheme = oscuro) { IngresarContenido(estado, {}, {}, {}, {}) }
}

@Preview(showSystemUi = true)
@Composable
private fun IngresarPreview() = VistaPrevia(CuentaUiState())

@Preview(showSystemUi = true)
@Composable
private fun IngresarOscuroPreview() = VistaPrevia(CuentaUiState(email = "ana@mail.com"), oscuro = true)

@Preview(name = "Sin conexión (37:745)", showSystemUi = true)
@Composable
private fun IngresarSinConexionPreview() = VistaPrevia(
    CuentaUiState(email = "ana@mail.com", contrasenia = "secreto", envio = EnvioDeCuenta.Fallo("sin red", sinConexion = true)),
)

@Preview(name = "Datos incorrectos (37:2849)", showSystemUi = true)
@Composable
private fun IngresarErrorPreview() = VistaPrevia(
    CuentaUiState(
        email = "ana@mail.com", contrasenia = "secreto",
        envio = EnvioDeCuenta.Fallo("Email o contraseña incorrectos", sinConexion = false),
    ),
)
