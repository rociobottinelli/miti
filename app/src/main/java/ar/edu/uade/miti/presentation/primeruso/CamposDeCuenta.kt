package ar.edu.uade.miti.presentation.primeruso

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ar.edu.uade.miti.R

// Piezas que comparten Crear cuenta e Ingresar (Figma 37:683 y 37:716).

/** Título centrado, con flecha para volver si hay a dónde. */
@Composable
internal fun EncabezadoDeCuenta(titulo: String, modifier: Modifier = Modifier, alVolver: (() -> Unit)? = null) {
    Box(modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        if (alVolver != null) {
            IconButton(onClick = alVolver, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cuenta_volver))
            }
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** Campo de texto del formulario: el error va debajo, como texto, y no solo como borde rojo (RNF08). */
@Composable
internal fun CampoDeCuenta(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    error: String?,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    tipoDeTeclado: KeyboardType = KeyboardType.Text,
    marcarError: Boolean = error != null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        isError = marcarError,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        enabled = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = tipoDeTeclado, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Contraseña con el botón del ojo para mostrarla u ocultarla (Figma: ícono "visibility"). */
@Composable
internal fun CampoDeContrasenia(
    valor: String,
    alCambiar: (String) -> Unit,
    error: String?,
    alConfirmar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    marcarError: Boolean = error != null,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(stringResource(R.string.cuenta_contrasenia)) },
        isError = marcarError,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        enabled = habilitado,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { alConfirmar() }),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(
                        if (visible) R.string.cuenta_ocultar_contrasenia else R.string.cuenta_mostrar_contrasenia,
                    ),
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}
