-- Miti · migración 2: acceso por hogar (RNF10) y límite de dos integrantes (preentrega, sección 14).
--
-- Regla general: cada usuario solo ve y modifica los datos del hogar al que pertenece. Lo aplica
-- Postgres con Row Level Security, así que vale para cualquier cliente que use la API, no solo la app.
-- Nada se borra de verdad (D07): no hay permiso de DELETE en ninguna tabla.
-- Crear un hogar y unirse a uno se hace con funciones, no con INSERT directos, para poder
-- validar el código y el límite de integrantes en el servidor.

-- ---------------------------------------------------------------------------
-- Permisos de base: la clave pública sin sesión (rol anon) no accede a nada.
-- ---------------------------------------------------------------------------
revoke all on all tables in schema public from anon;
revoke all on all functions in schema public from anon, public;
revoke delete, truncate on all tables in schema public from authenticated;

-- ---------------------------------------------------------------------------
-- El hogar del usuario que hace la consulta
-- ---------------------------------------------------------------------------
-- security definer: lee integrantes sin pasar por su propia política (si no, sería circular).
create or replace function public.mi_hogar()
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
    select hogar_id from public.integrantes where id = auth.uid() and not deleted limit 1;
$$;
grant execute on function public.mi_hogar() to authenticated;

-- ---------------------------------------------------------------------------
-- Activar RLS en todas las tablas
-- ---------------------------------------------------------------------------
alter table public.hogares enable row level security;
alter table public.integrantes enable row level security;
alter table public.rutinas enable row level security;
alter table public.registros_rutina enable row level security;
alter table public.gastos enable row level security;
alter table public.pagos enable row level security;
alter table public.pendientes enable row level security;

-- Hogar: se ve y se renombra solo el propio. El código de unión no se puede cambiar desde la app.
create policy "ver mi hogar" on public.hogares
    for select to authenticated using (id = (select public.mi_hogar()));
create policy "renombrar mi hogar" on public.hogares
    for update to authenticated
    using (id = (select public.mi_hogar())) with check (id = (select public.mi_hogar()));
revoke insert, update on public.hogares from authenticated;
grant update (nombre, updated_at) on public.hogares to authenticated;

-- Integrantes: se ven los dos del hogar; cada uno edita solo su propio nombre.
create policy "ver integrantes de mi hogar" on public.integrantes
    for select to authenticated using (hogar_id = (select public.mi_hogar()));
create policy "editar mi perfil" on public.integrantes
    for update to authenticated
    using (id = (select auth.uid())) with check (id = (select auth.uid()));
revoke insert, update on public.integrantes from authenticated;
grant update (nombre, updated_at) on public.integrantes to authenticated;

-- Rutinas, gastos, pagos y pendientes: leer, crear y editar solo dentro del propio hogar.
create policy "ver rutinas de mi hogar" on public.rutinas
    for select to authenticated using (hogar_id = (select public.mi_hogar()));
create policy "crear rutinas en mi hogar" on public.rutinas
    for insert to authenticated with check (hogar_id = (select public.mi_hogar()));
create policy "editar rutinas de mi hogar" on public.rutinas
    for update to authenticated
    using (hogar_id = (select public.mi_hogar())) with check (hogar_id = (select public.mi_hogar()));

create policy "ver gastos de mi hogar" on public.gastos
    for select to authenticated using (hogar_id = (select public.mi_hogar()));
create policy "crear gastos en mi hogar" on public.gastos
    for insert to authenticated with check (hogar_id = (select public.mi_hogar()));
create policy "editar gastos de mi hogar" on public.gastos
    for update to authenticated
    using (hogar_id = (select public.mi_hogar())) with check (hogar_id = (select public.mi_hogar()));

create policy "ver pagos de mi hogar" on public.pagos
    for select to authenticated using (hogar_id = (select public.mi_hogar()));
create policy "crear pagos en mi hogar" on public.pagos
    for insert to authenticated with check (hogar_id = (select public.mi_hogar()));
create policy "editar pagos de mi hogar" on public.pagos
    for update to authenticated
    using (hogar_id = (select public.mi_hogar())) with check (hogar_id = (select public.mi_hogar()));

create policy "ver pendientes de mi hogar" on public.pendientes
    for select to authenticated using (hogar_id = (select public.mi_hogar()));
create policy "crear pendientes en mi hogar" on public.pendientes
    for insert to authenticated with check (hogar_id = (select public.mi_hogar()));
create policy "editar pendientes de mi hogar" on public.pendientes
    for update to authenticated
    using (hogar_id = (select public.mi_hogar())) with check (hogar_id = (select public.mi_hogar()));

