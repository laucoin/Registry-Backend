package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.service.IConcurrencyService
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IMovementV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PlannedMovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.expectedLastEditions
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LabelSearchExt.matchesQuery
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocationExt.created
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementActivityReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementContentReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementParticipantsAndGroupsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PlannedMovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.GuestMovementWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.GuestWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ParticipantMovementWriterDtoMapper
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: REST adapter of the movement endpoints.
 * Scope: Maps request parameters and DTOs, and delegates to the movement use cases.
 * Limits: Holds no business rule; authorization is declared on the contract.
 */
@RestController
class MovementV2Controller(
	private val service: IMovementService,
	private val concurrencyService: IConcurrencyService,
	private val readerMapper: MovementReaderDtoMapper,
	private val plannedReaderMapper: PlannedMovementReaderDtoMapper,
	private val readerContentMapper: MovementContentReaderDtoMapper,
	private val reasonReaderMapper: MovementReasonReaderDtoMapper,
	private val activityReasonReaderMapper: MovementActivityReasonReaderDtoMapper,
	private val communicationReaderMapper: CommunicationReaderDtoMapper,
	private val movementParticipantsAndGroupsMapper: MovementParticipantsAndGroupsReaderDtoMapper,
	private val vehiclesMapper: VehicleReaderDtoMapper,
	private val writerMapper: ParticipantMovementWriterDtoMapper,
	private val guestMovementWriterMapper: GuestMovementWriterDtoMapper,
	private val guestWriterMapper: GuestWriterDtoMapper,
): IMovementV2Controller {

	override fun findMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		page: Int,
		size: Int,
		isCurrent: Boolean,
		hasActivity: Boolean?,
		isVisible: Boolean?,
		type: MovementTypeEnum?,
		from: ZonedDateTime?,
		to: ZonedDateTime?,
		activityId: UUID?,
		sort: String?,
	): Mono<PageModel<MovementReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = MovementSearchParamModel(
			isVisible,
			hasActivity,
			type,
			from,
			to,
			activityId = activityId,
			sort = sort.toSortModel(MovementSearchParamModel.SORT_FIELDS),
		)

		return movementsPage(isCurrent, currentUser, projectId, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	private fun movementsPage(
		isCurrent: Boolean,
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>> {
		return if (isCurrent) {
			service.findCurrentMovementsPage(currentUser, projectId, pageable, searchParams)
		} else {
			service.findMovementsPage(currentUser, projectId, pageable, searchParams)
		}
	}

	override fun findMovementsContents(
		projectId: UUID,
		movementIds: List<UUID>,
		isCurrent: Boolean,
	): Flux<Pair<UUID, List<MovementContentReaderDto>>> {
		return movementsContent(isCurrent, projectId, movementIds).map(::toContentPair)
	}

	private fun movementsContent(
		isCurrent: Boolean,
		projectId: UUID,
		movementIds: List<UUID>,
	): Flux<Pair<UUID, List<MovementModel.MovementContentModel>>> {
		return if (isCurrent) {
			service.findCurrentMovementsContent(projectId, movementIds)
		} else {
			service.findMovementsContent(projectId, movementIds)
		}
	}

	private fun toContentPair(pair: Pair<UUID, List<MovementModel.MovementContentModel>>): Pair<UUID, List<MovementContentReaderDto>> {
		return Pair(pair.first, pair.second.map(readerContentMapper::toDto))
	}

	override fun findMovementById(projectId: UUID, id: UUID): Mono<MovementReaderDto> {
		return service.findMovementById(projectId, id, isVisible = null).map(readerMapper::toDto)
	}

	override fun searchReasonsAndActivities(
		projectId: UUID,
		type: MovementTypeEnum,
		contentType: ParticipantTypeEnum,
		query: String?,
	): Flux<MovementReasonsReaderDto> {
		return service.searchActivitiesByText(projectId, contentType, query)
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
		query: String?
	): Mono<MovementParticipantsAndGroupsReaderDto> {
		return service.searchParticipantsAndGroupsByText(projectId, contentType, query)
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

	override fun findMovementCommunications(
		projectId: UUID,
		id: UUID,
		page: Int,
		size: Int,
		query: String?,
		isVisible: Boolean?,
		from: ZonedDateTime?,
		to: ZonedDateTime?,
		sort: String?,
	): Mono<PageModel<CommunicationReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = CommunicationSearchParamModel(
			query, isVisible, from, to,
			sort = sort.toSortModel(CommunicationSearchParamModel.SORT_FIELDS),
		)

		return service.findMovementCommunicationsPage(projectId, id, pageable, searchParams)
			.map(communicationReaderMapper::toDtoPage)
	}

	override fun findParticipantsStatus(projectId: UUID): Mono<ProjectStatusModel> {
		return service.findParticipantsStatus(projectId)
	}

	override fun findVehiclesStatus(projectId: UUID): Mono<VehicleStatusModel> {
		return service.findVehiclesStatus(projectId)
	}

	override fun createMovement(
		currentUser: CurrentUserModel,
		projectId: UUID,
		movement: ParticipantMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<MovementReaderDto>> {
		return service.createMovement(currentUser, writerMapper.toModel(movement, projectId))
			.map(readerMapper::toDto)
			.map { exchange.created(it) }
	}

	override fun updateMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		movement: ParticipantMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<MovementReaderDto> {
		val movementModel = writerMapper.toModel(movement, projectId)
		return concurrencyService.updateIfUnchanged(
			LockableResourceEnum.MOVEMENT,
			id,
			exchange.expectedLastEditions(),
			current = { service.findMovementById(projectId, id, isVisible = null) },
			action = { service.updateMovementById(currentUser, projectId, id, movementModel) },
		).map(readerMapper::toDto)
	}

	override fun createGuestsMovement(
		currentUser: CurrentUserModel,
		projectId: UUID,
		movement: GuestMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<MovementReaderDto>> {
		val movementModel = guestMovementWriterMapper.toModel(movement, projectId)
		val newGuestModels = guestWriterMapper.toModels(movement.guests ?: emptyList(), projectId)
		return service.createMovement(currentUser, movementModel, newGuestModels)
			.map(readerMapper::toDto)
			.map { exchange.created(it, "/guests") }
	}

	override fun updateGuestsMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		movement: GuestMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<MovementReaderDto> {
		val movementModel = guestMovementWriterMapper.toModel(movement, projectId)
		val newGuestModels = guestWriterMapper.toModels(movement.guests ?: emptyList(), projectId)

		return concurrencyService.updateIfUnchanged(
			LockableResourceEnum.MOVEMENT,
			id,
			exchange.expectedLastEditions(),
			current = { service.findMovementById(projectId, id, isVisible = null) },
			action = { service.updateMovementById(currentUser, projectId, id, movementModel, newGuestModels) },
		).map(readerMapper::toDto)
	}

	override fun disableMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<MovementReaderDto> {
		return service.disableMovementById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun enableMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<MovementReaderDto> {
		return service.enableMovementById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun deleteMovementById(projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteMovementById(projectId, id)
	}
}
