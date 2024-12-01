package fr.laucoin.registry.backend.infrastructure.internal.web.mapper

import fr.laucoin.registry.backend.domain.model.GenericModel
import java.util.UUID

interface IGenericEventDtoMapper<M: GenericModel, D> {
    fun toModel(dto: D, eventId: UUID): M
}
