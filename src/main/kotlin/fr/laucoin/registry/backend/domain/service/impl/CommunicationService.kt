package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_CONTENT_TYPE_NOT_REGISTERED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_IS_AFTER_COMMUNICATION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_TYPE_NOT_OUT
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.ICommunicationModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.domain.service.IProjectService
import java.util.Objects
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class CommunicationService(
    private val projectService: IProjectService,
    private val repository: ICommunicationModelRepository,
    private val movementRepository: IMovementModelRepository,
    @Value("\${registry.feature.communication.searched.max-activity-result}")
    private val maxActivityResult: Int,
): ICommunicationService, GenericService() {
    override fun findCommunicationPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: CommunicationSearchParamModel
    ): Mono<PageModel<CommunicationModel>> {
        return repository.findPage(projectId, pageable, searchParams)
    }

    override fun findCommunicationById(
        projectId: UUID,
        id: UUID,
        visibilitySearched: Boolean?
    ): Mono<CommunicationModel> {
        return repository.findById(projectId, id, visibilitySearched)
            .notFoundIfEmpty(id)
    }

    override fun searchOutMovementWithActivityByText(
        projectId: UUID,
        textSearched: String?
    ): Flux<MovementModel> {
        return movementRepository.findActivityWithLimit(
            maxActivityResult,
            projectId,
            ActivitySearchParamModel(
                textSearched = textSearched,
                visibilitySearched = true,
                availabilitySearched = true,
                dateTimeSearched = null,
            )
        )
    }

    override fun createCommunication(
        currentUser: CurrentUserModel,
        communication: CommunicationModel
    ): Mono<CommunicationModel> {
        return projectService.validateDateTime(
            communication.project !!.id !!,
            CustomDateTimeModel(communication.dateTime),
            COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE,
        )
            .flatMap { validateNoMovementConflict(communication) }
            .flatMap { repository.create(communication.apply { create(currentUser) }) }
    }

    override fun updateCommunicationById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID,
        communication: CommunicationModel
    ): Mono<CommunicationModel> {
        return projectService.validateDateTime(
            communication.project !!.id !!,
            CustomDateTimeModel(communication.dateTime),
            COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE,
        )
            .flatMap { findCommunicationById(projectId, id, visibilitySearched = null) }
            .flatMap { validateNoMovementConflict(communication, it) }
            .map {
                it.apply {
                    dateTime = communication.dateTime
                    movement = communication.movement
                    message = communication.message
                }
            }
            .updateCommunication(currentUser)
    }

    override fun disableCommunicationById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID
    ): Mono<CommunicationModel> {
        return findCommunicationById(projectId, id, visibilitySearched = true)
            .updateVisibility(visibility = false)
            .updateCommunication(currentUser)
    }

    override fun enableCommunicationById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID
    ): Mono<CommunicationModel> {
        return findCommunicationById(projectId, id, visibilitySearched = false)
            .updateVisibility(visibility = true)
            .updateCommunication(currentUser)
    }

    override fun deleteCommunicationById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID
    ): Mono<Void> {
        return findCommunicationById(projectId, id, visibilitySearched = null)
            .flatMap { repository.deleteById(it.id !!) }
    }

    private fun Mono<CommunicationModel>.updateCommunication(currentUser: CurrentUserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }

    private fun validateNoMovementConflict(
        communication: CommunicationModel,
        oldCommunication: CommunicationModel? = null
    ): Mono<CommunicationModel> {
        return if (Objects.isNull(communication.movement) || communication.movement?.id == oldCommunication?.movement?.id) Mono.just(
            oldCommunication ?: communication
        )
        else movementRepository.findById(communication.project !!.id !!, communication.movement !!.id !!, visibilitySearched = null)
            .switchIfEmpty { Mono.error(RegistryException(NOT_FOUND, COMMUNICATION_MOVEMENT_NOT_FOUND_IN_MOVEMENT_PROJECT)) }
            .handle { it, handle ->
                when {
                    it.isNotVisible() -> handle.error(
                        RegistryException(
                            CONFLICT,
                            COMMUNICATION_MOVEMENT_NOT_VISIBLE,
                        )
                    )

                    it.dateTime.isAfter(communication.dateTime) -> handle.error(
                        RegistryException(
                            CONFLICT,
                            COMMUNICATION_MOVEMENT_IS_AFTER_COMMUNICATION,
                        )
                    )

                    it.contentType !== REGISTERED -> handle.error(
                        RegistryException(
                            CONFLICT,
                            COMMUNICATION_MOVEMENT_CONTENT_TYPE_NOT_REGISTERED,
                        )
                    )

                    it.type !== OUT -> handle.error(
                        RegistryException(
                            CONFLICT,
                            COMMUNICATION_MOVEMENT_TYPE_NOT_OUT,
                        )
                    )

                    else -> handle.next(oldCommunication ?: communication)
                }
            }
    }
}
