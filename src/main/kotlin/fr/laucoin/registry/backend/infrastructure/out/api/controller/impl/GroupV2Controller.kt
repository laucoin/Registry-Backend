package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.GroupSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IGroupV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GroupMembersWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GroupWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.SortParamDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AddedGroupMembersReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.GroupReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.GroupWriterDtoMapper
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@RestController
class GroupV2Controller(
	private val service: IGroupService,
	private val readerMapper: GroupReaderDtoMapper,
	private val readerLightMapper: GroupWithoutMemberReaderDtoMapper,
	private val participantReaderMapper: ParticipantReaderDtoMapper,
	private val addedGroupMembersReaderMapper: AddedGroupMembersReaderDtoMapper,
	private val writerMapper: GroupWriterDtoMapper,
) : IGroupV2Controller {
	override fun findGroups(
		projectId: UUID,
		page: Int,
		size: Int,
		sort: List<String>?,
		direction: String,
		q: String?,
		visible: Boolean?,
		presence: Boolean?,
		dateTime: ZonedDateTime?,
	): Mono<PageModel<GroupWithoutMemberReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = GroupSearchParamModel(q, visible, presence, dateTime)
		val sortModels = SortParamDtoMapper.toSortModels(sort, direction, GroupSortFieldEnum::fromParamName)

		return service.findGroupsPage(projectId, pageable, searchParams, sortModels)
			.map(readerLightMapper::toDtoPage)
	}

	override fun findGroupById(projectId: UUID, id: UUID): Mono<GroupReaderDto> {
		return service.findGroupById(
			projectId,
			id,
			visibilitySearched = null,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null,
		).map(readerMapper::toDto)
	}

	override fun findGroupMembersByGroupId(
		projectId: UUID,
		id: UUID,
		page: Int,
		size: Int,
		q: String?,
		isMajor: Boolean?,
		type: ParticipantTypeEnum?,
		visible: Boolean?,
		status: PresenceStatusEnum?,
		dateTime: ZonedDateTime?,
	): Mono<PageModel<ParticipantReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = ParticipantSearchParamModel(q, isMajor, type, visible, status, dateTime)

		return service.findGroupMembersPageByGroupId(projectId, id, pageable, searchParams)
			.map(participantReaderMapper::toDtoPage)
	}

	override fun findAssignableParticipants(projectId: UUID, q: String?): Flux<ParticipantReaderDto> {
		return service.searchParticipantsByText(projectId, q).map(participantReaderMapper::toDto)
	}

	override fun createGroup(
		currentUser: CurrentUserModel,
		projectId: UUID,
		group: GroupWriterDto,
	): Mono<GroupWithoutMemberReaderDto> {
		val groupModel = writerMapper.toModel(group, projectId)
		return service.createGroup(currentUser, groupModel).map(readerLightMapper::toDto)
	}

	override fun updateGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		group: GroupWriterDto,
	): Mono<GroupWithoutMemberReaderDto> {
		val groupModel = writerMapper.toModel(group, projectId)
		return service.updateGroupById(currentUser, projectId, id, groupModel).map(readerLightMapper::toDto)
	}

	override fun addMembersToGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		members: GroupMembersWriterDto,
	): Mono<ResponseEntity<AddedGroupMembersReaderDto>> {
		return service.addMembersToGroupById(currentUser, projectId, id, members.participantIds.orEmpty())
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
		memberId: UUID,
	): Mono<GroupWithoutMemberReaderDto> {
		return service.removeMemberFromGroupById(currentUser, projectId, id, memberId).map(readerLightMapper::toDto)
	}

	override fun disableGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<GroupWithoutMemberReaderDto> {
		return service.disableGroupById(currentUser, projectId, id).map(readerLightMapper::toDto)
	}

	override fun enableGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<GroupWithoutMemberReaderDto> {
		return service.enableGroupById(currentUser, projectId, id).map(readerLightMapper::toDto)
	}

	override fun deleteGroupById(projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteGroupById(projectId, id)
	}
}
