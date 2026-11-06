package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.usecase.IMovementUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IMovementSearchV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PlannedMovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LabelSearchExt.matchesQuery
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementActivityReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementParticipantsAndGroupsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PlannedMovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.VehicleReaderDtoMapper
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur REST de la recherche des éléments d'un mouvement, des mouvements planifiés et des communications.
 * Périmètre : Mappe les paramètres de requête et les DTO, et délègue au cas d'usage des mouvements.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class MovementSearchV2Controller(
	private val service: IMovementUseCase,
	private val plannedReaderMapper: PlannedMovementReaderDtoMapper,
	private val reasonReaderMapper: MovementReasonReaderDtoMapper,
	private val activityReasonReaderMapper: MovementActivityReasonReaderDtoMapper,
	private val movementParticipantsAndGroupsMapper: MovementParticipantsAndGroupsReaderDtoMapper,
	private val vehiclesMapper: VehicleReaderDtoMapper,
): IMovementSearchV2Controller {
	override fun searchReasonsAndActivities(
		currentUser: CurrentUserModel,
		projectId: UUID,
		type: MovementTypeEnum,
		contentType: ParticipantTypeEnum,
		query: String?,
	): Flux<MovementReasonsReaderDto> {
		return service.searchActivitiesByText(currentUser, projectId, contentType, query)
			.map(activityReasonReaderMapper::toDto)
			.mergeWith(searchReasons(query, type, contentType))
	}

	private fun searchReasons(
		query: String?,
		type: MovementTypeEnum,
		contentType: ParticipantTypeEnum,
	): Flux<MovementReasonsReaderDto> {
		return service.searchReasonsByText(contentType, type)
			.map(reasonReaderMapper::toDto)
			.filter { it.label.matchesQuery(query) }
	}

	override fun searchParticipantsAndGroups(
		projectId: UUID,
		contentType: ParticipantTypeEnum,
		query: String?,
		at: ZonedDateTime,
		all: Boolean,
	): Mono<MovementParticipantsAndGroupsReaderDto> {
		return service.searchParticipantsAndGroupsByText(projectId, contentType, query, at, all)
			.map { Pair(it.t1, it.t2) }
			.map(movementParticipantsAndGroupsMapper::toDto)
	}

	override fun searchVehicles(projectId: UUID, query: String?): Flux<VehicleReaderDto> {
		return service.searchVehiclesByText(projectId, query).map(vehiclesMapper::toDto)
	}

	override fun findPlannedArrivals(projectId: UUID, date: LocalDate?): Flux<PlannedMovementReaderDto> {
		return service.findPlannedMovements(projectId, IN, date).map(plannedReaderMapper::toDto)
	}

	override fun findPlannedDepartures(projectId: UUID, date: LocalDate?): Flux<PlannedMovementReaderDto> {
		return service.findPlannedMovements(projectId, OUT, date).map(plannedReaderMapper::toDto)
	}
}
