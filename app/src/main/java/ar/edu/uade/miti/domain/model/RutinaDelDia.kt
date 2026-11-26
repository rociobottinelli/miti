package ar.edu.uade.miti.domain.model

/** Una rutina con las marcas que recibió en un día. El progreso se calcula, no se guarda (D09). */
data class RutinaDelDia(val rutina: Rutina, val registros: List<RegistroRutina>) {
    val hechas: Int get() = minOf(registros.size, rutina.vecesPorDia)
    val completa: Boolean get() = registros.size >= rutina.vecesPorDia
}
