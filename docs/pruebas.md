# Pruebas manuales con dos dispositivos y de accesibilidad

Evidencia de #25 (acciones simultáneas en dos dispositivos, corrección C3) y #26 (accesibilidad). Completa a los
~280 tests automáticos, que cubren las reglas, los ViewModels, la sincronización contra un servidor falso, Room y
las alarmas.

## Entorno

- **Dispositivos:** dos emuladores Pixel 6 con Android 15 (API 35), con aceleración por hardware (WHPX). En el
  **A** está la cuenta de prueba de **Tomás**; en el **B**, la de **Ana**. Los dos son del mismo hogar.
- **Backend:** el proyecto real de Supabase, con las cuatro migraciones.
- **App:** build debug de `develop` del 25/11/2026, más los PR indicados en cada caso.
- **Cómo se midió:** con `adb` (taps, texto, `uiautomator dump`, `dumpsys alarm`, `dumpsys notification` y
  `logcat`) y capturas de pantalla. Los tiempos se midieron leyendo la pantalla del otro dispositivo cada ~1 s, así
  que tienen ±2 s de error.

## Resultados

| # | Prueba | Pasos | Esperado | Observado | Resultado |
|---|---|---|---|---|---|
| P1 | **Lo que hace uno le llega al otro con las dos apps abiertas** (C1) | A y B en Compras. B agrega "Yerba"; después A agrega "Café" y B agrega "Fideos". | Aparece en el otro sin tocar nada, en 15 s como mucho. | 15 s, 9 s y 12 s. | ✅ (con #55) |
| P2 | **Los dos marcan la misma rutina a la vez** | Rutina "Sacar al perro", 1 vez por día. A y B tocan el check en el mismo segundo. | No se pierde ninguna marca; la rutina queda hecha en los dos. | Cada uno ve "Hecha · (su nombre) 18:49". En la base de los dos celulares quedan **las dos marcas**, sincronizadas. | ✅ Nada se pierde (las marcas solo se agregan, D08). Como son del mismo segundo, cada tarjeta muestra la propia. |
| P3 | **Cambio sin conexión y recuperación** (RNF01, RNF02, RNF09) | A en modo avión agrega "Manteca". Pasan 20 s. A sale del modo avión. | A lo ve al instante, con "Sin conexión" y el ícono de pendiente. B no lo ve mientras A no tiene red. Al volver la red, llega en menos de 1 min. | A: "Sin conexión · última sincronización 18:49" y el ícono "Guardado en este celular, pendiente de sincronizar". B no lo vio en 20 s. Al volver la red, B lo vio a los **18 s**. | ✅ |
| P4 | **Los dos editan el mismo gasto** (C2) | A en modo avión cambia "Supermercado" de $ 24.000 a $ 25.000. B, con conexión, cambia quién pagó a Ana. A vuelve a tener red. | A recibe un aviso que dice qué campos cambiaron; puede elegir su versión o la del otro. | Tarjeta en A: "Tu cambio en «Supermercado» no se guardó. Monto: vos pusiste $ 25.000 y quedó $ 24.000. Quién pagó: vos pusiste Tomás y quedó Ana." Con **Usar mi cambio**, la tarjeta desaparece y B ve $ 25.000 y pagó Tomás a los **2 s**. | ✅ |
| P4b | Conflicto viejo sin diferencias | — | — | Un conflicto de las pruebas automáticas aparecía sin ninguna línea de qué cambió, porque ya se había rehecho desde otro celular. | ❌ → arreglado en #61 |
| P5 | **Aviso de tarea a horario** (RNF06) | A crea la tarea "Pagar luz" con aviso a las 19:00. Se revisa `dumpsys alarm`. | Alarma exacta. | `window=0 exactAllowReason=policy_permission`: exacta. | ✅ (con #56) |
| P5b | **Aviso con modo avión y reinicio en el medio** (RNF06) | A en modo avión se reinicia a las 18:59. | El aviso suena a las 19:00. | El teléfono arrancó a las 18:59:13, pero Android avisó a la app del reinicio recién a las 19:00:05. La tarea ya estaba vencida y **el aviso no sonó**. | ❌ → arreglado en #60. Verificado: con el receptor de alarmas desactivado, se adelantó el reloj hasta pasada la hora; al abrir la app aparecieron los dos avisos perdidos. |
| P5c | Aviso que se borraba solo (prueba del 24/11) | Tarea con aviso, modo avión y reinicio. | El aviso queda a la vista. | La notificación se publicaba y la app la retiraba 200 ms después. | ❌ → arreglado en #54 y verificado en el emulador. |
| P5d | Alarma aproximada (prueba del 24/11) | Tarea con aviso en Android 15. | ±5 min. | Sin permiso de alarmas exactas, la ventana fue de 7 a 35 min y sonó 6 min 44 s tarde. | ❌ → arreglado en #56 (P5). |
| P6 | **Permiso de notificaciones rechazado** (preentrega, sección 11) | En B se revoca el permiso de notificaciones y se abre Hoy. Se toca "Configuración", se da el permiso y se vuelve. | Aviso en Hoy con acceso a la configuración, que desaparece al dar el permiso. | "Las notificaciones están desactivadas: no te van a llegar los avisos de tareas." con **Configuración**, que abre los ajustes de notificaciones de Miti. Al volver con el permiso, el aviso ya no está. | ✅ (con #57) |
| P7 | **Terminar la compra con gasto** (flujo 3) | Leche y Pan en la lista. Leche al carrito → "Terminé la compra" → "Registrar gasto" → $ 24.000, Supermercado → Guardar. | Se cierra la compra solo con lo del carrito y el gasto aparece en Balance, en los dos celulares. | Queda "Pan" en la lista. El gasto aparece en Balance en los dos, con el mismo saldo. | ✅ (con #53) |
| P8 | **No repetir un ítem de Compras** (C1) | Con "Leche" en la lista, se agrega "leche". | No se agrega y se avisa. | "Ya está en la lista". | ✅ |
| P9 | **Hoy abre rápido** (RNF04, menos de 1 s) | `am start -W` con la app ya abierta en segundo plano, y en frío. | Menos de 1 s. | En caliente: 0,13 a 0,17 s. En frío: 3,0 s, pero en la build **debug** sobre un emulador. | ⚠️ Falta medir la build release en un celular real. |

## Accesibilidad (#26, RNF07 y RNF08)

| # | Prueba | Observado | Resultado |
|---|---|---|---|
| A1 | **Fuente al 200 %** en Hoy, Compras y Balance | Todo entra y se lee; nada se superpone. En Balance, "Tomás le debe $" quedaba en un renglón y "74.500" en otro, y las descripciones largas se cortaban en seco. | ✅ después de #59 (verificado: "$ 74.000" junto y descripción en dos renglones) |
| A2 | **Cada control con nombre para TalkBack y de 48 dp o más.** Se revisó con un script el árbol de accesibilidad (lo que lee TalkBack) de Hoy, Compras, Balance (también con la tarjeta de conflicto), la hoja Agregar, Nueva tarea, el formulario de gasto y Ajustes. | Tres controles sin nombre: el interruptor "En cuotas" y los campos Fecha y Hora de la tarea. Todos los demás tienen nombre y miden 48 dp o más. El código del hogar se lee letra por letra. | ✅ después de #62 (verificado: "En cuotas", "Fecha, mié 25/11", "Hora, 20:00") |
| A3 | **TalkBack con voz, de punta a punta** | No se pudo confirmar en el emulador: no hay audio ni registro de lo que dice. | ⏳ Hacerlo a mano: activar TalkBack, recorrer el primer uso, marcar una rutina, agregar una compra y registrar un gasto. |

## Pendiente de probar a mano

- **Widget (RNF03, RNF05):** agregarlo a la pantalla de inicio, marcar desde el widget en A y ver cuánto tarda
  en reflejarse en la app de A (menos de 5 s) y en el widget de B.
- **Dos compras iguales sin conexión:** A y B, los dos sin red, agregan "Leche". Resultado esperado y aceptado:
  quedan dos (limitación conocida); el aviso de datos desactualizados lo advierte.
- **TalkBack con voz** (A3) y **arranque en frío de la build release** (P9).
