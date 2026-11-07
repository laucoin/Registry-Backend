package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.service.core.IParticipantSearchService
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
import fr.laucoin.registry.backend.domain.service.core.IParticipantService
import fr.laucoin.registry.backend.domain.service.usecase.IParticipantUseCase
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Point d'entrée des participants pour la couche REST.
 * Périmètre : Délègue chaque opération aux services Participant et recherche Participant sans logique propre.
 * Limites : Ne porte aucune règle métier ; elles restent dans le service.
 */
@Service
class ParticipantUseCase(
	private val service: IParticipantService,
	private val searchService: IParticipantSearchService,
) : IParticipantUseCase {
	override fun findParticipantsPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
	): Mono<PageModel<ParticipantModel>> {
		return service.findParticipantsPage(projectId, pageable, searchParams)
	}

	override fun findBirthdays(projectId: UUID): Flux<ParticipantModel> {
		return service.findBirthdays(projectId)
	}

	override fun findParticipantTimeline(projectId: UUID, id: UUID): Mono<ParticipantTimelineModel> {
		return service.findParticipantTimeline(projectId, id)
	}

	override fun findParticipantById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ParticipantModel> {
		return service.findParticipantById(projectId, id, isVisible)
	}

	override fun searchUsersByText(projectId: UUID, query: String?): Flux<UserModel> {
		return searchService.searchUsersByText(projectId, query)
	}

	override fun searchGroupsByText(projectId: UUID, query: String?): Flux<GroupModel> {
		return searchService.searchGroupsByText(projectId, query)
	}

	override fun findParticipantMovementsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>> {
		return service.findParticipantMovementsPage(currentUser, projectId, id, pageable, searchParams)
	}

	override fun createParticipant(
		currentUser: CurrentUserModel,
		participant: ParticipantModel,
	): Mono<ParticipantModel> {
		return service.createParticipant(currentUser, participant)
	}

	override fun updateParticipantById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		participant: ParticipantModel,
	): Mono<ParticipantModel> {
		return service.updateParticipantById(currentUser, projectId, id, participant)
	}

	override fun disableParticipantById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ParticipantModel> {
		return service.disableParticipantById(currentUser, projectId, id)
	}

	override fun enableParticipantById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ParticipantModel> {
		return service.enableParticipantById(currentUser, projectId, id)
	}

	override fun deleteParticipantById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteParticipantById(currentUser, projectId, id)
	}
}
