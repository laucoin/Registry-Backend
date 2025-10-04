package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IParticipantV1Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.ParticipantWriterDtoMapper
import java.time.ZonedDateTime
import java.util.TimeZone
import java.util.UUID
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class ParticipantV1Controller(
	private val service: IParticipantService,
	private val readerMapper: ParticipantReaderDtoMapper,
	private val groupReaderMapper: GroupWithoutMemberReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val partialUserReaderMapper: PartialUserReaderDtoMapper,
	private val writerMapper: ParticipantWriterDtoMapper,
): IParticipantV1Controller {
	override fun findParticipants(
		projectId: UUID,
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		isMajor: Boolean?,
		typeSearched: ParticipantTypeEnum?,
		visibilitySearched: Boolean?,
		statusSearched: PresenceStatusEnum?,
		dateTimeSearched: ZonedDateTime?
	): Mono<PageModel<ParticipantReaderDto>> {
		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = ParticipantSearchParamModel(
			textSearched,
			isMajor,
			typeSearched,
			visibilitySearched,
			statusSearched,
			dateTimeSearched
		)

		return service.findParticipantsPage(projectId, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findBirthdays(
		projectId: UUID
	): Flux<ParticipantReaderDto> {
		return service.findBirthdays(projectId).map(readerMapper::toDto)
	}

	override fun findParticipantById(projectId: UUID, id: UUID): Mono<ParticipantReaderDto> {
		return service.findParticipantById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun searchUsers(projectId: UUID, textSearched: String?): Flux<PartialUserReaderDto> {
		return service.searchUsersByText(projectId, textSearched).map(partialUserReaderMapper::toDto)
	}

	override fun searchGroups(
		projectId: UUID,
		textSearched: String?
	): Flux<GroupWithoutMemberReaderDto> {
		return service.searchGroupsByText(projectId, textSearched).map(groupReaderMapper::toDto)
	}

	override fun findParticipantMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		pageNumber: Int,
		pageSize: Int,
		visibilitySearched: Boolean?,
		linkedToActivity: Boolean?,
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

		return service.findParticipantMovementsPage(projectId, id, pageable, searchParams)
			.map(movementReaderMapper::toDtoPage)
	}

	override fun createParticipant(
		currentUser: CurrentUserModel,
		projectId: UUID,
		participant: ParticipantWriterDto
	): Mono<ParticipantReaderDto> {
		val participantModel = writerMapper.toModel(participant, projectId)
		return service.createParticipant(currentUser, participantModel).map(readerMapper::toDto)
	}

	override fun updateParticipantById(
		currentUser: CurrentUserModel,
		timeZone: TimeZone,
		projectId: UUID,
		id: UUID,
		participant: ParticipantWriterDto,
	): Mono<ParticipantReaderDto> {
		val participantModel = writerMapper.toModel(participant, projectId)
		return service.updateParticipantById(currentUser, projectId, id, participantModel).map(readerMapper::toDto)
	}

	override fun disableParticipantById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ParticipantReaderDto> {
		return service.disableParticipantById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun enableParticipantById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ParticipantReaderDto> {
		return service.enableParticipantById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun deleteParticipantById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteParticipantById(currentUser, projectId, id)
	}
}