-- Marcas de rutina: solo se agregan (D08) y cada uno marca a su nombre ("queda quién y cuándo").
create policy "ver marcas de mi hogar" on public.registros_rutina
    for select to authenticated
    using (exists (
        select 1 from public.rutinas r
        where r.id = rutina_id and r.hogar_id = (select public.mi_hogar())
    ));
create policy "marcar rutinas de mi hogar" on public.registros_rutina
    for insert to authenticated
    with check (
        integrante_id = (select auth.uid())
        and exists (
            select 1 from public.rutinas r
            where r.id = rutina_id and r.hogar_id = (select public.mi_hogar())
        )
    );
revoke update on public.registros_rutina from authenticated;

-- ---------------------------------------------------------------------------
-- Crear un hogar y unirse a uno
-- ---------------------------------------------------------------------------
-- Errores con código propio para que la app los distinga y muestre el mensaje de la preentrega.
--   MITI1: el usuario ya tiene hogar · MITI2: código inválido · MITI3: hogar completo

create or replace function public.crear_hogar(nombre_hogar text, mi_nombre text)
returns public.hogares
language plpgsql
security definer
set search_path = ''
as $$
declare
    usuario uuid := auth.uid();
    nuevo   public.hogares;
    ahora   bigint := (extract(epoch from now()) * 1000)::bigint;
    -- Sin letras ni números que se confundan al dictarlos (O/0, I/1).
    letras  constant text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    codigo  text;
begin
    if usuario is null then
        raise exception 'Necesitás iniciar sesión' using errcode = '28000';
    end if;
    if public.mi_hogar() is not null then
        raise exception 'Ya formás parte de un hogar' using errcode = 'MITI1';
    end if;

    loop
        codigo := '';
        for i in 1..6 loop
            codigo := codigo || substr(letras, 1 + floor(random() * length(letras))::int, 1);
        end loop;
        exit when not exists (select 1 from public.hogares h where h.codigo_union = codigo);
    end loop;

    insert into public.hogares (id, nombre, codigo_union, updated_at)
    values (gen_random_uuid(), btrim(nombre_hogar), codigo, ahora)
    returning * into nuevo;

    insert into public.integrantes (id, hogar_id, nombre, email, updated_at)
    values (usuario, nuevo.id, btrim(mi_nombre), coalesce(auth.jwt() ->> 'email', ''), ahora);

    return nuevo;
end;
$$;

create or replace function public.unirse_a_hogar(codigo text, mi_nombre text)
returns public.hogares
language plpgsql
security definer
set search_path = ''
as $$
declare
    usuario uuid := auth.uid();
    hogar   public.hogares;
begin
    if usuario is null then
        raise exception 'Necesitás iniciar sesión' using errcode = '28000';
    end if;
    if public.mi_hogar() is not null then
        raise exception 'Ya formás parte de un hogar' using errcode = 'MITI1';
    end if;

    -- FOR UPDATE: si dos personas usan el mismo código a la vez, la segunda espera a la primera
    -- y después ve el hogar ya completo (no pueden entrar las dos).
    select * into hogar from public.hogares h
    where h.codigo_union = upper(btrim(codigo)) and not h.deleted
    for update;
    if not found then
        raise exception 'Código inválido' using errcode = 'MITI2';
    end if;

    if (select count(*) from public.integrantes i where i.hogar_id = hogar.id and not i.deleted) >= 2 then
        raise exception 'El hogar ya tiene dos integrantes' using errcode = 'MITI3';
    end if;

    insert into public.integrantes (id, hogar_id, nombre, email, updated_at)
    values (
        usuario, hogar.id, btrim(mi_nombre), coalesce(auth.jwt() ->> 'email', ''),
        (extract(epoch from now()) * 1000)::bigint
    );

    return hogar;
end;
$$;

revoke all on function public.crear_hogar(text, text) from anon, public;
revoke all on function public.unirse_a_hogar(text, text) from anon, public;
grant execute on function public.crear_hogar(text, text) to authenticated;
grant execute on function public.unirse_a_hogar(text, text) to authenticated;

-- Red de seguridad: aunque alguien inserte integrantes por otro camino (por ejemplo, desde el panel),
-- un hogar nunca pasa de dos.
create or replace function public.limitar_integrantes()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    perform 1 from public.hogares where id = new.hogar_id for update;
    if (select count(*) from public.integrantes i where i.hogar_id = new.hogar_id and not i.deleted) >= 2 then
        raise exception 'El hogar ya tiene dos integrantes' using errcode = 'MITI3';
    end if;
    return new;
end;
$$;
create trigger integrantes_maximo_dos before insert on public.integrantes
    for each row execute function public.limitar_integrantes();
