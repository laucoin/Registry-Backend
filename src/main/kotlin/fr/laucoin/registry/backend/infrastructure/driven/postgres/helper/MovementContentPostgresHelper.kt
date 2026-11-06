package fr.laucoin.registry.backend.infrastructure.driven.postgres.helper

import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IMovementContentEntityRepository
import java.util.UUID
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Lit et écrit le contenu des mouvements (participants, groupes, véhicules) en base.
 * Périmètre : Regroupe les contenus par mouvement, insère les nouveaux éléments et supprime les éléments retirés.
 * Limites : Ne gère ni le mouvement lui-même ni la transaction ; l'appelant orchestre l'ensemble.
 */
@Component
class MovementContentPostgresHelper(
	private val contentRepository: IMovementContentEntityRepository,
	private val contentMapper: MovementContentEntityMapper,
) {
	fun findContent(projectId: UUID, movementIds: List<UUID>): Flux<Pair<UUID, List<MovementContentModel>>> {
		return if (movementIds.isEmpty()) Flux.empty()
		else contentRepository.findAllByMovementIds(projectId, movementIds)
			.groupBy { it.movementId!! }
			.flatMap {
				it.collectList().map { list -> it.key() to list.map(contentMapper::toModel) }
			}
	}

	fun findCurrentContent(
		projectId: UUID,
		movementIds: List<UUID>
	): Flux<Pair<UUID, List<MovementContentModel>>> {
		return if (movementIds.isEmpty()) Flux.empty()
		else contentRepository.findCurrentByMovementIds(projectId, movementIds)
			.groupBy { it.movementId!! }
			.flatMap {
				it.collectList().map { list -> it.key() to list.map(contentMapper::toModel) }
			}
	}

	fun saveNewContent(movement: Mono<MovementModel>, element: MovementModel): Mono<MovementModel> {
		return movement.flatMap { addNewContent(it, element) }
	}

	private fun addNewContent(movement: MovementModel, element: MovementModel): Mono<MovementModel> {
		val newContent = movement.getNewContent(element)
		if (newContent.isEmpty()) return Mono.just(movement)
		return contentRepository.saveAll(newContent.map { contentMapper.toEntity(movement.id!!, it) })
			.map(contentMapper::toModel)
			.collectList()
			.map { movement.apply { content = content.plus(it) } }
	}

	fun removeDeletedContent(movement: Mono<MovementModel>, element: MovementModel): Mono<MovementModel> {
		return movement.flatMap { dropRemovedContent(it, element) }
	}

	private fun dropRemovedContent(movement: MovementModel, element: MovementModel): Mono<MovementModel> {
		val removedIds = movement.getOldContentIds(element)
		if (removedIds.isEmpty()) return Mono.just(movement)
		return contentRepository.deleteAllById(removedIds)
			.then(Mono.fromCallable { movement.apply { content = content.filter { !removedIds.contains(it.id) } } })
	}
}
