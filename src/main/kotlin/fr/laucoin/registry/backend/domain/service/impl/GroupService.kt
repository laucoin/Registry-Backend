package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_LAST_MEMBERS_CANNOT_BE_REMOVED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_ALREADY_ADDED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_NOT_FOUND_IN_GROUP_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.domain.service.IProjectService
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class GroupService(
	private val projectService: IProjectService,
	private val port: IGroupPort,
	private val participantPort: IParticipantPort,
	@param:Value($$"${registry.feature.group.searched.max-participant-result}")
	private val maxParticipantResult: Int,
): IGroupService, GenericService() {
	override fun findGroupsPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: GroupSearchParamModel,
	): Mono<PageModel<GroupModel>> {
		return port.findPage(projectId, pageable, searchParams)
	}

	override fun findGroupMembersPageByGroupId(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
	): Mono<PageModel<ParticipantModel>> {
		return participantPort.findPageByGroupId(
			projectId,
			id,
			pageable,
			searchParams,
		)
	}

	override fun findGroupById(
		projectId: UUID,
		id: UUID,
		visibilitySearched: Boolean?,
		memberVisibilitySearched: Boolean?,
		memberAvailabilitySearched: Boolean?,
	): Mono<GroupModel> {
		return port.findByIdWithContent(
			projectId,
			id,
			visibilitySearched,
			memberVisibilitySearched,
			memberAvailabilitySearched
		)
			.notFoundIfEmpty(id)
	}

	override fun searchParticipantsByText(projectId: UUID, textSearched: String?): Flux<ParticipantModel> {
		return participantPort.findWithLimit(
			maxParticipantResult,
			projectId,
			ParticipantSearchParamModel(typeSearched = REGISTERED, visibilitySearched = true).apply {
				this.textSearched = textSearched
			},
		)
	}

	override fun createGroup(currentUser: CurrentUserModel, group: GroupModel): Mono<GroupModel> {
		return projectService.validateDateTimes(
			group.project!!.id!!,
			group.startAvailability,
			group.endAvailability,
			GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		)
			.flatMap { validateMembers(group.project!!.id!!, group, group.members.mapNotNull { p -> p.id }) }
			.flatMap { port.create(group.apply { create(currentUser) }) }
	}

	override fun updateGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		group: GroupModel
	): Mono<GroupModel> {
		return projectService.validateDateTimes(
			group.project!!.id!!,
			group.startAvailability,
			group.endAvailability,
			GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		)
			.flatMap {
				findGroupById(
					projectId,
					id,
					visibilitySearched = null,
					memberVisibilitySearched = null,
					memberAvailabilitySearched = null
				)
			}
			.flatMap {
				val newMemberIds: List<UUID> = it.getNewMemberIds(group)
				if (newMemberIds.isEmpty()) Mono.just(it)
				else validateMembers(projectId, it, newMemberIds)
			}
			.map {
				it.apply {
					it.name = group.name
					it.startAvailability = group.startAvailability
					it.endAvailability = group.endAvailability
					it.members = group.members
				}
			}
			.updateGroup(currentUser)
	}

	override fun addMembersToGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		memberIds: List<UUID>
	): Mono<Pair<List<UUID>, List<UUID>>> {
		return findGroupById(
			projectId,
			id,
			visibilitySearched = null,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null
		)
			.map { Pair(it, it.getNewMemberIds(memberIds)) }
			.handle { it, handle ->
				if (it.second.isEmpty()) {
					handle.error(
						RegistryException(
							UNPROCESSABLE_ENTITY,
							GROUP_MEMBERS_ALREADY_ADDED,
						)
					)
				} else {
					handle.next(it)
				}
			}
			.flatMap { (group, newMemberIds) ->
				validateMembers(projectId, group, newMemberIds)
					.map { _ -> newMemberIds.map { ParticipantModel().apply { this.id = it } } }
					.map { group.apply { members = members.plus(it) } }
					.updateGroup(currentUser)
					.map { Pair(newMemberIds, memberIds.minus(newMemberIds.toSet())) }
			}
	}

	override fun removeMemberFromGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		memberId: UUID
	): Mono<GroupModel> {
		return findGroupById(
			projectId,
			id,
			visibilitySearched = null,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null
		)
			.map { it.apply { members = members.filter { m -> m.id != memberId } } }
			.handle { it, handle ->
				if (it.members.isEmpty()) {
					handle.error(
						RegistryException(
							CONFLICT,
							GROUP_LAST_MEMBERS_CANNOT_BE_REMOVED,
						)
					)
				} else {
					handle.next(it)
				}
			}
			.updateGroup(currentUser)
	}

	private fun Mono<GroupModel>.updateGroup(currentUser: CurrentUserModel) = flatMap {
		port.update(it.apply { update(currentUser) })
	}

	private fun validateMembers(projectId: UUID, group: GroupModel, newMemberIds: List<UUID>): Mono<GroupModel> {
		return participantPort.findAllByIds(projectId, newMemberIds, visibilitySearched = null)
			.filter { it.type == REGISTERED }
			.collectList()
			.handle { it, handle ->
				when {
					it.size != newMemberIds.size -> handle.error(
						RegistryException(
							NOT_FOUND,
							GROUP_MEMBERS_NOT_FOUND_IN_GROUP_PROJECT,
						)
					)

					it.any(ParticipantModel::isNotUsable) -> handle.error(
						RegistryException(
							NOT_FOUND,
							GROUP_MEMBERS_NOT_VISIBLE,
						)
					)

					else -> handle.next(group)
				}
			}
	}

	override fun disableGroupById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<GroupModel> {
		return findGroupById(
			projectId,
			id,
			visibilitySearched = true,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null
		)
			.updateVisibility(visibility = false)
			.updateGroup(currentUser)
	}

	override fun enableGroupById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<GroupModel> {
		return findGroupById(
			projectId,
			id,
			visibilitySearched = false,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null
		)
			.updateVisibility(visibility = true)
			.updateGroup(currentUser)
	}

	override fun deleteGroupById(projectId: UUID, id: UUID): Mono<Unit> {
		return findGroupById(
			projectId,
			id,
			visibilitySearched = null,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null
		)
			.flatMap { port.deleteById(id) }
	}

	override fun purgeEmptyGroups(
		participantToExclude: List<UUID>,
		dryRun: Boolean
	): Flux<UUID> {
		log.info("Purging empty groups")
		return port.findEmpty(participantToExclude)
			.flatMap {
				if (dryRun) {
					log.info("[Dry run] group {} would be deleted", it)
					Mono.just(it)
				} else {
					log.info("Purging group {}", it)
					port.deleteById(it).thenReturn(it)
						.doOnNext { e -> log.info("Group {} was deleted", e) }
						.doOnError { err -> log.error("Failed to purge group{}", it, err) }
				}
			}
	}
}
