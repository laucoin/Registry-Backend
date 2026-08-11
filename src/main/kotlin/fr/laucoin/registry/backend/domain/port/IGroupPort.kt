package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.GroupSortFieldEnum
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.SortModel
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface IGroupPort {
	fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: GroupSearchParamModel,
		sort: List<SortModel<GroupSortFieldEnum>> = emptyList(),
	): Mono<PageModel<GroupModel>>

	fun findByIdWithContent(
		projectId: UUID,
		id: UUID,
		visibilitySearched: Boolean?,
		memberVisibilitySearched: Boolean?,
		memberAvailabilitySearched: Boolean?
	): Mono<GroupModel>

	fun findContent(
		projectId: UUID,
		groupIds: List<UUID>,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
	): Flux<Pair<UUID, List<ParticipantModel>>>

	fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<GroupModel>
	fun findWithLimit(limit: Int, projectId: UUID, searchParams: GroupSearchParamModel): Flux<GroupModel>
	fun findArrivingToday(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<GroupModel>
	fun findDepartingToday(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<GroupModel>
	fun findEmpty(participantToExclude: List<UUID>): Flux<UUID>
	fun create(element: GroupModel): Mono<GroupModel>
	fun update(element: GroupModel): Mono<GroupModel>
	fun deleteById(id: UUID): Mono<Unit>
}
