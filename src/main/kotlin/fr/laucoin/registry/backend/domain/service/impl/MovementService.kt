package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_EVENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_EVENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_EVENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
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
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IActivityModelRepository
import fr.laucoin.registry.backend.domain.repository.IGroupModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.repository.IVehicleModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IMovementService
import java.util.Objects
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.zip
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.util.function.Tuple2

@Service
class MovementService(
    private val eventService: IEventService,
    private val repository: IMovementModelRepository,
    private val participantRepository: IParticipantModelRepository,
    private val vehicleRepository: IVehicleModelRepository,
    private val activityRepository: IActivityModelRepository,
    private val groupRepository: IGroupModelRepository,
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
        eventId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>> {
        return repository.findPage(eventId, pageable, searchParams)
    }

    override fun findMovementsContent(
        eventId: UUID,
        movementIds: List<UUID>
    ): Flux<Pair<UUID, List<MovementContentModel>>> {
        return repository.findContent(eventId, movementIds)
    }

    override fun findMovementById(eventId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<MovementModel> {
        return repository.findById(eventId, id, visibilitySearched)
            .notFoundIfEmpty(id)
    }

    override fun searchParticipantsAndGroups(
        eventId: UUID,
        textSearched: String?
    ): Mono<Tuple2<List<ParticipantModel>, List<GroupModel>>> {
        return zip(
            participantRepository.findWithLimit(
                maxParticipantResult,
                eventId,
                ParticipantSearchParamModel(textSearched, visibilitySearched = true, presenceSearched = true),
            ).collectList(),
            groupRepository.findWithLimit(
                maxGroupResult,
                eventId,
                GroupSearchParamModel(textSearched, visibilitySearched = true, presenceSearched = true),
            ).collectList().flatMap { groups ->
                groupRepository.findContent(eventId, groups.mapNotNull(GroupModel::id))
                    .map {
                        groups.first { g -> g.id == it.first }.apply { members = it.second }
                    }.collectList()
            },
        )
    }

    override fun searchVehicles(eventId: UUID, textSearched: String?): Flux<VehicleModel> {
        return vehicleRepository.findWithLimit(
            maxVehicleResult,
            eventId,
            VehicleSearchParamModel(textSearched, visibilitySearched = true, availabilitySearched = true),
        )
    }

    override fun searchActivities(eventId: UUID, textSearched: String?): Flux<ActivityModel> {
        return activityRepository.findWithLimit(
            maxActivityResult,
            eventId,
            ActivitySearchParamModel(textSearched, visibilitySearched = true, availabilitySearched = true),
        )
    }

    override fun createMovement(currentUser: CurrentUserModel, movement: MovementModel): Mono<MovementModel> {
        return eventService.validateDateTime(
            movement.event !!.id !!,
            CustomDateTimeModel(movement.dateTime.toLocalDate(), movement.dateTime.toLocalTime()),
            MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap {
                if (Objects.isNull(movement.activity)) Mono.just(movement)
                else validateActivity(
                    movement.event !!.id !!,
                    movement,
                    movement.activity !!.id !!,
                )
            }
            .flatMap {
                validateParticipants(
                    movement.event !!.id !!,
                    movement,
                    movement.content.mapNotNull { c -> c.participant !!.id })
            }
            .flatMap {
                val newVehicleIds: List<UUID> = movement.content.mapNotNull { c -> c.vehicle?.id }
                if (newVehicleIds.isEmpty()) Mono.just(it)
                else validateVehicles(
                    movement.event !!.id !!,
                    movement,
                    newVehicleIds
                )
            }
            .flatMap { repository.create(movement.apply { create(currentUser) }) }
    }

    override fun updateMovementById(
        currentUser: CurrentUserModel,
        eventId: UUID,
        id: UUID,
        movement: MovementModel
    ): Mono<MovementModel> {
        return eventService.validateDateTime(
            movement.event !!.id !!,
            CustomDateTimeModel(movement.dateTime.toLocalDate(), movement.dateTime.toLocalTime()),
            MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap { findMovementById(eventId, id, visibilitySearched = null) }
            .flatMap {
                if (Objects.isNull(movement.activity) || it.activity?.id === movement.activity !!.id) Mono.just(it)
                else validateActivity(
                    movement.event !!.id !!,
                    movement,
                    movement.activity !!.id !!,
                )
            }
            .flatMap {
                val newParticipantIds: List<UUID> = it.getNewContentParticipantIds(movement)
                if (newParticipantIds.isEmpty()) Mono.just(it)
                else validateParticipants(eventId, it, newParticipantIds)
            }
            .flatMap {
                val newVehicleIds: List<UUID> = it.getNewContentVehicleIds(movement)
                if (newVehicleIds.isEmpty()) Mono.just(it)
                else validateVehicles(eventId, it, newVehicleIds)
            }
            .map {
                it.apply {
                    it.dateTime = movement.dateTime
                    it.activity = movement.activity
                    it.content = movement.content
                }
            }
            .updateMovement(currentUser)
    }

    private fun Mono<MovementModel>.updateMovement(currentUser: CurrentUserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }

    private fun validateParticipants(eventId: UUID, movement: MovementModel, newParticipantIds: List<UUID>): Mono<MovementModel> {
        return participantRepository.findAllByIds(eventId, newParticipantIds, visibilitySearched = null)
            .collectList()
            .handle { it, handle ->
                when {
                    it.size != newParticipantIds.size -> handle.error(
                        RegistryException(
                            NOT_FOUND,
                            MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_EVENT,
                        )
                    )

                    it.any(ParticipantModel::isNotUsable) -> handle.error(
                        RegistryException(
                            CONFLICT,
                            MOVEMENT_PARTICIPANTS_NOT_VISIBLE,
                        )
                    )

                    else -> handle.next(movement)
                }
            }
    }

    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_OPTION_VEHICLE')")
    private fun validateVehicles(eventId: UUID, movement: MovementModel, newVehicleIds: List<UUID>): Mono<MovementModel> {
        return vehicleRepository.findAllByIds(eventId, newVehicleIds, visibilitySearched = null)
            .collectList()
            .handle { it, handle ->
                when {
                    it.size != newVehicleIds.size -> handle.error(
                        RegistryException(
                            NOT_FOUND,
                            MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_EVENT,
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

    @PreAuthorize("hasPermission(#eventId, '$REGISTRY_EVENT_OPTION_ACTIVITY')")
    private fun validateActivity(eventId: UUID, movement: MovementModel, activityId: UUID): Mono<MovementModel> {
        return activityRepository.findById(eventId, activityId, visibilitySearched = null)
            .switchIfEmpty { Mono.error(RegistryException(NOT_FOUND, MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_EVENT)) }
            .handle { it, handle ->
                if (it.isNotVisible()) handle.error(
                    RegistryException(
                        CONFLICT,
                        MOVEMENT_ACTIVITY_NOT_VISIBLE,
                    )
                )
                else handle.next(movement)
            }
    }

    override fun disableMovementById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<MovementModel> {
        return findMovementById(eventId, id, visibilitySearched = true)
            .updateVisibility(visibility = false)
            .updateMovement(currentUser)
    }

    override fun enableMovementById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<MovementModel> {
        return findMovementById(eventId, id, visibilitySearched = false)
            .updateVisibility(visibility = true)
            .updateMovement(currentUser)
    }

    override fun deleteMovementById(eventId: UUID, id: UUID): Mono<Void> {
        return findMovementById(eventId, id, visibilitySearched = null)
            .flatMap { repository.deleteById(id) }
    }
}
