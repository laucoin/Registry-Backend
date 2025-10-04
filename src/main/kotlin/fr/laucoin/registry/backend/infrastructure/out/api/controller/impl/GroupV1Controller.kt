package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IGroupV1Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GroupWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AddedGroupMembersReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.GroupReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.GroupWriterDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class GroupV1Controller(
	private val service: IGroupService,
	private val readerMapper: GroupReaderDtoMapper,
	private val readerLightMapper: GroupWithoutMemberReaderDtoMapper,
	private val participantReaderMapper: ParticipantReaderDtoMapper,
	private val addedGroupMembersReaderMapper: AddedGroupMembersReaderDtoMapper,
	private val writerMapper: GroupWriterDtoMapper,
): IGroupV1Controller {
	override fun findGroups(
		projectId: UUID,
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		visibilitySearched: Boolean?,
		presenceSearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<GroupWithoutMemberReaderDto>> {
		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = GroupSearchParamModel(
			textSearched, visibilitySearched, presenceSearched, dateTimeSearched
		)

		return service.findGroupsPage(projectId, pageable, searchParams).map(readerLightMapper::toDtoPage)
	}

	override fun findGroupMembersByGroupId(
		projectId: UUID,
		id: UUID,
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
			textSearched, isMajor, typeSearched, visibilitySearched, statusSearched, dateTimeSearched
		)

		return service.findGroupMembersPageByGroupId(projectId, id, pageable, searchParams)
			.map(participantReaderMapper::toDtoPage)
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

	override fun searchParticipants(projectId: UUID, textSearched: String?): Flux<ParticipantReaderDto> {
		return service.searchParticipantsByText(projectId, textSearched).map(participantReaderMapper::toDto)
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
	): Mono<GroupWithoutMemberReaderDto> {
		return service.removeMemberFromGroupById(currentUser, projectId, id, memberId).map(readerLightMapper::toDto)
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
