package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.PlannedMovementModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IMovementSearchService
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.zip
import reactor.util.function.Tuple2

/**
* Objectif : Recherche les participants, groupes, véhicules, motifs et activités sélectionnables et calcule les
 *   mouvements planifiés.
* Périmètre : Plafonds de résultats des sélecteurs et brouillons d'arrivée ou de départ planifiés par participant ou
 *   par groupe.
 * Limites : Lecture seule ; ne valide ni ne crée de mouvement.
 */
@Service
class MovementSearchService(
	private val participantPort: IParticipantPort,
	private val vehiclePort: IVehiclePort,
	private val activityPort: IActivityPort,
	private val groupPort: IGroupPort,
	clock: Clock,
	@param:Value($$"${registry.feature.movement.searched.max-participant-result}")
	private val maxParticipantResult: Int,
	@param:Value($$"${registry.feature.movement.searched.max-group-result}")
	private val maxGroupResult: Int,
	@param:Value($$"${registry.feature.movement.searched.max-vehicle-result}")
	private val maxVehicleResult: Int,
	@param:Value($$"${registry.feature.movement.searched.max-activity-result}")
	private val maxActivityResult: Int,
	@param:Value($$"${registry.feature.movement.planned.max-result}")
	private val maxPlannedResult: Int,
) : IMovementSearchService, GenericService(clock) {
	override fun searchParticipantsAndGroupsByText(
		projectId: UUID,
		type: ParticipantTypeEnum,
		query: String?,
		at: ZonedDateTime,
		all: Boolean,
	): Mono<Tuple2<List<ParticipantModel>, List<GroupModel>>> =
		zip(
			findParticipantWithLimit(projectId, type, query, at, all).collectList(),
			findGroupWithLimit(projectId, query, all),
		)

	private fun findParticipantWithLimit(
		projectId: UUID, type: ParticipantTypeEnum, query: String?, at: ZonedDateTime, all: Boolean,
	): Flux<ParticipantModel> {
		val participantSearch = ParticipantSearchParamModel().apply {
			this.type = type
			this.query = query
			isVisible = true
			this.at = if (all) null else at
		}

		return participantPort.findWithLimit(maxParticipantResult, projectId, participantSearch)
	}

	private fun findGroupWithLimit(projectId: UUID, query: String?, all: Boolean): Mono<List<GroupModel>> {
		val groupSearch = GroupSearchParamModel(query, isVisible = true, isPresent = true)

		return groupPort.findWithLimit(maxGroupResult, projectId, groupSearch)
			.collectList()
			.flatMap { groups ->
				groupPort.findContent(
					projectId,
					groups.mapNotNull(GroupModel::id),
					isVisible = true,
					isAvailable = if (all) null else true,
				)
					.filter { (_, members) -> members.isNotEmpty() }
					.collectList()
					.flatMap { contents -> withMembersStatus(projectId, groups, contents) }
			}
	}

	private fun withMembersStatus(
		projectId: UUID,
		groups: List<GroupModel>,
		contents: List<Pair<UUID, List<ParticipantModel>>>,
	): Mono<List<GroupModel>> {
		val memberIds = contents.flatMap { it.second }.mapNotNull(ParticipantModel::id).distinct()

		return participantPort.findAllByIds(projectId, memberIds, isVisible = true)
			.collectMap { it.id!! }
			.map { detailed ->
				contents.map { (groupId, members) ->
					groups.first { it.id == groupId }.apply { this.members = members.map { detailed[it.id] ?: it } }
				}
			}
	}

	override fun searchVehiclesByText(projectId: UUID, query: String?): Flux<VehicleModel> {
		return vehiclePort.findWithLimit(
			maxVehicleResult,
			projectId,
			VehicleSearchParamModel(isVisible = true, isAvailable = true).apply {
				this.query = query
			},
		)
	}

	override fun searchReasonsByText(
		contentType: ParticipantTypeEnum,
		type: MovementTypeEnum
	): Flux<MovementReasonEnum> {
		return Flux.fromIterable(MovementReasonEnum.entries)
			.filter { it.type == type && contentType == it.participantType }
	}

	override fun searchActivitiesByText(
		projectId: UUID,
		contentType: ParticipantTypeEnum,
		query: String?
	): Flux<ActivityModel> {
		return if (contentType === GUEST) Flux.empty()
		else activityPort.findWithLimit(
			maxActivityResult,
			projectId,
			ActivitySearchParamModel(query, isVisible = true, isAvailable = true),
		)
	}

	override fun findPlannedMovements(
		projectId: UUID,
		type: MovementTypeEnum,
		date: LocalDate?,
	): Flux<PlannedMovementModel> {
		val day = date ?: LocalDate.now(clock)
		return Flux.concat(
			findIndividualPlannedMovements(projectId, type, day),
			findGroupPlannedMovements(projectId, type, day),
		)
	}

	private fun findIndividualPlannedMovements(
		projectId: UUID, type: MovementTypeEnum, day: LocalDate
	): Flux<PlannedMovementModel> {
		return participantPort.findPlannedOnOwnDate(projectId, type, day, maxPlannedResult)
			.map { PlannedMovementModel(type, plannedAt(type, it.startAvailability, it.endAvailability), listOf(it)) }
	}

	private fun findGroupPlannedMovements(
		projectId: UUID, type: MovementTypeEnum, day: LocalDate
	): Flux<PlannedMovementModel> {
		return participantPort.findPlannedOnGroupDate(projectId, type, day, maxPlannedResult)
			.collectList()
			.flatMapMany { members -> withGroupNames(projectId, type, members) }
	}

	private fun withGroupNames(
		projectId: UUID, type: MovementTypeEnum, members: List<Pair<UUID, ParticipantModel>>
	): Flux<PlannedMovementModel> {
		if (members.isEmpty()) return Flux.empty()
		val membersByGroupId = members.groupBy({ it.first }, { it.second })
		return groupPort.findAllByIds(projectId, membersByGroupId.keys.toList(), true)
			.sort(compareBy(GroupModel::name))
			.map { groupPlannedMovement(type, it, membersByGroupId.getValue(it.id!!)) }
	}

	private fun groupPlannedMovement(
		type: MovementTypeEnum, group: GroupModel, members: List<ParticipantModel>
	): PlannedMovementModel {
		val plannedAt = plannedAt(type, group.startAvailability, group.endAvailability)
		return PlannedMovementModel(type, plannedAt, members, group.name.orEmpty())
	}

	private fun plannedAt(
		type: MovementTypeEnum, start: CustomDateTimeModel?, end: CustomDateTimeModel?
	): CustomDateTimeModel? =
		if (type == IN) start else end
}
