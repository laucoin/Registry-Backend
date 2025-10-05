package fr.laucoin.registry.backend.infrastructure.`in`

fun interface IEntityWriterMapper<M, E> {
	fun toEntity(model: M): E
}
