# Backend en Supabase

Miti usa Supabase como backend central (preentrega, D11): **Auth** para las cuentas (email y contraseña) y
**Postgres + API REST** como fuente remota para sincronizar. Los celulares nunca hablan entre sí, y la app
nunca lee de la red para mostrar datos: Room es la única fuente de verdad (D04).

## Qué hay en esta carpeta

| Archivo | Qué hace |
|---|---|
| [`migrations/20261121000001_tablas.sql`](migrations/20261121000001_tablas.sql) | Tablas espejo de las entidades de Room, con las validaciones del dominio como `CHECK` y `server_updated_at` para la descarga incremental. |
| [`migrations/20261121000002_acceso_por_hogar.sql`](migrations/20261121000002_acceso_por_hogar.sql) | Row Level Security: cada usuario solo ve y modifica su hogar (RNF10). Funciones `crear_hogar` y `unirse_a_hogar` con el límite de dos integrantes. |
| [`migrations/20261121000003_ultima_modificacion_gana.sql`](migrations/20261121000003_ultima_modificacion_gana.sql) | D08: si llega tarde una versión con `updated_at` más viejo que el guardado, se ignora. |
| [`migrations/20261123000004_conflictos_de_gastos.sql`](migrations/20261123000004_conflictos_de_gastos.sql) | C2: una edición de gasto hecha sobre una versión vieja no pisa la nueva; queda en `conflictos_gasto` para avisarle a quien la hizo. |
| [`migrations/20261126000005_deshacer_marcas.sql`](migrations/20261126000005_deshacer_marcas.sql) | Cada uno puede deshacer **sus** marcas de rutina (solo `deleted = true`); una marca deshecha no vuelve. |
| [`tests/verificar_acceso_por_hogar.sql`](tests/verificar_acceso_por_hogar.sql) | Verificación con cuatro usuarios simulados. Corre en una transacción con `ROLLBACK`: no deja datos. |
| [`demo/cargar_demo.py`](demo/cargar_demo.py) | Carga el hogar de la demo (Ana y Tomás, con datos creíbles) usando la API pública con la sesión de cada uno. Se puede volver a correr: borra lo anterior y recarga con fechas del día siguiente. Ver [docs/guion-demo.md](../docs/guion-demo.md). |

## Reglas que aplica el servidor

- **Aislamiento por hogar:** las políticas RLS comparan el `hogar_id` de cada fila con el hogar del usuario
  logueado (`mi_hogar()`). Sin sesión (solo con la clave pública) no se ve nada.
- **Nada se borra de verdad (D07):** no hay permiso de `DELETE`; un borrado es `deleted = true`.
- **Las marcas de rutina solo se agregan (D08)** y cada uno marca a su propio nombre. Lo único que se puede
  hacer con una marca es deshacer la propia (migración 5).
- **Dos integrantes como máximo:** `unirse_a_hogar` bloquea el hogar mientras cuenta, así que si dos personas
  usan el mismo código a la vez, solo entra una.
- **Gana la última modificación (D08):** una versión con `updated_at` más viejo que el guardado no pisa la fila.
- **Conflictos en gastos (C2):** si un gasto cambió desde la versión que se editó, la edición no entra y queda
  en `conflictos_gasto`, visible solo para quien la hizo.
- **Coherencia:** quien paga, se beneficia o es responsable tiene que ser del mismo hogar (claves foráneas compuestas).

Errores propios que devuelve la API (campo `code` del JSON), para que la app muestre el mensaje de la preentrega:

| `code` | Mensaje |
|---|---|
| `MITI1` | Ya formás parte de un hogar |
| `MITI2` | Código inválido |
| `MITI3` | El hogar ya tiene dos integrantes |

## Configurar un proyecto desde cero

1. Crear el proyecto en [supabase.com](https://supabase.com) (región São Paulo) y guardar la contraseña de la base en un lugar seguro, **fuera del repo**.
2. En **SQL Editor**, ejecutar las migraciones **en orden** (`…0001_tablas.sql`, `…0002_acceso_por_hogar.sql`, `…0003_ultima_modificacion_gana.sql`, `…0004_conflictos_de_gastos.sql`, `…0005_deshacer_marcas.sql`).
3. Ejecutar `tests/verificar_acceso_por_hogar.sql`. Tiene que terminar con `OK: todas las verificaciones de acceso por hogar pasaron`.
4. Copiar los datos de conexión a `local.properties` (en la raíz del repo; ya está en `.gitignore`):

   ```properties
   SUPABASE_URL=https://<ref-del-proyecto>.supabase.co
   SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
   ```

   - **URL:** botón *Connect* del dashboard, o *Project Settings → Data API*. Sin `/rest/v1/` al final.
   - **Clave:** *Project Settings → API Keys → Publishable key*. Si el proyecto solo muestra *Legacy API Keys*, usar la `anon` `public`.

La app los lee como `BuildConfig.SUPABASE_URL` y `BuildConfig.SUPABASE_PUBLISHABLE_KEY`. Si faltan, compila
igual, pero no puede sincronizar.

## Qué nunca va al repo ni a la app

- La **contraseña de la base**.
- La **secret key** (`sb_secret_…`) o la **`service_role`**: saltean todas las reglas RLS.

La clave publicable sí puede ir dentro de la app: por sí sola no da acceso a ningún dato (ver punto 7 de la verificación).
