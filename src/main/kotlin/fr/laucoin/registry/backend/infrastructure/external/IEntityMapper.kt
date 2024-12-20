package fr.laucoin.registry.backend.infrastructure.external

interface IEntityMapper<M, E> {
    fun toModel(entity: E): M
    fun toEntity(model: M): E
}
