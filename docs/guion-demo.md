# Guion de la demo

Estructura que pide la consigna (§7.5): **Problema → Escenario → Acción del usuario → Respuesta de la app →
Valor generado**. Dura unos 8 minutos. Se hace con **dos celulares lado a lado** (o dos emuladores), uno de
Ana y uno de Tomás, para que se vea que lo que hace uno le llega al otro.

## Preparación (el día antes)

1. **Cargar el hogar de demo** con el script, que crea a Ana y Tomás con su perro Pancho y datos que parecen de
   una casa real. Las fechas se calculan para el día siguiente:

   ```bash
   python3 supabase/demo/cargar_demo.py
   ```

   La primera vez va con `--crear-cuentas`. Las contraseñas quedan en `~/.config/miti/supabase.env`
   (`DEMO_ANA_PASSWORD` y `DEMO_TOMAS_PASSWORD`). Cada vez que se corre, borra lo anterior y vuelve a cargar todo.
   Después, en cada celular: cerrar sesión e ingresar con `ana@miti.test` o `tomas@miti.test`.

2. **Qué carga:**
   - **Rutinas:** "Darle de comer a Pancho" (todos los días, 2 veces), "Regar las plantas" (todos los días) y
     "Sacar la basura" (lunes, miércoles y viernes). Sin marcas, así la demo arranca con "0 de 2".
   - **Tareas:** "Llamar al plomero por la canilla del baño" (Tomás, mañana a las 18), "Llevar a Pancho a la
     veterinaria" (Ana, en 2 días a las 10:30, con aviso) y "Pagar las expensas" (Ana, en 4 días, con aviso).
   - **Compras:** Leche, Pan y Yerba.
   - **El mes anterior:** alquiler ($ 600.000, Ana), expensas ($ 85.000, Tomás), supermercado, luz y la
     **heladera en 6 cuotas** ($ 900.000, Tomás). Tomás le pagó $ 150.000 a Ana, así que queda un saldo que pasa
     al mes de la demo ("De meses anteriores").
   - **El mes de la demo:** alquiler, expensas, supermercado, internet, vacunas de Pancho (todo a medias), la
     cuota 2 de 6 de la heladera, y "Remedios de Tomás", que pagó Ana pero son solo de Tomás.
3. **Widget 4x2** en la pantalla de inicio de los dos celulares.
4. **Permiso de notificaciones** dado en los dos. Volumen alto en el de Tomás.
5. Ensayar una vez de punta a punta con un cronómetro.

## Guion

### 0. Problema (30 s), lo dice una persona

> "En una casa de dos, las tareas, las compras y los gastos están conectados, pero se manejan por WhatsApp, las
> notas del celular, un calendario en la heladera y un Excel a fin de mes. Una persona carga con recordar, pedir
> y controlar, y la otra depende de que le avisen. En la entrevista, la persona que organiza nos dijo que lo que
> más le molesta es consolidar todo a fin de mes."

### 1. La mañana: la rutina desde el widget (1 min)

- **Escenario:** son las 8 y Tomás le da de comer a Pancho antes de salir.
- **Acción:** Tomás toca el check de "Darle de comer a Pancho" **en el widget**, sin abrir la app.
- **Respuesta:** su widget pasa a "1 de 2 · Tomás 8:15". En 15 segundos como mucho, la app de Ana muestra lo
  mismo. (Con la app de Ana abierta en Hoy se ve llegar.)
- **Valor:** Ana no le da de comer dos veces y no tuvo que preguntar. Es lo que hacía el calendario de la
  heladera, pero en el bolsillo.

### 2. En el súper: la compra y el gasto (2 min)

- **Escenario:** Ana anota que falta yerba; Tomás está en el súper.
- **Acción:**
  1. Ana agrega "Yerba". Si la vuelve a escribir, la app avisa "Ya está en la lista" (C1).
  2. Tomás, en Compras, tacha Leche y Yerba, que pasan al carrito.
  3. Toca **Terminé la compra** → **Registrar gasto** → $ 18.500, "Súper chino", pagó Tomás, a medias →
     Guardar.
- **Respuesta:** en Compras queda solo "Pan". En Balance, en los dos celulares, aparece el gasto y el saldo
  cambia solo.
- **Valor:** la compra y el gasto quedan conectados en un solo paso. No hay que pasar nada al Excel.

### 3. Sin señal (1 min)

- **Escenario:** en el fondo del súper no hay señal.
- **Acción:** Tomás pone el modo avión y tacha "Pan".
- **Respuesta:** la app sigue funcionando, y arriba dice "Sin conexión · última sincronización hh:mm". El ítem
  tiene el ícono de "pendiente de sincronizar". Al sacar el modo avión, el cambio le llega a Ana en segundos.
- **Valor:** se puede usar donde se necesita, y no se pierde nada (offline first, RNF01 y RNF02).

