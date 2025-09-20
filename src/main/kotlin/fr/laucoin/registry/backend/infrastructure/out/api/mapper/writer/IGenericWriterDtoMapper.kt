package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

interface IGenericWriterDtoMapper<M, D> {
	fun toModel(dto: D): M

	fun toModels(dtos: List<D>): List<M> {
		return dtos.map { toModel(it) }
	}
}
