package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage du participant.
 * Périmètre : Déclare ce que les contrôleurs et les autres services peuvent demander à propos du participant.
 * Limites : Contrat uniquement ; les règles métier vivent dans l'implémentation.
 */
interface IParticipantService {
	fun findParticipantsPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
	): Mono<PageModel<ParticipantModel>>

	fun findBirthdays(projectId: UUID): Flux<ParticipantModel>

	fun findParticipantsByIds(projectId: UUID, ids: List<UUID>, isVisible: Boolean?): Flux<ParticipantModel>
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

	fun createParticipant(currentUser: CurrentUserModel, participant: ParticipantModel): Mono<ParticipantModel>
	fun updateParticipantById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		participant: ParticipantModel
	): Mono<ParticipantModel>

	fun disableParticipantById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ParticipantModel>
	fun enableParticipantById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ParticipantModel>
	fun deleteParticipantById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit>
	fun purgeParticipantsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID>
}
