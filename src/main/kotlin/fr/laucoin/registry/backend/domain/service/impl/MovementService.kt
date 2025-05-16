package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_COMMUNICATION_OUT_OF_MOVEMENT_DATETIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_REMOVE_GUEST_CONTENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_UPDATE_CHANGE_CONTENT_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_UPDATE_CHANGE_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.DEFINITIVE_DEPARTURE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
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
import fr.laucoin.registry.backend.domain.repository.IActivityModelRepository
import fr.laucoin.registry.backend.domain.repository.ICommunicationModelRepository
import fr.laucoin.registry.backend.domain.repository.IGroupModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.repository.IVehicleModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.domain.service.IProjectService
import java.util.Objects
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
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

@Service
class MovementService(
    private val projectService: IProjectService,
    private val repository: IMovementModelRepository,
    private val participantRepository: IParticipantModelRepository,
    private val vehicleRepository: IVehicleModelRepository,
    private val activityRepository: IActivityModelRepository,
    private val communicationRepository: ICommunicationModelRepository,
    private val groupRepository: IGroupModelRepository,
    private val transactionalOperator: TransactionalOperator,
    @Value("\${registry.feature.movement.searched.max-participant-result}")
    private val maxParticipantResult: Int,
    @Value("\${registry.feature.movement.searched.max-group-result}")
    private val maxGroupResult: Int,
    @Value("\${registry.feature.movement.searched.max-vehicle-result}")
    private val maxVehicleResult: Int,
    @Value("\${registry.feature.movement.searched.max-activity-result}")
    private val maxActivityResult: Int,
): IMovementService, GenericService() {
    override fun findMovementsPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>> {
        return repository.findPage(projectId, pageable, searchParams)
    }

    override fun findCurrentMovementsPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel
    ): Mono<PageModel<MovementModel>> {
        return repository.findCurrentPage(projectId, pageable, searchParams)
    }

    override fun findMovementsContent(
        projectId: UUID,
        movementIds: List<UUID>
    ): Flux<Pair<UUID, List<MovementContentModel>>> {
        return repository.findContent(projectId, movementIds)
    }

    override fun findCurrentMovementsContent(
        projectId: UUID,
        movementIds: List<UUID>
    ): Flux<Pair<UUID, List<MovementContentModel>>> {
        return repository.findCurrentContent(projectId, movementIds)
    }

    override fun findMovementById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<MovementModel> {
        return repository.findById(projectId, id, visibilitySearched)
            .notFoundIfEmpty(id)
    }

    override fun searchParticipantsAndGroupsByText(
        projectId: UUID,
        typeSearched: ParticipantTypeEnum,
        textSearched: String?
    ): Mono<Tuple2<List<ParticipantModel>, List<GroupModel>>> {
        return zip(
            participantRepository.findWithLimit(
                maxParticipantResult,
                projectId,
                searchParams = ParticipantSearchParamModel(
                    isMajor = null,
                    typeSearched,
                    visibilitySearched = true,
                    presenceSearched = true
                ).apply { this.textSearched = textSearched },
            ).collectList(),
            groupRepository.findWithLimit(
                maxGroupResult,
                projectId,
                GroupSearchParamModel(textSearched, visibilitySearched = true, presenceSearched = true),
            ).collectList().flatMap { groups ->
                groupRepository.findContent(
                    projectId,
                    groups.mapNotNull(GroupModel::id),
                    visibilitySearched = true,
                    availabilitySearched = true,
                )
                    .map {
                        groups.first { g -> g.id == it.first }.apply { members = it.second }
                    }.collectList()
            },
        )
    }

    override fun searchVehiclesByText(projectId: UUID, textSearched: String?): Flux<VehicleModel> {
        return vehicleRepository.findWithLimit(
            maxVehicleResult,
            projectId,
            VehicleSearchParamModel(visibilitySearched = true, availabilitySearched = true).apply { this.textSearched = textSearched },
        )
    }

    override fun searchReasonsByText(
        contentTypeSearched: ParticipantTypeEnum,
        typeSearched: MovementTypeEnum
    ): Flux<MovementReasonEnum> {
        return Flux.fromIterable(MovementReasonEnum.entries)
            .filter { it.type == typeSearched && contentTypeSearched == it.participantType }
    }

    override fun searchActivitiesByText(
        projectId: UUID,
        contentTypeSearched: ParticipantTypeEnum,
        textSearched: String?
    ): Flux<ActivityModel> {
        return if (contentTypeSearched === GUEST) Flux.empty()
        else activityRepository.findWithLimit(
            maxActivityResult,
            projectId,
            ActivitySearchParamModel(textSearched, visibilitySearched = true, availabilitySearched = true),
        )
    }

    override fun findMovementCommunicationsPage(
        projectId: UUID,
        id: UUID,
        pageable: PageableModel,
        searchParams: CommunicationSearchParamModel
    ): Mono<PageModel<CommunicationModel>> {
        return communicationRepository.findPageByMovementId(projectId, id, pageable, searchParams)
    }

    override fun findParticipantsStatus(projectId: UUID): Mono<ProjectStatusModel> {
        return zip(
            participantRepository.countAll(
                projectId,
                searchParams = ParticipantSearchParamModel(
                    textSearched = null,
                    isMajor = true,
                    typeSearched = REGISTERED,
                    statusSearched = PresenceStatusEnum.IN,
                    visibilitySearched = true,
                    dateTimeSearched = null
                )
            ),
            participantRepository.countAll(
                projectId,
                searchParams = ParticipantSearchParamModel(
                    textSearched = null,
                    isMajor = true,
                    typeSearched = REGISTERED,
                    statusSearched = PresenceStatusEnum.OUT,
                    visibilitySearched = true,
                    dateTimeSearched = null
                )
            ),
            participantRepository.countAll(
                projectId,
                ParticipantSearchParamModel(
                    textSearched = null,
                    isMajor = false,
                    typeSearched = REGISTERED,
                    statusSearched = PresenceStatusEnum.IN,
                    visibilitySearched = true,
                    dateTimeSearched = null
                )
            ),
            participantRepository.countAll(
                projectId,
                searchParams = ParticipantSearchParamModel(
                    textSearched = null,
                    isMajor = false,
                    typeSearched = REGISTERED,
                    statusSearched = PresenceStatusEnum.OUT,
                    visibilitySearched = true,
                    dateTimeSearched = null
                )
            ),
            participantRepository.countAll(
                projectId,
                searchParams = ParticipantSearchParamModel(
                    textSearched = null,
                    isMajor = null,
                    typeSearched = GUEST,
                    statusSearched = PresenceStatusEnum.IN,
                    visibilitySearched = true,
                    dateTimeSearched = null
                )
            )
        )
            .map { (registeredPresentAdult, registeredAbsentAdult, registeredPresentChild, registeredAbsentChild, guestPresent) ->
                ProjectStatusModel(
                    registered = ProjectStatusModel.ParticipantStatusModel(
                        registeredPresentChild,
                        registeredPresentAdult,
                        registeredAbsentChild,
                        registeredAbsentAdult,
                    ),
                    guests = guestPresent,
                )
            }
    }

    override fun findVehiclesStatus(projectId: UUID): Mono<VehicleStatusModel> {
        return zip(
            vehicleRepository.countAll(
                projectId,
                searchParams = VehicleSearchParamModel(
                    textSearched = null,
                    visibilitySearched = true,
                    statusSearched = PresenceStatusEnum.IN,
                    dateTimeSearched = null
                )
            ),
            vehicleRepository.countAll(
                projectId,
                searchParams = VehicleSearchParamModel(
                    textSearched = null,
                    visibilitySearched = true,
                    statusSearched = PresenceStatusEnum.OUT,
                    dateTimeSearched = null
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
            movement.project !!.id !!,
            CustomDateTimeModel(movement.dateTime),
            MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE,
        )
    }

    override fun createMovement(
        currentUser: CurrentUserModel,
        movement: MovementModel,
        newGuests: List<ParticipantModel>,
    ): Mono<MovementModel> {
        return validateMovementDate(movement)
            .flatMap { validateActivity(movement) }
            .flatMap { saveGuestsIfNecessary(currentUser, movement, movement, newGuests) }
            .flatMap {
                validateParticipants(
                    movement.project !!.id !!,
                    movement,
                    movement.content.mapNotNull { c -> c.participant !!.id }
                )
            }
            .flatMap {
                val newVehicleIds: List<UUID> = movement.content.mapNotNull { c -> c.vehicle?.id }
                if (newVehicleIds.isEmpty()) Mono.just(it)
                else validateVehicles(
                    movement.project !!.id !!,
                    movement,
                    newVehicleIds
                )
            }
            .flatMap {
                if (movement.reason === DEFINITIVE_DEPARTURE || (movement.type === OUT && movement.isGuestsMovement())) {
                    updateParticipantsEndAvailability(it)
                } else Mono.just(it)
            }
            .flatMap { repository.create(movement.apply { create(currentUser) }) }
            .`as`(transactionalOperator::transactional)
    }

    override fun updateMovementById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID,
        movement: MovementModel,
        newGuests: List<ParticipantModel>,
    ): Mono<MovementModel> {
        return validateMovementDate(movement)
            .flatMap { findMovementById(projectId, id, visibilitySearched = null) }
            .validateUpdatableMovementFields(movement)
            .flatMap { validateNoCommunicationConflict(movement, it) }
            .flatMap { validateActivity(movement, it) }
            .flatMap { saveGuestsIfNecessary(currentUser, movement, it, newGuests) }
            .flatMap {
                val newParticipantIds: List<UUID> = it.getNewContentParticipantIds(movement)
                if (newParticipantIds.isEmpty()) Mono.just(it)
                else validateParticipants(projectId, it, newParticipantIds)
            }
            .flatMap {
                val newVehicleIds: List<UUID> = it.getNewContentVehicleIds(movement)
                if (newVehicleIds.isEmpty()) Mono.just(it)
                else validateVehicles(projectId, it, newVehicleIds)
            }
            .map {
                it.apply {
                    it.dateTime = movement.dateTime
                    it.reason = movement.reason
                    it.activity = movement.activity
                    it.content = movement.content
                }
            }
            .flatMap {
                if (movement.reason === DEFINITIVE_DEPARTURE || (movement.type === OUT && movement.isGuestsMovement())) {
                    updateParticipantsEndAvailability(it)
                } else Mono.just(it)
            }
            .updateMovement(currentUser)
            .`as`(transactionalOperator::transactional)
    }

    private fun Mono<MovementModel>.updateMovement(currentUser: CurrentUserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }

    private fun validateActivity(movement: MovementModel, oldMovement: MovementModel? = null): Mono<MovementModel> {
        return if (Objects.isNull(movement.activity) || movement.activity?.id == oldMovement?.activity?.id) Mono.just(
            oldMovement ?: movement
        )
        else activityRepository.findById(movement.project !!.id !!, movement.activity !!.id !!, visibilitySearched = null)
            .switchIfEmpty { Mono.error(RegistryException(NOT_FOUND, MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT)) }
            .handle { it, handle ->
                if (it.isNotVisible()) handle.error(
                    RegistryException(
                        CONFLICT,
                        MOVEMENT_ACTIVITY_NOT_VISIBLE,
                    )
                )
                else handle.next(oldMovement ?: movement)
            }
    }

    private fun validateNoCommunicationConflict(movement: MovementModel, oldMovement: MovementModel): Mono<MovementModel> {
        return if (movement.dateTime.isAfter(oldMovement.dateTime)) {
            val params = CommunicationSearchParamModel(
                visibilitySearched = null,
                startDateTimeSearched = null,
                endDateTimeSearched = movement.dateTime,
            )
            communicationRepository.countAllByMovementId(movement.project !!.id !!, oldMovement.id !!, params)
                .handle { it, handle ->
                    if (it > 0L) handle.error(
                        RegistryException(
                            CONFLICT,
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

        val guestIdsToUpdate: List<UUID> = guests.mapNotNull { it.id }
        val guestsToCreate: List<ParticipantModel> = guests
            .filter { Objects.isNull(it.id) }
            .map {
                it.apply {
                    startAvailability = CustomDateTimeModel(movement.dateTime)
                    create(currentUser)
                }
            }

        return participantRepository.findAllByIds(movement.project !!.id !!, guestIdsToUpdate, visibilitySearched = null)
            .map {
                val updatedGuest = guests.find { g -> g.id == it.id }
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
                participantRepository.saveAllGuest(it)
                    .collectList()
                    .map { l ->
                        movement.content = l.map { p -> MovementContentModel(participant = p) }
                        oldMovement
                    }
            }
    }

    private fun validateParticipants(projectId: UUID, oldMovement: MovementModel, newParticipantIds: List<UUID>): Mono<MovementModel> {
        return participantRepository.findAllByIds(projectId, newParticipantIds, visibilitySearched = null)
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
                            CONFLICT,
                            MOVEMENT_PARTICIPANTS_NOT_VISIBLE,
                        )
                    )

                    else -> handle.next(oldMovement)
                }
            }
    }

    private fun updateParticipantsEndAvailability(movement: MovementModel): Mono<MovementModel> {
        return participantRepository.updateAllEndAvailability(
            movement.content.mapNotNull { it.participant?.id },
            CustomDateTimeModel(movement.dateTime)
        )
            .collectList()
            .map { movement }
    }

    private fun validateVehicles(projectId: UUID, movement: MovementModel, newVehicleIds: List<UUID>): Mono<MovementModel> {
        return vehicleRepository.findAllByIds(projectId, newVehicleIds, visibilitySearched = null)
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
                            CONFLICT,
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
                        CONFLICT,
                        MOVEMENT_UPDATE_CHANGE_TYPE,
                    )
                )

                it.contentType !== newMovement.contentType -> handle.error(
                    RegistryException(
                        CONFLICT,
                        MOVEMENT_UPDATE_CHANGE_CONTENT_TYPE,
                    )
                )

                it.atLeastOldGuestIfGuestsEntrance(newMovement) -> handle.error(
                    RegistryException(
                        CONFLICT,
                        MOVEMENT_REMOVE_GUEST_CONTENT,
                    )
                )

                else -> handle.next(it)
            }
        }

    override fun disableMovementById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<MovementModel> {
        return findMovementById(projectId, id, visibilitySearched = true)
            .updateVisibility(visibility = false)
            .updateMovement(currentUser)
    }

    override fun enableMovementById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<MovementModel> {
        return findMovementById(projectId, id, visibilitySearched = false)
            .updateVisibility(visibility = true)
            .updateMovement(currentUser)
    }

    override fun deleteMovementById(projectId: UUID, id: UUID): Mono<Void> {
        return findMovementById(projectId, id, visibilitySearched = null)
            .flatMap { repository.deleteById(id) }
    }
}
