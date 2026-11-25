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

## Indicadores de sincronización y datos desactualizados (#19, C1 y C4)

- **Problema (C1, C4):** si la información llega tarde, se pueden repetir tareas. El docente pidió definir cómo
  advertir datos desactualizados.
- **Qué cambió (datos y dominio; las pantallas lo conectan):**
  - `ObservarEstadoDeSincronizacionUseCase` da en un solo `Flow`: última sincronización, si hay conexión, si
    está sincronizando, si el último intento falló y se reintenta, cuántos cambios faltan enviar y si los datos
    están desactualizados. Junta DataStore, el estado de la red (`MonitorDeConexion`), WorkManager y Room.
  - `ObservarItemsSinEnviarUseCase` da los ids con cambios sin enviar, para el ícono de cada ítem (RNF09) sin
    meter el estado de sincronización en los modelos del dominio. Una rutina figura si tiene una marca sin enviar.
  - `SincronizarAhoraUseCase` para el botón Reintentar.
- **Umbral de "datos desactualizados" (regla `DatosDesactualizados` en el dominio, con tests):** nunca se
  sincronizó, o la última sincronización completa fue hace **más de 30 minutos**. Con conexión se sincroniza al
  abrir la app, después de cada cambio y cada 15 minutos, así que pasar los 30 minutos indica un problema (sin
  conexión o un error que se repite). Se recalcula cada minuto, aunque no pase nada más.
