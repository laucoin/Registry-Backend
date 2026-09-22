package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

interface IGenericWriterDtoMapper<M, D> {
	fun toModel(dto: D): M

	fun toModels(dtos: List<D>): List<M> {
		return dtos.map(this::toModel)
	}
}
