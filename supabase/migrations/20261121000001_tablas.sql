-- Miti · migración 1: tablas del backend (preentrega D11, sección 14).
--
-- Espejo de las entidades de Room. Diferencias deliberadas:
--   * Columnas en snake_case (convención de la cátedra para SQL).
--   * updated_at: milisegundos del celular que hizo el cambio (decide los conflictos, D08).
--   * server_updated_at: hora del servidor, la pone un trigger. La usa la sincronización para
--     descargar "lo que cambió desde la última vez" sin depender del reloj de cada celular.
--   * syncState no existe acá: es un dato local de cada celular.
--   * Nunca se borran filas: deleted = true viaja como un cambio más (D07).
-- Las validaciones del dominio se repiten como CHECK: si una app vieja o un bug manda un dato
-- inválido, la base lo rechaza en vez de guardarlo.

-- ---------------------------------------------------------------------------
-- Hora del servidor en cada cambio
-- ---------------------------------------------------------------------------
create or replace function public.marcar_hora_servidor()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.server_updated_at := now();
    return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- Hogar e integrantes
-- ---------------------------------------------------------------------------
create table public.hogares (
    id                uuid primary key,
    nombre            text not null check (btrim(nombre) <> ''),
    -- 6 caracteres para que el segundo integrante se una (preentrega, sección 8).
    codigo_union      text not null unique check (codigo_union ~ '^[A-Z0-9]{6}$'),
    updated_at        bigint not null,
    deleted           boolean not null default false,
    server_updated_at timestamptz not null default now()
);

create table public.integrantes (
    -- El integrante es el usuario de Supabase Auth: un usuario, un integrante.
    id                uuid primary key references auth.users (id),
    hogar_id          uuid not null references public.hogares (id),
    nombre            text not null check (btrim(nombre) <> ''),
    email             text not null,
    updated_at        bigint not null,
    deleted           boolean not null default false,
    server_updated_at timestamptz not null default now(),
    -- Permite que otras tablas exijan "este integrante es de este mismo hogar".
    unique (id, hogar_id)
);
create index integrantes_hogar_id_idx on public.integrantes (hogar_id);

-- ---------------------------------------------------------------------------
-- Rutinas y sus marcas
-- ---------------------------------------------------------------------------
create table public.rutinas (
    id                uuid primary key,
    hogar_id          uuid not null references public.hogares (id),
    titulo            text not null check (btrim(titulo) <> ''),
    frecuencia        text not null check (frecuencia in ('DIARIA', 'DIAS_DE_LA_SEMANA')),
    -- Igual que en Room: "MONDAY,THURSDAY"; vacío para las diarias.
    dias              text not null default '',
    veces_por_dia     integer not null check (veces_por_dia between 1 and 3),
    updated_at        bigint not null,
    deleted           boolean not null default false,
    server_updated_at timestamptz not null default now(),
    check (frecuencia = 'DIARIA' or dias <> '')
);
create index rutinas_hogar_id_idx on public.rutinas (hogar_id);

-- Cada marca es una fila nueva que nunca se edita (D08): dos marcas simultáneas no chocan.
create table public.registros_rutina (
    id                uuid primary key,
    rutina_id         uuid not null references public.rutinas (id),
    integrante_id     uuid not null references public.integrantes (id),
    -- Hora local del celular que marcó (LocalDateTime en la app).
    fecha_hora        timestamp not null,
    updated_at        bigint not null,
    deleted           boolean not null default false,
    server_updated_at timestamptz not null default now()
);
create index registros_rutina_rutina_id_idx on public.registros_rutina (rutina_id);
create index registros_rutina_integrante_id_idx on public.registros_rutina (integrante_id);

-- ---------------------------------------------------------------------------
-- Gastos, pagos y pendientes (tareas y compras)
-- ---------------------------------------------------------------------------
create table public.gastos (
    id                    uuid primary key,
    hogar_id              uuid not null references public.hogares (id),
    descripcion           text not null check (btrim(descripcion) <> ''),
    monto_total_centavos  bigint not null check (monto_total_centavos > 0),
    pagador_id            uuid not null,
    reparto               text not null check (reparto in ('A_MEDIAS', 'UNO_SOLO')),
    beneficiario_id       uuid,
    cuotas_totales        integer not null default 1 check (cuotas_totales >= 1),
    cuota_inicial         integer not null default 1,
    -- YearMonth.toString(): "2026-11".
    mes_inicio            text not null check (mes_inicio ~ '^\d{4}-(0[1-9]|1[0-2])$'),
    updated_at            bigint not null,
    deleted               boolean not null default false,
    server_updated_at     timestamptz not null default now(),
    check (cuota_inicial between 1 and cuotas_totales),
    check ((reparto = 'UNO_SOLO') = (beneficiario_id is not null)),
    -- Quien pagó y quien se beneficia tienen que ser del mismo hogar que el gasto.
    foreign key (pagador_id, hogar_id) references public.integrantes (id, hogar_id),
    foreign key (beneficiario_id, hogar_id) references public.integrantes (id, hogar_id),
    unique (id, hogar_id)
);
create index gastos_hogar_id_idx on public.gastos (hogar_id);
create index gastos_pagador_idx on public.gastos (pagador_id, hogar_id);
create index gastos_beneficiario_idx on public.gastos (beneficiario_id, hogar_id);

