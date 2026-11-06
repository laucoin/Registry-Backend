package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.PlannedMovementModel
import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import fr.laucoin.registry.backend.domain.service.core.IMovementQueryService
import fr.laucoin.registry.backend.domain.service.core.IMovementSearchService
import fr.laucoin.registry.backend.domain.service.core.IMovementService
import fr.laucoin.registry.backend.domain.service.usecase.IMovementUseCase
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2

/**
 * Objectif : Point d'entrée des mouvements pour la couche REST.
 * Périmètre : Délègue chaque opération au service Movement sans logique propre.
 * Limites : Ne porte aucune règle métier ; elles restent dans le service.
 */
@Service
class MovementUseCase(
	private val queryService: IMovementQueryService,
	private val searchService: IMovementSearchService,
	private val service: IMovementService,
) : IMovementUseCase {
	override fun findMovementsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>> {
		return queryService.findMovementsPage(currentUser, projectId, pageable, searchParams)
	}

	override fun findCurrentMovementsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>> {
		return queryService.findCurrentMovementsPage(currentUser, projectId, pageable, searchParams)
	}

	override fun findMovementsContent(
		projectId: UUID,
		movementIds: List<UUID>,
	): Flux<Pair<UUID, List<MovementContentModel>>> {
		return queryService.findMovementsContent(projectId, movementIds)
	}

	override fun findCurrentMovementsContent(
		projectId: UUID,
		movementIds: List<UUID>,
	): Flux<Pair<UUID, List<MovementContentModel>>> {
		return queryService.findCurrentMovementsContent(projectId, movementIds)
	}

	override fun findMovementById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<MovementModel> {
		return queryService.findMovementById(projectId, id, isVisible)
	}

	override fun searchParticipantsAndGroupsByText(
		projectId: UUID,
		type: ParticipantTypeEnum,
		query: String?,
		at: ZonedDateTime,
		all: Boolean,
	): Mono<Tuple2<List<ParticipantModel>, List<GroupModel>>> {
		return searchService.searchParticipantsAndGroupsByText(projectId, type, query, at, all)
	}

	override fun searchVehiclesByText(projectId: UUID, query: String?): Flux<VehicleModel> {
		return searchService.searchVehiclesByText(projectId, query)
	}

	override fun searchReasonsByText(
		contentType: ParticipantTypeEnum,
		type: MovementTypeEnum,
	): Flux<MovementReasonEnum> {
		return searchService.searchReasonsByText(contentType, type)
	}

	override fun searchActivitiesByText(
		currentUser: CurrentUserModel,
		projectId: UUID,
		contentType: ParticipantTypeEnum,
		query: String?,
	): Flux<ActivityModel> {
		return searchService.searchActivitiesByText(currentUser, projectId, contentType, query)
	}

	override fun findMovementCommunicationsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel,
	): Mono<PageModel<CommunicationModel>> {
		return queryService.findMovementCommunicationsPage(projectId, id, pageable, searchParams)
	}

	override fun findPlannedMovements(
		projectId: UUID,
		type: MovementTypeEnum,
		date: LocalDate?,
	): Flux<PlannedMovementModel> {
		return searchService.findPlannedMovements(projectId, type, date)
	}

	override fun findParticipantsStatus(projectId: UUID): Mono<ProjectStatusModel> {
		return queryService.findParticipantsStatus(projectId)
	}

	override fun findVehiclesStatus(projectId: UUID): Mono<VehicleStatusModel> {
		return queryService.findVehiclesStatus(projectId)
	}

	override fun createMovement(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		newGuests: List<ParticipantModel>,
	): Mono<MovementModel> {
		return service.createMovement(currentUser, movement, newGuests)
	}

	override fun updateMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		movement: MovementModel,
		newGuests: List<ParticipantModel>,
	): Mono<MovementModel> {
		return service.updateMovementById(currentUser, projectId, id, movement, newGuests)
	}

	override fun disableMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<MovementModel> {
		return service.disableMovementById(currentUser, projectId, id)
	}

	override fun enableMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<MovementModel> {
		return service.enableMovementById(currentUser, projectId, id)
	}

	override fun deleteMovementById(projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteMovementById(projectId, id)
	}
}
