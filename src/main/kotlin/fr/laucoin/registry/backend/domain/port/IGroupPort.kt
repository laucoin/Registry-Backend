package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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
	fun findWithLimit(limit: Int, projectId: UUID, searchParams: GroupSearchParamModel): Flux<GroupModel>
	fun findEmpty(participantToExclude: List<UUID>): Flux<UUID>
	fun create(element: GroupModel): Mono<GroupModel>
	fun update(element: GroupModel): Mono<GroupModel>
	fun deleteById(id: UUID): Mono<Unit>
}
