package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.GenericModel
import java.util.UUID

interface IGenericProjectWriterDtoMapper<M : GenericModel, D> {
	fun toModel(dto: D, projectId: UUID): M
	fun toModels(dtos: List<D>, projectId: UUID): List<M> {
		return dtos.map { toModel(it, projectId) }
	}
}
