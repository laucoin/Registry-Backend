package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.UsableElementStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IGroupController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.GroupWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.AddedGroupMembersReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.GroupReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.GroupWriterDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class GroupController(
    private val service: IGroupService,
    private val readerMapper: GroupReaderDtoMapper,
    private val participantReaderMapper: ParticipantReaderDtoMapper,
    private val addedGroupMembersReaderMapper: AddedGroupMembersReaderDtoMapper,
    private val writerMapper: GroupWriterDtoMapper,
): IGroupController {
    override fun findGroups(
        locale: Locale,
        projectId: UUID,
        pageNumber: Int,
        pageSize: Int,
        textSearched: String?,
        visibilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<GroupReaderDto>> {
        return service.findGroupsPage(
            projectId,
            PageableModel(pageNumber * pageSize, pageSize),
            GroupSearchParamModel(textSearched, visibilitySearched, presenceSearched, dateTimeSearched),
        ).map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findGroupMembers(
        locale: Locale,
        projectId: UUID,
        groupIds: List<UUID>
    ): Flux<Pair<UUID, List<ParticipantReaderDto>>> {
        return service.findGroupsMembers(projectId, groupIds)
            .map { Pair(it.first, it.second.map { members -> participantReaderMapper.toDto(members, locale) }) }
    }

    override fun findGroupMembersByGroupId(
        locale: Locale,
        projectId: UUID,
        id: UUID,
        pageNumber: Int,
        pageSize: Int,
        textSearched: String?,
        typeSearched: ParticipantTypeEnum?,
        visibilitySearched: Boolean?,
        statusSearched: UsableElementStatusEnum?,
        dateTimeSearched: ZonedDateTime?
    ): Mono<PageModel<ParticipantReaderDto>> {
        return service.findGroupMembersPageByGroupId(
            projectId,
            id,
            PageableModel(pageNumber * pageSize, pageSize),
            ParticipantSearchParamModel(textSearched, typeSearched, visibilitySearched, statusSearched, dateTimeSearched),
        ).map { participantReaderMapper.toDtoPage(it, locale) }
    }

    override fun findGroupById(locale: Locale, projectId: UUID, id: UUID): Mono<GroupReaderDto> {
        return service.findGroupById(projectId, id, visibilitySearched = null)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun searchParticipants(locale: Locale, projectId: UUID, textSearched: String?): Flux<ParticipantReaderDto> {
        return service.searchParticipants(projectId, textSearched)
            .map { participantReaderMapper.toDto(it, locale) }
    }

    override fun createGroup(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        group: GroupWriterDto,
    ): Mono<GroupReaderDto> {
        return service.createGroup(currentUser, writerMapper.toModel(group, projectId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateGroupById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
        group: GroupWriterDto,
    ): Mono<GroupReaderDto> {
        return service.updateGroupById(currentUser, projectId, id, writerMapper.toModel(group, projectId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun addMembersToGroupById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
        memberIds: List<UUID>,
    ): Mono<ResponseEntity<AddedGroupMembersReaderDto>> {
        return service.addMembersToGroupById(currentUser, projectId, id, memberIds)
            .map { addedGroupMembersReaderMapper.toDto(it, locale) }
            .map { ResponseEntity.status(if (it.notAddedMemberIds.isEmpty()) OK else MULTI_STATUS).body(it) }
    }

    override fun removeMemberFromGroupById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
        memberId: UUID
    ): Mono<GroupReaderDto> {
        return service.removeMemberFromGroupById(currentUser, projectId, id, memberId)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun disableGroupById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID
    ): Mono<GroupReaderDto> {
        return service.disableGroupById(currentUser, projectId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun enableGroupById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID
    ): Mono<GroupReaderDto> {
        return service.enableGroupById(currentUser, projectId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteGroupById(projectId: UUID, id: UUID): Mono<Void> {
        return service.deleteGroupById(projectId, id)
    }
}
