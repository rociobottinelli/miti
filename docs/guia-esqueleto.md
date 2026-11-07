# Guía del esqueleto de Miti

Recorrido por todo lo que tiene el proyecto hoy, pensado para alguien que nunca usó Kotlin ni Jetpack Compose.
Está dividido en tres partes:

1. [El proyecto y cómo arranca la app](#parte-1-el-proyecto-y-cómo-arranca-la-app)
2. [La interfaz: Compose, pantallas y navegación](#parte-2-la-interfaz-compose-pantallas-y-navegación)
3. [El dominio: modelos, repositorios y tests](#parte-3-el-dominio-modelos-repositorios-y-tests)

Las referencias del tipo D01, RF04 o RNF11 son las decisiones y requisitos de la
[preentrega](preentrega-miti.pdf).

---

## Parte 1: el proyecto y cómo arranca la app

### El mapa

```
miti/
├── settings.gradle.kts      ← "el proyecto se llama Miti y tiene un módulo: app"
├── build.gradle.kts         ← configuración general (declara los plugins)
├── gradle.properties        ← opciones de Gradle
├── gradle/
│   ├── libs.versions.toml   ← el catálogo: todas las librerías y sus versiones
│   └── wrapper/             ← descarga y fija la versión de Gradle
├── gradlew / gradlew.bat    ← scripts para compilar (Linux/Mac y Windows)
└── app/                     ← la app en sí
    ├── build.gradle.kts     ← cómo se compila la app y qué librerías usa
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml     ← la "ficha técnica" de la app para Android
        │   ├── res/                    ← recursos: textos, íconos, colores
        │   └── java/ar/edu/uade/miti/  ← el código Kotlin
        └── test/                       ← tests unitarios
```

Hay dos mundos separados:

- **El código de la app** (`app/src/`): lo que se ejecuta en el celular.
- **La configuración de compilación** (los `.gradle.kts` y el `.toml`): le dice a una herramienta cómo convertir ese código en un `.apk` instalable.

### Gradle: la herramienta que compila

**Gradle** lee la configuración, descarga las librerías, compila el código Kotlin y empaqueta todo en un `.apk`.

```bash
./gradlew assembleDebug        # arma la versión de desarrollo de la app
./gradlew testDebugUnitTest    # corre los tests unitarios
```

#### `gradlew` y `gradle/wrapper/`

El *wrapper* garantiza que todos usen la misma versión de Gradle (9.5.0). Quien clone el repo no necesita tener Gradle
instalado: `gradlew` baja esa versión exacta. Por eso esos archivos van commiteados.

#### `settings.gradle.kts`

```kotlin
rootProject.name = "Miti"
include(":app")
```

Dice cómo se llama el proyecto y qué módulos tiene. El resto del archivo lista de dónde se bajan las librerías:
`google()` (todo lo de Android) y `mavenCentral()` (librerías generales de Java/Kotlin, como Retrofit o Hilt).

#### `gradle/libs.versions.toml`: el catálogo

```toml
[versions]
room = "2.8.4"                 # un nombre y una versión

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
#                        quién la publica        cómo se llama          usa la versión de arriba

[plugins]
room = { id = "androidx.room", version.ref = "room" }
```

Todas las versiones quedan en un solo lugar: para subir Room se cambia una línea. Desde los otros archivos se las nombra
como `libs.androidx.room.runtime` (los guiones pasan a ser puntos).

#### Librería vs. plugin

- Una **librería** es código que la app usa al ejecutarse (Room da funciones para guardar en la base). Viaja dentro del `.apk`.
- Un **plugin** le enseña a Gradle a hacer cosas nuevas mientras compila. No viaja dentro de la app.

| Plugin | Qué le enseña a Gradle |
|---|---|
| `android.application` | Compilar una app Android. |
| `kotlin.compose` | Entender el código de Compose (las funciones `@Composable`). |
| `kotlin.serialization` | Convertir objetos Kotlin a texto (JSON) y viceversa. Lo usan Retrofit y las rutas de navegación. |
| `ksp` | Correr generadores de código al compilar. Hilt y Room escriben código por nosotros y KSP los ejecuta. |
| `hilt` | Preparar la inyección de dependencias. |
| `room` | Configurar Room, por ejemplo dónde guardar el esquema de la base. |

#### `build.gradle.kts` (raíz)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    ...
}
```

Declara los plugins para el proyecto sin usarlos acá (`apply false`). Los aplica cada módulo; en nuestro caso, solo `app`.

#### `app/build.gradle.kts`

**a) Los plugins que se aplican de verdad**, en el bloque `plugins { ... }`.

**b) La configuración de Android:**

```kotlin
android {
    namespace = "ar.edu.uade.miti"        // el "apellido" del código Kotlin
    compileSdk { version = release(37) }  // se compila con las herramientas de Android 17
    defaultConfig {
        applicationId = "ar.edu.uade.miti" // ID único de la app (el de la Play Store)
        minSdk = 26                        // no se instala en Android menor a 8.0 (RNF12)
        targetSdk = 37                     // versión de Android para la que está pensada
        versionCode = 1                    // número interno de versión
        versionName = "0.1.0"              // versión que ve el usuario
    }
    buildFeatures { compose = true }       // activa Jetpack Compose
}

room {
    schemaDirectory("$projectDir/schemas") // esquemas versionados para escribir migraciones
}
```

Cada versión de Android tiene un número de "API": Android 8.0 es la 26.

**c) Las dependencias.** Lo importante es la palabra del principio:

| Palabra | Significa |
|---|---|
| `implementation(...)` | La app usa esta librería y viaja dentro del `.apk`. |
| `ksp(...)` | Generador de código: corre solo al compilar. |
| `testImplementation(...)` | Solo para tests unitarios, que corren en la PC. |
| `androidTestImplementation(...)` | Solo para tests que corren en un emulador o celular. |
| `debugImplementation(...)` | Solo en la versión de desarrollo (por ejemplo, la vista previa del diseño). |
| `platform(...)` | Un BOM: una lista de versiones que combinan bien entre sí. |

Compose son muchas librerías (ui, material3, tooling...). En vez de versionar cada una se versiona el **BOM de Compose**
(`2026.08.00`) y él elige versiones compatibles. Por eso en el catálogo esas librerías no tienen `version.ref`.

| Grupo | Librerías | Para qué |
|---|---|---|
| UI | Compose, Material 3, activity-compose | Dibujar las pantallas. |
| UI | lifecycle (runtime, viewmodel) | Los ViewModel de MVVM (D01). |
| UI | navigation-compose | Moverse entre pantallas. |
| UI | glance | El widget de la pantalla de inicio. |
| Inyección | hilt, hilt-navigation-compose, hilt-work | Conectar las piezas entre capas (D03, D05). |
| Datos | room | Base local, única fuente de verdad (D04). |
| Datos | datastore | Preferencias: sesión, onboarding visto, última sincronización. |
| Datos | work-runtime | `SyncWorker`, sincronización en segundo plano (D06). |
| Datos | retrofit, okhttp, kotlinx-serialization | Hablar con la API de Supabase. |
| Tests | junit, coroutines-test, turbine, room-testing, work-testing | Tests del dominio, de los Flows y de los datos (RNF11). |

Room, Retrofit, WorkManager, DataStore y Glance están agregadas pero todavía no se usan: van a tener código cuando se
implementen la base de datos, el `SyncWorker` y el widget.

#### ¿Qué es la inyección de dependencias (Hilt)?

La pantalla de Balance necesita algo que calcule el balance, que necesita el repositorio de gastos, que necesita la base
de datos. Sin Hilt habría que crear esa cadena a mano en cada lugar. Con Hilt, cada clase declara "necesito X" y Hilt lo
crea y se lo pasa. Eso permite:

- En los tests, darle un repositorio falso y probar sin base de datos (D05).
- Que el widget y las notificaciones reciban los mismos casos de uso que la app (D03).

### `AndroidManifest.xml`: la ficha técnica

Android lo lee antes de abrir la app, para saber qué contiene.

```xml
<application
    android:name=".MitiApplication"     ← la clase que arranca primero
    android:icon="@mipmap/ic_launcher"  ← el ícono
    android:label="@string/app_name"    ← el nombre que se ve ("Miti")
    android:theme="@style/Theme.Miti">
    <activity android:name=".MainActivity" ...>
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

- Una **Activity** es una "ventana" de la app. Hay una sola, `MainActivity`, y Compose dibuja todas las pantallas adentro.
- El `intent-filter` con `MAIN` + `LAUNCHER` significa "esta se abre al tocar el ícono".
- Más adelante acá se declaran el widget, el receptor de notificaciones y los permisos (internet, notificaciones).

### `res/`: los recursos

- `values/strings.xml`: los textos. Se separan del código para poder traducir la app sin tocarlo.
- `drawable/ic_hoy.xml`, `ic_compras.xml`, `ic_balance.xml`: íconos vectoriales de la barra inferior (nítidos en cualquier pantalla).
- `drawable/ic_launcher_*` y `mipmap-anydpi-v26/`: el ícono de la app (por ahora, el robot por defecto).
- `xml/backup_rules.xml` y `data_extraction_rules.xml`: reglas de backup automático; vienen por defecto.

Desde el código, un recurso se nombra como `R.string.destino_hoy` o `R.drawable.ic_hoy`. Gradle genera la clase `R` solo.

### Cómo arranca la app

```
Android lee el Manifest
   ↓
1. Crea MitiApplication    (una sola vez, vive mientras la app esté abierta)
   ↓
2. Abre MainActivity       (la "ventana")
   ↓
3. setContent { ... }      (de acá en adelante dibuja Compose)
   ↓
4. MitiTheme { ... }       (aplica colores y tipografías)
   ↓
5. MitiApp()               (barra inferior + pantalla Hoy)
```

#### 1. [`MitiApplication.kt`](../app/src/main/java/ar/edu/uade/miti/MitiApplication.kt)

```kotlin
package ar.edu.uade.miti                 // en qué "carpeta lógica" vive este código

import android.app.Application           // traigo clases de otros lados
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp                          // anotación: "Hilt, arrancá acá"
class MitiApplication : Application()    // mi clase es un tipo de Application
```

- **`package`**: la dirección del archivo; coincide con las carpetas `ar/edu/uade/miti/`.
- **`import`**: "voy a usar esta clase de otro paquete".
- **`class A : B()`**: "A es un tipo de B" (herencia).
- **`@Algo`** es una **anotación**, una etiqueta que lee otra herramienta. KSP ve `@HiltAndroidApp` al compilar y genera el código que Hilt necesita.

La clase está vacía a propósito: su único trabajo es encender Hilt.

#### 2 y 3. [`MainActivity.kt`](../app/src/main/java/ar/edu/uade/miti/MainActivity.kt)

```kotlin
@AndroidEntryPoint                                        // "Hilt, esta Activity puede pedirte cosas"
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {  // Android llama a esto al abrirla
        super.onCreate(savedInstanceState)                // primero, lo que hace Android por defecto
        enableEdgeToEdge()                                // dibujar detrás de la barra de estado
        setContent {                                      // a partir de acá, la UI es Compose
            MitiTheme {
                MitiApp()
            }
        }
    }
}
```

- **`fun`** declara una función. `onCreate` se ejecuta cuando se crea la ventana.
- **`override`**: reemplaza la versión de la clase padre; `super.onCreate(...)` llama primero a la original.
- **`Bundle?`**: el `?` significa que puede ser `null` (vacío). Kotlin obliga a marcar qué puede estar vacío.
- **`setContent { ... }`**: el puente entre Android "clásico" y Compose.
- **`MitiTheme { MitiApp() }`**: dibujá `MitiApp` usando el tema de Miti.

---

## Parte 2: la interfaz (Compose, pantallas y navegación)

### La idea central de Jetpack Compose

Antes, las pantallas se armaban en XML y se modificaban a mano desde el código ("buscá este texto y cambialo").
Compose es **UI declarativa**: se escriben funciones que describen cómo se ve la pantalla para un estado dado. Cuando el
estado cambia, Compose vuelve a ejecutarlas y actualiza solo lo que cambió.

```
estado  ──►  función @Composable  ──►  lo que se ve en pantalla
  ▲                                          │
  └──────── el usuario toca algo ◄───────────┘
```

Es la idea de D01: cada pantalla es una función de su estado. Más adelante, el ViewModel guarda ese estado.

### Las funciones `@Composable`

[`HoyScreen.kt`](../app/src/main/java/ar/edu/uade/miti/presentacion/hoy/HoyScreen.kt):

```kotlin
@Composable
fun HoyScreen(modifier: Modifier = Modifier) {
    PantallaEnConstruccion(
        titulo = "Hoy",
        descripcion = "Rutinas de hoy y próximas tareas (RF02).",
        modifier = modifier,
    )
}
```

- **`@Composable`** marca que la función dibuja UI. Solo se puede llamar desde otra función `@Composable`.
- **No devuelven nada**: lo que llaman adentro aparece en pantalla.
- **Por convención el nombre va con mayúscula**, como un componente.
- **`modifier: Modifier = Modifier`**: parámetro con valor por defecto.
- **`titulo = "Hoy"`**: argumentos con nombre; se lee mucho mejor.
- **La coma final** está permitida y evita que agregar un parámetro cambie la línea anterior en el diff.

`HoyScreen`, `ComprasScreen` y `BalanceScreen` son placeholders. Cuando se implementen, cada una tendrá su ViewModel y
mostrará los estados de la sección 11: carga, contenido, vacío, error y offline.

### Layouts y modifiers

[`PantallaEnConstruccion.kt`](../app/src/main/java/ar/edu/uade/miti/presentacion/componentes/PantallaEnConstruccion.kt):

```kotlin
@Composable
fun PantallaEnConstruccion(titulo: String, descripcion: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium)
        Text(
            descripcion,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
```

**Layouts:** `Column` pone sus hijos uno abajo del otro, `Row` uno al lado del otro y `Box` uno encima del otro. Lo que
va entre llaves después de `Column(...)` son sus hijos. Esa sintaxis (*trailing lambda*: si el último parámetro es un
bloque de código, se escribe afuera de los paréntesis) aparece en todo Compose.

**Modifiers:** una cadena de instrucciones que se aplican en orden.

```kotlin
modifier
    .fillMaxSize()     // ocupá todo el espacio disponible
    .padding(24.dp)    // dejá 24 de margen interno
```

- **`dp`** es la unidad de medida de Android: ocupa lo mismo físicamente en cualquier pantalla. RNF07 (48 × 48 dp) usa esta unidad.
- Todo componente acepta un `modifier` desde afuera para que quien lo usa pueda ajustarlo.

**Material 3:** `Text` es un componente de Material 3. `MaterialTheme.typography.headlineMedium` toma el estilo del tema
en vez de un tamaño fijo, así toda la app es consistente.

### El tema: [`Theme.kt`](../app/src/main/java/ar/edu/uade/miti/presentacion/tema/Theme.kt)

```kotlin
// Paleta provisoria; se reemplaza por la del Figma (miti-UI).
private val VerdeMiti = Color(0xFF2E7D5B)
...
private val EsquemaClaro = lightColorScheme(primary = VerdeMiti, secondary = Arena)
private val EsquemaOscuro = darkColorScheme(primary = VerdeMitiClaro, secondary = ArenaClara)

@Composable
fun MitiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        content = content,
    )
}
```

- **`val`** es un valor que no cambia; **`var`** uno que sí.
- **`private`**: solo se usa dentro de este archivo.
- **`Color(0xFF2E7D5B)`**: color hexadecimal; `FF` es la opacidad y `2E7D5B` el verde.
- **Un color scheme** asigna colores a **roles** (`primary`, `secondary`...). Los componentes piden "el color primary", así que cambiar el esquema cambia toda la app.
- **`darkTheme = isSystemInDarkTheme()`**: por defecto sigue el modo oscuro del celular.
- **`if (...) A else B`** devuelve un valor y se puede usar directo como argumento.
- **`content: @Composable () -> Unit`**: un parámetro que es una función (composable, sin parámetros, que no devuelve nada). Por eso se puede escribir `MitiTheme { MitiApp() }`.

### La navegación

#### Las rutas: [`Ruta.kt`](../app/src/main/java/ar/edu/uade/miti/presentacion/navegacion/Ruta.kt)

```kotlin
@Serializable
sealed interface Ruta {
    @Serializable
    data object Hoy : Ruta

    @Serializable
    data object Compras : Ruta

    @Serializable
    data object Balance : Ruta
}
```

- **`interface Ruta`** define un tipo: "algo que es una ruta".
- **`sealed`**: todas las rutas posibles están declaradas acá. Si se agrega `Ruta.Ajustes`, el compilador avisa dónde falta manejarla.
- **`object`**: una clase con un único ejemplar. `data object` agrega un `toString()` legible.
- **`@Serializable`**: permite convertir la ruta a texto para guardar en qué pantalla estabas si Android cierra la app en segundo plano.

Es **navegación tipada**: escribir `Ruta.Hoyy` no compila, a diferencia de usar textos como `"hoy"`. Las rutas con datos
vienen después, por ejemplo `data class DetalleGasto(val gastoId: String) : Ruta`.

#### Los destinos de la barra inferior: [`MitiApp.kt`](../app/src/main/java/ar/edu/uade/miti/presentacion/MitiApp.kt)

```kotlin
enum class Destino(val ruta: Ruta, @param:StringRes val etiqueta: Int, @param:DrawableRes val icono: Int) {
    HOY(Ruta.Hoy, R.string.destino_hoy, R.drawable.ic_hoy),
    COMPRAS(Ruta.Compras, R.string.destino_compras, R.drawable.ic_compras),
    BALANCE(Ruta.Balance, R.string.destino_balance, R.drawable.ic_balance),
}
```

- **`enum class`**: una lista cerrada de valores, cada uno con su ruta, texto e ícono.
- **`R.string.destino_hoy`** es un número (`Int`) que identifica al recurso.
- **`@param:StringRes`** avisa a las herramientas que ese `Int` debe ser un texto de `strings.xml`; si se pasa un ícono por error, Android Studio lo marca.

La barra se arma recorriendo esta lista: para agregar una pestaña alcanza con sumar una línea.

#### `MitiApp`

```kotlin
@Composable
fun MitiApp() {
    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val destinoActual = entradaActual?.destination
    ...
```

- **El `NavController`** maneja la navegación y lleva la **pila de pantallas** (*back stack*): navegar apila, "atrás" desapila.
- **`remember`**: como Compose re-ejecuta las funciones cada vez que algo cambia, una variable normal se crearía de nuevo cada vez. `remember...` la crea la primera vez y la recuerda.
- **Estado y recomposición**: `currentBackStackEntryAsState()` es un estado que Compose observa. Al cambiar de pantalla, Compose re-ejecuta `MitiApp` y la barra marca la pestaña correcta. Eso es la **recomposición**.
- **`by`**: delegación; permite usar `entradaActual` sin escribir `.value`.
- **`?.`** (*safe call*): si `entradaActual` es `null` (al arrancar todavía no hay pantalla), devuelve `null` en vez de fallar.

**El `Scaffold`** arma la estructura típica de una pantalla Material (barra superior, inferior, botón flotante):

```kotlin
    Scaffold(
        bottomBar = { NavigationBar { ... } },
    ) { innerPadding ->
        NavHost(...)
    }
```

`innerPadding` le dice al contenido cuánto ocupa la barra, para que nada quede tapado.

**La barra inferior:**

```kotlin
NavigationBar {
    Destino.entries.forEach { destino ->
        NavigationBarItem(
            selected = destinoActual?.hasRoute(destino.ruta::class) == true,
            onClick = { navController.navigate(destino.ruta) { ... } },
            icon = { Icon(painterResource(destino.icono), contentDescription = null) },
            label = { Text(stringResource(destino.etiqueta)) },
        )
    }
}
```

- **`Destino.entries.forEach { destino -> ... }`**: una pestaña por cada valor del enum.
- **`selected`**: marcada si la pantalla actual es su ruta. `== true` maneja el caso `null`.
- **`painterResource` / `stringResource`**: cargan el ícono y el texto desde `res/`.
- **`contentDescription = null`**: el texto para **TalkBack** (lector de pantalla). Es `null` porque la etiqueta visible ya se lee. **RNF08:** un ícono sin texto al lado (como el botón "+") sí necesita descripción.

**Navegar entre pestañas sin apilar:**

```kotlin
navController.navigate(destino.ruta) {
    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

Sin esto, tocar Compras → Balance → Compras → Balance dejaría 4 pantallas apiladas.

- **`popUpTo(inicio) { saveState = true }`**: saca todo hasta Hoy, guardando el estado.
- **`launchSingleTop`**: si ya estás en esa pestaña, no la abre de nuevo encima.
- **`restoreState`**: al volver a una pestaña, la recupera como la dejaste (por ejemplo, el scroll).

"Atrás" desde cualquier pestaña lleva a Hoy, y desde Hoy sale de la app.

**El `NavHost`**: el lugar donde se muestra la pantalla actual y el mapa de ruta → pantalla.

```kotlin
NavHost(
    navController = navController,
    startDestination = Ruta.Hoy,
    modifier = Modifier.padding(innerPadding),
) {
    composable<Ruta.Hoy> { HoyScreen() }
    composable<Ruta.Compras> { ComprasScreen() }
    composable<Ruta.Balance> { BalanceScreen() }
}
```

Cuando se implemente el flujo 1 (splash → onboarding → cuenta → hogar → Hoy), esas pantallas se suman a este mapa y
`startDestination` dependerá de si hay sesión iniciada.

**`@Preview`**: una función solo para el editor. Android Studio la dibuja en un panel sin ejecutar la app en un emulador.

### Dónde va a entrar MVVM

```
BalanceScreen (Compose)  ──► solo dibuja el estado y avisa lo que toca el usuario
      ▲   │ "tocaron Registrar pago"
      │   ▼
BalanceViewModel ──► guarda el estado (StateFlow<UiState>) y llama a casos de uso
      ▲   │
      │   ▼
ObtenerBalanceDelMes / RegistrarPago (casos de uso, dominio)
      ▲   │
      │   ▼
GastoRepositorio (interfaz del dominio) ──► implementado con Room en la capa de datos
```

La pantalla pide el ViewModel a Hilt con `viewModel: BalanceViewModel = hiltViewModel()` (de `hilt-navigation-compose`).
Los datos suben siempre desde Room y la pantalla se redibuja sola cuando cambian (D04).

---

## Parte 3: el dominio (modelos, repositorios y tests)

### Qué es el dominio y por qué está separado

El dominio es **qué cosas existen en el problema** (rutinas, gastos, pagos) y **qué reglas cumplen**.

- **D02 / RNF13:** es Kotlin puro, sin Android. Ningún `import` es de `android.*`, `androidx.*` ni Room. Los tests corren en la PC en milisegundos.
- **D05:** el dominio define interfaces y la capa de datos las implementa. El dominio no sabe que Room existe.

```
dominio/
├── modelo/        ← Hogar, Integrante, Rutina, RegistroRutina, Pendiente, Gasto, Pago
└── repositorio/   ← los contratos para leer y guardar esas cosas
```

Falta `casouso/` (MarcarRutina, RegistrarGasto, ObtenerBalanceDelMes...), que se crea al implementar cada funcionalidad.

### Los modelos: `data class`

[`Hogar.kt`](../app/src/main/java/ar/edu/uade/miti/dominio/modelo/Hogar.kt):

```kotlin
/** Un hogar tiene 1 o 2 integrantes y es dueño de todos los ítems. */
data class Hogar(
    val id: String,
    val nombre: String,
    /** Código de 6 caracteres para que el segundo integrante se una. */
    val codigoUnion: String,
)
```

- **`/** ... */`** es documentación (KDoc): Android Studio la muestra al pasar el mouse.
- Los datos de la clase se declaran entre paréntesis; cada `val` es una propiedad que **no se puede cambiar**.
- **`data class`** agrega automáticamente:
  - comparación por contenido (`==`), clave para tests y para que Compose redibuje solo si algo cambió;
  - un `toString()` legible;
  - **`copy()`**: para "modificar" se crea una copia con un campo distinto, por ejemplo `pendiente.copy(resuelto = true)`.
- **Inmutabilidad:** nadie modifica un objeto "por atrás". Cuando algo cambia se crea uno nuevo, Room lo guarda y la pantalla se redibuja (D04).
- **Los `id` son `String`** porque serán UUID generados en el celular (D07), para crear sin conexión sin chocar ids.
- **Las relaciones son por id** (`Integrante.hogarId`), como en la tabla de entidades de la sección 14.

### [`Rutina.kt`](../app/src/main/java/ar/edu/uade/miti/dominio/modelo/Rutina.kt)

```kotlin
enum class Frecuencia { DIARIA, DIAS_DE_LA_SEMANA }

data class Rutina(
    val id: String,
    val hogarId: String,
    val titulo: String,
    val frecuencia: Frecuencia,
    /** Solo se usa con [Frecuencia.DIAS_DE_LA_SEMANA]. */
    val dias: Set<DayOfWeek> = emptySet(),
    val vecesPorDia: Int,
) {
    init {
        require(vecesPorDia in 1..3) { "vecesPorDia debe estar entre 1 y 3" }
    }
}
```

- **`enum class Frecuencia`**: solo las dos opciones del alcance; "cada 15 días" ni siquiera se puede representar.
- **`Set<DayOfWeek>`**: un conjunto sin repetidos. `DayOfWeek` viene de **`java.time`**, la librería estándar de fechas.
- **`= emptySet()`**: valor por defecto; una rutina diaria no necesita días.
- **`init { ... }`** se ejecuta cada vez que se crea una `Rutina`.
- **`require(condición) { "mensaje" }`** lanza un error si la condición es falsa. **`in 1..3`** = "está entre 1 y 3". Así no puede existir una rutina inválida, y la regla vive en el dominio, no en la pantalla.

```kotlin
data class RegistroRutina(
    val id: String,
    val rutinaId: String,
    val integranteId: String,
    val fechaHora: LocalDateTime,
)
```

- Cada toque en "hecho" **crea un registro nuevo**; nunca se edita uno existente. Si dos personas marcan la misma rutina sin conexión, quedan los dos registros y no hay conflicto (D08).
- **No hay un campo "hecha hoy".** El progreso se **calcula** contando los registros de hoy. Al día siguiente los de ayer no cuentan, así que la rutina vuelve a estar pendiente sin que nadie la reinicie (D09).
- **`LocalDateTime`**: fecha con hora, para mostrar "Tomás · 8:15".

### [`Pendiente.kt`](../app/src/main/java/ar/edu/uade/miti/dominio/modelo/Pendiente.kt): tipos que pueden ser `null`

```kotlin
enum class TipoPendiente { TAREA, COMPRA }

data class Pendiente(
    val id: String,
    val hogarId: String,
    val tipo: TipoPendiente,
    val titulo: String,
    val responsableId: String? = null,      // solo tareas
    val fechaHora: LocalDateTime? = null,   // solo tareas
    val avisar: Boolean = false,
    val resuelto: Boolean = false,
    val gastoId: String? = null,            // gasto generado al terminar la compra
)
```

Tareas y compras comparten entidad, diferenciadas por `tipo` (sección 14).

- **`String`** nunca puede ser `null`; **`String?`** sí. El compilador obliga a contemplar el caso vacío antes de usar el valor.
- `gastoId` es el vínculo compra → gasto; queda `null` si al terminar la compra eligen no registrar el gasto (flujo 3).
- Con los valores por defecto, crear una compra es corto: `Pendiente(id = ..., hogarId = ..., tipo = TipoPendiente.COMPRA, titulo = "Leche")`.

### [`Gasto.kt`](../app/src/main/java/ar/edu/uade/miti/dominio/modelo/Gasto.kt): plata, cuotas y pagos

```kotlin
enum class Reparto { A_MEDIAS, UNO_SOLO }

data class Gasto(
    val id: String,
    val hogarId: String,
    val descripcion: String,
    val montoTotalCentavos: Long,
    val pagadorId: String,
    val reparto: Reparto,
    val beneficiarioId: String? = null,
    val cuotasTotales: Int = 1,
    /** Cuota que corresponde a [mesInicio] (permite cargar una compra en cuotas ya empezada). */
    val cuotaInicial: Int = 1,
    val mesInicio: YearMonth,
)
```

**Centavos en `Long`.** Los números con coma (`Double`) se guardan de forma aproximada: `0.1 + 0.2` da
`0.30000000000000004`. En un balance que tiene que coincidir con el cálculo manual (RNF11) esos errores se acumulan.
Con enteros en centavos ($24.000 = `2400000`) las cuentas son exactas. `Int` llegaría a ~21 millones de pesos en
centavos, que podría no alcanzar; por eso `Long`.

**Cuotas.** `cuotasTotales = 1` es un gasto común. `mesInicio: YearMonth` representa solo año y mes. `cuotaInicial` es la
cuota que cae en `mesInicio` (si se carga una compra que ya va por la cuota 3 de 6, `cuotaInicial = 3`).

El ejemplo de la sección 9 (Tomás paga $30.000 en 3 cuotas, a medias, desde noviembre):

```kotlin
Gasto(
    id = "...", hogarId = "...", descripcion = "Heladera",
    montoTotalCentavos = 3_000_000,        // $30.000 (el _ es solo para leer mejor)
    pagadorId = "tomas", reparto = Reparto.A_MEDIAS,
    cuotasTotales = 3, mesInicio = YearMonth.of(2026, 11),
)
```

`ObtenerBalanceDelMes` calcularía para noviembre, diciembre y enero: cuota $10.000, la mitad es de Ana, "Ana le debe
$5.000 a Tomás".

```kotlin
/** Pago entre integrantes para saldar el balance; se descuenta del mes de su [fecha]. */
data class Pago(
    val id: String,
    val hogarId: String,
    val deIntegranteId: String,
    val aIntegranteId: String,
    val montoCentavos: Long,
    val fecha: LocalDate,
)
```

**No existe un modelo "Balance"** (D09): se calcula siempre a partir de gastos y pagos, así da bien aunque cada celular
cargue gastos sin conexión.

**Lo que no está en los modelos (a propósito).** `updatedAt`, `deleted` y `syncState` (sección 14) son detalles de cómo
se sincroniza, no del problema del hogar. Van en las entidades de Room, en la capa de datos, y un **mapper** traduce entre
una cosa y otra. Si cambia la estrategia de sincronización (por ejemplo, por la corrección C2), el dominio no se entera.

### Los repositorios: [`Repositorios.kt`](../app/src/main/java/ar/edu/uade/miti/dominio/repositorio/Repositorios.kt)

```kotlin
interface GastoRepositorio {
    fun observarGastos(): Flow<List<Gasto>>
    fun observarPagos(): Flow<List<Pago>>
    suspend fun guardarGasto(gasto: Gasto)
    suspend fun guardarPago(pago: Pago)
}
```

Hay uno por cada grupo de D05: `HogarRepositorio`, `RutinaRepositorio`, `PendienteRepositorio` y `GastoRepositorio`.

**`interface`: un contrato sin implementación.** Dice qué se puede hacer, no cómo. Después habrá un
`GastoRepositorioImpl` (Room + Supabase) y un `GastoRepositorioFake` para tests (una lista en memoria). El caso de uso
recibe "algún `GastoRepositorio`": Hilt le pasa el real en la app y el test le pasa el falso. Eso es la inversión de
dependencias de D05, y permite trabajar en paralelo: la UI programa contra la interfaz mientras se implementa Room.

**Leer: `Flow`.** `List<Gasto>` es una lista; `Flow<...>` es **un valor que va cambiando en el tiempo**. Primero emite la
lista actual y después una nueva cada vez que cambia algo en la base. Por eso las funciones se llaman `observar...`. Es lo
que cumple RF04: *"el balance se actualiza sin otra acción"*.

**Escribir: `suspend`.** Guardar en la base lleva tiempo. Hacerlo en el hilo principal (el que dibuja) congelaría la app.
Una función `suspend` puede **pausarse sin bloquear**: la pantalla sigue fluida y el código continúa cuando termina. Es
parte de las **coroutines** de Kotlin. Desde el ViewModel se llama, por ejemplo, con `viewModelScope.launch { registrarGasto(gasto) }`.

**Sin `hogarId`.** Las funciones no piden de qué hogar traer los datos: un usuario pertenece a un solo hogar, guardado en
DataStore como "hogar activo" (sección 14), y la implementación filtra por él. Es una decisión revisable si al
implementar la capa de datos conviene pasarlo explícito.

### El test: [`RutinaTest.kt`](../app/src/test/java/ar/edu/uade/miti/dominio/modelo/RutinaTest.kt)

```kotlin
class RutinaTest {

    private fun rutina(vecesPorDia: Int) = Rutina(
        id = "r1",
        hogarId = "h1",
        titulo = "Darle de comer a Pancho",
        frecuencia = Frecuencia.DIARIA,
        vecesPorDia = vecesPorDia,
    )

    @Test
    fun `acepta de 1 a 3 veces por dia`() {
        (1..3).forEach { assertEquals(it, rutina(it).vecesPorDia) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza 0 veces por dia`() {
        rutina(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza mas de 3 veces por dia`() {
        rutina(4)
    }
}
```

- Vive en `src/test/`: no viaja en la app.
- **JUnit** es la librería de tests; **`@Test`** marca qué ejecutar.
- **`private fun rutina(...) = Rutina(...)`**: función auxiliar que arma una rutina válida. El `=` es la forma corta de una función que devuelve una sola cosa.
- **Nombres entre comillas invertidas**: Kotlin permite frases con espacios en los tests.
- **`it`** es el nombre automático del elemento actual en `forEach`. `assertEquals(esperado, real)` falla si no son iguales.
- **`@Test(expected = ...)`**: el test pasa si se lanza ese error, que es justo el que lanza `require`.

Los tests importantes de RNF11 (balance con 50/50, 100/0, cuotas, pagos parciales y saldo arrastrado) serán de
`ObtenerBalanceDelMes`, con un repositorio falso. Para probar los `Flow` está **Turbine**.

### Lo que falta validar en el dominio

Hoy solo `Rutina` valida sus datos. Faltan reglas que se desprenden de la preentrega:

- `Rutina`: si es `DIAS_DE_LA_SEMANA`, `dias` no puede estar vacío.
- `Gasto`: monto mayor a 0, `cuotaInicial` entre 1 y `cuotasTotales` (la sección 11 menciona el error "cuota mayor al total"), y `beneficiarioId` obligatorio si el reparto es `UNO_SOLO`.
- `Pago`: monto mayor a 0, y que no sea de alguien a sí mismo.
