package fr.laucoin.registry.backend.infrastructure.internal.web.mapper

import fr.laucoin.registry.backend.domain.model.GenericModel

interface IGenericDtoMapper<M: GenericModel, D> {
    fun toModel(dto: D): M
}
