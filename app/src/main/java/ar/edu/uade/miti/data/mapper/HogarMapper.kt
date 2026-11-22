package ar.edu.uade.miti.data.mapper

import ar.edu.uade.miti.data.local.entity.HogarEntity
import ar.edu.uade.miti.domain.model.Hogar

// Traducen entre tablas y modelos de dominio. Al leer se descartan los datos de sincronización;
// al guardar, el repositorio los agrega con [MetadatosSync].

fun HogarEntity.toDomain() = Hogar(id, nombre, codigoUnion)
