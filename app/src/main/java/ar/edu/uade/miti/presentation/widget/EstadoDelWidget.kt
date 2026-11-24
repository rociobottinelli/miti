package ar.edu.uade.miti.presentation.widget

import ar.edu.uade.miti.domain.model.EstadoDeSincronizacion
import ar.edu.uade.miti.domain.model.Integrante
import ar.edu.uade.miti.domain.model.RutinaDelDia
import ar.edu.uade.miti.domain.model.Usuario
import ar.edu.uade.miti.presentation.components.AvisoDeSync
import ar.edu.uade.miti.presentation.hoy.Marca
import ar.edu.uade.miti.presentation.hoy.RutinaDeHoy
import java.time.LocalDate
import java.time.ZoneId

/** Lo que muestran los dos widgets (#22). Sale de los mismos casos de uso que Hoy (D03). */
sealed interface EstadoDelWidget {
    /** Sin sesión o sin hogar: el widget invita a abrir la app. */
    data object SinHogar : EstadoDelWidget

    /** Figma "Widget - sin rutinas". */
    data object SinRutinas : EstadoDelWidget

    data class ConRutinas(
        /** Las que faltan primero, como en Hoy. */
        val rutinas: List<RutinaDeHoy>,
        val aviso: AvisoDeSync?,
        val hoy: LocalDate,
    ) : EstadoDelWidget {
        val hechas: Int get() = rutinas.count { it.completa }
        val total: Int get() = rutinas.size

        /** Figma "Widget - todo al día". */
        val todoAlDia: Boolean get() = rutinas.all { it.completa }

        /** La que muestra el widget compacto: la primera que falta. */
        val proxima: RutinaDeHoy? get() = rutinas.firstOrNull { !it.completa }
    }

    companion object {
        fun desde(
            usuario: Usuario?,
            integrantes: List<Integrante>,
            rutinas: List<RutinaDelDia>,
            sync: EstadoDeSincronizacion,
            sinEnviar: Set<String>,
            hoy: LocalDate,
            zona: ZoneId,
        ): EstadoDelWidget {
            if (usuario == null || integrantes.isEmpty()) return SinHogar
            if (rutinas.isEmpty()) return SinRutinas
            val nombres = integrantes.associate { it.id to it.nombre }
            val enPantalla = rutinas.map { r ->
                RutinaDeHoy(
                    id = r.rutina.id,
                    titulo = r.rutina.titulo,
                    hechas = r.hechas,
                    vecesPorDia = r.rutina.vecesPorDia,
                    ultimaMarca = r.registros.maxByOrNull { it.fechaHora }?.let { Marca(nombres[it.integranteId], it.fechaHora.toLocalTime()) },
                    pendienteDeSync = r.rutina.id in sinEnviar,
                )
            }
            return ConRutinas(enPantalla.sortedBy { it.completa }, AvisoDeSync.segun(sync, zona), hoy)
        }
    }
}
