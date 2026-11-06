# Guía de la fase 1: dominio y datos

Qué se agregó en la fase 1 y qué hay que entender para poder explicarlo en la defensa. Continúa la
[guía del esqueleto](guia-esqueleto.md): conviene leer esa primero.

Cubre los issues #4 (Room), #5 (validaciones), #6 (rutinas), #7 (pendientes) y #8 (balance).

1. [El mapa de lo nuevo](#1-el-mapa-de-lo-nuevo)
2. [Kotlin nuevo que aparece en esta fase](#2-kotlin-nuevo-que-aparece-en-esta-fase)
3. [Dominio: validaciones y casos de uso](#3-dominio-validaciones-y-casos-de-uso)
4. [El balance, paso a paso](#4-el-balance-paso-a-paso)
5. [Datos: Room, repositorios y DataStore](#5-datos-room-repositorios-y-datastore)
6. [Hilt: cómo se conecta todo](#6-hilt-cómo-se-conecta-todo)
7. [Los tests](#7-los-tests)
8. [Decisiones que agregan o cambian algo de la preentrega](#8-decisiones-que-agregan-o-cambian-algo-de-la-preentrega)
9. [Limitaciones conocidas](#9-limitaciones-conocidas)

---

## 1. El mapa de lo nuevo

```
app/src/main/java/ar/edu/uade/miti/
├── dominio/
│   ├── modelo/
│   │   ├── Rutina.kt, Pendiente.kt, Gasto.kt  ← ahora validan sus datos (#5)
│   │   └── Balance.kt                          ← Deuda, CuotaDelMes, BalanceDelMes (#8)
│   ├── repositorio/Repositorios.kt             ← contratos ampliados + GeneradorDeIds
│   └── casouso/                                ← NUEVO: las acciones de la app
│       ├── rutinas/CasosDeUsoRutinas.kt        (#6)
│       ├── pendientes/CasosDeUsoPendientes.kt  (#7)
│       └── gastos/CasosDeUsoGastos.kt + CalculadoraDeBalance.kt (#8)
├── datos/                                      ← NUEVO (#4)
│   ├── local/        Entidades, Convertidores, Daos, MitiDatabase, Mappers
│   ├── repositorio/  RepositoriosRoom.kt (implementan los contratos del dominio)
│   └── sesion/       HogarActivo.kt (DataStore)
└── di/DatosModule.kt                           ← NUEVO: recetas de Hilt

app/schemas/.../1.json                          ← esquema de la base, versión 1
app/src/test/java/ar/edu/uade/miti/
├── dominio/fakes/Fakes.kt                      ← repositorios en memoria para tests
├── dominio/...Test.kt                          ← tests de modelos y casos de uso
└── datos/RepositoriosRoomTest.kt               ← tests de Room (con Robolectric)
```

Las tres capas de la preentrega ya existen. La presentación todavía no usa nada de esto: eso llega en la fase 2.

```
presentación (fase 2)  ──►  casos de uso  ──►  interfaces de repositorio
                            (dominio)          (dominio)
                                                   ▲ implementa
                                               RepositoriosRoom  ──►  Room / DataStore
                                               (datos)
```

---

## 2. Kotlin nuevo que aparece en esta fase

| Sintaxis | Qué significa | Dónde aparece |
|---|---|---|
| `class X @Inject constructor(...)` | "Hilt, para crear un X pasale estos parámetros". | Todos los casos de uso y repositorios. |
| `operator fun invoke(...)` | Permite usar el objeto como si fuera una función: `marcarRutina(id, quien)` en vez de `marcarRutina.ejecutar(id, quien)`. | Casos de uso. |
| `fun Tipo.algo()` | **Función de extensión**: le "agrega" una función a un tipo que no es tuyo. `rutina.aEntidad(...)` parece un método de `Rutina`, pero está definida en `Mappers.kt`. | Mappers, `requerirHogarActivo`. |
| `object Algo` | Un único ejemplar, sin estado. Se usa como "caja de funciones". | `CalculadoraDeBalance`. |
| `fun interface` | Interfaz con una sola función. Se puede implementar con una lambda. | `GeneradorDeIds`. |
| `when (x) { A -> ...; B -> ... }` | Como un `switch`, pero devuelve un valor. Con enums, el compilador obliga a cubrir todos los casos. | Validaciones, cálculo del balance. |
| `requireNotNull(x) { "msg" }` / `checkNotNull` | Si `x` es null lanza un error con el mensaje; si no, devuelve `x` ya como no-null. `require` es para argumentos inválidos; `check`, para un estado inválido (por ejemplo, no hay hogar). | Casos de uso. |
| `a ?: b` | **Elvis**: si `a` es null, usá `b`. | `datos.mesInicio ?: YearMonth.now(reloj)`. |
| `x?.let { ... }` | Ejecutá el bloque solo si `x` no es null. | `gasto?.let { registrarGasto(it) }`. |
| `val (a, b) = lista` | **Desestructuración**: saca los primeros elementos en variables. | `ObtenerBalanceDelMes`, `partition`. |
| `groupBy`, `partition`, `filter`, `map`, `sumOf` | Operaciones sobre listas: agrupar por una clave, partir en dos según una condición, filtrar, transformar, sumar. | Casos de uso y calculadora. |

---

## 3. Dominio: validaciones y casos de uso

### 3.1 Validaciones (#5)

Cada modelo valida sus datos en el `init`, así **no puede existir un objeto inválido** en ningún lugar de la app:

| Modelo | Reglas |
|---|---|
| `Rutina` | Título no vacío; 1 a 3 veces por día; si es por días de la semana, al menos un día. |
| `Pendiente` | Título no vacío. Una tarea necesita responsable y fecha. Una compra no tiene responsable, fecha ni aviso. |
| `Gasto` | Descripción no vacía; monto mayor a 0; cuota actual entre 1 y el total ("cuota mayor al total", sección 11); de uno solo necesita beneficiario y a medias no lleva. |
| `Pago` | Monto mayor a 0; no puede ser de alguien a sí mismo. |

`Rutina` además sabe si le toca un día (`correspondeA(fecha)`), y `Gasto` sabe cuánto vale cada cuota (`montoDeCuota`) y qué cuota cae en un mes (`cuotaEn`).

### 3.2 Qué es un caso de uso

Un **caso de uso** es una acción del usuario o una consulta de la app, escrita una sola vez en el dominio. Ejemplos: "marcar una rutina", "terminar la compra", "ver el balance de noviembre".

¿Por qué no se escribe esa lógica directo en la pantalla? Por D03: la misma acción se dispara desde la app, el widget y la notificación. Si cada uno la escribiera por su cuenta, terminarían haciendo cosas distintas.

Todos tienen la misma forma:

```kotlin
class MarcarRutina @Inject constructor(        // Hilt sabe crearlo
    private val rutinas: RutinaRepositorio,    // pide INTERFACES, no Room
    private val reloj: Clock,
    private val ids: GeneradorDeIds,
) {
    suspend operator fun invoke(rutinaId: String, integranteId: String): Boolean { ... }
}
```

- **Recibe interfaces**, no implementaciones: en la app Hilt le pasa Room y en los tests le pasamos fakes (D05).
- **Recibe un `Clock`** (reloj) en vez de preguntar la hora directamente. Así los tests pueden "congelar" la hora y simular, por ejemplo, el cambio de día.
- **Recibe un `GeneradorDeIds`**: en la app genera UUID (D07) y en los tests ids predecibles (`id-1`, `id-2`...).
- Las **acciones** son `suspend` (escriben). Las **consultas** devuelven `Flow` (se actualizan solas).

### 3.3 Rutinas (#6) — [`CasosDeUsoRutinas.kt`](../app/src/main/java/ar/edu/uade/miti/dominio/casouso/rutinas/CasosDeUsoRutinas.kt)

| Caso de uso | Qué hace |
|---|---|
| `RegistrarRutina` | Crea una rutina en el hogar activo. Si es diaria, descarta los días. |
| `ObservarRutinasDelDia(fecha)` | Las rutinas que tocan ese día, con su progreso. |
| `MarcarRutina(rutinaId, integranteId)` | Agrega una marca con la hora actual. |

**`ObservarRutinasDelDia`** combina dos `Flow` con `combine`:

```kotlin
combine(rutinas.observarRutinas(), rutinas.observarRegistrosDelDia(fecha)) { todas, registros ->
    ...
}
```

`combine` emite un valor nuevo **cada vez que cualquiera de los dos cambia**. Si llega una marca nueva (desde el widget o por sincronización), el progreso se recalcula solo.

El resultado es una lista de `RutinaDelDia`, que tiene `hechas` y `completa` **calculados** a partir de las marcas (D09). Por eso no hace falta "reiniciar" nada a medianoche: el día siguiente simplemente no tiene marcas.

**`MarcarRutina` no agrega una marca si la rutina ya está completa** y devuelve `false`. Es una primera respuesta a la corrección C1 ("pueden repetir tareas"): si el celular ya sabe que Pancho comió dos veces, no deja marcar una tercera. Si los dos marcan **sin conexión**, igual quedan ambas marcas y la rutina figura completa, sin superar el máximo (`hechas` está topeado por `vecesPorDia`).

### 3.4 Tareas y compras (#7) — [`CasosDeUsoPendientes.kt`](../app/src/main/java/ar/edu/uade/miti/dominio/casouso/pendientes/CasosDeUsoPendientes.kt)

| Caso de uso | Qué hace |
|---|---|
| `RegistrarTarea` / `RegistrarCompra` | Crean el pendiente en el hogar activo. |
| `CompletarTarea` | Marca la tarea como resuelta. |
| `MarcarEnCarrito` | Tacha (o destacha) un ítem en el supermercado. |
| `TerminarCompra(gasto?)` | Resuelve todo lo del carrito. Si el usuario aceptó registrar el gasto, lo crea y lo vincula a cada ítem. |
| `ObservarProximasTareas(responsable?)` | Tareas sin resolver ordenadas por fecha, opcionalmente filtradas por persona. |
| `ObservarListaDeCompras` | La lista partida en "por comprar" y "en el carrito". |

`TerminarCompra` es el **flujo 3** de la preentrega. Primero crea el gasto (para tener su id) y después guarda los ítems apuntando a él. Lo que no estaba en el carrito sigue en la lista.

Notar que **un caso de uso puede usar otro**: `TerminarCompra` recibe `RegistrarGasto` en vez de repetir cómo se crea un gasto.

### 3.5 Gastos y pagos (#8) — [`CasosDeUsoGastos.kt`](../app/src/main/java/ar/edu/uade/miti/dominio/casouso/gastos/CasosDeUsoGastos.kt)

| Caso de uso | Qué hace |
|---|---|
| `RegistrarGasto(DatosDeGasto)` | Crea el gasto. Sin mes, usa el actual. |
| `RegistrarPago(de, a, monto)` | Registra un pago total o parcial. Sin fecha, usa la de hoy. |
| `ObtenerBalanceDelMes(mes)` | El balance de ese mes, que se recalcula solo (RF04). Emite `null` si el hogar todavía no tiene dos integrantes. |

`DatosDeGasto` es lo que completa el usuario en el formulario. El id y el hogar los agrega el caso de uso: la pantalla no tiene por qué conocerlos.

---

## 4. El balance, paso a paso

La regla vive en [`CalculadoraDeBalance.kt`](../app/src/main/java/ar/edu/uade/miti/dominio/casouso/gastos/CalculadoraDeBalance.kt). Es una **función pura**: recibe gastos y pagos y devuelve el resultado, sin leer ni guardar nada. Por eso se puede probar con cualquier combinación (RNF11).

### La idea: un solo número, el "saldo"

En vez de llevar "cuánto le debe Ana a Tomás" y "cuánto le debe Tomás a Ana" por separado, todo se suma en un único número visto desde un integrante (A):

- **positivo**: B le debe a A;
- **negativo**: A le debe a B;
- **cero**: están a mano.

Cada gasto y cada pago suma o resta:

| Movimiento | Efecto sobre el saldo |
|---|---|
| A pagó un gasto | `+` lo que le toca a B |
| B pagó un gasto | `−` lo que le toca a A |
| B le pagó a A | `−` el monto (B debe menos) |
| A le pagó a B | `+` el monto |

"Lo que le toca al otro" de una cuota es la mitad si es **a medias**, toda si es **de uno solo** para el otro, o nada si es de uno solo para quien pagó.

### El ejemplo de la preentrega

Tomás paga una heladera de $30.000 en 3 cuotas, a medias, desde noviembre. A = Ana, B = Tomás.

| Mes | Saldo anterior | Cuota del mes | Pagos | Resultado |
|---|---|---|---|---|
| Noviembre | 0 | Tomás pagó $10.000 → Ana debe $5.000 | — | Ana le debe $5.000 |
| Diciembre | Ana debe $5.000 | Ana debe $5.000 más | — | Ana le debe $10.000 |
| Diciembre (con pago) | Ana debe $5.000 | Ana debe $5.000 más | Ana paga $10.000 | Están a mano |

El **saldo anterior** es la suma de todas las cuotas y pagos de los meses anteriores. Así se arrastra lo que no se saldó, como pide RF04.

### Detalles que conviene saber explicar

- **Todo en centavos (`Long`)**, para que no haya errores de redondeo.
- **Cuotas que no dan exacto**: $10 en 3 cuotas son 334 + 333 + 333 centavos. El resto se reparte en las primeras cuotas, así la suma da siempre el total.
- **A medias con monto impar**: el centavo que sobra lo absorbe quien pagó (de $10,01, el otro debe $5,00).
- **Compras ya empezadas**: con `cuotaInicial = 3`, las cuotas 1 y 2 se consideran pagadas antes de usar la app y no suman.
- **Orden de los integrantes**: `ObtenerBalanceDelMes` ordena los ids alfabéticamente para decidir quién es A. Así los dos celulares calculan exactamente lo mismo. El resultado no depende del orden (hay un test que lo verifica).

---

## 5. Datos: Room, repositorios y DataStore

### 5.1 Las tablas — [`Entidades.kt`](../app/src/main/java/ar/edu/uade/miti/datos/local/Entidades.kt)

Una **entidad** es una clase que Room convierte en una tabla de SQLite: cada propiedad es una columna y cada objeto, una fila.

```kotlin
@Entity(tableName = "rutinas", indices = [Index("hogarId")])
data class RutinaEntidad(
    @PrimaryKey val id: String,
    val hogarId: String,
    ...
    @Embedded val sync: MetadatosSync,
)
```

- **`@Entity`** marca la clase como tabla. **`@PrimaryKey`**, la columna que identifica cada fila.
- **`Index("hogarId")`** hace que buscar por hogar sea rápido, como el índice de un libro.
- **`@Embedded val sync: MetadatosSync`** agrega las columnas `updatedAt`, `deleted` y `syncState` en la misma tabla. Se escriben una vez y se reusan en todas.

**¿Por qué entidades separadas de los modelos?** Porque las tablas tienen datos que al dominio no le importan (`updatedAt`, `deleted`, `syncState`). Los **mappers** ([`Mappers.kt`](../app/src/main/java/ar/edu/uade/miti/datos/local/Mappers.kt)) traducen entre una cosa y otra. Si mañana cambia la sincronización (por ejemplo, por C2), el dominio no se entera.

**`EstadoSync`**: cada cambio local queda `PENDIENTE` hasta que el `SyncWorker` (issue #18) lo envíe y lo marque `SINCRONIZADO` (D06).

**Sin claves foráneas, a propósito.** Con la sincronización, una fila puede llegar antes que aquella a la que apunta (por ejemplo, la marca de una rutina antes que la rutina). Una clave foránea rechazaría esa fila.

### 5.2 Guardar tipos que SQLite no conoce — [`Convertidores.kt`](../app/src/main/java/ar/edu/uade/miti/datos/local/Convertidores.kt)

SQLite solo guarda números y texto. Un **`@TypeConverter`** le enseña a Room a convertir los demás tipos:

| Tipo | Se guarda como |
|---|---|
| `LocalDateTime` | `"2026-11-05T08:15:00"` |
| `LocalDate` / `YearMonth` | `"2026-11-05"` / `"2026-11"` |
| `Set<DayOfWeek>` | `"MONDAY,THURSDAY"` |
| enums (`Frecuencia`, `Reparto`...) | su nombre (Room lo hace solo) |

Las fechas en formato ISO tienen una ventaja: **se ordenan bien como texto**. Por eso la consulta "marcas entre las 00:00 de hoy y las 00:00 de mañana" funciona comparando textos. Para eso se recortan los milisegundos: así todas las fechas tienen el mismo largo.

### 5.3 Las consultas — [`Daos.kt`](../app/src/main/java/ar/edu/uade/miti/datos/local/Daos.kt)

Un **DAO** es una interfaz con las consultas. Room escribe el código al compilar (con KSP):

```kotlin
@Query("SELECT * FROM rutinas WHERE hogarId = :hogarId AND deleted = 0 ORDER BY titulo")
fun observarRutinas(hogarId: String): Flow<List<RutinaEntidad>>

@Upsert
suspend fun guardarRutina(rutina: RutinaEntidad)
```

- **`@Query`** es SQL. `:hogarId` se reemplaza por el parámetro.
- **Si devuelve `Flow`, Room vuelve a emitir cada vez que cambia la tabla.** Esa es la base de D04: la pantalla se actualiza sola.
- **`@Upsert`** inserta, o reemplaza si ya existe una fila con ese id.
- **Todas las lecturas filtran `deleted = 0`**: borrar es marcar la fila (D07), y esas filas no se muestran.
- Si un `@Query` falla (una columna mal escrita, por ejemplo), **no compila**. Room revisa el SQL contra las tablas.

### 5.4 La base — [`MitiDatabase.kt`](../app/src/main/java/ar/edu/uade/miti/datos/local/MitiDatabase.kt)

Junta las tablas, los convertidores y los DAOs. Tiene `version = 1`. **Si se cambia una tabla hay que subir la versión y escribir una migración**, o los celulares que ya tienen la app instalada fallan al abrirla. El esquema de cada versión queda guardado en `app/schemas/` para poder comparar.

### 5.5 Los repositorios de Room — [`RepositoriosRoom.kt`](../app/src/main/java/ar/edu/uade/miti/datos/repositorio/RepositoriosRoom.kt)

Implementan las interfaces del dominio. Hacen tres cosas:

1. **Leer del hogar activo.** Toman el id del hogar desde `HogarActivo` y consultan el DAO con ese id. Usan `flatMapLatest`: si cambia el hogar, abandonan la consulta anterior y empiezan una nueva. Sin hogar, devuelven una lista vacía.
2. **Traducir** entidades a modelos con los mappers.
3. **Marcar cada escritura como pendiente**, con la hora del cambio (`updatedAt`), para que la sincronización sepa qué enviar y quién gana un conflicto (D08).

### 5.6 El hogar activo — [`HogarActivo.kt`](../app/src/main/java/ar/edu/uade/miti/datos/sesion/HogarActivo.kt)

Se guarda en **DataStore**, una forma simple de guardar pares clave-valor (sección 14 de la preentrega). Es una interfaz con una implementación, así los tests usan una versión en memoria. La sesión, el onboarding visto y la última sincronización se suman acá en el issue #16.

---

## 6. Hilt: cómo se conecta todo

[`DatosModule.kt`](../app/src/main/java/ar/edu/uade/miti/di/DatosModule.kt) tiene las **recetas** que usa Hilt para construir las cosas.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object BaseDeDatosModule {
    @Provides @Singleton
    fun baseDeDatos(@ApplicationContext context: Context): MitiDatabase = Room.databaseBuilder(...).build()
    @Provides fun rutinaDao(base: MitiDatabase): RutinaDao = base.rutinaDao()
    @Provides @Singleton fun reloj(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {
    @Binds abstract fun rutinaRepositorio(impl: RutinaRepositorioRoom): RutinaRepositorio
    ...
}
```

| Anotación | Significa |
|---|---|
| `@Module` | "Acá hay recetas". |
| `@InstallIn(SingletonComponent::class)` | Las recetas valen para toda la app. |
| `@Provides` | "Para obtener X, ejecutá esta función." Se usa cuando hay que construir algo a mano, como la base. |
| `@Binds` | "Cuando pidan la interfaz X, dale la clase Y." Así el dominio pide `RutinaRepositorio` y recibe `RutinaRepositorioRoom`. |
| `@Singleton` | Se crea una sola vez. Hay **una sola** base de datos para toda la app. |
| `@Inject constructor` | En las clases propias, "sabés construirme con estos parámetros". |

Cuando la pantalla Balance (fase 2) pida `ObtenerBalanceDelMes`, Hilt va a armar toda la cadena sola: `ObtenerBalanceDelMes` ← `GastoRepositorioRoom` ← `GastoDao` ← `MitiDatabase` ← `Context`.

En `app/build.gradle.kts` se activó `dagger.fullBindingGraphValidation`. Así Hilt verifica al compilar que todas las recetas se puedan cumplir, aunque todavía ninguna pantalla las use.

---

## 7. Los tests

```bash
./gradlew testDebugUnitTest
```

Son 61 tests y corren en la PC, sin emulador.

| Archivo | Qué prueba |
|---|---|
| `RutinaTest`, `PendienteTest`, `GastoTest` | Las validaciones y el cálculo de cuotas. |
| `CasosDeUsoRutinasTest` | Rutinas del día, progreso, cambio de día, marcas simultáneas, no repetir una rutina completa. |
| `CasosDeUsoPendientesTest` | Tareas ordenadas y filtradas, carrito, terminar la compra con y sin gasto. |
| `CalculadoraDeBalanceTest` | **RNF11**: el ejemplo de la preentrega, 50/50, 100/0, cuotas, compras empezadas, pagos parciales, saldo arrastrado, pagos de más, redondeos. |
| `CasosDeUsoGastosTest` | El balance se actualiza solo al registrar gastos y pagos. |
| `RepositoriosRoomTest` | Room de verdad (en memoria): guardar y leer, rango de fechas, borrado lógico, aislamiento entre hogares, balance de punta a punta. |

Herramientas nuevas:

- **Fakes** ([`Fakes.kt`](../app/src/test/java/ar/edu/uade/miti/dominio/fakes/Fakes.kt)): repositorios que guardan todo en un `MutableStateFlow`. Se comportan como Room (avisan cuando cambian), pero en memoria. Es D05 en acción.
- **`relojEn(...)`**: un `Clock` congelado en una fecha y hora. Los tests no dependen de cuándo se corren.
- **`runTest { ... }`**: permite llamar funciones `suspend` dentro de un test.
- **Turbine** (`flow.test { awaitItem() }`): permite verificar **cada valor** que emite un `Flow`. Por ejemplo: "primero a mano; registro un gasto → Ana debe $12.000; registro el pago → a mano otra vez".
- **Robolectric** (`@RunWith(RobolectricTestRunner::class)`): simula Android en la PC, así Room corre en un test unitario. Se sumó al catálogo en esta fase.

---

## 8. Decisiones que agregan o cambian algo de la preentrega

La consigna pide poder justificar cualquier cambio respecto de la preentrega. Estos son los de esta fase:

| Decisión | Por qué |
|---|---|
| `Pendiente` tiene un campo nuevo, **`enCarrito`**. | La preentrega dice que la compra "pasa al carrito" antes de terminarse, pero la tabla de entidades (sección 14) no tenía dónde guardarlo. |
| **`MarcarRutina` no agrega marcas a una rutina completa.** | Evita repetir tareas cuando el celular ya lo sabe (corrección C1). Sin conexión, igual pueden quedar dos marcas. |
| Los casos de uso reciben un **`Clock`** y un **`GeneradorDeIds`**. | Para poder probar con hora e ids fijos. |
| Los repositorios **no reciben el hogar**: usan el hogar activo de DataStore. | Un usuario pertenece a un solo hogar, y así ninguna pantalla puede pedir por error datos de otro. |
| Se agregaron operaciones de **borrado lógico** (`eliminar...`). | La sección 14 las describe ("borrado vs. edición") y D07 las requiere. |
| **Sin claves foráneas** en Room. | La sincronización puede traer filas en cualquier orden. |
| `ObtenerBalanceDelMes` emite **`null`** con menos de dos integrantes. | No hay entre quiénes repartir. La pantalla deberá mostrar un estado para eso. |
| Redondeos: el resto de las cuotas va a las primeras y el centavo impar de "a medias" lo absorbe quien pagó. | La preentrega no lo definía, y el balance tiene que ser exacto (RNF11). |
| Se sumó **Robolectric** a los tests. | Para probar Room sin emulador. |

---

## 9. Limitaciones conocidas

- **`TerminarCompra` guarda el gasto y los ítems en dos pasos.** Si la app se cerrara justo en el medio, quedaría un gasto sin ítems vinculados. Para resolverlo haría falta una transacción que abarque dos repositorios.
- **`ObservarRutinasDelDia` recibe la fecha desde afuera.** Si la pantalla queda abierta a medianoche, tiene que volver a pedirla con la fecha nueva. El widget también se tiene que actualizar a medianoche (issue #22).
- **Las columnas de sincronización se escriben pero todavía nadie las lee**: el envío y la descarga son el issue #18.
- **Los conflictos siguen siendo "gana la última modificación"** (D08). El cambio pedido en la corrección C2 es el issue #20.
- **El hogar y los integrantes no se crean desde la app todavía**: eso llega con la autenticación y el primer uso (#16 y #17).
