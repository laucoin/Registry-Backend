package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage du groupe.
 * Périmètre : Déclare ce que les contrôleurs et les autres services peuvent demander à propos du groupe.
 * Limites : Contrat uniquement ; les règles métier vivent dans l'implémentation.
 */
interface IGroupService {
	fun findGroupsPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: GroupSearchParamModel,
	): Mono<PageModel<GroupModel>>

	fun findGroupMembersPageByGroupId(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
	): Mono<PageModel<ParticipantModel>>

	fun findGroupById(
		projectId: UUID,
		id: UUID,
		isVisible: Boolean?,
		memberIsVisible: Boolean?,
		memberIsAvailable: Boolean?
	): Mono<GroupModel>

	fun findGroupSummaryById(projectId: UUID, id: UUID): Mono<GroupModel>

	fun searchParticipantsByText(projectId: UUID, query: String?): Flux<ParticipantModel>
	fun createGroup(currentUser: CurrentUserModel, group: GroupModel): Mono<GroupModel>
	fun updateGroupById(currentUser: CurrentUserModel, projectId: UUID, id: UUID, group: GroupModel): Mono<GroupModel>
	fun addMembersToGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		memberIds: List<UUID>
	): Mono<Pair<List<UUID>, List<UUID>>>

	fun removeMemberFromGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		memberId: UUID
	): Mono<GroupModel>

	fun disableGroupById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<GroupModel>
	fun enableGroupById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<GroupModel>
	fun deleteGroupById(projectId: UUID, id: UUID): Mono<Unit>
	fun purgeEmptyGroups(participantToExclude: List<UUID>, dryRun: Boolean): Flux<UUID>
}
