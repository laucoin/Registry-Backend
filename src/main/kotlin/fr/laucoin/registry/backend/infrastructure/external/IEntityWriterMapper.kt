package fr.laucoin.registry.backend.infrastructure.external

interface IEntityWriterMapper<M, E> {
    fun toEntity(model: M): E
}
