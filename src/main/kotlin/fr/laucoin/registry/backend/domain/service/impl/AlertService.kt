package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_COMMUNICATION_OUT_OF_ALERT_DATETIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_DELETE_HAS_COMMUNICATION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_STATUS_IS_NOT_UPDATABLE
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IAlertModelRepository
import fr.laucoin.registry.backend.domain.repository.ICommunicationModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.domain.service.IProjectService
import java.time.LocalDate
import java.util.UUID
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class AlertService(
    private val projectService: IProjectService,
    private val repository: IAlertModelRepository,
    private val communicationRepository: ICommunicationModelRepository,
    private val transactionalOperator: TransactionalOperator,
): IAlertService, GenericService() {
    override fun findAlertsPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: AlertSearchParamModel
    ): Mono<PageModel<AlertModel>> {
        return repository.findPage(projectId, pageable, searchParams)
    }

    override fun findAlertById(
        projectId: UUID,
        id: UUID,
        visibilitySearched: Boolean?
    ): Mono<AlertModel> {
        return repository.findById(projectId, id, visibilitySearched)
    }

    override fun findAlertCommunicationsPage(
        projectId: UUID,
        id: UUID,
        pageable: PageableModel,
        searchParams: CommunicationSearchParamModel
    ): Mono<PageModel<CommunicationModel>> {
        return communicationRepository.findPageByAlertId(projectId, id, pageable, searchParams)
    }

    private fun validateAlertDateWithProjectDates(alert: AlertModel): Mono<UUID> {
        return projectService.validateDateTime(
            alert.project !!.id !!,
            CustomDateTimeModel(alert.dateTime),
            ALERT_DATETIME_OUT_OF_PROJECT_DATE_RANGE,
        )
    }

    override fun createAlert(
        currentUser: CurrentUserModel,
        alert: AlertModel
    ): Mono<AlertModel> {
        return validateAlertDateWithProjectDates(alert)
            .flatMap { repository.create(alert) }
            .flatMap { newAlert ->
                val initialCommunication = alert.communications !!.first().apply { this.alert = newAlert }
                communicationRepository.create(initialCommunication)
                    .map { createdCommunication ->
                        newAlert.communications = listOf(createdCommunication)
                        newAlert
                    }
            }
            .`as`(transactionalOperator::transactional)
    }

    override fun updateAlertById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID,
        alert: AlertModel
    ): Mono<AlertModel> {
        return validateAlertDateWithProjectDates(alert)
            .flatMap { findAlertById(projectId, id, visibilitySearched = null) }
            .handle { it, handle ->
                if (it.status !== IN_PROGRESS) {
                    log.warn("Only {} alert can be update", IN_PROGRESS)
                    handle.error(RegistryException(UNPROCESSABLE_ENTITY, ALERT_STATUS_IS_NOT_UPDATABLE))
                } else handle.next(it)
            }
            .validateAlertDateWithLinkedCommunications(alert)
            .map {
                it.apply {
                    this.title = alert.title
                    this.dateTime = alert.dateTime
                }
            }
            .updateAlert(currentUser)
    }

    override fun updateAlertStatusById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID,
        status: AlertStatusEnum
    ): Mono<AlertModel> {
        return findAlertById(projectId, id, visibilitySearched = null)
            .map {
                it.apply {
                    this.status = status
                }
            }
            .updateAlert(currentUser)
    }

    private fun Mono<AlertModel>.validateAlertDateWithLinkedCommunications(newAlert: AlertModel): Mono<AlertModel> =
        flatMap { oldAlert ->
            communicationRepository.countAllByAlertId(
                oldAlert.project !!.id !!,
                oldAlert.id !!,
                CommunicationSearchParamModel(
                    textSearched = null,
                    visibilitySearched = null,
                    startDateTimeSearched = null,
                    endDateTimeSearched = newAlert.dateTime,
                )
            ).handle { it, handle ->
                if (it >= 0) {
                    val exception = RegistryException(
                        UNPROCESSABLE_ENTITY,
                        ALERT_COMMUNICATION_OUT_OF_ALERT_DATETIME,
                        arrayListOf(it)
                    )
                    log.warn("Existing communications are out the alert date", exception)
                    handle.error(exception)
                } else handle.next(oldAlert)
            }
        }

    private fun Mono<AlertModel>.updateAlert(currentUser: CurrentUserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }

    override fun disableAlertById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID
    ): Mono<AlertModel> {
        return findAlertById(projectId, id, visibilitySearched = true)
            .updateVisibility(visibility = false)
            .updateAlert(currentUser)
    }

    override fun enableAlertById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID
    ): Mono<AlertModel> {
        return findAlertById(projectId, id, visibilitySearched = false)
            .updateVisibility(visibility = true)
            .updateAlert(currentUser)
    }

    override fun deleteAlertById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID
    ): Mono<Void> {
        return findAlertById(projectId, id, visibilitySearched = null)
            .validateHasNoMovementLinked(ALERT_DELETE_HAS_COMMUNICATION)
            .flatMap { repository.deleteById(it.id !!) }
    }

    override fun purgeAlertsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
        log.info("Purging alerts older than {} and uncommented since {}", dateThreshold, dateThreshold)
        return repository.findOlderThanAndUncommentedSince(dateThreshold)
            .flatMap {
                if (dryRun) {
                    log.info("[Dry run] alert {} would be deleted", it)
                    Mono.just(it)
                } else {
                    log.info("Purging alert {}", it)
                    repository.deleteById(it).thenReturn(it)
                        .doOnNext { e -> log.info("{} alert was deleted", e) }
                        .doOnError { err -> log.error("Failed to purge alert", err) }
                }
            }
    }

    private fun Mono<AlertModel>.validateHasNoMovementLinked(error: String) = flatMap { oldAlert ->
        communicationRepository.countAllByAlertId(
            oldAlert.project !!.id !!,
            oldAlert.id !!,
            CommunicationSearchParamModel(),
        ).handle { it, handle ->
            if (it > 0) {
                log.warn("The alert {} already linked to communication(s)", oldAlert.id)
                handle.error(RegistryException(CONFLICT, error))
            } else handle.next(oldAlert)
        }
    }
}
