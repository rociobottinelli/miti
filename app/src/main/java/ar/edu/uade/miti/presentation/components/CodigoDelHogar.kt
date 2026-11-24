package ar.edu.uade.miti.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import ar.edu.uade.miti.R

// Copiar y compartir el código del hogar (Ajustes, #21). Son funciones de Android y no de un ViewModel:
// el portapapeles y el menú de compartir son del sistema.

fun copiarCodigoDelHogar(contexto: Context, codigo: String) {
    contexto.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText(contexto.getString(R.string.codigo_del_hogar), codigo))
}

/** Abre el menú de compartir de Android con un mensaje listo para mandar (WhatsApp, mail…). */
fun compartirCodigoDelHogar(contexto: Context, codigo: String) {
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, contexto.getString(R.string.codigo_del_hogar_mensaje, codigo))
    }
    contexto.startActivity(Intent.createChooser(envio, null))
}
