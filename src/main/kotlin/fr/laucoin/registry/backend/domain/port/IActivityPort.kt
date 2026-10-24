package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Port driven donnant au domaine l'accès à la persistance de l'activité.
 * Périmètre : Déclare les lectures, recherches et écritures de l'activité dont dépendent les services.
 * Limites : Contrat uniquement ; l'adaptateur Postgres porte le SQL, les entités et le mapping.
 */
interface IActivityPort {
	fun findById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ActivityModel>
	fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ActivitySearchParamModel,
	): Mono<PageModel<ActivityModel>>

	fun findAllByIds(projectId: UUID, ids: List<UUID>, isVisible: Boolean?): Flux<ActivityModel>
	fun findWithLimit(limit: Int, projectId: UUID, searchParams: ActivitySearchParamModel): Flux<ActivityModel>
	fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID>
	fun create(element: ActivityModel): Mono<ActivityModel>
	fun update(element: ActivityModel): Mono<ActivityModel>
	fun deleteById(id: UUID): Mono<Unit>
}