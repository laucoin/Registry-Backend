package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Port driven donnant au domaine l'accès à la persistance du participant.
 * Périmètre : Déclare les lectures, recherches et écritures du participant dont dépendent les services.
 * Limites : Contrat uniquement ; l'adaptateur Postgres porte le SQL, les entités et le mapping.
 */
interface IParticipantPort {
	fun findById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ParticipantModel>
	fun findPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
	): Mono<PageModel<ParticipantModel>>

	fun findBirthdays(projectId: UUID, isVisible: Boolean?): Flux<ParticipantModel>
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
	fun findPlannedOnOwnDate(
		projectId: UUID,
		type: MovementTypeEnum,
		day: LocalDate,
		limit: Int,
	): Flux<ParticipantModel>

	fun findPlannedOnGroupDate(
		projectId: UUID,
		type: MovementTypeEnum,
		day: LocalDate,
		limit: Int,
	): Flux<Pair<UUID, ParticipantModel>>

	fun refreshEffectiveDates(ids: List<UUID>): Mono<Unit>

	fun lockByIds(ids: List<UUID>): Flux<UUID>
	fun saveAllGuest(guests: List<ParticipantModel>): Flux<ParticipantModel>
	fun deleteAll(ids: List<UUID>): Mono<Unit>
	fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID>
	fun create(element: ParticipantModel): Mono<ParticipantModel>
	fun update(element: ParticipantModel): Mono<ParticipantModel>
	fun deleteById(id: UUID): Mono<Unit>
}
