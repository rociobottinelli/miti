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

## Esquema de Supabase con acceso por hogar (#10)

- **Qué agrega a la preentrega:** la preentrega define las entidades (sección 14) y pide aislar los hogares con
  RLS (RNF10, D11), pero no detalla el esquema remoto. Decisiones tomadas:
  - **El integrante es el usuario de Supabase Auth** (`integrantes.id` = `auth.users.id`). Así las políticas
    usan `auth.uid()` directamente y "quién marcó" o "quién pagó" no se puede falsificar.
  - **`server_updated_at`** además de `updated_at`: `updated_at` es la hora del celular y decide los conflictos
    (D08); `server_updated_at` la pone el servidor y la usa la sincronización (#18) para descargar "lo que cambió
    desde la última vez" sin depender del reloj de cada celular.
  - **Crear un hogar y unirse son funciones del servidor** (`crear_hogar`, `unirse_a_hogar`), no inserts directos:
    el código de 6 caracteres y el límite de dos integrantes se validan donde nadie los puede saltear.
    `unirse_a_hogar` bloquea el hogar mientras cuenta, así dos personas que usan el mismo código a la vez no
    pueden entrar las dos (relacionado con C3).
  - **Claves foráneas compuestas** (`pagador_id, hogar_id`, etc.): quien paga o es responsable tiene que ser del
    mismo hogar que el ítem.
  - Las columnas remotas están en `snake_case`; el mapeo con Room lo harán los DTOs de #18.
  - El SQL vive en `supabase/migrations/` (no en un único `schema.sql` como decía el issue) para poder sumar
    cambios versionados, por ejemplo los de C2 (#20).
- **Riesgo que resuelve:** el aislamiento no depende de que la app filtre bien; lo aplica Postgres para cualquier
  cliente. `supabase/tests/verificar_acceso_por_hogar.sql` lo prueba con cuatro usuarios simulados.

## Cuenta y sesión con Supabase Auth (#16)

- **Qué hace:** crear cuenta, ingresar y cerrar sesión con email y contraseña (preentrega, sección 8), con
  Retrofit + Moshi contra Supabase Auth. La sesión (tokens, no la contraseña) queda en un DataStore propio, y
  `ProveedorDeToken` la renueva un minuto antes de que venza. Lo va a usar la sincronización (#18).
- **Decisiones:**
  - **Validación antes de la red:** email con formato válido y contraseña de 6 caracteres o más (el mínimo de
    Supabase). Al ingresar solo se exige que estén completos, para no revelar nada de una cuenta existente.
  - **Errores separados** según los tres tipos de fallo de red: sin conexión ("Necesitás conexión para
    ingresar"), error HTTP (traducido por código: "Email o contraseña incorrectos", "Ya existe una cuenta con
    ese email"…) y respuesta rota. `JsonEncodingException` se atrapa antes que `IOException` porque hereda de
    ella: un JSON roto no es un problema de conexión.
  - **Cerrar sesión funciona sin conexión:** revocar el token en el servidor es lo mejor posible; en el celular
    se borran siempre la sesión, el hogar activo y la base local (criterio del issue).
  - Si el refresh token ya no sirve (por ejemplo, se cerró la sesión desde otro lado), se borra la sesión local
    y se pide volver a ingresar. Sin conexión, en cambio, la sesión se conserva.
  - El log de red (solo en debug) usa nivel `BASIC`: método, URL y código. Nunca headers (tokens) ni cuerpos
    (contraseñas).
- **Limitaciones conocidas:**
  - Los tokens se guardan en DataStore sin cifrar, dentro del almacenamiento privado de la app. Para el alcance
    del TPO alcanza; una versión real los cifraría con Android Keystore.
  - Cerrar sesión borra la base local aunque haya cambios sin sincronizar. Cuando exista el sync (#18), conviene
    avisar antes ("tenés N cambios sin enviar").

## Sincronización con WorkManager (#18)

- **Qué hace (D06):** `SyncWorker` corre el `Sincronizador`. Primero sube lo `PENDIENTE` de Room y después
  descarga lo que cambió en Supabase desde la última vez. Room sigue siendo la única fuente de verdad (D04):
  la UI ve lo que el sync escribe en Room, nunca la red.
- **Disparadores** (preentrega, sección 14): al abrir la app (`MainActivity.onStart`), después de cada
  escritura (los repositorios avisan con `AvisoDeCambios`), al recuperar la red (el trabajo espera la
  restricción `CONNECTED`) y cada 15 minutos. Ante errores, WorkManager reintenta con espera creciente (RNF09).
- **Decisiones:**
  - **Gana la última modificación también en el servidor** (migración 3): si un celular sube tarde una versión
    más vieja que la guardada, se ignora. Sin esto, el último en *sincronizar* pisaba al último en *editar*.
    Al descargar, una fila remota solo pisa a la local si es igual o más nueva.
  - **Una fila no se marca sincronizada si cambió mientras se subía** (se compara `updatedAt`): ese cambio
    sube en la vuelta siguiente.
  - **Descarga incremental por `server_updated_at`** (hora del servidor, no del celular), con un minuto de
    margen hacia atrás para no perder cambios de transacciones que confirmaron tarde. Repetir filas no hace daño.
  - **Una fila que el servidor rechaza por sus datos no frena a las demás:** el lote se reintenta de a una y la
    rechazada queda `PENDIENTE` (se informa en el resultado).
  - Se mandan los `null` explícitos y la lista de columnas (`columns`) en cada subida: PostgREST toma las
    columnas del primer objeto del lote y podía ignorar, por ejemplo, el `gasto_id` de las demás compras.
  - Si el celular no tiene hogar activo (recién ingresó), lo busca en el servidor.
  - Al cerrar sesión se borran también los cursores de descarga, para que otra cuenta baje todo de nuevo.
  - El sync vive en `data/sync/` (no está en la estructura de la cátedra): junta piezas de `local` y `remote`
    y el worker de WorkManager, que no son ni un repositorio ni una fuente.
- **Cómo se probó:** 15 tests con servidor falso (subida, nulls explícitos, descarga, cursor y margen,
  paginación, conflictos en los dos sentidos, borrado, fila rechazada, error del servidor, sin conexión, cambio
  durante la subida, búsqueda del hogar) y una prueba de **dos celulares contra el Supabase real**
  (`e2e/SincronizacionDosCelularesE2ETest`, solo corre con `MITI_E2E=1`).
- **Limitación conocida (C2, #20):** con "gana la última modificación", si los dos editan el mismo gasto sin
  conexión, una edición se pierde sin aviso. La prueba de punta a punta lo muestra a propósito.

## Splash con la SplashScreen API, sin el texto del Figma (#9)

- **Desvío:** el splash del Figma muestra el logo, la palabra "miti" y el lema "la casa, miti y miti".
- **Problema:** desde Android 12 el sistema dibuja siempre su propio splash al abrir la app, con el fondo y
  el ícono que se le indiquen. Una pantalla de splash propia con el texto se vería *después* del splash del
  sistema (dos splash seguidos) y alargaría el arranque, que RNF04 pide que sea inmediato.
- **Qué cambió:** se usa la SplashScreen API (`androidx.core:core-splashscreen`) con el fondo petróleo y el
  logo del Figma. La librería emula el mismo splash en Android 8 a 11 (minSdk 26). El nombre de la app sigue
  visible debajo del ícono en el launcher; si se quiere mostrar el lema, el lugar es el onboarding (#17).
- **Riesgo que resuelve:** un solo splash, igual en todas las versiones, que desaparece apenas la app está
  lista, en lugar de una pantalla extra que demora la entrada.

## Conflictos en gastos: avisar en vez de perder ediciones (#20, C2)

- **Desvío respecto de la preentrega (D08):** la preentrega aceptaba que, si los dos editaban el mismo gasto sin
  conexión, una edición se perdiera sin aviso. El docente lo marcó (C2) porque contradice la confianza que promete
  la app. **Para los gastos, D08 cambia; para el resto de las tablas sigue igual.**
- **Qué cambió:**
  - Cada edición de un gasto guarda sobre qué versión del servidor se hizo (`baseUpdatedAt`). Si ya había una
    edición sin subir, se conserva la base de la primera, que es la versión que vio la persona.
  - **El servidor detecta el conflicto** (migración 4): si el gasto cambió desde esa versión, la edición no pisa.
    Queda registrada en `conflictos_gasto`, visible solo para quien la hizo, y el gasto se marca como cambiado
    para que los dos celulares bajen la misma versión. No depende del reloj de los celulares: gana quien
    sincronizó primero, y quien perdió se entera.
  - El celular baja sus conflictos y el dominio ofrece `ObservarConflictosDeGastoUseCase` (las dos versiones),
    `RehacerMiEdicionUseCase` (vuelve a aplicar la propia sobre la actual) y `DescartarConflictoUseCase`.
  - **Editar un gasto** (`EditarGastoUseCase`): RF04 lo pedía ("al guardar o editar un gasto") y no existía.
    Borrar un gasto también lleva la versión base, así un borrado no pisa una edición del otro.
- **Room pasa a la versión 2** con `Migration(1, 2)` escrita a mano: agrega una columna en gastos y la tabla
  `conflictos_gasto`, sin tocar datos. Probada con `MigrationTestHelper`: se crea una base v1 con un gasto, se
  migra y se verifica que el gasto sigue igual y la base valida contra el esquema v2.
- **Cómo se probó:**
  - 16 tests nuevos: versión base, editar, rehacer, descartar, sync y migración.
  - El script SQL de verificación suma el caso, y pasa en el Supabase real.
  - **Prueba real con dos celulares:** los dos editan el mismo gasto sin conexión, con el reloj de Tomás
    adelantado una hora (con la regla vieja, la de Tomás habría pisado la de Ana sin aviso). Gana Ana, que
    sincronizó primero; Tomás recibe el aviso con los dos montos; al rehacer, su versión le llega a Ana y el
    aviso desaparece.
- **Limitación:** marcar un conflicto como resuelto se guarda solo en el celular. Si se reinstala la app,
  vuelven a aparecer los conflictos viejos.
