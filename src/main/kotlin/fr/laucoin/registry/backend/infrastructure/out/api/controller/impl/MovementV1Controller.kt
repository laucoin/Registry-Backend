package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
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
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IMovementV1Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementActivityReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementContentReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementParticipantsAndGroupsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.GuestMovementWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.GuestWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.ParticipantMovementWriterDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class MovementV1Controller(
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
): IMovementV1Controller {
	private val similarity: JaroWinklerSimilarity = JaroWinklerSimilarity()

	override fun findMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageNumber: Int,
		pageSize: Int,
		currentMovements: Boolean,
		linkedToActivity: Boolean?,
		visibilitySearched: Boolean?,
		typeSearched: MovementTypeEnum?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?
	): Mono<PageModel<MovementReaderDto>> {
		if (!currentUser.hasAuthority(projectId, REGISTRY_PROJECT_OPTION_ACTIVITY) && linkedToActivity == true) {
			throw RegistryException(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION)
		}

		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = MovementSearchParamModel(
			visibilitySearched,
			linkedToActivity,
			typeSearched,
			startDateTimeSearched,
			endDateTimeSearched
		)

		val movements = if (currentMovements) {
			service.findCurrentMovementsPage(projectId, pageable, searchParams)
		} else {
			service.findMovementsPage(projectId, pageable, searchParams)
		}

		return movements.map(readerMapper::toDtoPage)
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
		typeSearched: MovementTypeEnum,
		contentTypeSearched: ParticipantTypeEnum,
		textSearched: String?,
	): Flux<MovementReasonsReaderDto> {
		return service.searchActivitiesByText(projectId, contentTypeSearched, textSearched)
			.map(activityReasonReaderMapper::toDto)
			.mergeWith(searchReasons(textSearched, typeSearched, contentTypeSearched))
	}

	private fun searchReasons(
		textSearched: String?,
		typeSearched: MovementTypeEnum,
		contentTypeSearched: ParticipantTypeEnum,
	): Flux<MovementReasonsReaderDto> {
		return service.searchReasonsByText(contentTypeSearched, typeSearched)
			.map(reasonReaderMapper::toDto)
			.map { Pair(it, similarity.apply(it.label, textSearched ?: it.label)) }
			.filter { it.second > 0 }
			.map(Pair<MovementReasonsReaderDto, Double>::first)
	}

	override fun searchParticipantsAndGroups(
		projectId: UUID,
		contentTypeSearched: ParticipantTypeEnum,
		textSearched: String?
	): Mono<MovementParticipantsAndGroupsReaderDto> {
		return service.searchParticipantsAndGroupsByText(projectId, contentTypeSearched, textSearched)
			.map { Pair(it.t1, it.t2) }
			.map(movementParticipantsAndGroupsMapper::toDto)
	}

	override fun searchVehicles(projectId: UUID, textSearched: String?): Flux<VehicleReaderDto> {
		return service.searchVehiclesByText(projectId, textSearched).map(vehiclesMapper::toDto)
	}

	override fun findMovementCommunications(
		projectId: UUID,
		id: UUID,
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		visibilitySearched: Boolean?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?
	): Mono<PageModel<CommunicationReaderDto>> {
		val pageable = PageableModel(pageNumber, pageSize)
		val searchParams = CommunicationSearchParamModel(
			textSearched, visibilitySearched, startDateTimeSearched, endDateTimeSearched
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
	): Mono<MovementReaderDto> {
		return service.createMovement(currentUser, writerMapper.toModel(movement, projectId)).map(readerMapper::toDto)
	}

	override fun updateMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		movement: ParticipantMovementWriterDto
	): Mono<MovementReaderDto> {
		val movementModel = writerMapper.toModel(movement, projectId)
		return service.updateMovementById(currentUser, projectId, id, movementModel).map(readerMapper::toDto)
	}

	override fun createGuestsMovement(
		currentUser: CurrentUserModel,
		projectId: UUID,
		movement: GuestMovementWriterDto
	): Mono<MovementReaderDto> {
		val movementModel = guestMovementWriterMapper.toModel(movement, projectId)
		val newGuestModels = guestWriterMapper.toModels(movement.guests ?: emptyList(), projectId)
		return service.createMovement(currentUser, movementModel, newGuestModels).map(readerMapper::toDto)
	}

	override fun updateGuestsMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		movement: GuestMovementWriterDto
	): Mono<MovementReaderDto> {
		val movementModel = guestMovementWriterMapper.toModel(movement, projectId)
		val newGuestModels = guestWriterMapper.toModels(movement.guests ?: emptyList(), projectId)

		return service.updateMovementById(currentUser, projectId, id, movementModel, newGuestModels)
			.map(readerMapper::toDto)
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
