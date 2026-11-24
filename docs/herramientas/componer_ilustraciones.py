"""Arma las ilustraciones del primer uso a partir de las piezas que exporta Figma.

Cada pieza es un SVG con paths que en Figma está posicionado con `inset` (porcentajes del contenedor).
Se reubica cada path con un transform y el texto se pasa a trazos con la Roboto, porque los vectores
de Android no admiten texto. Salida: un SVG por ilustración, listo para svg2vectordrawable.

Uso (las piezas se bajan del Figma con el MCP; las rutas están en ilustraciones.json):
    python3 componer_ilustraciones.py ilustraciones.json
    npx svg2vectordrawable@2.9.1 -i ilustracion_rutinas.svg -o app/src/main/res/drawable/ilustracion_rutinas.xml
Después se ajusta android:width/height al tamaño en dp (el Figma está a escala 0,849).
"""
import json
import re
import sys
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

FUENTE = "/usr/share/fonts/truetype/google-fonts/ofl/roboto/Roboto[wdth,wght].ttf"
_fuentes = {}


def fuente(peso):
    if peso not in _fuentes:
        _fuentes[peso] = instantiateVariableFont(TTFont(FUENTE), {"wght": peso, "wdth": 100})
    return _fuentes[peso]


def caja(inset, ancho, alto):
    """inset = [arriba, derecha, abajo, izquierda] en % del contenedor -> (x, y, w, h)."""
    t, r, b, l = inset
    x, y = l / 100 * ancho, t / 100 * alto
    return x, y, ancho - x - r / 100 * ancho, alto - y - b / 100 * alto


def pieza(archivo, inset, ancho, alto, interior=None, interior_px=None):
    x, y, w, h = caja(inset, ancho, alto)
    if interior:  # inset interno negativo (en % de la propia caja): la imagen sobresale
        t, r, b, l = interior
        x, y, w, h = x + l / 100 * w, y + t / 100 * h, w * (1 - (l + r) / 100), h * (1 - (t + b) / 100)
    if interior_px:  # lo mismo, en píxeles (Figma lo usa para las líneas de grosor fijo)
        t, r, b, l = interior_px
        x, y, w, h = x + l, y + t, w - l - r, h - t - b
    svg = open(archivo).read()
    vb = [float(v) for v in re.search(r'viewBox="([^"]+)"', svg).group(1).split()]
    sx, sy = w / vb[2], h / vb[3]
    cuerpo = svg[svg.index(">", svg.index("<svg")) + 1: svg.rindex("</svg>")]
    # Figma escribe los colores como var(--fill-0, #XXXXXX): queda el color de respaldo.
    cuerpo = re.sub(r'var\(--[a-z0-9-]+,\s*([^)]+)\)', r'\1', cuerpo)
    cuerpo = re.sub(r'\sid="[^"]*"', "", cuerpo)
    return f'<g fill="none" transform="translate({x - vb[0] * sx:.3f} {y - vb[1] * sy:.3f}) scale({sx:.5f} {sy:.5f})">{cuerpo}</g>'


def texto(tramos, inset, ancho, alto, tam, color, alinear="left", tachado=False):
    """tramos = [[texto, peso], ...]. Devuelve paths con el texto en trazos."""
    x0, y0, w, _ = caja(inset, ancho, alto)
    f = fuente(tramos[0][1])
    upm = f["head"].unitsPerEm
    asc = f["hhea"].ascent
    escala = tam / upm
    base = y0 + asc * escala
    # Ancho total para alinear.
    total = 0
    for t, peso in tramos:
        ff = fuente(peso)
        cmap, hmtx = ff.getBestCmap(), ff["hmtx"]
        total += sum(hmtx[cmap.get(ord(c), cmap[ord("?")])][0] for c in t) * escala
    x = {"left": x0, "center": x0 + (w - total) / 2, "right": x0 + w - total}[alinear]
    inicio, salida = x, []
    for t, peso in tramos:
        ff = fuente(peso)
        cmap, hmtx, glifos = ff.getBestCmap(), ff["hmtx"], ff.getGlyphSet()
        for c in t:
            nombre = cmap.get(ord(c), cmap[ord("?")])
            pen = SVGPathPen(glifos)
            glifos[nombre].draw(TransformPen(pen, (escala, 0, 0, -escala, x, base)))
            d = pen.getCommands()
            if d:
                salida.append(f'<path d="{d}" fill="{color}"/>')
            x += hmtx[nombre][0] * escala
    if tachado:
        y = base - tam * 0.3
        salida.append(f'<path d="M{inicio:.2f} {y:.2f}H{x:.2f}" stroke="{color}" stroke-width="{tam * 0.07:.2f}"/>')
    return "".join(salida)


def componer(spec):
    ancho, alto = spec["ancho"], spec["alto"]
    partes = []
    for capa in spec["capas"]:
        if "archivo" in capa:
            partes.append(pieza(capa["archivo"], capa["inset"], ancho, alto, capa.get("interior"), capa.get("interior_px")))
        else:
            partes.append(texto(capa["tramos"], capa["inset"], ancho, alto, capa["tam"], capa["color"],
                                capa.get("alinear", "left"), capa.get("tachado", False)))
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{ancho}" height="{alto}" '
            f'viewBox="0 0 {ancho} {alto}">{"".join(partes)}</svg>')


if __name__ == "__main__":
    specs = json.load(open(sys.argv[1]))
    for nombre, spec in specs.items():
        open(f"{nombre}.svg", "w").write(componer(spec))
        print("ok", nombre)
