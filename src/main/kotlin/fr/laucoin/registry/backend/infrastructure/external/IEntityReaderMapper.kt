package fr.laucoin.registry.backend.infrastructure.external

interface IEntityReaderMapper<M, E> {
    fun toModel(entity: E): M
}
