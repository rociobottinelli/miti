# Correcciones de la preentrega (Entrega TPO - Parte 1)

Devolución docente recibida por Teams sobre la preentrega entregada el 9/10/2026
([preentrega-miti.pdf](preentrega-miti.pdf)). Captura original: [correcciones-preentrega.jpg](correcciones-preentrega.jpg).

> Nota: la captura empieza a mitad de frase; el comienzo del primer párrafo no se ve.

## Texto de la devolución

> [...] conectan bien compras y gastos, y el widget responde al contexto de uso. El punto débil es la
> coordinación entre celulares: si la información llega tarde, pueden repetir tareas; además, perder una
> edición de un gasto sin aviso contradice la confianza prometida. Para la entrega final, prueben acciones
> simultáneas en dos dispositivos y definan cómo advertir datos desactualizados y resolver conflictos.
>
> La entrevista inicial aporta dirección, pero necesitan validar con ambos convivientes: centralizar
> información no garantiza repartir la carga. Comercialmente, conectar compras, cuotas y balance es una
> diferenciación plausible; falta comprobar disposición a pagar y costos de mantenimiento.
>
> **Prioricen demostrar uso sostenido en varios hogares antes de ampliar funciones.**

## Puntos accionables

| # | Corrección | Afecta a | Qué implica |
|---|------------|----------|-------------|
| C1 | Coordinación entre celulares: si la info llega tarde, se pueden repetir tareas. | Sync (D06), RF02, RF03 | Reducir la latencia percibida y advertir al usuario cuando los datos pueden estar desactualizados. |
| C2 | Perder una edición de un gasto sin aviso contradice la confianza prometida. | D08 (last-write-wins), gastos | Revisar la resolución de conflictos en gastos: detectar el conflicto y avisar (o dejar elegir) en vez de pisar en silencio. |
| C3 | Probar acciones simultáneas en dos dispositivos. | Testing, demo final | Diseñar y documentar casos de prueba con dos dispositivos (offline/online, misma rutina, mismo gasto, borrado vs. edición). |
| C4 | Definir cómo advertir datos desactualizados y resolver conflictos. | UX (estados), sección 14 | Especificar en UI y en la documentación final el indicador de datos viejos y el flujo de conflicto. |
| C5 | Validar con ambos convivientes, no solo con quien organiza. | Problema y usuario | Entrevistar también al usuario secundario; centralizar no garantiza repartir la carga. |
| C6 | Comprobar disposición a pagar y costos de mantenimiento. | Caso de negocio | Agregar evidencia de willingness-to-pay y estimar costos (Supabase, mantenimiento). |
| C7 | **Prioridad:** demostrar uso sostenido en varios hogares antes de ampliar funciones. | Alcance | No sumar features; conseguir hogares reales usando la app y mostrar evidencia de uso. |
