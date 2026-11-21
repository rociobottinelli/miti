-- Miti · migración 3: "gana la última modificación" en el servidor (D08, #18).
--
-- Cada celular sube sus cambios con el updated_at (hora del celular) del momento en que se hicieron.
-- Si un celular estuvo offline y sube tarde una versión más vieja que la que ya está en el servidor,
-- esa versión se ignora: la fila queda como estaba. Sin esto, el último en sincronizar pisaría al
-- último en editar.
--
-- Las marcas de rutina no lo necesitan: solo se insertan (D08).
-- La corrección C2 (#20) va a cambiar esto para los gastos: detectar el conflicto y avisar.

create or replace function public.ignorar_version_vieja()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    -- Devolver NULL en un trigger BEFORE UPDATE deja la fila sin cambios. También aplica al
    -- "insert ... on conflict do update" que usa la API para subir cambios.
    if new.updated_at < old.updated_at then
        return null;
    end if;
    return new;
end;
$$;

create trigger hogares_ultima_modificacion_gana before update on public.hogares
    for each row execute function public.ignorar_version_vieja();
create trigger integrantes_ultima_modificacion_gana before update on public.integrantes
    for each row execute function public.ignorar_version_vieja();
create trigger rutinas_ultima_modificacion_gana before update on public.rutinas
    for each row execute function public.ignorar_version_vieja();
create trigger gastos_ultima_modificacion_gana before update on public.gastos
    for each row execute function public.ignorar_version_vieja();
create trigger pagos_ultima_modificacion_gana before update on public.pagos
    for each row execute function public.ignorar_version_vieja();
create trigger pendientes_ultima_modificacion_gana before update on public.pendientes
    for each row execute function public.ignorar_version_vieja();
