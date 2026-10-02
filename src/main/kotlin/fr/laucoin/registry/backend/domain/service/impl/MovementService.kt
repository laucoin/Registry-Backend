package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_DELETED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_DISABLED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_ENABLED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DRIVERS_NOT_MAJOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_VISIBLE
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
import java.time.LocalDate
import java.util.Objects
import java.util.UUID

/**
 * [IMovementService] implementation: validates a Movement's date, its Activity/Participants/
 * Vehicles/Drivers, creates or updates Guest content inline, keeps a Participant's end-availability in
 * sync with their last outing, and blocks structural changes (type, content type, alteration) on a
 * still-current Movement, then delegates persistence to [IMovementPort] and cross-resource ports.
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
	@param:Value($$"${registry.feature.movement.searched.max-participant-result}")
	private val maxParticipantResult: Int,
	@param:Value($$"${registry.feature.movement.searched.max-group-result}")
	private val maxGroupResult: Int,
	@param:Value($$"${registry.feature.movement.searched.max-vehicle-result}")
	private val maxVehicleResult: Int,
	@param:Value($$"${registry.feature.movement.searched.max-activity-result}")
	private val maxActivityResult: Int,
) : IMovementService, GenericService() {
	override fun findMovementsPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>> {
		return port.findPage(projectId, pageable, searchParams)
	}

	override fun findCurrentMovementsPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return port.findCurrentPage(projectId, pageable, searchParams)
	}

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
				).map {
					groups.first { group -> group.id == it.first }.apply { members = it.second }
				}.collectList()
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
			participantPort.countAll(
				projectId,
				searchParams = ParticipantSearchParamModel(
					query = null,
					isMajor = true,
					type = REGISTERED,
					status = PresenceStatusEnum.IN,
					isVisible = true,
					dateTime = null
				)
			),
			participantPort.countAll(
				projectId,
				searchParams = ParticipantSearchParamModel(
					query = null,
					isMajor = true,
					type = REGISTERED,
					status = PresenceStatusEnum.OUT,
					isVisible = true,
					dateTime = null
				)
			),
			participantPort.countAll(
				projectId,
				ParticipantSearchParamModel(
					query = null,
					isMajor = false,
					type = REGISTERED,
					status = PresenceStatusEnum.IN,
					isVisible = true,
					dateTime = null
				)
			),
			participantPort.countAll(
				projectId,
				searchParams = ParticipantSearchParamModel(
					query = null,
					isMajor = false,
					type = REGISTERED,
					status = PresenceStatusEnum.OUT,
					isVisible = true,
					dateTime = null
				)
			),
			participantPort.countAll(
				projectId,
				searchParams = ParticipantSearchParamModel(
					query = null,
					isMajor = null,
					type = GUEST,
					status = PresenceStatusEnum.IN,
					isVisible = true,
					dateTime = null
				)
			)
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
				)
			}
	}

	override fun findVehiclesStatus(projectId: UUID): Mono<VehicleStatusModel> {
		return zip(
			vehiclePort.countAll(
				projectId,
				searchParams = VehicleSearchParamModel(
					query = null,
					isVisible = true,
					status = PresenceStatusEnum.IN,
					dateTime = null
				)
			),
			vehiclePort.countAll(
				projectId,
				searchParams = VehicleSearchParamModel(
					query = null,
					isVisible = true,
					status = PresenceStatusEnum.OUT,
					dateTime = null
				)
			)
		)
			.map { (vehiclePresent, vehicleAbsent) ->
				VehicleStatusModel(
					present = vehiclePresent,
					absent = vehicleAbsent
				)
			}
	}

	private fun validateMovementDate(movement: MovementModel): Mono<UUID> {
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
		return Mono.zip(
			validateMovementDate(movement),
			validateActivity(movement),
			saveGuestsIfNecessary(currentUser, movement, movement, newGuests),
		)
			.flatMap {
				Mono.zip(
					validateParticipantsIfAny(
						movement.projectId!!,
						movement,
						movement.content.mapNotNull { content -> content.participant!!.id },
						movement.content.filter { content -> Objects.nonNull(content.vehicle) }
							.mapNotNull { content -> content.participant!!.id },
					),
					validateVehiclesIfAny(
						movement.projectId!!,
						movement,
						movement.content.mapNotNull { content -> content.vehicle?.id },
					),
				)
			}
			.flatMap {
				if (movement.isLastParticipantMovement()) updateParticipantsEndAvailability(movement)
				else Mono.just(movement)
			}
			.flatMap { port.create(movement.apply { create(currentUser) }) }
			.`as`(transactionalOperator::transactional)
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
		port.update(it.apply { update(currentUser) })
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

	private fun saveGuestsIfNecessary(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		oldMovement: MovementModel,
		guests: List<ParticipantModel>,
	): Mono<MovementModel> {
		if (movement.type !== IN || guests.isEmpty()) return Mono.just(oldMovement)

		val guestIdsToUpdate: List<UUID> = guests.mapNotNull { it.id }
		val guestsToCreate: List<ParticipantModel> = guests
			.filter { Objects.isNull(it.id) }
			.map {
				it.apply {
					startAvailability = CustomDateTimeModel(movement.dateTime)
					create(currentUser)
				}
			}

		return participantPort.findAllByIds(movement.projectId!!, guestIdsToUpdate, isVisible = null)
			.map {
				val updatedGuest = guests.find { guest -> guest.id == it.id }
				it.apply {
					firstName = updatedGuest?.firstName
					lastName = updatedGuest?.lastName
					birthday = updatedGuest?.birthday
					startAvailability = CustomDateTimeModel(movement.dateTime)
					update(currentUser)
				}
			}
			.collectList()
			.handle { it, handle ->
				if (it.size != guestIdsToUpdate.size) handle.error(
					RegistryException(
						NOT_FOUND,
						MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT,
					)
				)
				else handle.next(it)
			}
			.switchIfEmpty { Mono.just(emptyList<ParticipantModel>()) }
			.map {
				it.addAll(guestsToCreate)
				it
			}
			.flatMap {
				participantPort.saveAllGuest(it)
					.collectList()
					.map { participants ->
						movement.content = participants.map { participant -> MovementContentModel(participant = participant) }
						oldMovement
					}
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
			.handle { it, handle ->
				when {
					it.size != newParticipantIds.size -> handle.error(
						RegistryException(
							NOT_FOUND,
							MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT,
						)
					)

					it.any(ParticipantModel::isNotUsable) -> handle.error(
						RegistryException(
							NOT_FOUND,
							MOVEMENT_PARTICIPANTS_NOT_VISIBLE,
						)
					)

					it.any { participant -> !participant.birthday.isMajor() && driverIds.contains(participant.id) } -> handle.error(
						RegistryException(
							UNPROCESSABLE_CONTENT,
							MOVEMENT_DRIVERS_NOT_MAJOR,
						)
					)

					else -> handle.next(oldMovement)
				}
			}
	}

	private fun updateParticipantsEndAvailability(movement: MovementModel): Mono<MovementModel> {
		return participantPort.updateAllEndAvailability(
			movement.content.mapNotNull { it.participant?.id },
			CustomDateTimeModel(movement.dateTime)
		)
			.collectList()
			.map { movement }
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

					it.any { vehicle -> vehicle.isNotVisible() } -> handle.error(
						RegistryException(
							NOT_FOUND,
							MOVEMENT_VEHICLES_NOT_VISIBLE,
						)
					)

					else -> handle.next(movement)
				}
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
		return findMovementById(projectId, id, isVisible = true)
			.validateMovementIsAlterable(MOVEMENT_CANNOT_BE_DISABLED)
			.updateVisibility(isVisible = false)
			.updateMovement(currentUser)
	}

	override fun enableMovementById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<MovementModel> {
		return findMovementById(projectId, id, isVisible = false)
			.validateMovementIsAlterable(MOVEMENT_CANNOT_BE_ENABLED)
			.updateVisibility(isVisible = true)
			.updateMovement(currentUser)
	}

	override fun deleteMovementById(projectId: UUID, id: UUID): Mono<Unit> {
		return findMovementById(projectId, id, isVisible = null)
			.validateMovementIsAlterable(MOVEMENT_CANNOT_BE_DELETED)
			.flatMap { port.deleteById(id) }
	}

	override fun purgeMovementsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
		log.info("Purging movements older than {} and uncommented since {}", dateThreshold, dateThreshold)
		return port.findOlderThanAndUncommentedSince(dateThreshold)
			.flatMap({
				if (dryRun) {
					log.info("[Dry run] movement {} would be deleted", it)
					Mono.just(it)
				} else {
					log.info("Purging movement {}", it)
					port.deleteById(it).thenReturn(it)
						.doOnNext { purgedId -> log.info("Movement {} was deleted", purgedId) }
						.doOnError { error -> log.error("Failed to purge movement {}", it, error) }
				}
			}, PURGE_DELETE_CONCURRENCY)
	}
}
