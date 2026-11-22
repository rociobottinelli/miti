# Cambios y desvíos respecto de la preentrega

Registro de los cambios relevantes y de todo lo que se aparta de lo que entregamos en la
[preentrega](docs/preentrega-miti.pdf). La consigna pide justificar los desvíos, así que cada entrada
sigue el formato **problema → qué cambió → qué riesgo resuelve**. Al cerrar la entrega final, lo más
importante de este archivo pasa a la sección "Decisiones relevantes" del README.

---

## Moshi en lugar de kotlinx.serialization para la red (#30)

- **Desvío:** la preentrega (sección 15) preveía Retrofit + kotlinx.serialization.
- **Problema:** la cátedra trabaja Retrofit con Moshi y generación de adaptadores con KSP. Usar otro conversor
  nos aparta del material con el que se evalúa y de los ejemplos de manejo de errores
  (`JsonDataException` ante una respuesta inesperada).
- **Qué cambió:** se reemplazó `converter-kotlinx-serialization` por `converter-moshi`, y se sumaron `moshi`
  y `moshi-kotlin-codegen` (con KSP, sin reflexión). Los DTOs van a usar `@JsonClass(generateAdapter = true)`.
  kotlinx.serialization **se queda**, pero solo para las rutas tipadas de Navigation Compose (`@Serializable`
  en `Ruta.kt`), que es lo que esa librería exige.
- **Riesgo que resuelve:** todavía no había código de red, así que el cambio no toca lógica. Hacerlo ahora
  evita reescribir los DTOs de la sincronización (#18) más adelante.
