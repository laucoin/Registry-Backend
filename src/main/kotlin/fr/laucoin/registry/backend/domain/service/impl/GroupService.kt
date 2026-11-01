package fr.laucoin.registry.backend.domain.service.impl

import java.time.Clock
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
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Gère les groupes d'un projet et leur composition.
 * Périmètre : CRUD des groupes, ajout/retrait de membres et règles de sécurité du dernier membre.
 * Limites : Ne déplace pas les groupes ; les mouvements relèvent de MovementService.
 */
@Service
class GroupService(
	private val projectService: IProjectService,
	private val port: IGroupPort,
	private val participantPort: IParticipantPort,
	private val transactionalOperator: TransactionalOperator,
	clock: Clock,
	@param:Value($$"${registry.feature.group.searched.max-participant-result}")
	private val maxParticipantResult: Int,
): IGroupService, GenericService(clock) {
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
		isVisible: Boolean?,
		memberIsVisible: Boolean?,
		memberIsAvailable: Boolean?,
	): Mono<GroupModel> {
		return port.findByIdWithContent(
			projectId,
			id,
			isVisible,
			memberIsVisible,
			memberIsAvailable
		)
			.notFoundIfEmpty(id)
	}

	override fun findGroupSummaryById(projectId: UUID, id: UUID): Mono<GroupModel> {
		return port.findSummaryById(projectId, id).notFoundIfEmpty(id)
	}

	override fun searchParticipantsByText(projectId: UUID, query: String?): Flux<ParticipantModel> {
		return participantPort.findWithLimit(
			maxParticipantResult,
			projectId,
			ParticipantSearchParamModel(type = REGISTERED, isVisible = true).apply {
				this.query = query
			},
		)
	}

	override fun createGroup(currentUser: CurrentUserModel, group: GroupModel): Mono<GroupModel> {
		return projectService.validateDateTimes(
			group.projectId!!,
			group.startAvailability,
			group.endAvailability,
			GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		)
			.flatMap { validateMembers(group.projectId!!, group, group.members.mapNotNull { p -> p.id }) }
			.flatMap { port.create(group.apply { create(currentUser, now()) }) }
	}

	override fun updateGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		group: GroupModel
	): Mono<GroupModel> {
		return projectService.validateDateTimes(
			group.projectId!!,
			group.startAvailability,
			group.endAvailability,
			GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		)
			.flatMap { findLockedGroup(projectId, id) }
			.flatMap { validateNewMembers(projectId, it, group) }
			.map { applyUpdate(it, group) }
			.updateGroup(currentUser)
			.`as`(transactionalOperator::transactional)
	}

	private fun validateNewMembers(projectId: UUID, existing: GroupModel, group: GroupModel): Mono<GroupModel> {
		val newMemberIds: List<UUID> = existing.getNewMemberIds(group)
		return if (newMemberIds.isEmpty()) Mono.just(existing)
		else validateMembers(projectId, existing, newMemberIds)
	}

	private fun applyUpdate(existing: GroupModel, group: GroupModel): GroupModel {
		return existing.apply {
			name = group.name
			startAvailability = group.startAvailability
			endAvailability = group.endAvailability
			members = group.members
		}
	}

	private fun findLockedGroup(projectId: UUID, id: UUID): Mono<GroupModel> {
		return port.lockByIds(listOf(id))
			.then(Mono.defer { findGroupById(projectId, id, isVisible = null, memberIsVisible = null, memberIsAvailable = null) })
	}

	override fun addMembersToGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		memberIds: List<UUID>
	): Mono<Pair<List<UUID>, List<UUID>>> {
		return findLockedGroup(projectId, id)
			.map { Pair(it, it.getNewMemberIds(memberIds)) }
			.handle { it, handle ->
				if (it.second.isEmpty()) {
					handle.error(
						RegistryException(
							UNPROCESSABLE_CONTENT,
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
			.`as`(transactionalOperator::transactional)
	}

	override fun removeMemberFromGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		memberId: UUID
	): Mono<GroupModel> {
		return findLockedGroup(projectId, id)
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
			.`as`(transactionalOperator::transactional)
	}

	private fun Mono<GroupModel>.updateGroup(currentUser: CurrentUserModel) = flatMap {
		port.update(it.apply { update(currentUser, now()) })
	}

	private fun validateMembers(projectId: UUID, group: GroupModel, newMemberIds: List<UUID>): Mono<GroupModel> {
		return participantPort.findAllByIds(projectId, newMemberIds, isVisible = null)
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
			isVisible = true,
			memberIsVisible = null,
			memberIsAvailable = null
		)
			.updateVisibility(visibility = false)
			.updateGroup(currentUser)
	}

	override fun enableGroupById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<GroupModel> {
		return findGroupById(
			projectId,
			id,
			isVisible = false,
			memberIsVisible = null,
			memberIsAvailable = null
		)
			.updateVisibility(visibility = true)
			.updateGroup(currentUser)
	}

	override fun deleteGroupById(projectId: UUID, id: UUID): Mono<Unit> {
		return findGroupById(
			projectId,
			id,
			isVisible = null,
			memberIsVisible = null,
			memberIsAvailable = null
		)
			.flatMap { port.deleteById(id) }
	}

	override fun purgeEmptyGroups(
		participantToExclude: List<UUID>,
		dryRun: Boolean
	): Flux<UUID> {
		log.info("Purging empty groups")
		return port.findEmpty(participantToExclude)
			.purgeEach("group", dryRun, port::deleteById)
	}
}
