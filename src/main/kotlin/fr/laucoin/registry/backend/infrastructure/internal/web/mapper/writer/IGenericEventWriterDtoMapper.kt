package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.GenericModel
import java.util.UUID

interface IGenericEventWriterDtoMapper<M: GenericModel, D> {
    fun toModel(dto: D, eventId: UUID): M
    fun toModels(dtos: List<D>, eventId: UUID): List<M> {
        return dtos.map { toModel(it, eventId) }
    }
}
