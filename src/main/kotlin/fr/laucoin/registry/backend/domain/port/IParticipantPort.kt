package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Persistence port for [ParticipantModel]: CRUD, paginated/filtered search, group-scoped listing,
 * dashboard queries (birthdays, arriving/departing today), bulk Guest creation/availability updates,
 * and the retention lookup used by the purge job. Implemented by the jOOQ Postgres adapter.
 */
interface IParticipantPort {
	fun findById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ParticipantModel>
	fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
		sortFields: List<SortModel<ParticipantSortFieldEnum>> = emptyList(),
	): Mono<PageModel<ParticipantModel>>

	fun findBirthdays(projectId: UUID, isVisible: Boolean?, limit: Int): Flux<ParticipantModel>
	fun findArrivingToday(projectId: UUID, isVisible: Boolean?, limit: Int): Flux<ParticipantModel>
	fun findDepartingToday(projectId: UUID, isVisible: Boolean?, limit: Int): Flux<ParticipantModel>
	fun countAll(projectId: UUID, searchParams: ParticipantSearchParamModel): Mono<Long>
	fun findPageByGroupId(
		projectId: UUID,
		groupId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
	): Mono<PageModel<ParticipantModel>>

	fun findAllByIds(projectId: UUID, ids: List<UUID>, isVisible: Boolean?): Flux<ParticipantModel>
	fun findByUserId(projectId: UUID, userId: UUID): Flux<ParticipantModel>
	fun findAllByUserId(userId: UUID): Flux<ParticipantModel>
	fun findWithLimit(limit: Int, projectId: UUID, searchParams: ParticipantSearchParamModel): Flux<ParticipantModel>
	fun updateAllEndAvailability(ids: List<UUID>, endAvailability: CustomDateTimeModel): Flux<ParticipantModel>
	fun saveAllGuest(guests: List<ParticipantModel>): Flux<ParticipantModel>
	fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID>
	fun create(element: ParticipantModel): Mono<ParticipantModel>
	fun update(element: ParticipantModel): Mono<ParticipantModel>
	fun deleteById(id: UUID): Mono<Unit>
}
