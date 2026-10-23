package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: Driven port giving the domain access to the persisted group.
 * Scope: Declares the reads, searches and writes of group that the services rely on.
 * Limits: Contract only; the Postgres adapter owns SQL, entities and mapping.
 */
interface IGroupPort {
	fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: GroupSearchParamModel,
	): Mono<PageModel<GroupModel>>

	fun findByIdWithContent(
		projectId: UUID,
		id: UUID,
		isVisible: Boolean?,
		memberIsVisible: Boolean?,
		memberIsAvailable: Boolean?
	): Mono<GroupModel>

	fun findContent(
		projectId: UUID,
		groupIds: List<UUID>,
		isVisible: Boolean?,
		isAvailable: Boolean?,
	): Flux<Pair<UUID, List<ParticipantModel>>>

	fun findAllByIds(projectId: UUID, ids: List<UUID>, isVisible: Boolean?): Flux<GroupModel>
	fun lockByIds(ids: List<UUID>): Flux<UUID>
	fun findWithLimit(limit: Int, projectId: UUID, searchParams: GroupSearchParamModel): Flux<GroupModel>
	fun findEmpty(participantToExclude: List<UUID>): Flux<UUID>
	fun create(element: GroupModel): Mono<GroupModel>
	fun update(element: GroupModel): Mono<GroupModel>
	fun deleteById(id: UUID): Mono<Unit>
}
