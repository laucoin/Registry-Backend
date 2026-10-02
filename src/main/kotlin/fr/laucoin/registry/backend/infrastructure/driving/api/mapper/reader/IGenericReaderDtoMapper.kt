package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader


interface IGenericReaderDtoMapper<M, D> {
	fun toDto(model: M): D

	fun toDtoList(modelList: List<M>): List<D> {
		return modelList.map(this::toDto)
	}
}
