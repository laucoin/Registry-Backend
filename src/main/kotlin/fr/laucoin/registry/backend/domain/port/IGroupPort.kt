package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.GroupSortFieldEnum
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.SortModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Persistence port for [GroupModel]: CRUD, paginated/filtered search, member-content loading,
 * dashboard queries (arriving/departing today), and empty-group detection for cleanup. Implemented by
 * the jOOQ Postgres adapter; the domain only depends on this contract.
 */
interface IGroupPort {
	fun findAllByCreatorId(userId: UUID): Flux<GroupModel>
	fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: GroupSearchParamModel,
		sortFields: List<SortModel<GroupSortFieldEnum>> = emptyList(),
	): Mono<PageModel<GroupModel>>

	fun findByIdWithContent(
		projectId: UUID,
		id: UUID,
		isVisible: Boolean?,
		isMemberVisible: Boolean?,
		isMemberAvailable: Boolean?
	): Mono<GroupModel>

	fun findContent(
		projectId: UUID,
		groupIds: List<UUID>,
		isVisible: Boolean?,
		isAvailable: Boolean?,
	): Flux<Pair<UUID, List<ParticipantModel>>>

	fun findAllByIds(projectId: UUID, ids: List<UUID>, isVisible: Boolean?): Flux<GroupModel>
	fun findWithLimit(limit: Int, projectId: UUID, searchParams: GroupSearchParamModel): Flux<GroupModel>
	fun findArrivingToday(projectId: UUID, limit: Int): Flux<GroupModel>
	fun findDepartingToday(projectId: UUID, limit: Int): Flux<GroupModel>
	fun findEmpty(participantToExclude: List<UUID>): Flux<UUID>
	fun create(element: GroupModel): Mono<GroupModel>
	fun update(element: GroupModel): Mono<GroupModel>
	fun deleteById(id: UUID): Mono<Unit>
}