create table public.pagos (
    id                uuid primary key,
    hogar_id          uuid not null references public.hogares (id),
    de_integrante_id  uuid not null,
    a_integrante_id   uuid not null,
    monto_centavos    bigint not null check (monto_centavos > 0),
    fecha             date not null,
    updated_at        bigint not null,
    deleted           boolean not null default false,
    server_updated_at timestamptz not null default now(),
    check (de_integrante_id <> a_integrante_id),
    foreign key (de_integrante_id, hogar_id) references public.integrantes (id, hogar_id),
    foreign key (a_integrante_id, hogar_id) references public.integrantes (id, hogar_id)
);
create index pagos_hogar_id_idx on public.pagos (hogar_id);
create index pagos_de_idx on public.pagos (de_integrante_id, hogar_id);
create index pagos_a_idx on public.pagos (a_integrante_id, hogar_id);

create table public.pendientes (
    id                uuid primary key,
    hogar_id          uuid not null references public.hogares (id),
    tipo              text not null check (tipo in ('TAREA', 'COMPRA')),
    titulo            text not null check (btrim(titulo) <> ''),
    responsable_id    uuid,
    fecha_hora        timestamp,
    avisar            boolean not null default false,
    en_carrito        boolean not null default false,
    resuelto          boolean not null default false,
    gasto_id          uuid,
    updated_at        bigint not null,
    deleted           boolean not null default false,
    server_updated_at timestamptz not null default now(),
    check (
        (tipo = 'TAREA' and responsable_id is not null and fecha_hora is not null and not en_carrito and gasto_id is null)
        or (tipo = 'COMPRA' and responsable_id is null and fecha_hora is null and not avisar)
    ),
    foreign key (responsable_id, hogar_id) references public.integrantes (id, hogar_id),
    -- Una compra solo se vincula a un gasto de su mismo hogar.
    foreign key (gasto_id, hogar_id) references public.gastos (id, hogar_id)
);
create index pendientes_hogar_id_idx on public.pendientes (hogar_id);
create index pendientes_responsable_idx on public.pendientes (responsable_id, hogar_id);
create index pendientes_gasto_idx on public.pendientes (gasto_id, hogar_id);

-- ---------------------------------------------------------------------------
-- Descarga incremental y hora del servidor
-- ---------------------------------------------------------------------------
create index hogares_server_updated_at_idx on public.hogares (server_updated_at);
create index integrantes_hogar_server_updated_at_idx on public.integrantes (hogar_id, server_updated_at);
create index rutinas_hogar_server_updated_at_idx on public.rutinas (hogar_id, server_updated_at);
create index registros_rutina_server_updated_at_idx on public.registros_rutina (server_updated_at);
create index gastos_hogar_server_updated_at_idx on public.gastos (hogar_id, server_updated_at);
create index pagos_hogar_server_updated_at_idx on public.pagos (hogar_id, server_updated_at);
create index pendientes_hogar_server_updated_at_idx on public.pendientes (hogar_id, server_updated_at);

create trigger hogares_hora_servidor before insert or update on public.hogares
    for each row execute function public.marcar_hora_servidor();
create trigger integrantes_hora_servidor before insert or update on public.integrantes
    for each row execute function public.marcar_hora_servidor();
create trigger rutinas_hora_servidor before insert or update on public.rutinas
    for each row execute function public.marcar_hora_servidor();
create trigger registros_rutina_hora_servidor before insert or update on public.registros_rutina
    for each row execute function public.marcar_hora_servidor();
create trigger gastos_hora_servidor before insert or update on public.gastos
    for each row execute function public.marcar_hora_servidor();
create trigger pagos_hora_servidor before insert or update on public.pagos
    for each row execute function public.marcar_hora_servidor();
create trigger pendientes_hora_servidor before insert or update on public.pendientes
    for each row execute function public.marcar_hora_servidor();
