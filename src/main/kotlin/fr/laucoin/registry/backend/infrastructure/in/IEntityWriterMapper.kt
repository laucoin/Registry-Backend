package fr.laucoin.registry.backend.infrastructure.`in`

interface IEntityWriterMapper<M, E> {
	fun toEntity(model: M): E
}
