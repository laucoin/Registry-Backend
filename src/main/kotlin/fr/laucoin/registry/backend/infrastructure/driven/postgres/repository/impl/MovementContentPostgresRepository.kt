package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementContentEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IMovementContentEntityRepository
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Lit et écrit le contenu (participants, véhicules) des mouvements dans Postgres.
 * Périmètre : Regroupement du contenu par mouvement, ajout du nouveau contenu et suppression du contenu retiré.
* Limites : Ne porte aucune règle métier ; n'est pas un port du domaine, il est composé par l'adaptateur du mouvement.
 */
@Service
class MovementContentPostgresRepository(
	private val contentRepository: IMovementContentEntityRepository,
	private val contentMapper: MovementContentEntityMapper,
) {
	fun findContent(projectId: UUID, movementIds: List<UUID>): Flux<Pair<UUID, List<MovementContentModel>>> =
		groupByMovement(movementIds) { contentRepository.findAllByMovementIds(projectId, movementIds) }

	fun findCurrentContent(projectId: UUID, movementIds: List<UUID>): Flux<Pair<UUID, List<MovementContentModel>>> =
		groupByMovement(movementIds) { contentRepository.findCurrentByMovementIds(projectId, movementIds) }

	private fun groupByMovement(
		movementIds: List<UUID>,
		load: () -> Flux<MovementContentEntity>,
	): Flux<Pair<UUID, List<MovementContentModel>>> =
		if (movementIds.isEmpty()) Flux.empty()
		else load()
			.groupBy { it.movementId!! }
			.flatMap { group -> group.collectList().map { list -> group.key() to list.map(contentMapper::toModel) } }

	fun saveNewContent(movement: MovementModel, element: MovementModel): Mono<MovementModel> {
		val newContent = movement.getNewContent(element)
		if (newContent.isEmpty()) return Mono.just(movement)

		return contentRepository.saveAll(newContent.map { contentMapper.toEntity(movement.id!!, it) })
			.map(contentMapper::toModel)
			.collectList()
			.map { movement.apply { content = content.plus(it) } }
	}

	fun removeDeletedContent(movement: MovementModel, element: MovementModel): Mono<MovementModel> {
		val removedIds = movement.getOldContentIds(element)
		if (removedIds.isEmpty()) return Mono.just(movement)

		return contentRepository.deleteAllById(removedIds)
			.then(Mono.fromCallable { movement.apply { content = content.filter { !removedIds.contains(it.id) } } })
	}
}
