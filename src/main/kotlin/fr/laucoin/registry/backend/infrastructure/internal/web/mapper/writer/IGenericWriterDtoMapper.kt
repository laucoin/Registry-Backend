package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.GenericModel

interface IGenericWriterDtoMapper<M: GenericModel, D> {
    fun toModel(dto: D): M
}
