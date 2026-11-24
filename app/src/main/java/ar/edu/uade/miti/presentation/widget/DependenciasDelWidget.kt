package ar.edu.uade.miti.presentation.widget

import ar.edu.uade.miti.domain.usecase.rutinas.MarcarRutinaUseCase
import ar.edu.uade.miti.domain.usecase.sesion.ObservarUsuarioUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Los widgets no son pantallas de Hilt (los crea Android), así que piden sus dependencias por acá. Son
 * los mismos casos de uso que usa la app: el widget nunca escribe directo en Room (D03).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DependenciasDelWidget {
    fun observarEstado(): ObservarEstadoDelWidget
    fun marcarRutina(): MarcarRutinaUseCase
    fun observarUsuario(): ObservarUsuarioUseCase
}
