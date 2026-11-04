package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.extension.OutingExt.toOutings
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.OutingModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ParticipantTimelineModel
import fr.laucoin.registry.backend.domain.model.TimelineRangeModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IParticipantQueryService
import java.time.Clock
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Consulte les participants d'un projet : pages, détail, chronologie, anniversaires et sélecteurs.
 * Périmètre : Lecture seule, avec la garde de l'option d'activité sur les mouvements d'un participant.
 * Limites : Ne modifie rien.
 */
@Service
class ParticipantQueryService(
	private val port: IParticipantPort,
	private val userPort: IUserPort,
	private val movementPort: IMovementPort,
	private val groupPort: IGroupPort,
	clock: Clock,
	@param:Value($$"${registry.feature.participant.searched.max-user-result}")
	private val maxUserResult: Int,
	@param:Value($$"${registry.feature.participant.searched.max-group-result}")
	private val maxGroupResult: Int,
) : IParticipantQueryService, GenericService(clock) {
	override fun findParticipantsPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
	): Mono<PageModel<ParticipantModel>> =
		port.findPage(projectId, pageable, searchParams)

	override fun findBirthdays(projectId: UUID): Flux<ParticipantModel> =
		port.findBirthdays(projectId, isVisible = true)

	override fun findParticipantsByIds(
		projectId: UUID,
		ids: List<UUID>,
		isVisible: Boolean?
	): Flux<ParticipantModel> =
		port.findAllByIds(projectId, ids, isVisible)

	override fun findParticipantById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ParticipantModel> =
		port.findById(projectId, id, isVisible)
			.notFoundIfEmpty(id)

	override fun findParticipantTimeline(projectId: UUID, id: UUID): Mono<ParticipantTimelineModel> =
		findParticipantById(projectId, id, isVisible = null)
			.flatMap(::toTimeline)

	private fun toTimeline(participant: ParticipantModel): Mono<ParticipantTimelineModel> {
		return findOutings(participant)
			.map { ParticipantTimelineModel(plannedRanges(participant), it) }
	}

	private fun findOutings(participant: ParticipantModel): Mono<List<OutingModel>> {
		if (participant.type == GUEST) return Mono.just(emptyList())

		return movementPort.findEventsByParticipantId(participant.projectId!!, participant.id!!)
			.collectList()
			.map { it.toOutings(now()) }
	}

	private fun plannedRanges(participant: ParticipantModel): List<TimelineRangeModel> {
		if (participant.startAvailability == null && participant.endAvailability == null) return emptyList()

		return listOf(TimelineRangeModel(participant.startAvailability, participant.endAvailability))
	}

	override fun searchUsersByText(projectId: UUID, query: String?): Flux<UserModel> {
		return userPort.findWithLimit(
			maxUserResult,
			UserSearchParamModel(query, isVisible = true).apply { memberOfProjectId = projectId },
		)
	}

	override fun searchGroupsByText(projectId: UUID, query: String?): Flux<GroupModel> {
		return groupPort.findWithLimit(
			maxGroupResult,
			projectId,
			GroupSearchParamModel(query, isVisible = true),
		)
	}

	override fun findParticipantMovementsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return Mono.defer { movementPort.findPageByParticipantId(projectId, id, pageable, searchParams) }
			.requireActivityOption(currentUser, projectId, searchParams.hasActivity == true)
	}
}
