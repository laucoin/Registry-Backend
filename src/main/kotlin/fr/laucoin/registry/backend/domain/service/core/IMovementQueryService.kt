package fr.laucoin.registry.backend.domain.service.core

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de lecture des mouvements, de leur contenu, de leurs communications et des statuts de présence.
 * Périmètre : Déclare les lectures paginées, la lecture par identifiant et le comptage des présents.
 * Limites : Contrat uniquement ; la recherche textuelle et les écritures sont dans leurs propres services.
 */
interface IMovementQueryService {
	fun findMovementsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>>

	fun findCurrentMovementsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>>

	fun findMovementsContent(
		projectId: UUID,
		movementIds: List<UUID>,
	): Flux<Pair<UUID, List<MovementContentModel>>>

	fun findCurrentMovementsContent(
		projectId: UUID,
		movementIds: List<UUID>,
	): Flux<Pair<UUID, List<MovementContentModel>>>

	fun findMovementById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<MovementModel>

	fun findMovementCommunicationsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel,
	): Mono<PageModel<CommunicationModel>>

	fun findParticipantsStatus(projectId: UUID): Mono<ProjectStatusModel>

	fun findVehiclesStatus(projectId: UUID): Mono<VehicleStatusModel>
}
