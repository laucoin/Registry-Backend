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
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IParticipantController
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
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class ParticipantController(
	private val service: IParticipantService,
	private val readerMapper: ParticipantReaderDtoMapper,
	private val groupReaderMapper: GroupWithoutMemberReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val partialUserReaderMapper: PartialUserReaderDtoMapper,
	private val writerMapper: ParticipantWriterDtoMapper,
): IParticipantController {
	override fun findParticipants(
		locale: Locale,
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
		return service.findParticipantsPage(
			projectId,
			PageableModel(pageNumber * pageSize, pageSize),
			ParticipantSearchParamModel(
				textSearched,
				isMajor,
				typeSearched,
				visibilitySearched,
				statusSearched,
				dateTimeSearched
			),
		).map { readerMapper.toDtoPage(it, locale) }
	}

	override fun findBirthdays(
		locale: Locale,
		projectId: UUID
	): Flux<ParticipantReaderDto> {
		return service.findBirthdays(projectId)
			.map { readerMapper.toDto(it, locale) }
	}

	override fun findParticipantById(locale: Locale, projectId: UUID, id: UUID): Mono<ParticipantReaderDto> {
		return service.findParticipantById(projectId, id, visibilitySearched = null)
			.map { readerMapper.toDto(it, locale) }
	}

	override fun searchUsers(locale: Locale, projectId: UUID, textSearched: String?): Flux<PartialUserReaderDto> {
		return service.searchUsersByText(projectId, textSearched)
			.map { partialUserReaderMapper.toDto(it, locale) }
	}

	override fun searchGroups(
		locale: Locale,
		projectId: UUID,
		textSearched: String?
	): Flux<GroupWithoutMemberReaderDto> {
		return service.searchGroupsByText(projectId, textSearched)
			.map { groupReaderMapper.toDto(it, locale) }
	}

	override fun findParticipantMovements(
		currentUser: CurrentUserModel,
		locale: Locale,
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
			throw RegistryException(
				status = FORBIDDEN,
				code = NOT_ENOUGH_PERMISSION,
			)
		}

		return service.findParticipantMovementsPage(
			projectId,
			id,
			PageableModel(pageNumber * pageSize, pageSize),
			MovementSearchParamModel(
				visibilitySearched,
				linkedToActivity,
				typeSearched,
				startDateTimeSearched,
				endDateTimeSearched
			),
		).map { movementReaderMapper.toDtoPage(it, locale) }
	}

	override fun createParticipant(
		currentUser: CurrentUserModel,
		locale: Locale,
		projectId: UUID,
		participant: ParticipantWriterDto
	): Mono<ParticipantReaderDto> {
		return service.createParticipant(currentUser, writerMapper.toModel(participant, projectId))
			.map { readerMapper.toDto(it, locale) }
	}

	override fun updateParticipantById(
		currentUser: CurrentUserModel,
		timeZone: TimeZone,
		locale: Locale,
		projectId: UUID,
		id: UUID,
		participant: ParticipantWriterDto,
	): Mono<ParticipantReaderDto> {
		return service.updateParticipantById(currentUser, projectId, id, writerMapper.toModel(participant, projectId))
			.map { readerMapper.toDto(it, locale) }
	}

	override fun disableParticipantById(
		currentUser: CurrentUserModel,
		locale: Locale,
		projectId: UUID,
		id: UUID,
	): Mono<ParticipantReaderDto> {
		return service.disableParticipantById(currentUser, projectId, id)
			.map { readerMapper.toDto(it, locale) }
	}

	override fun enableParticipantById(
		currentUser: CurrentUserModel,
		locale: Locale,
		projectId: UUID,
		id: UUID,
	): Mono<ParticipantReaderDto> {
		return service.enableParticipantById(currentUser, projectId, id)
			.map { readerMapper.toDto(it, locale) }
	}

	override fun deleteParticipantById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Void> {
		return service.deleteParticipantById(currentUser, projectId, id)
	}
}
