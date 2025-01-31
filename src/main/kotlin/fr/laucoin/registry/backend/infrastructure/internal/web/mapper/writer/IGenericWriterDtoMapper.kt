package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

interface IGenericWriterDtoMapper<M, D> {
    fun toModel(dto: D): M
}
