package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

interface IParticipantPort {
	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ParticipantModel>
	fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
		sort: List<SortModel<ParticipantSortFieldEnum>> = emptyList(),
	): Mono<PageModel<ParticipantModel>>

	fun findBirthdays(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<ParticipantModel>
	fun findArrivingToday(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<ParticipantModel>
	fun findDepartingToday(projectId: UUID, visibilitySearched: Boolean?, limit: Int): Flux<ParticipantModel>
	fun countAll(projectId: UUID, searchParams: ParticipantSearchParamModel): Mono<Long>
	fun findPageByGroupId(
		projectId: UUID,
		groupId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
	): Mono<PageModel<ParticipantModel>>

	fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ParticipantModel>
	fun findByUserId(projectId: UUID, userId: UUID): Flux<ParticipantModel>
	fun findWithLimit(limit: Int, projectId: UUID, searchParams: ParticipantSearchParamModel): Flux<ParticipantModel>
	fun markAllAsDeparted(ids: List<UUID>, departedAt: ZonedDateTime): Flux<ParticipantModel>
	fun saveAllGuest(guests: List<ParticipantModel>): Flux<ParticipantModel>
	fun deleteAll(ids: List<UUID>): Mono<Unit>
	fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID>
	fun create(element: ParticipantModel): Mono<ParticipantModel>
	fun update(element: ParticipantModel): Mono<ParticipantModel>
	fun deleteById(id: UUID): Mono<Unit>
}
