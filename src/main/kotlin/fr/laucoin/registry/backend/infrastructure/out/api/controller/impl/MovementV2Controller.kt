package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.MovementSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IMovementV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementContentsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.SortParamDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementActivityReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementContentsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementParticipantsAndGroupsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.GuestMovementWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.GuestWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.ParticipantMovementWriterDtoMapper
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@RestController
class MovementV2Controller(
	private val service: IMovementService,
	private val readerMapper: MovementReaderDtoMapper,
	private val readerContentsMapper: MovementContentsReaderDtoMapper,
	private val reasonReaderMapper: MovementReasonReaderDtoMapper,
	private val activityReasonReaderMapper: MovementActivityReasonReaderDtoMapper,
	private val communicationReaderMapper: CommunicationReaderDtoMapper,
	private val movementParticipantsAndGroupsMapper: MovementParticipantsAndGroupsReaderDtoMapper,
	private val vehiclesMapper: VehicleReaderDtoMapper,
	private val writerMapper: ParticipantMovementWriterDtoMapper,
	private val guestMovementWriterMapper: GuestMovementWriterDtoMapper,
	private val guestWriterMapper: GuestWriterDtoMapper,
) : IMovementV2Controller {
	private val similarity: JaroWinklerSimilarity = JaroWinklerSimilarity()

	override fun findMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		page: Int,
		size: Int,
		sort: List<String>?,
		direction: String,
		currentMovements: Boolean,
		linkedToActivity: Boolean?,
		visible: Boolean?,
		type: MovementTypeEnum?,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Mono<PageModel<MovementReaderDto>> {
		if (!currentUser.hasAuthority(projectId, REGISTRY_PROJECT_OPTION_ACTIVITY) && linkedToActivity == true) {
			throw RegistryException(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION)
		}

		val pageable = PageableModel(page * size, size)
		val searchParams = MovementSearchParamModel(visible, linkedToActivity, type, startDateTime, endDateTime)
		val sortModels = SortParamDtoMapper.toSortModels(sort, direction, MovementSortFieldEnum::fromParamName)

		val movements = if (currentMovements) {
			service.findCurrentMovementsPage(projectId, pageable, searchParams, sortModels)
		} else {
			service.findMovementsPage(projectId, pageable, searchParams, sortModels)
		}

		return movements.map(readerMapper::toDtoPage)
	}

	override fun findMovementsContents(
		projectId: UUID,
		movementIds: List<UUID>,
		currentMovements: Boolean,
	): Flux<MovementContentsReaderDto> {
		val contents = if (currentMovements) {
			service.findCurrentMovementsContent(projectId, movementIds)
		} else {
			service.findMovementsContent(projectId, movementIds)
		}

		return contents.map(readerContentsMapper::toDto)
	}

	override fun findMovementById(projectId: UUID, id: UUID): Mono<MovementReaderDto> {
		return service.findMovementById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun findReasons(
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

	override fun findEligibleParticipantsAndGroups(
		projectId: UUID,
		contentType: ParticipantTypeEnum,
		q: String?,
	): Mono<MovementParticipantsAndGroupsReaderDto> {
		return service.searchParticipantsAndGroupsByText(projectId, contentType, q)
			.map { Pair(it.t1, it.t2) }
			.map(movementParticipantsAndGroupsMapper::toDto)
	}

	override fun findEligibleVehicles(projectId: UUID, q: String?): Flux<VehicleReaderDto> {
		return service.searchVehiclesByText(projectId, q).map(vehiclesMapper::toDto)
	}

	override fun findMovementCommunications(
		projectId: UUID,
		id: UUID,
		page: Int,
		size: Int,
		q: String?,
		visible: Boolean?,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Mono<PageModel<CommunicationReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = CommunicationSearchParamModel(q, visible, startDateTime, endDateTime)

		return service.findMovementCommunicationsPage(projectId, id, pageable, searchParams)
			.map(communicationReaderMapper::toDtoPage)
	}

	override fun findParticipantsStatus(projectId: UUID): Mono<ProjectStatusModel> {
		return service.findParticipantsStatus(projectId)
	}

	override fun findVehiclesStatus(projectId: UUID): Mono<VehicleStatusModel> {
		return service.findVehiclesStatus(projectId)
	}

	override fun findOngoingActivities(projectId: UUID, limit: Int): Flux<MovementReaderDto> {
		return service.findOngoingActivities(projectId, limit).map(readerMapper::toDto)
	}

	override fun createMovement(
		currentUser: CurrentUserModel,
		projectId: UUID,
		movement: ParticipantMovementWriterDto,
	): Mono<MovementReaderDto> {
		return service.createMovement(currentUser, writerMapper.toModel(movement, projectId)).map(readerMapper::toDto)
	}

	override fun createGuestsMovement(
		currentUser: CurrentUserModel,
		projectId: UUID,
		movement: GuestMovementWriterDto,
	): Mono<MovementReaderDto> {
		val movementModel = guestMovementWriterMapper.toModel(movement, projectId)
		val newGuestModels = guestWriterMapper.toModels(movement.guests ?: emptyList(), projectId)
		return service.createMovement(currentUser, movementModel, newGuestModels).map(readerMapper::toDto)
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
