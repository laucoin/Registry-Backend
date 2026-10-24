package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Port driven donnant au domaine l'accès à la persistance de la communication.
 * Périmètre : Déclare les lectures, recherches et écritures de la communication dont dépendent les services.
 * Limites : Contrat uniquement ; l'adaptateur Postgres porte le SQL, les entités et le mapping.
 */
interface ICommunicationPort {
	fun findById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<CommunicationModel>
	fun findByMovementIdsWithLimit(
		limit: Int,
		projectId: UUID,
		movementIds: List<UUID>,
		isVisible: Boolean?,
	): Flux<Pair<UUID, List<CommunicationModel>>>

	fun findPageByMovementId(
		projectId: UUID,
		movementId: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel
	): Mono<PageModel<CommunicationModel>>

	fun findByAlertIdsWithLimit(
		limit: Int,
		projectId: UUID,
		alertIds: List<UUID>,
		isVisible: Boolean?,
	): Flux<Pair<UUID, List<CommunicationModel>>>

	fun findPageByAlertId(
		projectId: UUID,
		alertId: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel
	): Mono<PageModel<CommunicationModel>>

	fun findAllByIds(projectId: UUID, ids: List<UUID>, isVisible: Boolean?): Flux<CommunicationModel>
	fun countAllByMovementId(projectId: UUID, movementId: UUID, searchParams: CommunicationSearchParamModel): Mono<Long>
	fun countAllByAlertId(projectId: UUID, alertId: UUID, searchParams: CommunicationSearchParamModel): Mono<Long>

	fun findOrphan(
		movementsToExclude: List<UUID>,
		alertsToExclude: List<UUID>,
	): Flux<UUID>

	fun create(element: CommunicationModel): Mono<CommunicationModel>
	fun update(element: CommunicationModel): Mono<CommunicationModel>
	fun deleteById(id: UUID): Mono<Unit>
}
