-- Miti · verificación de las reglas de acceso (issue #10, RNF10).
--
-- Cómo usarlo: pegarlo entero en el SQL Editor de Supabase y ejecutarlo.
-- Todo corre en una transacción que termina en ROLLBACK: no deja usuarios ni datos.
-- Si una regla no se cumple, se corta con un error "FALLÓ: ..." que dice cuál.
-- Si todo está bien, el último resultado es "OK: ..." .
--
-- Usuarios de prueba (se crean y se descartan):
--   Ana y Tomás comparten un hogar · Otra tiene su propio hogar · Cuarta intenta unirse al de Ana.

begin;

insert into auth.users (id, email) values
    ('00000000-0000-4000-a000-00000000000a', 'ana@prueba.miti'),
    ('00000000-0000-4000-a000-00000000000b', 'tomas@prueba.miti'),
    ('00000000-0000-4000-a000-00000000000c', 'otra@prueba.miti'),
    ('00000000-0000-4000-a000-00000000000d', 'cuarta@prueba.miti');

-- Cambia el usuario "logueado" de las consultas siguientes.
create function pg_temp.como(usuario uuid, email text) returns void language plpgsql as $$
begin
    perform set_config('request.jwt.claims',
        json_build_object('sub', usuario, 'email', email, 'role', 'authenticated')::text, true);
end;
$$;

-- 1. Ana crea un hogar y Tomás se une con el código.
select pg_temp.como('00000000-0000-4000-a000-00000000000a', 'ana@prueba.miti');
set local role authenticated;
select set_config('prueba.hogar_ana', (select id::text from public.crear_hogar('Casa de Ana', 'Ana')), true);
select set_config('prueba.codigo', (select codigo_union from public.hogares), true);

reset role;
select pg_temp.como('00000000-0000-4000-a000-00000000000b', 'tomas@prueba.miti');
set local role authenticated;
do $$
begin
    if (select id::text from public.unirse_a_hogar(lower(current_setting('prueba.codigo')), 'Tomás'))
        <> current_setting('prueba.hogar_ana') then
        raise exception 'FALLÓ: Tomás no quedó en el hogar de Ana';
    end if;
    if (select count(*) from public.integrantes) <> 2 then
        raise exception 'FALLÓ: Tomás debería ver a los dos integrantes';
    end if;
end $$;

-- 2. Ana carga datos en su hogar.
reset role;
select pg_temp.como('00000000-0000-4000-a000-00000000000a', 'ana@prueba.miti');
set local role authenticated;
do $$
declare
    hogar uuid := current_setting('prueba.hogar_ana')::uuid;
    ana   uuid := '00000000-0000-4000-a000-00000000000a';
    tomas uuid := '00000000-0000-4000-a000-00000000000b';
begin
    insert into public.rutinas (id, hogar_id, titulo, frecuencia, veces_por_dia, updated_at)
    values ('10000000-0000-4000-a000-000000000001', hogar, 'Darle de comer a Pancho', 'DIARIA', 2, 1);
    insert into public.gastos (id, hogar_id, descripcion, monto_total_centavos, pagador_id, reparto, mes_inicio, updated_at)
    values ('20000000-0000-4000-a000-000000000001', hogar, 'Supermercado', 2400000, tomas, 'A_MEDIAS', '2026-11', 1);
    insert into public.pagos (id, hogar_id, de_integrante_id, a_integrante_id, monto_centavos, fecha, updated_at)
    values ('30000000-0000-4000-a000-000000000001', hogar, ana, tomas, 1200000, '2026-11-20', 1);
    insert into public.pendientes (id, hogar_id, tipo, titulo, gasto_id, resuelto, updated_at)
    values ('40000000-0000-4000-a000-000000000001', hogar, 'COMPRA', 'Leche', '20000000-0000-4000-a000-000000000001', true, 1);
    insert into public.registros_rutina (id, rutina_id, integrante_id, fecha_hora, updated_at)
    values ('50000000-0000-4000-a000-000000000001', '10000000-0000-4000-a000-000000000001', ana, '2026-11-21 08:15', 1);
end $$;

-- 3. Tomás ve todo, marca la rutina a su nombre y no puede marcar a nombre de Ana.
reset role;
select pg_temp.como('00000000-0000-4000-a000-00000000000b', 'tomas@prueba.miti');
set local role authenticated;
do $$
begin
    if (select count(*) from public.rutinas) <> 1 or (select count(*) from public.gastos) <> 1
        or (select count(*) from public.pagos) <> 1 or (select count(*) from public.pendientes) <> 1
        or (select count(*) from public.registros_rutina) <> 1 then
        raise exception 'FALLÓ: Tomás debería ver los datos de su hogar';
    end if;

    insert into public.registros_rutina (id, rutina_id, integrante_id, fecha_hora, updated_at)
    values ('50000000-0000-4000-a000-000000000002', '10000000-0000-4000-a000-000000000001',
            '00000000-0000-4000-a000-00000000000b', '2026-11-21 20:00', 2);

    begin
        insert into public.registros_rutina (id, rutina_id, integrante_id, fecha_hora, updated_at)
        values ('50000000-0000-4000-a000-000000000003', '10000000-0000-4000-a000-000000000001',
                '00000000-0000-4000-a000-00000000000a', '2026-11-21 21:00', 3);
        raise exception 'FALLÓ: Tomás pudo marcar una rutina a nombre de Ana';
    exception when insufficient_privilege then null;
    end;

    -- Editar un gasto del hogar sí se puede (borrado lógico incluido).
    update public.gastos set deleted = true, updated_at = 4 where id = '20000000-0000-4000-a000-000000000001';
    if not found then
        raise exception 'FALLÓ: Tomás no pudo editar un gasto de su hogar';
    end if;

    -- Borrar de verdad, no (D07).
    begin
        delete from public.pagos where id = '30000000-0000-4000-a000-000000000001';
        raise exception 'FALLÓ: se pudo borrar un pago';
    exception when insufficient_privilege then null;
    end;

    -- Las marcas no se editan (D08).
    begin
        update public.registros_rutina set fecha_hora = '2026-11-21 09:00'
        where id = '50000000-0000-4000-a000-000000000002';
        raise exception 'FALLÓ: se pudo editar una marca de rutina';
    exception when insufficient_privilege then null;
    end;
end $$;

-- 4. Otra, con su propio hogar, no ve ni toca nada del hogar de Ana.
reset role;
select pg_temp.como('00000000-0000-4000-a000-00000000000c', 'otra@prueba.miti');
set local role authenticated;
select public.crear_hogar('Casa de Otra', 'Otra');
do $$
declare
    ajeno uuid := current_setting('prueba.hogar_ana')::uuid;
begin
    if exists (select 1 from public.hogares where id = ajeno)
        or exists (select 1 from public.rutinas) or exists (select 1 from public.gastos)
        or exists (select 1 from public.pagos) or exists (select 1 from public.pendientes)
        or exists (select 1 from public.registros_rutina)
        or (select count(*) from public.integrantes) <> 1 then
        raise exception 'FALLÓ: Otra puede leer datos de un hogar ajeno';
    end if;

    begin
        insert into public.rutinas (id, hogar_id, titulo, frecuencia, veces_por_dia, updated_at)
        values ('10000000-0000-4000-a000-000000000009', ajeno, 'Intrusa', 'DIARIA', 1, 1);
        raise exception 'FALLÓ: Otra pudo crear una rutina en un hogar ajeno';
    exception when insufficient_privilege then null;
    end;

    update public.gastos set descripcion = 'Hackeado' where hogar_id = ajeno;
    if found then
        raise exception 'FALLÓ: Otra pudo editar un gasto ajeno';
    end if;

    begin
        insert into public.registros_rutina (id, rutina_id, integrante_id, fecha_hora, updated_at)
        values ('50000000-0000-4000-a000-000000000009', '10000000-0000-4000-a000-000000000001',
                '00000000-0000-4000-a000-00000000000c', '2026-11-21 10:00', 1);
        raise exception 'FALLÓ: Otra pudo marcar una rutina ajena';
    exception when insufficient_privilege then null;
    end;

    begin
        perform public.crear_hogar('Segundo hogar', 'Otra');
        raise exception 'FALLÓ: Otra pudo crear un segundo hogar';
    exception when sqlstate 'MITI1' then null;
    end;
end $$;

-- 5. Cuarta no puede entrar al hogar de Ana (ya tiene dos) ni usar un código inventado.
reset role;
select pg_temp.como('00000000-0000-4000-a000-00000000000d', 'cuarta@prueba.miti');
set local role authenticated;
do $$
begin
    begin
        perform public.unirse_a_hogar(current_setting('prueba.codigo'), 'Cuarta');
        raise exception 'FALLÓ: Cuarta pudo unirse a un hogar completo';
    exception when sqlstate 'MITI3' then null;
    end;

    begin
        perform public.unirse_a_hogar('ZZZZZZ', 'Cuarta');
        raise exception 'FALLÓ: un código inexistente no dio error';
    exception when sqlstate 'MITI2' then null;
    end;
end $$;

-- 6. La base rechaza datos inválidos aunque vengan del propio hogar.
reset role;
select pg_temp.como('00000000-0000-4000-a000-00000000000a', 'ana@prueba.miti');
set local role authenticated;
do $$
declare
    hogar uuid := current_setting('prueba.hogar_ana')::uuid;
begin
    begin
        insert into public.gastos (id, hogar_id, descripcion, monto_total_centavos, pagador_id, reparto,
                                   cuotas_totales, cuota_inicial, mes_inicio, updated_at)
        values ('20000000-0000-4000-a000-000000000002', hogar, 'Heladera', 3000000,
                '00000000-0000-4000-a000-00000000000a', 'A_MEDIAS', 3, 4, '2026-11', 1);
        raise exception 'FALLÓ: se guardó un gasto con cuota mayor al total';
    exception when check_violation then null;
    end;

    begin
        insert into public.gastos (id, hogar_id, descripcion, monto_total_centavos, pagador_id, reparto, mes_inicio, updated_at)
        values ('20000000-0000-4000-a000-000000000003', hogar, 'Cine', 500000,
                '00000000-0000-4000-a000-00000000000c', 'A_MEDIAS', '2026-11', 1);
        raise exception 'FALLÓ: se guardó un gasto pagado por alguien de otro hogar';
    exception when foreign_key_violation then null;
    end;

    begin
        update public.hogares set codigo_union = 'AAAAAA' where id = hogar;
        raise exception 'FALLÓ: se pudo cambiar el código del hogar';
    exception when insufficient_privilege then null;
    end;
end $$;

-- 7. Sin sesión (clave pública sola) no se ve nada.
reset role;
set local role anon;
do $$
begin
    begin
        perform 1 from public.gastos;
        raise exception 'FALLÓ: el rol anon puede leer gastos';
    exception when insufficient_privilege then null;
    end;
end $$;

reset role;
select 'OK: todas las verificaciones de acceso por hogar pasaron' as resultado;

rollback;
