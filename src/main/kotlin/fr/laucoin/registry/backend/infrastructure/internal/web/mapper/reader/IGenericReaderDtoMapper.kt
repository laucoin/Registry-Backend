package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

interface IGenericReaderDtoMapper<M, D> {
    fun toDto(model: M): D

    fun toDtoList(modelList: List<M>): List<D> {
        return modelList.map { toDto(it) }
    }
}
