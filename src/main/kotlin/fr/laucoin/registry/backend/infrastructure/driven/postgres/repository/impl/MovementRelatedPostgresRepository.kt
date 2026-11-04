package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IMovementEntityRepository
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Objectif : Lit les pages et les compteurs de mouvements rattachés à un participant, un véhicule ou une activité.
 * Périmètre : Appels paramétrés au repository d'entités et mappage des pages vers les modèles du domaine.
* Limites : Ne porte aucune règle métier ; n'est pas un port du domaine, il est composé par l'adaptateur du mouvement.
 */
@Service
class MovementRelatedPostgresRepository(
	private val repository: IMovementEntityRepository,
	private val mapper: MovementEntityMapper,
	private val clock: Clock,
) {
	fun findPageByParticipantId(
		projectId: UUID,
		participantId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> =
		repository.findAllByParticipantId(
			projectId,
			participantId,
			searchParams.isVisible,
			searchParams.hasActivity,
			searchParams.type,
			searchParams.from,
			searchParams.to,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, clock, MovementEntity::fullCount, mapper::toModel)

	fun findPageByVehicleId(
		projectId: UUID,
		vehicleId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> =
		repository.findAllByVehicleId(
			projectId,
			vehicleId,
			searchParams.isVisible,
			searchParams.hasActivity,
			searchParams.type,
			searchParams.from,
			searchParams.to,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, clock, MovementEntity::fullCount, mapper::toModel)

	fun findPageByActivityId(
		projectId: UUID,
		activityId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return repository.findAllByActivityId(
			projectId,
			activityId,
			searchParams.isVisible,
			searchParams.type,
			searchParams.from,
			searchParams.to,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, clock, MovementEntity::fullCount, mapper::toModel)
	}

	fun countAllByParticipantId(
		projectId: UUID,
		participantId: UUID,
		searchParams: MovementSearchParamModel,
	): Mono<Long> {
		return repository.countAllByParticipantId(
			projectId,
			participantId,
			searchParams.isVisible,
			searchParams.hasActivity,
			searchParams.type,
			searchParams.from,
			searchParams.to,
		)
	}

	fun countAllByVehicleId(
		projectId: UUID,
		vehicleId: UUID,
		searchParams: MovementSearchParamModel,
	): Mono<Long> {
		return repository.countAllByVehicleId(
			projectId,
			vehicleId,
			searchParams.isVisible,
			searchParams.hasActivity,
			searchParams.type,
			searchParams.from,
			searchParams.to,
		)
	}

	fun countAllByActivityId(
		projectId: UUID,
		activityId: UUID,
		searchParams: MovementSearchParamModel,
	): Mono<Long> {
		return repository.countAllByActivityId(
			projectId,
			activityId,
			searchParams.isVisible,
			searchParams.type,
			searchParams.from,
			searchParams.to,
		)
	}
}
