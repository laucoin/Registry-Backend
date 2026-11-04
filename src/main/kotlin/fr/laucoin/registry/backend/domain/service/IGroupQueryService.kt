package fr.laucoin.registry.backend.domain.service

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
 * Objectif : Contrat de lecture des groupes.
 * Périmètre : Déclare la consultation (page, détail, membres) et le sélecteur de participants.
 * Limites : Contrat uniquement ; ne modifie aucun groupe.
 */
interface IGroupQueryService {
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
}
