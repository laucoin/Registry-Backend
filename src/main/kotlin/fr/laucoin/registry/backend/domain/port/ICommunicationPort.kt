package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.CommunicationSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface ICommunicationPort {
	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<CommunicationModel>
	fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel,
		sort: List<SortModel<CommunicationSortFieldEnum>> = emptyList(),
	): Mono<PageModel<CommunicationModel>>

	fun findByMovementIdsWithLimit(
		limit: Int,
		projectId: UUID,
		movementIds: List<UUID>,
		visibilitySearched: Boolean?,
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
		visibilitySearched: Boolean?,
	): Flux<Pair<UUID, List<CommunicationModel>>>

	fun findPageByAlertId(
		projectId: UUID,
		alertId: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel
	): Mono<PageModel<CommunicationModel>>

	fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<CommunicationModel>
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
