# Miti — "La casa, miti y miti"

TPO de **Desarrollo de Aplicaciones I** (UADE, 2026-2C). App Android para que dos personas que conviven
coordinen rutinas, tareas, compras y gastos compartidos en un solo lugar.

Integrantes: Rocío Bottinelli (UI/UX y presentación) · Juan Cruz Bautista Serra (datos y sincronización).

## Documentos de referencia (consultarlos siempre que haga falta)

| Archivo | Qué es |
|---------|--------|
| [docs/consigna-tpo-2026.pdf](docs/consigna-tpo-2026.pdf) | Consigna oficial del TPO: requisitos de la preentrega y de la entrega final, criterios de evaluación, uso de IA. |
| [docs/preentrega-miti.pdf](docs/preentrega-miti.pdf) | Nuestra preentrega (Etapa 1): problema, usuarios, alcance, RF/RNF, pantallas, arquitectura (D01–D11), persistencia, offline first, tecnologías. **Es la especificación de lo que se implementa.** |
| [docs/correcciones-preentrega.md](docs/correcciones-preentrega.md) | Devolución docente de la preentrega, transcripta, con puntos accionables C1–C7 (captura original en `docs/correcciones-preentrega.jpg`). |
| [Figma: archivo de diseño](https://www.figma.com/design/sWFMJHbqIjYUaBmoOeMcj5/miti-UI?node-id=0-1) | **Referencia visual de la app.** Páginas "Wireframe (Pre entrega)" (todas las pantallas con sus estados: carga, contenido, vacío, error, sin conexión) y "High Fidelity" (diseño final). |
| [Figma: prototipo](https://www.figma.com/proto/sWFMJHbqIjYUaBmoOeMcj5/miti-UI?node-id=22-13360&page-id=0%3A1&starting-point-node-id=22%3A13374) | Prototipo navegable del wireframe. |

Si algo de lo que se implementa se aparta de la preentrega, hay que poder justificarlo (la consigna lo exige)
y conviene registrarlo para el README final.

## Estado actual

- Etapa 1 (preentrega) entregada el 9/10/2026, con correcciones recibidas.
- Etapa 2 (implementación y entrega final): por empezar. El repo todavía no tiene código.

## Lo que pide la entrega final (consigna §7)

- Implementar de verdad RF01–RF04 (no solo pantallas navegables).
- Kotlin + Android + Jetpack Compose, navegación, ViewModel/MVVM, Clean Architecture, repositorios,
  Room, Retrofit, manejo de errores, offline.
- Estados: carga, contenido, vacío, error, sin conexión, permisos rechazados, recuperación.
- README con: instrucciones de ejecución, arquitectura final, tecnologías, funcionalidades, RF alcanzados,
  decisiones relevantes y limitaciones conocidas.
- Demo: Problema → Escenario → Acción → Respuesta → Valor. Defensa individual: cada integrante debe
  poder explicar cualquier parte del código, incluido el asistido por IA.

## Correcciones a tener en cuenta (resumen de docs/correcciones-preentrega.md)

- **Prioridad del docente:** demostrar uso sostenido en varios hogares *antes* de ampliar funciones. No sumar features fuera del alcance.
- La sincronización entre dos celulares es el punto débil: advertir datos desactualizados, evitar tareas repetidas.
- "Gana la última modificación" en gastos pierde ediciones sin aviso → revisar D08: detectar y avisar el conflicto.
- Probar acciones simultáneas en dos dispositivos (y documentarlo).
- Validar con ambos convivientes; evidenciar disposición a pagar y costos de mantenimiento.

## Producto (resumen de la preentrega)

Cuatro tipos de ítem:

- **Rutina:** diaria o ciertos días, 1 a 3 veces por día, sin responsable fijo; cualquiera la marca (queda quién y cuándo); se reinicia cada día. Se marca desde el widget.
- **Tarea programada:** una vez, con fecha/hora, responsable y aviso opcional (alarma local).
- **Compra:** lista compartida; al terminar la compra se ofrece registrar el gasto vinculado.
- **Gasto:** monto, quién pagó, reparto 50/50 o 100/0, cuotas iguales. Alimenta el **balance** mensual; se registran **pagos** (totales o parciales) y lo no saldado pasa como saldo pendiente al mes siguiente.

Requisitos funcionales: RF01 registrar ítem · RF02 consultar lo de hoy · RF03 completar ítem · RF04 gastos, pagos y balance.
RNF clave: offline completo (RNF01), nada se pierde y se envía en < 1 min (RNF02), widget en 1 toque (RNF03),
Hoy en < 1 s (RNF04), widget–app consistentes en < 5 s (RNF05), avisos ±5 min (RNF06), 48dp y una mano (RNF07),
TalkBack y fuente 200% (RNF08), aislamiento por hogar (RNF10), balance = cálculo manual con tests (RNF11),
minSdk 26 (RNF12), dominio sin Android (RNF13).

Fuera de alcance: hogares de más de 2, recurrencias complejas, tareas repetitivas, push desde servidor,
recuperación de contraseña / Google login, calendario, categorías/precios, repartos arbitrarios, pagos reales.

Navegación: barra inferior Hoy · Compras · Balance; Ajustes desde el avatar. Material 3. Diseño: ver los links de Figma arriba.

## Arquitectura (decisiones D01–D11)

`UI (Compose / Glance) → ViewModel (StateFlow<UiState>) → Caso de uso → Repositorio (interfaz en dominio) → Room / Retrofit`

- **D01** MVVM, un ViewModel por pantalla con un único `StateFlow<UiState>`.
- **D02** Tres capas; dominio en Kotlin puro (reglas de balance, cuotas, reinicio diario testeables con JUnit).
- **D03** Widget y notificación usan los mismos casos de uso que la app (vía Hilt), nunca escriben directo en Room.
- **D04** Room es la única fuente de verdad; la UI nunca lee de la red.
- **D05** Repositorios como interfaces en el dominio; Supabase queda encerrado en la capa de datos.
- **D06** Sync con WorkManager: un `SyncWorker` envía lo pendiente y después descarga; reintento con backoff.
- **D07** IDs UUID generados en el dispositivo; borrado lógico (`deleted`).
- **D08** Marcas de rutina append-only; resto last-write-wins por `updatedAt` (**a revisar por C2**).
- **D09** Datos derivados (balance, progreso de rutina) se calculan, no se guardan.
- **D10** Avisos con AlarmManager + notificaciones locales; se reprograman al reiniciar.
- **D11** Backend central Supabase (Auth + Postgres con RLS + API REST); los celulares no hablan entre sí.

Entidades: Hogar, Integrante, Rutina, RegistroRutina, Pendiente (tarea/compra), Gasto, Pago.
Todas las sincronizables llevan `id` (UUID), `updatedAt`, `deleted`, `syncState`.
DataStore: sesión, hogar activo, onboarding visto, última sincronización.

Stack: Kotlin, Compose + Material 3, Glance, Navigation Compose, ViewModel, Coroutines/Flow, Room, DataStore,
Retrofit + kotlinx.serialization, WorkManager, AlarmManager, Supabase, Hilt, JUnit + Turbine.

## Forma de trabajo

- Ramas: `main` (solo merges desde `develop` al cerrar entregas), `develop` (integración), `feature/<tema>` desde `develop`.
- Commits pequeños, en español, imperativo, con prefijo: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`.
  Ej.: `feat: calcular cuota del mes en el balance`.
- Sin líneas de atribución a Claude: ni `Co-Authored-By` en commits ni "Generated with Claude Code" en PRs.
- Todo entra a `develop` por PR revisado por el otro integrante. Nada directo a `main` ni `develop`.
- Issues de GitHub vinculados a los PR. Se evalúa la evolución del repo: evitar commits gigantes al final.
- Código, comentarios y documentación en español (los nombres del dominio también: `Rutina`, `Gasto`, `MarcarRutina`...).
- Uso de IA permitido, pero el equipo debe poder explicar y justificar todo lo que se incorpora.
