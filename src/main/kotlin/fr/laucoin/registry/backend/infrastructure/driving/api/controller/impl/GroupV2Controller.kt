package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.service.IConcurrencyService
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IGroupV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GroupWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.expectedLastEditions
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocationExt.created
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AddedGroupMembersReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.GroupReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.GroupWriterDtoMapper
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_D

/**
 * Objectif : Adaptateur REST des endpoints du groupe.
 * Périmètre : Mappe les paramètres de requête et les DTO, et délègue aux cas d'usage du groupe.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class GroupV2Controller(
	private val service: IGroupService,
	private val concurrencyService: IConcurrencyService,
	private val readerMapper: GroupReaderDtoMapper,
	private val readerLightMapper: GroupWithoutMemberReaderDtoMapper,
	private val participantReaderMapper: ParticipantReaderDtoMapper,
	private val addedGroupMembersReaderMapper: AddedGroupMembersReaderDtoMapper,
	private val writerMapper: GroupWriterDtoMapper,
): IGroupV2Controller {
	override fun findGroups(
		currentUser: CurrentUserModel,
		projectId: UUID,
		page: Long,
		size: Int,
		query: String?,
		isVisible: Boolean?,
		isPresent: Boolean?,
		at: ZonedDateTime?,
		mine: Boolean?,
		hasOnSiteMembers: Boolean?,
		sort: String?,
	): Mono<PageModel<GroupWithoutMemberReaderDto>> {
		val visibility = currentUser.visibilityFor(isVisible, projectId, REGISTRY_PROJECT_GROUP_D)
		val pageable = PageableModel(page * size, size)
		val searchParams = GroupSearchParamModel(
			query, visibility, isPresent, at,
			memberUserId = currentUser.id.takeIf { mine == true },
			hasOnSiteMembers = hasOnSiteMembers,
			sort = sort.toSortModel(GroupSearchParamModel.SORT_FIELDS),
		)

		return service.findGroupsPage(projectId, pageable, searchParams).map(readerLightMapper::toDtoPage)
	}

	override fun findGroupMembersByGroupId(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		page: Long,
		size: Int,
		query: String?,
		isMajor: Boolean?,
		type: ParticipantTypeEnum?,
		isVisible: Boolean?,
		status: PresenceStatusEnum?,
		at: ZonedDateTime?,
		hasUser: Boolean?,
		onSite: Boolean?,
		birthdayFrom: LocalDate?,
		birthdayTo: LocalDate?,
		sort: String?,
	): Mono<PageModel<ParticipantReaderDto>> {
		val visibility = currentUser.visibilityFor(isVisible, projectId, REGISTRY_PROJECT_PARTICIPANT_D)
		val pageable = PageableModel(page * size, size)
		val searchParams = ParticipantSearchParamModel(
			query, isMajor, type, visibility, status, at,
			hasUser = hasUser,
			isOnSite = onSite,
			birthdayFrom = birthdayFrom,
			birthdayTo = birthdayTo,
			sort = sort.toSortModel(ParticipantSearchParamModel.SORT_FIELDS),
		)

		return service.findGroupMembersPageByGroupId(projectId, id, pageable, searchParams)
			.map(participantReaderMapper::toDtoPage)
	}

	override fun findGroupById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<GroupReaderDto> {
		return service.findGroupById(
			projectId,
			id,
			isVisible = currentUser.visibilityFor(null, projectId, REGISTRY_PROJECT_GROUP_D),
			memberIsVisible = null,
			memberIsAvailable = null,
		).map(readerMapper::toDto)
	}

	override fun searchParticipants(projectId: UUID, query: String?): Flux<ParticipantReaderDto> {
		return service.searchParticipantsByText(projectId, query).map(participantReaderMapper::toDto)
	}

	override fun createGroup(
		currentUser: CurrentUserModel,
		projectId: UUID,
		group: GroupWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<GroupWithoutMemberReaderDto>> {
		val groupModel = writerMapper.toModel(group, projectId)
		return service.createGroup(currentUser, groupModel)
			.map(readerLightMapper::toDto)
			.map { exchange.created(it) }
	}

	override fun updateGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		group: GroupWriterDto,
		exchange: ServerWebExchange,
	): Mono<GroupWithoutMemberReaderDto> {
		val groupModel = writerMapper.toModel(group, projectId)
		return concurrencyService.updateIfUnchanged(
			LockableResourceEnum.GROUP,
			id,
			exchange.expectedLastEditions(),
			current = { service.findGroupSummaryById(projectId, id) },
			action = { service.updateGroupById(currentUser, projectId, id, groupModel) },
		).map(readerLightMapper::toDto)
	}

	override fun addMembersToGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		memberIds: List<UUID>,
	): Mono<ResponseEntity<AddedGroupMembersReaderDto>> {
		return service.addMembersToGroupById(currentUser, projectId, id, memberIds)
			.map(addedGroupMembersReaderMapper::toDto)
			.map {
				val status = if (it.notAddedMemberIds.isEmpty()) OK else MULTI_STATUS
				ResponseEntity.status(status).body(it)
			}
	}

	override fun removeMemberFromGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		memberId: UUID
	): Mono<Unit> {
		return service.removeMemberFromGroupById(currentUser, projectId, id, memberId).thenReturn(Unit)
	}

	override fun disableGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<GroupWithoutMemberReaderDto> {
		return service.disableGroupById(currentUser, projectId, id).map(readerLightMapper::toDto)
	}

	override fun enableGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<GroupWithoutMemberReaderDto> {
		return service.enableGroupById(currentUser, projectId, id).map(readerLightMapper::toDto)
	}

	override fun deleteGroupById(projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteGroupById(projectId, id)
	}
}
