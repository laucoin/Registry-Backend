package fr.laucoin.registry.backend.infrastructure.driven

fun interface IEntityWriterMapper<M, E> {
	fun toEntity(model: M): E
}
