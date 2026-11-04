package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ParticipantTimelineModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de lecture des participants.
* Périmètre : Déclare la consultation (page, détail, chronologie, anniversaires), les sélecteurs et les mouvements
 *   d'un participant.
 * Limites : Contrat uniquement ; ne modifie aucun participant.
 */
interface IParticipantQueryService {
	fun findParticipantsPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
	): Mono<PageModel<ParticipantModel>>

	fun findBirthdays(projectId: UUID): Flux<ParticipantModel>

	fun findParticipantsByIds(projectId: UUID, ids: List<UUID>, isVisible: Boolean?): Flux<ParticipantModel>

	fun findParticipantTimeline(projectId: UUID, id: UUID): Mono<ParticipantTimelineModel>

	fun findParticipantById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ParticipantModel>

	fun searchUsersByText(projectId: UUID, query: String?): Flux<UserModel>

	fun searchGroupsByText(projectId: UUID, query: String?): Flux<GroupModel>

	fun findParticipantMovementsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>>
}
