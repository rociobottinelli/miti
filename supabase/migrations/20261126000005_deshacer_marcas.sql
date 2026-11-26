-- Miti · migración 5: deshacer una marca de rutina propia.
--
-- Hasta ahora las marcas solo se agregaban (D08). Pero con el widget de un toque, un toque sin querer marcaba
-- la rutina para los dos sin vuelta atrás. Ahora cada uno puede deshacer SUS marcas, y solo así: con un borrado
-- lógico (deleted = true). Una marca no cambia de rutina, de persona ni de hora, y una marca deshecha no vuelve.

-- Solo esas dos columnas: cambiar rutina_id, integrante_id o fecha_hora sigue dando "permission denied".
grant update (deleted, updated_at) on public.registros_rutina to authenticated;

create policy "deshacer mis marcas" on public.registros_rutina
    for update to authenticated
    using (integrante_id = (select auth.uid()))
    with check (integrante_id = (select auth.uid()));

create or replace function public.solo_deshacer_marcas()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    -- Una marca deshecha no se vuelve a activar (la app crea una marca nueva) y, si no cambia nada, no se toca
    -- (así no se mueve server_updated_at y los celulares no la vuelven a bajar).
    if old.deleted or not new.deleted then
        return null;
    end if;
    new.updated_at := greatest(new.updated_at, old.updated_at + 1);
    return new;
end;
$$;

create trigger registros_rutina_solo_deshacer before update on public.registros_rutina
    for each row execute function public.solo_deshacer_marcas();