- **Qué se le muestra a la persona:**
  - Siempre que no hay conexión: "Sin conexión · última sincronización hh:mm" (`BannerSinConexion`, #37).
  - Si el último intento falló: el aviso de error con Reintentar, aclarando que los cambios siguen guardados.
  - En cada ítem sin enviar: el ícono de pendiente (`IndicadorPendienteDeSync`, #37).
  - Si los datos están desactualizados, al marcar una rutina: avisar que la otra persona pudo haberla hecho ya.
  - Todo desaparece solo al sincronizar, porque se calcula de Room y DataStore.
- Sin cambio de esquema de Room (solo una consulta nueva).
## Avisos de tareas con alarmas locales (#23) y aviso de tareas nuevas (#24)

- **Qué hace (D10):** las tareas con aviso suenan en el celular de su responsable a la hora fijada, sin conexión
  y sin servidor de push. La notificación tiene la acción **"Marcar como hecha"**, que usa el mismo
  `CompletarTareaUseCase` que la app (D03). Cuando la sincronización baja una tarea nueva asignada a quien usa
  el celular, avisa "Te asignaron una tarea" (#24).
- **Decisiones:**
  - **Qué suena lo decide una regla del dominio** (`TareasConAviso`, con tests): con aviso, sin resolver, del
    usuario de este celular y todavía no vencida. Todo lo demás se cancela.
  - **Un único punto que pone las alarmas al día** (`ReprogramarAvisosUseCase`, corrido por `AvisosWorker`).
    Siempre parte de Room, así que llamarlo de más no hace daño. Se dispara después de cada cambio local, al
    terminar una sincronización que bajó algo, al abrir la app, al cerrar sesión (cancela todo) y al reiniciar
    el teléfono o cambiar la hora (`ReceptorDeReinicio`), porque Android borra las alarmas al apagarse.
  - **Alarma exacta si el sistema lo permite y aproximada si no** (D10). Android 12+ puede negar las exactas: la
    alarma suena igual, pero sin garantizar los ±5 min de RNF06. `ConsultarPermisosDeAvisosUseCase` le dice a
    Hoy si faltan los permisos de notificaciones o de alarmas exactas, para mostrar el aviso de la preentrega.
  - Al sonar, se vuelve a mirar Room: si la tarea ya se resolvió (por ejemplo, la otra persona y llegó con la
    sincronización), no se avisa.
  - #24 no avisa en la primera descarga (al ingresar se baja todo el hogar; no son tareas "nuevas"), ni dos veces
    la misma tarea (solo cuenta si no estaba en el celular).
- **Cómo se probó:**
  - Tests de la regla y de la reprogramación.
  - Alarmas reales de Android con Robolectric: exacta y aproximada, cancelar sin tocar las demás, reprogramar sin
    duplicar.
  - Notificación con su acción, sin permiso, tareas nuevas agrupadas.
  - #24 en el sync: solo las propias, nada en la primera descarga ni dos veces.
- **Pendiente de probar en un celular:** RNF06 en modo avión con un reinicio intermedio. Se hace a mano en un
  dispositivo y se anota en las pruebas de #25.
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

## Pantalla Balance y formulario de gasto (#14, #15)

- **Qué hace (RF04):**
  - **Balance:** selector de mes (no deja ir al futuro), la tarjeta "Ana le debe $ X a Tomás" / "Están a mano"
    con Registrar pago y lo que puso cada uno, el saldo de meses anteriores, los movimientos del mes (cada gasto
    con la cuota que cae en el mes, "A medias" / "Solo Ana" y "Cuota 2 de 6", y los pagos) y los estados de carga,
    vacío, sin la otra persona y error con Reintentar.
  - **Formulario de gasto** para registrar y editar: monto grande con error al lado, descripción, quién pagó,
    cómo se reparte, cuotas iguales y borrar (con confirmación).
  - **Hoja de pago:** dirección, monto (propone saldar la deuda entera) y fecha.
  - **Avisos de conflicto (C2)** arriba de todo: "Tu cambio en «Heladera» no se guardó. Vos pusiste $ X y quedó
    $ Y", con "Usar mi cambio" o "Dejar como está".
- **Desvíos respecto del Figma:**
  - **Sin íconos por categoría** en los movimientos: el Figma muestra supermercado, auto, gas, etc., pero las
    categorías están fuera del alcance de la preentrega (sección 8). Todos usan un ícono genérico de comprobante.
  - **El formulario de gasto es una pantalla y no una hoja inferior.** Como destino de navegación tiene su propio
    ViewModel, sobrevive a la rotación y a la muerte del proceso (`SavedStateHandle`), y lo pueden abrir Balance y,
    más adelante, Compras al terminar una compra. Se ve igual (cierre con X arriba a la izquierda).
  - Los chips "Estado demo" del Figma son controles del prototipo; no van en la app.
- **Cómo se probó:**
  - Tests de los dos ViewModels (AGENTS.md §14): carga inicial, validación, una solicitud inválida nunca guarda,
    guardado, error de escritura, editar, borrar, deuda y totales por persona, cuotas del mes, pago propuesto y
    validado, cambiar dirección, conflictos y meses.
  - Tests del formato de dinero ("$ 18.750", "18.750,5").
  - Previews de cada estado.
- **Pendiente:** migrar `hiltViewModel` al paquete nuevo (`androidx.hilt.lifecycle.viewmodel.compose`) en todas las
  pantallas a la vez (Hoy también lo usa).

## "Tu hogar": el código aparece después de crear el hogar (#17)

- **Desvío:** el Figma (37:780) muestra la tarjeta "Crear un hogar" con el código ya generado y los botones
  Copiar y Compartir, al lado de "Unirme con un código".
- **Problema:** el código lo genera el servidor al crear el hogar (`crear_hogar`, #10). Para mostrarlo antes
  habría que crear el hogar apenas se entra a la pantalla, y entonces quien viene a unirse con el código de
  su conviviente ya tendría un hogar propio y el servidor le diría "Ya formás parte de un hogar".
- **Qué cambió:** la tarjeta "Crear un hogar" tiene un botón **Crear mi hogar**. Al tocarlo se crea el hogar y
  la pantalla pasa a mostrar el código grande con Copiar y Compartir (como en el Figma) y un botón
  **Continuar**. Unirse sigue igual. El hogar se crea con el nombre "Nuestro hogar", porque el diseño no lo
  pide. Por ahora no se puede cambiar: el nombre no se muestra en ninguna pantalla.
- **Además:** al entrar, "Tu hogar" primero busca si la persona ya tenía uno (cambió de celular o reinstaló).
  Si lo encuentra, sigue directo sin pedirle nada.
- **Riesgo que resuelve:** que la persona que se une quede trabada con un hogar vacío creado sin querer.

## Arranque del primer uso decidido por el estado, no por la navegación (#17)

- **Qué hace:** una sola regla del dominio (`ObservarInicioUseCase`, con tests) decide dónde arranca la app:
  onboarding la primera vez, cuenta sin sesión, "Tu hogar" con sesión y sin hogar, y Hoy con las dos cosas.
  `RaizViewModel` la observa y cambia de pantalla solo: al crear la cuenta, al tener hogar o al cerrar sesión.
- **Por qué así:** las pantallas no tienen que saber a dónde ir después de cada acción, y no puede pasar que
  alguien con sesión vea el onboarding o que al cerrar sesión quede adentro. El splash del sistema se queda
  hasta saber el destino, así no hay pantallazos.
- **Permiso de notificaciones:** se pide una vez, al terminar el primer uso, con una pantalla que explica para
  qué es antes del diálogo de Android (13 o más; en versiones anteriores no hace falta). Sin permiso la app
  funciona igual; solo no suenan los avisos de tareas.


## Compras: no repetir ítems y terminar la compra sin el pop up por ahora (#13)

- **Problema:** con la lista compartida, los dos pueden agregar "Leche" casi a la vez; es la versión de
  Compras de "si la información llega tarde, pueden repetir tareas" (corrección C1).
- **Qué cambió:** `RegistrarCompraUseCase` no agrega un ítem si ya hay uno sin comprar con el mismo nombre
  (sin importar mayúsculas ni espacios de más) y avisa "Ya está en la lista". Lo ya comprado no cuenta: se
  puede volver a pedir. La pantalla muestra además los avisos de sincronización de Hoy (sin conexión, error,
  datos desactualizados) y el ícono de pendiente en cada ítem.
- **Qué riesgo resuelve:** ítems repetidos cuando los dos celulares están al día. **Limitación:** si los dos
  agregan el mismo ítem sin conexión, cada celular no ve el del otro y quedan dos; el aviso de datos
  desactualizados es lo que lo advierte.
- **Desvío temporal:** la preentrega (flujo 3) ofrece registrar el gasto al terminar la compra. Hasta que esté
  el formulario de gasto (#44), "Terminé la compra" la cierra sin gasto. `TerminarCompraUseCase` ya recibe el
  gasto opcional, así que sumar el pop up no cambia el dominio. **Resuelto:** ver "Terminar la compra ofrece registrar el gasto".


## Widgets de rutinas: cómo se mantienen al día y qué quedó afuera (#22)

- **Problema:** el widget tiene que mostrar lo mismo que la app en menos de 5 segundos (RNF05), también cuando
  marca la otra persona, y reiniciarse a la medianoche aunque nadie abra Miti.
- **Qué cambió:**
  - Los dos widgets (4x2 y 2x2) leen un mismo estado, `ObservarEstadoDelWidget`, armado con los casos de uso de
    Hoy. Marcar usa `MarcarRutinaUseCase` vía Hilt (D03).
  - Mientras el proceso de Miti está vivo (app abierta, o el `SyncWorker` trayendo cambios del otro celular),
    `ActualizadorDeWidgets` escucha ese estado y actualiza los widgets apenas cambia Room.
  - Con la app cerrada, una alarma no exacta (`AlarmaDeMedianoche`, sin permiso de alarmas exactas) los actualiza
    a la medianoche; se reprograma al reiniciar el celular. `updatePeriodMillis` de 30 minutos queda de respaldo.
- **Qué riesgo resuelve:** que el widget muestre rutinas de ayer o no vea la marca del otro (C1).
- **Desvíos:**
  - El widget **no pide confirmación** al marcar con datos desactualizados, como sí hace Hoy: un widget no puede
    mostrar diálogos y el requisito es un toque (RNF03). En su lugar, el encabezado muestra "Sin conexión · hh:mm"
    o "Puede no estar al día".
  - El Figma muestra un ícono por rutina (huella, planta, tacho); el modelo `Rutina` de la preentrega no tiene
    ícono, así que el widget no lo muestra.
  - En Android 11 o anterior las esquinas del widget son rectas (Glance solo redondea desde Android 12).

## Terminar la compra ofrece registrar el gasto (flujo 3)

- **Problema:** "Terminé la compra" cerraba la compra sin gasto, así que el flujo 3 de la preentrega
  (compra → gasto → balance) se cortaba en el medio y había que cargar el gasto aparte desde Balance.
- **Qué cambió:** "Terminé la compra" abre el pop up "¿Registrar el gasto?". "Ahora no" la cierra sin gasto,
  como antes. "Registrar gasto" abre el mismo formulario de gasto (`Ruta.FormularioDeGasto(deCompra = true)`,
  título "Gasto de la compra"), y guardarlo llama a `TerminarCompraUseCase` con los datos: crea el gasto y
  deja los ítems del carrito comprados y vinculados a él. Tocar afuera del pop up o cerrar el formulario
  deja la compra abierta.
- **Qué riesgo resuelve:** que el gasto del súper quede sin cargar y el balance no refleje lo que se gastó.
  Si mientras se completa el formulario la otra persona termina la compra, el carrito queda vacío y el
  formulario avisa "No hay nada en el carrito" sin registrar un gasto suelto.
- **Cómo se probó:** tests del ViewModel del formulario (cierra la compra y vincula solo lo del carrito,
  carrito vacío, falla al guardar con la compra todavía abierta).

## La tarjeta de conflicto dice qué campo cambió (#51, C2)

- **Problema:** la tarjeta comparaba solo montos. Si el conflicto era por quién pagó o por el reparto, decía
  "Vos pusiste $ 31.000 y quedó $ 31.000" y no se entendía qué se perdía.
- **Qué cambió:** `ConflictoDeGasto.camposDistintos` compara las dos versiones (descripción, monto, quién pagó,
  reparto, cuotas) y la tarjeta muestra una línea por cada campo distinto: "Quién pagó: vos pusiste Tomás y quedó Ana."
- **Qué riesgo resuelve:** que alguien elija "Usar mi cambio" o "Dejar como está" sin saber qué está eligiendo.
- **Cómo se probó:** tests del dominio (cada campo, varios a la vez, gasto borrado) y del ViewModel (conflicto por
  pagador, por reparto y sin gasto); caso 9.7 de la guía de testing en dos celulares.

## Con la app en pantalla se sincroniza cada 15 segundos (C1)

- **Problema:** con los dos celulares con la app abierta, lo que cambiaba uno no le llegaba al otro hasta
  cerrar y volver a abrir la app, o hasta la sincronización periódica (15 min). Es el punto débil que marcó la
  corrección C1.
- **Qué cambió:** mientras la app está en pantalla, `MainActivity` sincroniza al volver a primer plano y después
  cada 15 s (`repetirCada(SYNC_EN_PANTALLA)` dentro de `repeatOnLifecycle(STARTED)`, que corta solo en `onStop`).
  En segundo plano queda la periódica de 15 min. No cambia el backend: es el mismo `SyncWorker` (D06) más seguido,
  y cada consulta baja solo lo nuevo.
- **Por qué no Realtime de Supabase:** daría 1–2 s en vez de 15, pero suma una librería y se parece a "push desde
  el servidor", que la preentrega deja fuera de alcance. Si hiciera falta, se puede sumar encima sin tirar esto.
- **Cómo se probó:** tests con tiempo virtual (sincroniza al entrar, cada 15 s y deja de hacerlo al salir).