### 4. La tarea con aviso (1 min)

- **Preparación en el momento:** justo antes de empezar la demo, Ana crea la tarea **"Pagar el ABL"**, con
  responsable Tomás, aviso activado y la hora de esta escena (unos 6 minutos después del arranque). Las tareas
  no se editan, así que si la demo se atrasa hay que crear otra.
- **Acción:** no hace falta tocar nada: llega la hora.
- **Respuesta:** **solo en el celular de Tomás**, el responsable, suena la notificación "Pagar el ABL", aunque
  la app esté cerrada. Tomás toca **Marcar como hecha** desde la notificación, y en el celular de Ana la tarea
  desaparece de las pendientes.
- **Valor:** Ana no tiene que acordarse de recordárselo. La carga se reparte.

### 5. Los dos editan el mismo gasto (1 min 30 s), la corrección C2 del profe

- **Escenario:** los dos corrigen el mismo gasto sin estar conectados.
- **Acción:**
  1. Tomás, en modo avión, edita "Súper chino" a $ 19.000.
  2. Ana, con conexión, cambia quién pagó a "Ana".
  3. Tomás saca el modo avión.
- **Respuesta:** a Tomás le aparece arriba de Balance: "Tu cambio en «Súper chino» no se guardó. La otra
  persona lo cambió antes. Monto: vos pusiste $ 19.000 y quedó $ 18.500. Quién pagó: vos pusiste Tomás y quedó
  Ana." Puede elegir **Usar mi cambio** o **Dejar como está**.
- **Valor:** en la preentrega, una de las dos ediciones se perdía sin aviso. Ahora nadie pierde un cambio sin
  enterarse, y la app no depende del reloj de los celulares para decidir.

### 6. Fin de mes (1 min)

- **Acción:** en Balance, Ana mira el mes: "Tomás le debe $ X a Ana", lo que pagó cada uno y los movimientos,
  con "Heladera · Cuota 2 de 6". Tomás registra un **pago parcial**.
- **Respuesta:** el saldo baja al instante. Al pasar al mes siguiente, lo que faltó aparece como "De meses anteriores: Tomás le debía $ … a Ana".
- **Valor:** lo que más le molestaba a la persona entrevistada (consolidar a fin de mes) ya no existe: el balance
  está siempre calculado.

### 7. Cierre (30 s)

> "Miti pone a la vista lo que hay que hacer y mantiene las cuentas al día sin que una persona tenga que
> recordar, pedir ni consolidar. Funciona sin señal, y cuando los dos tocan lo mismo, nadie pierde nada sin
> enterarse."

Si hay resultados de la validación con hogares reales (#27), mostrarlos acá: cuántos hogares, cuántos días de
uso y qué dijeron los dos convivientes.

## Si algo falla en vivo

- **No llega un cambio:** en Hoy o en Compras, tocar **Actualizar** en el aviso de datos desactualizados, o
  salir y volver a entrar a la app (sincroniza al abrir).
- **No suena el aviso:** mostrar la tarea en la lista de Tomás con su campana. Si el aviso de permisos de Hoy
  está a la vista, explicarlo: es justamente el estado de "permiso rechazado".
- **Se corta internet:** la demo sigue igual. Es la escena 3 sin querer.

## Preguntas probables en la defensa (cada uno tiene que poder contestarlas)

- **¿Por qué Room y no leer de Supabase directo?** D04: offline first, Room como única fuente de verdad; la red
  solo sincroniza.
- **¿Cómo llega un cambio al otro celular?** El repositorio guarda en Room como `PENDIENTE`, el `SyncWorker`
  sube y después baja lo que cambió desde la última vez (`server_updated_at`). Con la app abierta, cada 15 s.
- **¿Qué pasa si los dos editan lo mismo?** Las marcas de rutina solo se agregan. En el resto gana la última
  modificación, con un trigger en el servidor. En los gastos, el servidor detecta la edición hecha sobre una
  versión vieja y la deja en `conflictos_gasto` para avisarle a quien la hizo.
- **¿Cómo se aísla un hogar de otro?** Row Level Security en Postgres con `mi_hogar()`. Lo verifica
  `supabase/tests/verificar_acceso_por_hogar.sql`.
- **¿Por qué `USE_EXACT_ALARM`?** RNF06 pide ±5 min, y desde Android 14 el otro permiso viene negado (medido:
  hasta 35 min de desvío).
- **¿Qué hace un ViewModel acá?** Junta los casos de uso en un único `StateFlow<UiState>` por pantalla. Los
  eventos de una vez van por `SharedFlow`. La pantalla solo dibuja y manda intenciones.
- **¿Dónde usaron IA y cómo lo verificaron?** Ver CAMBIOS.md: cada cambio tiene su problema, qué cambió y cómo
  se probó (tests y pruebas en dos dispositivos, docs/pruebas.md).
