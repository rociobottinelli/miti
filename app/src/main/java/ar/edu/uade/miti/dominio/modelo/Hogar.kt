package ar.edu.uade.miti.dominio.modelo

/** Un hogar tiene 1 o 2 integrantes y es dueño de todos los ítems. */
data class Hogar(
    val id: String,
    val nombre: String,
    /** Código de 6 caracteres para que el segundo integrante se una. */
    val codigoUnion: String,
)

data class Integrante(
    val id: String,
    val hogarId: String,
    val nombre: String,
    val email: String,
)
