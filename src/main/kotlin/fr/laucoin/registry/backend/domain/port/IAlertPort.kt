package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IAlertPort {
	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<AlertModel>
	fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: AlertSearchParamModel,
	): Mono<PageModel<AlertModel>>

	fun findWithLimit(limit: Int, projectId: UUID, searchParams: AlertSearchParamModel): Flux<AlertModel>
	fun findOlderThanAndUncommentedSince(dateThreshold: LocalDate): Flux<UUID>
	fun create(element: AlertModel): Mono<AlertModel>
	fun update(element: AlertModel): Mono<AlertModel>
	fun deleteById(id: UUID): Mono<Unit>
}