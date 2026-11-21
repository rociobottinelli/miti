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

## Nombres y paquetes según las convenciones de la cátedra (#30)

- **Desvío:** la preentrega (sección 13) nombra los casos de uso sin sufijo (`MarcarRutina`, `RegistrarGasto`…),
  y la fase 1 usaba paquetes en español (`dominio/`, `datos/`, `presentacion/`) con varias clases por archivo.
- **Problema:** la cátedra pide `…UseCase` con una clase por archivo, `…Repository` / `…RepositoryImpl`,
  `…Entity`, `AppDatabase`, mappers `toDomain()` / `toEntity()` y la estructura `core/`, `data/`, `domain/`,
  `presentation/`, `di/`. Con otros nombres, cada revisión obliga a traducir entre el material y el código.
- **Qué cambió:** solo nombres y ubicación de archivos. Los identificadores del dominio siguen en español
  (`Rutina`, `Gasto`, `guardarGasto`), así que hay un solo idioma para el problema; el inglés queda para los
  sufijos técnicos. La Application pasa a ser `MitiApp`, el composable raíz `MitiNavHost`, y las rutas de
  navegación van a `core/navigation`. Las funciones auxiliares de los repositorios pasaron de `private` a
  `internal` (`data/repository/Comunes.kt`) porque ahora las usan varios archivos.
- **Riesgo que resuelve:** el esquema de Room **no cambió** (mismo `identityHash`; solo se movió el JSON a la
  carpeta del paquete nuevo), así que no hace falta migración. Compila y pasan los 61 tests sin tocar su lógica.
- **Pendiente:** las columnas de Room siguen en camelCase. La cátedra prefiere `snake_case` con `@ColumnInfo`,
  pero cambiarlo es un cambio de esquema (versión 2 + migración), así que queda para cuando haya que tocar la
  base por otro motivo (por ejemplo, la detección de conflictos de C2, #20).

## Errores como `Resource` en lugar de excepciones (#30)

- **Problema:** en la fase 1, los casos de uso lanzaban excepciones (`require`, `checkNotNull`) ante datos
  inválidos, falta de hogar activo o ítems inexistentes, y los repositorios no avisaban si Room fallaba. Una
  pantalla que no atrapara la excepción cerraba la app, y "no se pudo guardar" no se distinguía de "no hay datos".
- **Qué cambió:**
  - `core/Resource.kt` (`Success` / `Error`), con `map` y `getOrElse` para encadenar sin `try` en cada caso de uso.
  - Las escrituras de los repositorios devuelven `Resource<Unit>` y `obtenerPendiente`, `Resource<Pendiente?>`.
    `enBase` (en `data/repository/Comunes.kt`) atrapa **solo** `SQLiteException`; la cancelación de la corrutina
    y cualquier otra excepción siguen su curso. Las lecturas siguen siendo `Flow` (Room es la fuente de verdad).
  - Los casos de uso que escriben devuelven `Resource`. Los modelos siguen validándose en su `init`; `validar { }`
    convierte esa validación en `Resource.Error` antes de llegar al repositorio, así **un dato inválido nunca se
    guarda** (hay un test por cada caso).
  - Los fakes de test tienen una `falla` configurable para simular que la base falla.
- **Riesgo que resuelve:** la app no se cierra por un error de datos, y el ViewModel (fase 2) puede mostrar un
  estado `Error` distinto de "vacío", con el mensaje del `Resource`.
- **Limitación conocida:** `TerminarCompraUseCase` guarda el gasto y después cierra los ítems en dos escrituras.
  Si la segunda falla, el gasto queda guardado y la compra sigue abierta. Es raro (la base local falla muy poco),
  pero para resolverlo bien habría que hacer las dos escrituras en una sola transacción de Room; queda anotado.
- **Pendiente:** los mensajes de error son textos en el dominio y la capa de datos. Cuando existan los ViewModels,
  conviene que las pantallas los muestren desde `strings.xml` (por tipo de error) en lugar de mostrar el texto tal cual.
