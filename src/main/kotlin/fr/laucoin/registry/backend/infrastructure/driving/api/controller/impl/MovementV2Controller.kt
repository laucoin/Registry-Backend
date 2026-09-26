package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IMovementV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectStatusReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleStatusReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.PageQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementActivityReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementContentReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementParticipantsAndGroupsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PageReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.VehicleStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.GuestMovementWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.GuestWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ParticipantMovementWriterDtoMapper
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class MovementV2Controller(
	private val service: IMovementService,
	private val readerMapper: MovementReaderDtoMapper,
	private val readerContentMapper: MovementContentReaderDtoMapper,
	private val reasonReaderMapper: MovementReasonReaderDtoMapper,
	private val activityReasonReaderMapper: MovementActivityReasonReaderDtoMapper,
	private val communicationReaderMapper: CommunicationReaderDtoMapper,
	private val movementParticipantsAndGroupsMapper: MovementParticipantsAndGroupsReaderDtoMapper,
	private val vehiclesMapper: VehicleReaderDtoMapper,
	private val writerMapper: ParticipantMovementWriterDtoMapper,
	private val guestMovementWriterMapper: GuestMovementWriterDtoMapper,
	private val guestWriterMapper: GuestWriterDtoMapper,
	private val pageQueryMapper: PageQueryDtoMapper,
	private val pageReaderMapper: PageReaderDtoMapper,
	private val projectStatusReaderMapper: ProjectStatusReaderDtoMapper,
	private val vehicleStatusReaderMapper: VehicleStatusReaderDtoMapper,
): IMovementV2Controller {
	private val similarity: JaroWinklerSimilarity = JaroWinklerSimilarity()

	override fun findMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		page: PageQueryDto,
		currentMovements: Boolean,
		linkedToActivity: Boolean?,
		visible: Boolean?,
		type: MovementTypeEnum?,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Mono<PageReaderDto<MovementReaderDto>> {
		if (!currentUser.hasAuthority(projectId, REGISTRY_PROJECT_OPTION_ACTIVITY) && linkedToActivity == true) {
			throw RegistryException(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION)
		}

		val pageable = pageQueryMapper.toPageable(page)
		val searchParams = MovementSearchParamModel(visible, linkedToActivity, type, startDateTime, endDateTime)

		val movements = if (currentMovements) {
			service.findCurrentMovementsPage(projectId, pageable, searchParams)
		} else {
			service.findMovementsPage(projectId, pageable, searchParams)
		}

		return movements.map { pageReaderMapper.toDto(it, readerMapper::toDto) }
	}

	override fun findMovementsContents(
		projectId: UUID,
		movementIds: List<UUID>,
		currentMovements: Boolean,
	): Flux<Pair<UUID, List<MovementContentReaderDto>>> {
		val contents = if (currentMovements) {
			service.findCurrentMovementsContent(projectId, movementIds)
		} else {
			service.findMovementsContent(projectId, movementIds)
		}

		return contents.map { Pair(it.first, it.second.map(readerContentMapper::toDto)) }
	}

	override fun findMovementById(projectId: UUID, id: UUID): Mono<MovementReaderDto> {
		return service.findMovementById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun searchReasonsAndActivities(
		projectId: UUID,
		type: MovementTypeEnum,
		contentType: ParticipantTypeEnum,
		q: String?,
	): Flux<MovementReasonsReaderDto> {
		return service.searchActivitiesByText(projectId, contentType, q)
			.map(activityReasonReaderMapper::toDto)
			.mergeWith(searchReasons(q, type, contentType))
	}

	private fun searchReasons(
		q: String?,
		type: MovementTypeEnum,
		contentType: ParticipantTypeEnum,
	): Flux<MovementReasonsReaderDto> {
		return service.searchReasonsByText(contentType, type)
			.map(reasonReaderMapper::toDto)
			.map { Pair(it, similarity.apply(it.label, q ?: it.label)) }
			.filter { it.second > 0 }
			.map(Pair<MovementReasonsReaderDto, Double>::first)
	}

	override fun searchParticipantsAndGroups(
		projectId: UUID,
		contentType: ParticipantTypeEnum,
		q: String?,
	): Mono<MovementParticipantsAndGroupsReaderDto> {
		return service.searchParticipantsAndGroupsByText(projectId, contentType, q)
			.map { Pair(it.t1, it.t2) }
			.map(movementParticipantsAndGroupsMapper::toDto)
	}

	override fun searchVehicles(projectId: UUID, q: String?): Flux<VehicleReaderDto> {
		return service.searchVehiclesByText(projectId, q).map(vehiclesMapper::toDto)
	}

	override fun findMovementCommunications(
		projectId: UUID,
		id: UUID,
		page: PageQueryDto,
		q: String?,
		visible: Boolean?,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Mono<PageReaderDto<CommunicationReaderDto>> {
		val pageable = pageQueryMapper.toPageable(page)
		val searchParams = CommunicationSearchParamModel(q, visible, startDateTime, endDateTime)

		return service.findMovementCommunicationsPage(projectId, id, pageable, searchParams)
			.map { pageReaderMapper.toDto(it, communicationReaderMapper::toDto) }
	}

	override fun findParticipantsStatus(projectId: UUID): Mono<ProjectStatusReaderDto> {
		return service.findParticipantsStatus(projectId).map(projectStatusReaderMapper::toDto)
	}

	override fun findVehiclesStatus(projectId: UUID): Mono<VehicleStatusReaderDto> {
		return service.findVehiclesStatus(projectId).map(vehicleStatusReaderMapper::toDto)
	}

	override fun createMovement(
		currentUser: CurrentUserModel,
		projectId: UUID,
		movement: ParticipantMovementWriterDto,
	): Mono<ResponseEntity<MovementReaderDto>> {
		return service.createMovement(currentUser, writerMapper.toModel(movement, projectId))
			.map(readerMapper::toDto)
			.map { ResponseEntity.created(URI.create("$API_V2/projects/$projectId/movements/${it.id}")).body(it) }
	}

	override fun updateMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		movement: ParticipantMovementWriterDto,
	): Mono<MovementReaderDto> {
		val movementModel = writerMapper.toModel(movement, projectId)
		return service.updateMovementById(currentUser, projectId, id, movementModel).map(readerMapper::toDto)
	}

	override fun createGuestsMovement(
		currentUser: CurrentUserModel,
		projectId: UUID,
		movement: GuestMovementWriterDto,
	): Mono<ResponseEntity<MovementReaderDto>> {
		val movementModel = guestMovementWriterMapper.toModel(movement, projectId)
		val newGuestModels = guestWriterMapper.toModels(movement.guests ?: emptyList(), projectId)
		return service.createMovement(currentUser, movementModel, newGuestModels)
			.map(readerMapper::toDto)
			.map { ResponseEntity.created(URI.create("$API_V2/projects/$projectId/movements/${it.id}")).body(it) }
	}

	override fun updateGuestsMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		movement: GuestMovementWriterDto,
	): Mono<MovementReaderDto> {
		val movementModel = guestMovementWriterMapper.toModel(movement, projectId)
		val newGuestModels = guestWriterMapper.toModels(movement.guests ?: emptyList(), projectId)

		return service.updateMovementById(currentUser, projectId, id, movementModel, newGuestModels)
			.map(readerMapper::toDto)
	}

	override fun disableMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<MovementReaderDto> {
		return service.disableMovementById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun enableMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<MovementReaderDto> {
		return service.enableMovementById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun deleteMovementById(projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteMovementById(projectId, id)
	}
}
