-- Miti · migración 4: conflictos en gastos (corrección C2, #20).
--
-- Con "gana la última modificación" (migración 3), si los dos editaban el mismo gasto sin conexión, una
-- edición se perdía sin aviso. Ahora cada celular manda, junto con su edición, la versión sobre la que
-- editó (base_updated_at). Si cuando llega el gasto ya cambió desde esa versión, es un conflicto:
--   * no se pisa: el gasto queda como lo dejó quien sincronizó primero;
--   * la edición que no entró se guarda en conflictos_gasto, a nombre de quien la hizo, para avisarle y
--     que pueda rehacerla o descartarla;
--   * el gasto se marca como cambiado (updated_at nuevo) para que los dos celulares bajen la misma versión.

alter table public.gastos add column base_updated_at bigint;

create table public.conflictos_gasto (
    id                 uuid primary key default gen_random_uuid(),
    hogar_id           uuid not null references public.hogares (id),
    gasto_id           uuid not null references public.gastos (id),
    -- Quien perdió la edición: a esa persona se le avisa.
    integrante_id      uuid not null references public.integrantes (id),
    -- La edición que no entró, con las mismas columnas que gastos.
    version_rechazada  jsonb not null,
    updated_at         bigint not null,
    deleted            boolean not null default false,
    server_updated_at  timestamptz not null default now()
);
create index conflictos_gasto_hogar_id_idx on public.conflictos_gasto (hogar_id);
create index conflictos_gasto_gasto_id_idx on public.conflictos_gasto (gasto_id);
create index conflictos_gasto_integrante_server_idx on public.conflictos_gasto (integrante_id, server_updated_at);
create trigger conflictos_gasto_hora_servidor before insert or update on public.conflictos_gasto
    for each row execute function public.marcar_hora_servidor();

-- Cada uno ve solo sus conflictos. Los crea el trigger, nunca la app.
alter table public.conflictos_gasto enable row level security;
create policy "ver mis conflictos" on public.conflictos_gasto
    for select to authenticated using (integrante_id = (select auth.uid()));
revoke all on public.conflictos_gasto from anon;
revoke insert, update, delete, truncate on public.conflictos_gasto from authenticated;

-- security definer: inserta en conflictos_gasto aunque la app no tenga permiso de insertar ahí.
create or replace function public.detectar_conflicto_de_gasto()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    ahora bigint := (extract(epoch from now()) * 1000)::bigint;
begin
    if new.base_updated_at is not null and new.base_updated_at <> old.updated_at then
        -- Si la edición que llega deja el gasto igual a como está, no hay nada que avisar.
        if (new.descripcion, new.monto_total_centavos, new.pagador_id, new.reparto, new.beneficiario_id,
            new.cuotas_totales, new.cuota_inicial, new.mes_inicio, new.deleted)
           is not distinct from
           (old.descripcion, old.monto_total_centavos, old.pagador_id, old.reparto, old.beneficiario_id,
            old.cuotas_totales, old.cuota_inicial, old.mes_inicio, old.deleted) then
            return null;
        end if;

        -- Sin usuario (por ejemplo, una edición desde el panel de Supabase) no hay a quién avisarle.
        if auth.uid() is not null then
            insert into public.conflictos_gasto (hogar_id, gasto_id, integrante_id, version_rechazada, updated_at)
            values (
                old.hogar_id, old.id, auth.uid(),
                to_jsonb(new) - 'server_updated_at' - 'base_updated_at',
                ahora
            );
        end if;
        -- Se conserva lo que había, pero con un updated_at mayor que cualquiera de las dos ediciones:
        -- así el celular que perdió también baja esta versión y los dos quedan iguales.
        old.updated_at := greatest(old.updated_at, new.updated_at) + 1;
        old.base_updated_at := null;
        return old;
    end if;

    if new.base_updated_at is null then
        -- Sin versión base (filas viejas o borrados de otras tablas): se mantiene la regla de la migración 3.
        if new.updated_at < old.updated_at then
            return null;
        end if;
        return new;
    end if;

    -- Editó sobre la última versión: entra. Si el reloj del celular atrasa, se corrige para que la
    -- versión nueva quede después de la anterior y el otro celular la baje.
    new.updated_at := greatest(new.updated_at, old.updated_at + 1);
    new.base_updated_at := null;
    return new;
end;
$$;

drop trigger gastos_ultima_modificacion_gana on public.gastos;
create trigger gastos_detectar_conflicto before update on public.gastos
    for each row execute function public.detectar_conflicto_de_gasto();
