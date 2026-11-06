package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.service.core.IGroupService
import fr.laucoin.registry.backend.domain.service.usecase.IGroupUseCase
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Point d'entrée des groupes pour la couche REST.
 * Périmètre : Délègue chaque opération au service Group sans logique propre.
 * Limites : Ne porte aucune règle métier ; elles restent dans le service.
 */
@Service
class GroupUseCase(private val service: IGroupService) : IGroupUseCase {
	override fun findGroupsPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: GroupSearchParamModel,
	): Mono<PageModel<GroupModel>> {
		return service.findGroupsPage(projectId, pageable, searchParams)
	}

	override fun findGroupMembersPageByGroupId(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
	): Mono<PageModel<ParticipantModel>> {
		return service.findGroupMembersPageByGroupId(projectId, id, pageable, searchParams)
	}

	override fun findGroupById(
		projectId: UUID,
		id: UUID,
		isVisible: Boolean?,
		memberIsVisible: Boolean?,
		memberIsAvailable: Boolean?,
	): Mono<GroupModel> {
		return service.findGroupById(projectId, id, isVisible, memberIsVisible, memberIsAvailable)
	}

	override fun findGroupSummaryById(projectId: UUID, id: UUID): Mono<GroupModel> {
		return service.findGroupSummaryById(projectId, id)
	}

	override fun searchParticipantsByText(projectId: UUID, query: String?): Flux<ParticipantModel> {
		return service.searchParticipantsByText(projectId, query)
	}

	override fun createGroup(currentUser: CurrentUserModel, group: GroupModel): Mono<GroupModel> {
		return service.createGroup(currentUser, group)
	}

	override fun updateGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		group: GroupModel,
	): Mono<GroupModel> {
		return service.updateGroupById(currentUser, projectId, id, group)
	}

	override fun addMembersToGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		memberIds: List<UUID>,
	): Mono<Pair<List<UUID>, List<UUID>>> {
		return service.addMembersToGroupById(currentUser, projectId, id, memberIds)
	}

	override fun removeMemberFromGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		memberId: UUID,
	): Mono<GroupModel> {
		return service.removeMemberFromGroupById(currentUser, projectId, id, memberId)
	}

	override fun disableGroupById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<GroupModel> {
		return service.disableGroupById(currentUser, projectId, id)
	}

	override fun enableGroupById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<GroupModel> {
		return service.enableGroupById(currentUser, projectId, id)
	}

	override fun deleteGroupById(projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteGroupById(projectId, id)
	}
}
