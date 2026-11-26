#!/usr/bin/env python3
"""
Carga un hogar de demo creíble en Supabase: Ana y Tomás, con su perro Pancho, rutinas, tareas, la lista de
compras y los gastos de este mes y del anterior (ver docs/guion-demo.md).

Todo pasa por la API pública con la sesión de cada uno, así que respeta las mismas reglas que la app (RLS,
funciones crear_hogar y unirse_a_hogar). No usa la contraseña de la base ni la service_role.

Uso (la víspera de la demo, para que las fechas queden bien):

    python3 supabase/demo/cargar_demo.py --crear-cuentas   # la primera vez
    python3 supabase/demo/cargar_demo.py                   # las siguientes: borra lo anterior y vuelve a cargar

Lee de ~/.config/miti/supabase.env (o del entorno): SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY,
DEMO_ANA_PASSWORD y DEMO_TOMAS_PASSWORD. Con --crear-cuentas, si faltan las contraseñas, las genera y las
agrega a ese archivo. Después de recargar, en los celulares conviene cerrar sesión y volver a ingresar.
"""
import argparse
import json
import os
import secrets
import sys
import time
import urllib.error
import urllib.request
import uuid
from datetime import date, datetime, timedelta
from pathlib import Path

ARCHIVO_ENV = Path.home() / ".config" / "miti" / "supabase.env"
ANA = {"nombre": "Ana", "email": "ana@miti.test", "clave": "DEMO_ANA_PASSWORD"}
TOMAS = {"nombre": "Tomás", "email": "tomas@miti.test", "clave": "DEMO_TOMAS_PASSWORD"}
MESES = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre",
         "noviembre", "diciembre"]


def leer_env():
    valores = {}
    if ARCHIVO_ENV.exists():
        for linea in ARCHIVO_ENV.read_text(encoding="utf-8").splitlines():
            if "=" in linea and not linea.lstrip().startswith("#"):
                clave, valor = linea.split("=", 1)
                valores[clave.strip()] = valor.strip().strip("'\"")
    valores.update({k: v for k, v in os.environ.items() if k.startswith(("SUPABASE_", "DEMO_"))})
    return valores


def agregar_al_env(clave, valor):
    with ARCHIVO_ENV.open("a", encoding="utf-8") as f:
        f.write(f"{clave}='{valor}'\n")


class Supabase:
    def __init__(self, url, clave_publica):
        self.url = url.rstrip("/")
        self.clave = clave_publica

    def pedir(self, metodo, ruta, cuerpo=None, token=None, extra=None):
        cabeceras = {"apikey": self.clave, "Content-Type": "application/json"}
        if token:
            cabeceras["Authorization"] = f"Bearer {token}"
        cabeceras.update(extra or {})
        datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
        pedido = urllib.request.Request(self.url + ruta, data=datos, method=metodo, headers=cabeceras)
        try:
            with urllib.request.urlopen(pedido, timeout=20) as respuesta:
                texto = respuesta.read().decode()
                return json.loads(texto) if texto else None
        except urllib.error.HTTPError as e:
            raise RuntimeError(f"{metodo} {ruta} → {e.code}: {e.read().decode()[:300]}") from None

    def ingresar(self, email, contrasenia):
        r = self.pedir("POST", "/auth/v1/token?grant_type=password", {"email": email, "password": contrasenia})
        return r["access_token"], r["user"]["id"]

    def crear_cuenta(self, email, contrasenia, nombre):
        r = self.pedir("POST", "/auth/v1/signup", {"email": email, "password": contrasenia, "data": {"nombre": nombre}})
        if not r.get("access_token"):
            sys.exit("Supabase pidió confirmar el email: desactivar 'Confirm email' en Authentication → Sign In.")
        return r["access_token"], r["user"]["id"]


def ms():
    return int(time.time() * 1000)


def nuevo_id():
    return str(uuid.uuid4())


