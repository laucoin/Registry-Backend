package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ParticipantQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ElementMovementQueryDto
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.service.support.IConcurrencyService
import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.service.usecase.IExportUseCase
import fr.laucoin.registry.backend.domain.service.usecase.IParticipantUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantExportReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ExportExt.asAttachment
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ExportReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IParticipantV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantTimelineReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.expectedLastEditions
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocationExt.created
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ParticipantTimelineReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ParticipantWriterDtoMapper
import java.util.TimeZone
import fr.laucoin.registry.backend.infrastructure.driving.security.service.ClientContextService
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_RESTORE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_RESTORE

/**
 * Objectif : Adaptateur REST des endpoints du participant.
 * Périmètre : Mappe les paramètres de requête et les DTO, et délègue aux cas d'usage du participant.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class ParticipantV2Controller(
	private val service: IParticipantUseCase,
	private val concurrencyService: IConcurrencyService,
	private val readerMapper: ParticipantReaderDtoMapper,
	private val timelineReaderMapper: ParticipantTimelineReaderDtoMapper,
	private val groupReaderMapper: GroupWithoutMemberReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val partialUserReaderMapper: PartialUserReaderDtoMapper,
	private val writerMapper: ParticipantWriterDtoMapper,
	private val exportUseCase: IExportUseCase,
	private val exportMapper: ExportReaderDtoMapper,
	private val clientContextService: ClientContextService,
): IParticipantV2Controller {
	override fun findParticipants(
		currentUser: CurrentUserModel,
		projectId: UUID,
		search: ParticipantQueryDto,
	): Mono<PageModel<ParticipantReaderDto>> {
		val visibility = currentUser.visibilityFor(search.isVisible, projectId, REGISTRY_PROJECT_PARTICIPANT_RESTORE)
		val pageable = PageableModel(search.page * search.size, search.size)
		val searchParams = ParticipantSearchParamModel(
			search.query,
			search.isMajor,
			search.type,
			visibility,
			search.status,
			search.at,
			hasUser = search.hasUser,
			isOnSite = search.onSite,
			birthdayFrom = search.birthdayFrom,
			birthdayTo = search.birthdayTo,
			sort = search.sort.toSortModel(ParticipantSearchParamModel.SORT_FIELDS),
		)

		return service.findParticipantsPage(projectId, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findBirthdays(
		projectId: UUID
	): Flux<ParticipantReaderDto> {
		return service.findBirthdays(projectId).map(readerMapper::toDto)
	}

	override fun findParticipantById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ParticipantReaderDto> {
		return service.findParticipantById(projectId, id, isVisible = currentUser.visibilityFor(null, projectId, REGISTRY_PROJECT_PARTICIPANT_RESTORE)).map(readerMapper::toDto)
	}

	override fun searchUsers(projectId: UUID, query: String?): Flux<PartialUserReaderDto> {
		return service.searchUsersByText(projectId, query).map(partialUserReaderMapper::toDto)
	}

	override fun searchGroups(
		projectId: UUID,
		query: String?
	): Flux<GroupWithoutMemberReaderDto> {
		return service.searchGroupsByText(projectId, query).map(groupReaderMapper::toDto)
	}

	override fun findParticipantTimeline(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ParticipantTimelineReaderDto> {
		return service.findParticipantTimeline(projectId, id).map(timelineReaderMapper::toDto)
	}

	override fun findParticipantMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		search: ElementMovementQueryDto,
	): Mono<PageModel<MovementReaderDto>> {
		val visibility = currentUser.visibilityFor(search.isVisible, projectId, REGISTRY_PROJECT_MOVEMENT_RESTORE)
		val pageable = PageableModel(search.page * search.size, search.size)
		val searchParams = MovementSearchParamModel(
			visibility,
			search.hasActivity,
			search.type,
			search.from,
			search.to,
			sort = search.sort.toSortModel(MovementSearchParamModel.SORT_FIELDS),
		)

		return service.findParticipantMovementsPage(currentUser, projectId, id, pageable, searchParams)
			.map(movementReaderMapper::toDtoPage)
	}

	override fun createParticipant(
		currentUser: CurrentUserModel,
		projectId: UUID,
		participant: ParticipantWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<ParticipantReaderDto>> {
		val participantModel = writerMapper.toModel(participant, projectId)
		return service.createParticipant(currentUser, participantModel)
			.map(readerMapper::toDto)
			.map { exchange.created(it) }
	}

	override fun updateParticipantById(
		currentUser: CurrentUserModel,
		timeZone: TimeZone,
		projectId: UUID,
		id: UUID,
		participant: ParticipantWriterDto,
		exchange: ServerWebExchange,
	): Mono<ParticipantReaderDto> {
		val participantModel = writerMapper.toModel(participant, projectId)
		return concurrencyService.updateIfUnchanged(
			LockableResourceEnum.PARTICIPANT,
			id,
			exchange.expectedLastEditions(),
			current = { service.findParticipantById(projectId, id, isVisible = null) },
			action = { service.updateParticipantById(currentUser, projectId, id, participantModel) },
		).map(readerMapper::toDto)
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

	override fun exportParticipantById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		request: ServerHttpRequest,
	): Mono<ResponseEntity<ParticipantExportReaderDto>> {
		return exportUseCase.exportParticipant(currentUser, projectId, id, clientContextService.resolve(request))
			.map(exportMapper::toDto)
			.map { it.asAttachment("participant-$id.json") }
	}

	override fun deleteParticipantById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteParticipantById(currentUser, projectId, id)
	}
}
