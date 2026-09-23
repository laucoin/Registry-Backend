package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IParticipantV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantDataExportReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.PageQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.SortParamDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PageReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ParticipantDataExportReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ParticipantWriterDtoMapper
import java.net.URI
import java.time.ZonedDateTime
import java.util.TimeZone
import java.util.UUID
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class ParticipantV2Controller(
	private val service: IParticipantService,
	private val readerMapper: ParticipantReaderDtoMapper,
	private val partialUserReaderMapper: PartialUserReaderDtoMapper,
	private val groupReaderMapper: GroupWithoutMemberReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val dataExportReaderMapper: ParticipantDataExportReaderDtoMapper,
	private val writerMapper: ParticipantWriterDtoMapper,
	private val pageQueryMapper: PageQueryDtoMapper,
	private val sortParamMapper: SortParamDtoMapper,
	private val pageReaderMapper: PageReaderDtoMapper,
): IParticipantV2Controller {
	override fun findParticipants(
		projectId: UUID,
		page: SortedPageQueryDto,
		q: String?,
		isMajor: Boolean?,
		type: ParticipantTypeEnum?,
		visible: Boolean?,
		status: PresenceStatusEnum?,
		dateTime: ZonedDateTime?,
	): Mono<PageReaderDto<ParticipantReaderDto>> {
		val pageable = pageQueryMapper.toPageable(page)
		val sortFields = sortParamMapper.toSortModels(page.sort, page.direction) { key ->
			ParticipantSortFieldEnum.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
		}
		val searchParams = ParticipantSearchParamModel(q, isMajor, type, visible, status, dateTime)

		return service.findParticipantsPage(projectId, pageable, searchParams, sortFields)
			.map { pageReaderMapper.toDto(it, readerMapper::toDto) }
	}

	override fun findBirthdays(projectId: UUID, limit: Int): Flux<ParticipantReaderDto> {
		return service.findBirthdays(projectId, limit).map(readerMapper::toDto)
	}

	override fun findArrivingToday(projectId: UUID, limit: Int): Flux<ParticipantReaderDto> {
		return service.findArrivingToday(projectId, limit).map(readerMapper::toDto)
	}

	override fun findDepartingToday(projectId: UUID, limit: Int): Flux<ParticipantReaderDto> {
		return service.findDepartingToday(projectId, limit).map(readerMapper::toDto)
	}

	override fun findParticipantById(projectId: UUID, id: UUID): Mono<ParticipantReaderDto> {
		return service.findParticipantById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun searchUsers(projectId: UUID, q: String?): Flux<PartialUserReaderDto> {
		return service.searchUsersByText(projectId, q).map(partialUserReaderMapper::toDto)
	}

	override fun searchGroups(projectId: UUID, q: String?): Flux<GroupWithoutMemberReaderDto> {
		return service.searchGroupsByText(projectId, q).map(groupReaderMapper::toDto)
	}

	override fun findParticipantMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		page: PageQueryDto,
		visible: Boolean?,
		linkedToActivity: Boolean?,
		type: MovementTypeEnum?,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Mono<PageReaderDto<MovementReaderDto>> {
		if (!currentUser.hasAuthority(projectId, REGISTRY_PROJECT_OPTION_ACTIVITY) && linkedToActivity == true) {
			throw RegistryException(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION)
		}

		val pageable = pageQueryMapper.toPageable(page)
		val searchParams = MovementSearchParamModel(visible, linkedToActivity, type, startDateTime, endDateTime)

		return service.findParticipantMovementsPage(projectId, id, pageable, searchParams)
			.map { pageReaderMapper.toDto(it, movementReaderMapper::toDto) }
	}

	override fun exportParticipantDataById(projectId: UUID, id: UUID): Mono<ParticipantDataExportReaderDto> {
		return service.exportParticipantData(projectId, id).map(dataExportReaderMapper::toDto)
	}

	override fun createParticipant(
		currentUser: CurrentUserModel,
		projectId: UUID,
		participant: ParticipantWriterDto,
	): Mono<ResponseEntity<ParticipantReaderDto>> {
		val participantModel = writerMapper.toModel(participant, projectId)
		return service.createParticipant(currentUser, participantModel).map(readerMapper::toDto).map {
			ResponseEntity.created(URI.create("$API_V2/projects/$projectId/participants/${it.id}")).body(it)
		}
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
