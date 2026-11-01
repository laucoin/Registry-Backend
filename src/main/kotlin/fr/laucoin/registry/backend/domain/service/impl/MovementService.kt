package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_DELETED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_DISABLED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_ENABLED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_UPDATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_COMMUNICATION_OUT_OF_MOVEMENT_DATETIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_IN_FUTURE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DRIVERS_NOT_MAJOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_REMOVE_GUEST_CONTENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_UPDATE_CHANGE_CONTENT_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_UPDATE_CHANGE_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.isMajor
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.PlannedMovementModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.domain.service.IProjectService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.zip
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.kotlin.core.util.function.component3
import reactor.kotlin.core.util.function.component4
import reactor.kotlin.core.util.function.component5
import reactor.util.function.Tuple2
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID

/**
 * Objectif : Gère les arrivées, départs et mouvements planifiés des participants, groupes et véhicules.
 * Périmètre : Validation des mouvements, dates effectives, statut de présence et mises à jour de disponibilité.
 * Limites : Ne gère pas les définitions des participants ou des groupes.
 */
@Service
class MovementService(
	private val projectService: IProjectService,
	private val port: IMovementPort,
	private val participantPort: IParticipantPort,
	private val vehiclePort: IVehiclePort,
	private val activityPort: IActivityPort,
	private val communicationPort: ICommunicationPort,
	private val groupPort: IGroupPort,
	private val transactionalOperator: TransactionalOperator,
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
	@param:Value($$"${registry.feature.movement.future-tolerance}")
	private val futureTolerance: Duration,
) : IMovementService, GenericService(clock) {
	override fun findMovementsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>> {
		return Mono.defer { port.findPage(projectId, pageable, searchParams) }
			.requireActivityOption(currentUser, projectId, searchParams.isActivityRequested())
	}

	override fun findCurrentMovementsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return Mono.defer { port.findCurrentPage(projectId, pageable, searchParams) }
			.requireActivityOption(currentUser, projectId, searchParams.isActivityRequested())
	}

	private fun MovementSearchParamModel.isActivityRequested() = hasActivity == true || activityId != null

	override fun findMovementsContent(
		projectId: UUID,
		movementIds: List<UUID>
	): Flux<Pair<UUID, List<MovementContentModel>>> {
		return port.findContent(projectId, movementIds)
	}

	override fun findCurrentMovementsContent(
		projectId: UUID,
		movementIds: List<UUID>
	): Flux<Pair<UUID, List<MovementContentModel>>> {
		return port.findCurrentContent(projectId, movementIds)
	}

	override fun findMovementById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<MovementModel> {
		return port.findById(projectId, id, isVisible)
			.notFoundIfEmpty(id)
	}

	override fun searchParticipantsAndGroupsByText(
		projectId: UUID,
		type: ParticipantTypeEnum,
		query: String?
	): Mono<Tuple2<List<ParticipantModel>, List<GroupModel>>> {
		return zip(
			findParticipantWithLimit(projectId, type, query).collectList(),
			findGroupWithLimit(projectId, query),
		)
	}

	private fun findParticipantWithLimit(
		projectId: UUID, type: ParticipantTypeEnum, query: String?
	): Flux<ParticipantModel> {
		val participantSearch = ParticipantSearchParamModel().apply {
			this.type = type
			this.query = query
			isVisible = true
			isAvailable = true
		}

		return participantPort.findWithLimit(maxParticipantResult, projectId, participantSearch)
	}

	private fun findGroupWithLimit(projectId: UUID, query: String?): Mono<List<GroupModel>> {
		val groupSearch = GroupSearchParamModel(query, isVisible = true, isPresent = true)

		return groupPort.findWithLimit(maxGroupResult, projectId, groupSearch)
			.collectList()
			.flatMap { groups ->
				groupPort.findContent(
					projectId,
					groups.mapNotNull(GroupModel::id),
					isVisible = true,
					isAvailable = true,
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

	override fun findPlannedMovements(projectId: UUID, type: MovementTypeEnum, date: LocalDate?): Flux<PlannedMovementModel> {
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
	): CustomDateTimeModel? {
		return if (type == IN) start else end
	}

	override fun findMovementCommunicationsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel
	): Mono<PageModel<CommunicationModel>> {
		return communicationPort.findPageByMovementId(projectId, id, pageable, searchParams)
	}

	override fun findParticipantsStatus(projectId: UUID): Mono<ProjectStatusModel> {
		return zip(
			countParticipants(projectId, isMajor = true, type = REGISTERED, presence = PresenceStatusEnum.IN),
			countParticipants(projectId, isMajor = true, type = REGISTERED, presence = PresenceStatusEnum.OUT),
			countParticipants(projectId, isMajor = false, type = REGISTERED, presence = PresenceStatusEnum.IN),
			countParticipants(projectId, isMajor = false, type = REGISTERED, presence = PresenceStatusEnum.OUT),
			countParticipants(projectId, isMajor = null, type = GUEST, presence = PresenceStatusEnum.IN),
		)
			.map { (registeredPresentAdult, registeredAbsentAdult, registeredPresentMinor, registeredAbsentMinor, guestPresent) ->
				ProjectStatusModel(
					registered = ProjectStatusModel.ParticipantStatusModel(
						registeredPresentMinor,
						registeredPresentAdult,
						registeredAbsentMinor,
						registeredAbsentAdult,
					),
					guests = guestPresent,
					lastRefresh = now(),
				)
			}
	}

	private fun countParticipants(
		projectId: UUID,
		isMajor: Boolean?,
		type: ParticipantTypeEnum,
		presence: PresenceStatusEnum,
	): Mono<Long> {
		return participantPort.countAll(
			projectId,
			ParticipantSearchParamModel(
				query = null,
				isMajor = isMajor,
				type = type,
				presence = presence,
				isVisible = true,
				at = null,
			),
		)
	}

	override fun findVehiclesStatus(projectId: UUID): Mono<VehicleStatusModel> {
		return zip(
			countVehicles(projectId, PresenceStatusEnum.IN),
			countVehicles(projectId, PresenceStatusEnum.OUT),
		)
			.map { (vehiclePresent, vehicleAbsent) -> VehicleStatusModel(present = vehiclePresent, absent = vehicleAbsent, lastRefresh = now()) }
	}

	private fun countVehicles(projectId: UUID, status: PresenceStatusEnum): Mono<Long> {
		return vehiclePort.countAll(
			projectId,
			VehicleSearchParamModel(query = null, isVisible = true, status = status, at = null),
		)
	}

	private fun validateMovementDate(movement: MovementModel): Mono<UUID> {
		return validateMovementIsNotInFuture(movement)
			.flatMap { validateMovementDateInProject(it) }
	}

	private fun validateMovementIsNotInFuture(movement: MovementModel): Mono<MovementModel> {
		return if (movement.dateTime.isAfter(ZonedDateTime.now(clock).plus(futureTolerance)))
			Mono.error(RegistryException(BAD_REQUEST, MOVEMENT_DATETIME_IN_FUTURE))
		else Mono.just(movement)
	}

	private fun validateMovementDateInProject(movement: MovementModel): Mono<UUID> {
		return projectService.validateDateTime(
			movement.projectId!!,
			CustomDateTimeModel(movement.dateTime),
			MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE,
		)
	}

	override fun createMovement(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		newGuests: List<ParticipantModel>,
	): Mono<MovementModel> {
		return lockParticipants((movement.participantIds() + newGuests.mapNotNull { it.id }).distinct())
			.then(Mono.defer { validateNewMovement(currentUser, movement, newGuests) })
			.flatMap { createMovement(currentUser, movement) }
			.refreshParticipantsEffectiveDates()
			.`as`(transactionalOperator::transactional)
	}

	private fun validateNewMovement(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		newGuests: List<ParticipantModel>,
	): Mono<*> {
		return Mono.zip(
			validateMovementDate(movement),
			validateActivity(movement),
			saveGuestsIfNecessary(currentUser, movement, movement, newGuests),
		)
			.flatMap { validateNewMovementContent(movement) }
	}

	private fun validateNewMovementContent(movement: MovementModel): Mono<*> {
		return Mono.zip(
			validateParticipantsIfAny(
				movement.projectId!!,
				movement,
				movement.content.mapNotNull { c -> c.participant!!.id },
				movement.content.filter { c -> Objects.nonNull(c.vehicle) }.mapNotNull { c -> c.participant!!.id },
			),
			validateVehiclesIfAny(movement.projectId!!, movement, movement.content.mapNotNull { c -> c.vehicle?.id }),
		)
	}

	private fun createMovement(currentUser: CurrentUserModel, movement: MovementModel): Mono<MovementModel> {
		return port.create(movement.apply { create(currentUser, now()) })
	}

	private fun lockParticipants(ids: List<UUID>): Mono<Unit> {
		return participantPort.lockByIds(ids).then(Mono.just(Unit))
	}

	override fun updateMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		movement: MovementModel,
		newGuests: List<ParticipantModel>,
	): Mono<MovementModel> {
		return validateMovementDate(movement)
			.flatMap { findLockedMovement(projectId, id, isVisible = null) }
			.validateUpdatableMovementFields(movement)
			.validateMovementIsAlterable(MOVEMENT_CANNOT_BE_UPDATED)
			.flatMap { validateNoCommunicationConflict(movement, it) }
			.flatMap { oldMovement ->
				Mono.zip(
					validateActivity(movement, oldMovement),
					saveGuestsIfNecessary(currentUser, movement, oldMovement, newGuests),
				).thenReturn(oldMovement)
			}
			.flatMap { validateUpdatedContent(projectId, it, movement).thenReturn(it) }
			.flatMap { applyUpdate(currentUser, it, movement) }
			.`as`(transactionalOperator::transactional)
	}

	private fun validateUpdatedContent(projectId: UUID, oldMovement: MovementModel, movement: MovementModel): Mono<*> {
		return Mono.zip(
			validateParticipantsIfAny(
				projectId,
				oldMovement,
				oldMovement.getNewContentParticipantIds(movement),
				oldMovement.getNewContentDriverIds(movement),
			),
			validateVehiclesIfAny(projectId, oldMovement, oldMovement.getNewContentVehicleIds(movement)),
		)
	}

	private fun applyUpdate(
		currentUser: CurrentUserModel,
		oldMovement: MovementModel,
		movement: MovementModel,
	): Mono<MovementModel> {
		val previousParticipantIds = oldMovement.participantIds()
		return lockParticipants((previousParticipantIds + movement.participantIds()).distinct())
			.then(Mono.fromCallable {
				oldMovement.apply {
					dateTime = movement.dateTime
					reason = movement.reason
					activity = movement.activity
					content = movement.content
				}
			})
			.updateMovement(currentUser)
			.refreshParticipantsEffectiveDates(previousParticipantIds)
	}

	private fun validateParticipantsIfAny(
		projectId: UUID,
		movement: MovementModel,
		participantIds: List<UUID>,
		driverIds: List<UUID>,
	): Mono<MovementModel> =
		if (participantIds.isEmpty()) Mono.just(movement)
		else validateParticipants(projectId, movement, participantIds, driverIds)

	private fun validateVehiclesIfAny(
		projectId: UUID,
		movement: MovementModel,
		vehicleIds: List<UUID>,
	): Mono<MovementModel> =
		if (vehicleIds.isEmpty()) Mono.just(movement) else validateVehicles(projectId, movement, vehicleIds)

	private fun Mono<MovementModel>.updateMovement(currentUser: CurrentUserModel) = flatMap {
		port.update(it.apply { update(currentUser, now()) })
	}

	private fun validateActivity(movement: MovementModel, oldMovement: MovementModel? = null): Mono<MovementModel> {
		return if (Objects.isNull(movement.activity) || movement.activity?.id == oldMovement?.activity?.id) Mono.just(
			oldMovement ?: movement
		)
		else activityPort.findById(movement.projectId!!, movement.activity!!.id!!, isVisible = null)
			.switchIfEmpty { Mono.error(RegistryException(NOT_FOUND, MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT)) }
			.handle { it, handle ->
				if (it.isNotVisible()) handle.error(
					RegistryException(
						NOT_FOUND,
						MOVEMENT_ACTIVITY_NOT_VISIBLE,
					)
				)
				else handle.next(oldMovement ?: movement)
			}
	}

	private fun validateNoCommunicationConflict(
		movement: MovementModel,
		oldMovement: MovementModel
	): Mono<MovementModel> {
		return if (movement.dateTime.isAfter(oldMovement.dateTime)) {
			val params = CommunicationSearchParamModel(
				isVisible = null,
				from = null,
				to = movement.dateTime,
			)
			communicationPort.countAllByMovementId(movement.projectId!!, oldMovement.id!!, params)
				.handle { it, handle ->
					if (it > 0L) handle.error(
						RegistryException(
							UNPROCESSABLE_CONTENT,
							MOVEMENT_COMMUNICATION_OUT_OF_MOVEMENT_DATETIME,
							arrayListOf(it)
						)
					)
					else handle.next(oldMovement)
				}
		} else Mono.just(oldMovement)
	}

	private fun saveGuestsIfNecessary(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		oldMovement: MovementModel,
		guests: List<ParticipantModel>,
	): Mono<MovementModel> {
		if (movement.type !== IN || guests.isEmpty()) return Mono.just(oldMovement)

		val guestsToCreate: List<ParticipantModel> = guests
			.filter { Objects.isNull(it.id) }
			.map {
				it.apply {
					startAvailability = CustomDateTimeModel(movement.dateTime)
					create(currentUser, now())
				}
			}

		return findGuestsToUpdate(currentUser, movement, guests)
			.map { it.apply { addAll(guestsToCreate) } }
			.flatMap { saveGuests(it, movement, oldMovement) }
	}

	private fun findGuestsToUpdate(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		guests: List<ParticipantModel>,
	): Mono<MutableList<ParticipantModel>> {
		val guestIdsToUpdate: List<UUID> = guests.mapNotNull { it.id }

		return participantPort.findAllByIds(movement.projectId!!, guestIdsToUpdate, isVisible = null)
			.map { existing -> refreshGuest(existing, guests.find { g -> g.id == existing.id }, movement, currentUser) }
			.collectList()
			.handle { it, handle ->
				if (it.size != guestIdsToUpdate.size) handle.error(
					RegistryException(NOT_FOUND, MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT)
				)
				else handle.next(it)
			}
			.switchIfEmpty { Mono.just(mutableListOf<ParticipantModel>()) }
	}

	private fun refreshGuest(
		existing: ParticipantModel,
		updatedGuest: ParticipantModel?,
		movement: MovementModel,
		currentUser: CurrentUserModel,
	): ParticipantModel {
		return existing.apply {
			firstName = updatedGuest?.firstName
			lastName = updatedGuest?.lastName
			birthday = updatedGuest?.birthday
			startAvailability = CustomDateTimeModel(movement.dateTime)
			update(currentUser, now())
		}
	}

	private fun saveGuests(
		guests: List<ParticipantModel>,
		movement: MovementModel,
		oldMovement: MovementModel,
	): Mono<MovementModel> {
		return participantPort.saveAllGuest(guests)
			.collectList()
			.map { saved ->
				movement.content = saved.map { p -> MovementContentModel(participant = p) }
				oldMovement
			}
	}

	private fun validateParticipants(
		projectId: UUID,
		oldMovement: MovementModel,
		newParticipantIds: List<UUID>,
		driverIds: List<UUID>
	): Mono<MovementModel> {
		return participantPort.findAllByIds(projectId, newParticipantIds, isVisible = null)
			.collectList()
			.handle { found, handle ->
				val error = participantsError(found, newParticipantIds, driverIds)
				if (error != null) handle.error(error) else handle.next(oldMovement)
			}
	}

	private fun participantsError(
		found: List<ParticipantModel>,
		requestedIds: List<UUID>,
		driverIds: List<UUID>,
	): RegistryException? = when {
		found.size != requestedIds.size ->
			RegistryException(NOT_FOUND, MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT)

		found.any(ParticipantModel::isNotUsable) ->
			RegistryException(NOT_FOUND, MOVEMENT_PARTICIPANTS_NOT_VISIBLE)

		found.any { p -> !p.birthday.isMajor(clock) && driverIds.contains(p.id) } ->
			RegistryException(UNPROCESSABLE_CONTENT, MOVEMENT_DRIVERS_NOT_MAJOR)

		else -> null
	}

	private fun Mono<MovementModel>.refreshParticipantsEffectiveDates(
		previousParticipantIds: List<UUID> = emptyList(),
	): Mono<MovementModel> = flatMap { movement ->
		participantPort.refreshEffectiveDates((previousParticipantIds + movement.participantIds()).distinct())
			.thenReturn(movement)
	}

	private fun validateVehicles(
		projectId: UUID,
		movement: MovementModel,
		newVehicleIds: List<UUID>
	): Mono<MovementModel> {
		return vehiclePort.findAllByIds(projectId, newVehicleIds, isVisible = null)
			.collectList()
			.handle { it, handle ->
				when {
					it.size != newVehicleIds.size -> handle.error(
						RegistryException(
							NOT_FOUND,
							MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_PROJECT,
						)
					)

					it.any { m -> m.isNotVisible() } -> handle.error(
						RegistryException(
							NOT_FOUND,
							MOVEMENT_VEHICLES_NOT_VISIBLE,
						)
					)

					else -> handle.next(movement)
				}
			}
	}

	private fun Mono<MovementModel>.validateUpdatableMovementFields(newMovement: MovementModel): Mono<MovementModel> =
		handle { it, handle ->
			when {
				it.type !== newMovement.type -> handle.error(
					RegistryException(
						UNPROCESSABLE_CONTENT,
						MOVEMENT_UPDATE_CHANGE_TYPE,
					)
				)

				it.contentType !== newMovement.contentType -> handle.error(
					RegistryException(
						UNPROCESSABLE_CONTENT,
						MOVEMENT_UPDATE_CHANGE_CONTENT_TYPE,
					)
				)

				it.atLeastOldGuestIfGuestsEntrance(newMovement) -> handle.error(
					RegistryException(
						UNPROCESSABLE_CONTENT,
						MOVEMENT_REMOVE_GUEST_CONTENT,
					)
				)

				else -> handle.next(it)
			}
		}

	private fun Mono<MovementModel>.validateMovementIsAlterable(errorMessage: String): Mono<MovementModel> =
		handle { it, handle ->
			when {
				it.isLastParticipantMovement() -> handle.error(
					RegistryException(
						UNPROCESSABLE_CONTENT,
						errorMessage,
					)
				)

				else -> handle.next(it)
			}
		}

	override fun disableMovementById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<MovementModel> {
		return findLockedMovement(projectId, id, isVisible = true)
			.validateMovementIsAlterable(MOVEMENT_CANNOT_BE_DISABLED)
			.updateVisibility(visibility = false)
			.updateMovement(currentUser)
			.refreshParticipantsEffectiveDates()
			.`as`(transactionalOperator::transactional)
	}

	override fun enableMovementById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<MovementModel> {
		return findLockedMovement(projectId, id, isVisible = false)
			.validateMovementIsAlterable(MOVEMENT_CANNOT_BE_ENABLED)
			.updateVisibility(visibility = true)
			.updateMovement(currentUser)
			.refreshParticipantsEffectiveDates()
			.`as`(transactionalOperator::transactional)
	}

	override fun deleteMovementById(projectId: UUID, id: UUID): Mono<Unit> {
		return findLockedMovement(projectId, id, isVisible = null)
			.validateMovementIsAlterable(MOVEMENT_CANNOT_BE_DELETED)
			.flatMap { deleteAndRefresh(id, it) }
			.`as`(transactionalOperator::transactional)
	}

	private fun findLockedMovement(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<MovementModel> {
		return findMovementById(projectId, id, isVisible)
			.flatMap { lockParticipants(it.participantIds()).thenReturn(it) }
			.flatMap { findMovementById(projectId, id, isVisible) }
	}

	private fun deleteAndRefresh(id: UUID, movement: MovementModel): Mono<Unit> {
		return port.deleteById(id)
			.then(participantPort.refreshEffectiveDates(movement.participantIds()))
	}

	override fun purgeMovementsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
		log.info("Purging movements older than {} and uncommented since {}", dateThreshold, dateThreshold)
		return port.findOlderThanAndUncommentedSince(dateThreshold)
			.purgeEach("movement", dryRun, port::deleteById)
	}
}