def mes_de(fecha):
    return f"{fecha.year:04d}-{fecha.month:02d}"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--crear-cuentas", action="store_true", help="crear las cuentas si todavía no existen")
    parser.add_argument("--fecha", help="día de la demo, AAAA-MM-DD (por defecto, mañana)")
    args = parser.parse_args()

    env = leer_env()
    api = Supabase(env["SUPABASE_URL"], env["SUPABASE_PUBLISHABLE_KEY"])
    dia = date.fromisoformat(args.fecha) if args.fecha else date.today() + timedelta(days=1)

    # --- Cuentas ---
    sesiones = {}
    for persona in (ANA, TOMAS):
        contrasenia = env.get(persona["clave"])
        if not contrasenia:
            if not args.crear_cuentas:
                sys.exit(f"Falta {persona['clave']} en {ARCHIVO_ENV}. La primera vez, correr con --crear-cuentas.")
            contrasenia = secrets.token_urlsafe(16)
            agregar_al_env(persona["clave"], contrasenia)
        try:
            sesiones[persona["nombre"]] = api.ingresar(persona["email"], contrasenia)
        except RuntimeError:
            if not args.crear_cuentas:
                raise
            sesiones[persona["nombre"]] = api.crear_cuenta(persona["email"], contrasenia, persona["nombre"])
            print(f"Cuenta creada: {persona['email']}")
    token_ana, ana = sesiones["Ana"]
    token_tomas, tomas = sesiones["Tomás"]

    # --- Hogar ---
    mio = api.pedir("GET", f"/rest/v1/integrantes?id=eq.{ana}&select=hogar_id", token=token_ana)
    if mio:
        hogar = mio[0]["hogar_id"]
    else:
        hogar = api.pedir("POST", "/rest/v1/rpc/crear_hogar", {"nombre_hogar": "Nuestro hogar", "mi_nombre": "Ana"},
                          token=token_ana)["id"]
        print("Hogar creado")
    if not api.pedir("GET", f"/rest/v1/integrantes?id=eq.{tomas}&select=hogar_id", token=token_tomas):
        codigo = api.pedir("GET", f"/rest/v1/hogares?id=eq.{hogar}&select=codigo_union", token=token_ana)[0]["codigo_union"]
        api.pedir("POST", "/rest/v1/rpc/unirse_a_hogar", {"codigo": codigo, "mi_nombre": "Tomás"}, token=token_tomas)
        print("Tomás se unió al hogar")

    # --- Borrar lo anterior (borrado lógico, D07: los celulares lo bajan como borrado) ---
    for tabla in ("rutinas", "pendientes", "gastos", "pagos"):
        api.pedir("PATCH", f"/rest/v1/{tabla}?hogar_id=eq.{hogar}&deleted=eq.false",
                  {"deleted": True, "updated_at": ms()}, token=token_ana, extra={"Prefer": "return=minimal"})

    def insertar(tabla, filas, token=token_ana):
        ahora = ms()
        for f in filas:
            f.setdefault("id", nuevo_id())
            f["hogar_id"] = hogar
            f["updated_at"] = ahora
            f["deleted"] = False
        api.pedir("POST", f"/rest/v1/{tabla}", filas, token=token, extra={"Prefer": "return=minimal"})

    # --- Rutinas: sin marcas, así la demo arranca con "0 de 2" ---
    insertar("rutinas", [
        {"titulo": "Darle de comer a Pancho", "frecuencia": "DIARIA", "dias": "", "veces_por_dia": 2},
        {"titulo": "Regar las plantas", "frecuencia": "DIARIA", "dias": "", "veces_por_dia": 1},
        {"titulo": "Sacar la basura", "frecuencia": "DIAS_DE_LA_SEMANA", "dias": "MONDAY,WEDNESDAY,FRIDAY",
         "veces_por_dia": 1},
    ])

    # --- Tareas (la de la demo, "Pagar el ABL", se crea en vivo) y compras ---
    def tarea(titulo, quien, dias_despues, hora, avisar):
        cuando = datetime.combine(dia + timedelta(days=dias_despues), datetime.strptime(hora, "%H:%M").time())
        return {"tipo": "TAREA", "titulo": titulo, "responsable_id": quien, "fecha_hora": cuando.isoformat(),
                "avisar": avisar, "en_carrito": False, "resuelto": False, "gasto_id": None}

    def compra(titulo):
        return {"tipo": "COMPRA", "titulo": titulo, "responsable_id": None, "fecha_hora": None, "avisar": False,
                "en_carrito": False, "resuelto": False, "gasto_id": None}

    insertar("pendientes", [
        tarea("Llevar a Pancho a la veterinaria", ana, 2, "10:30", True),
        tarea("Llamar al plomero por la canilla del baño", tomas, 1, "18:00", False),
        tarea("Pagar las expensas", ana, 4, "09:00", True),
        compra("Leche"), compra("Pan"), compra("Yerba"),
    ])

    # --- Gastos: el mes anterior (con un pago parcial que deja saldo) y el de la demo ---
    este = dia.replace(day=1)
    anterior = (este - timedelta(days=1)).replace(day=1)
    nombre_este, nombre_anterior = MESES[este.month - 1], MESES[anterior.month - 1]

    def gasto(descripcion, pesos, pagador, mes, beneficiario=None, cuotas=1):
        return {"descripcion": descripcion, "monto_total_centavos": pesos * 100, "pagador_id": pagador,
                "reparto": "UNO_SOLO" if beneficiario else "A_MEDIAS", "beneficiario_id": beneficiario,
                "cuotas_totales": cuotas, "cuota_inicial": 1, "mes_inicio": mes_de(mes), "base_updated_at": None}

    insertar("gastos", [
        # Mes anterior
        gasto(f"Alquiler de {nombre_anterior}", 600_000, ana, anterior),
        gasto(f"Expensas de {nombre_anterior}", 85_000, tomas, anterior),
        gasto("Supermercado", 42_300, ana, anterior),
        gasto("Luz", 28_900, tomas, anterior),
        gasto("Heladera", 900_000, tomas, anterior, cuotas=6),
        # Mes de la demo
        gasto(f"Alquiler de {nombre_este}", 600_000, ana, este),
        gasto(f"Expensas de {nombre_este}", 87_500, tomas, este),
        gasto("Supermercado", 31_750, tomas, este),
        gasto("Internet", 24_990, ana, este),
        gasto("Vacunas de Pancho", 38_000, ana, este),
        gasto("Remedios de Tomás", 15_800, ana, este, beneficiario=tomas),
    ])
    # Tomás le paga una parte de lo del mes anterior: el resto pasa como "De meses anteriores".
    insertar("pagos", [{"de_integrante_id": tomas, "a_integrante_id": ana, "monto_centavos": 150_000 * 100,
                        "fecha": anterior.replace(day=28).isoformat()}], token=token_tomas)

    print(f"Listo: hogar de demo cargado para el {dia.strftime('%d/%m/%Y')} ({nombre_este}).")
    print(f"Cuentas: {ANA['email']} y {TOMAS['email']} (contraseñas en {ARCHIVO_ENV}).")


if __name__ == "__main__":
    main()
