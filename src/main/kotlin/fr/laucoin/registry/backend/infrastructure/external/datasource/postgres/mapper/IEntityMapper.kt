package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

interface IEntityMapper<M, E> {
    fun toModel(entity: E): M
    fun toEntity(model: M): E
}
