package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IAlertService {
    fun findAlertPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: AlertSearchParamModel,
    ): Mono<PageModel<AlertModel>>

    fun findAlertById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<AlertModel>

    fun findAlertCommunicationsPage(
        projectId: UUID,
        id: UUID,
        pageable: PageableModel,
        searchParams: CommunicationSearchParamModel,
    ): Mono<PageModel<CommunicationModel>>

    fun createAlert(currentUser: CurrentUserModel, alert: AlertModel): Mono<AlertModel>
    fun updateAlertById(currentUser: CurrentUserModel, projectId: UUID, id: UUID, alert: AlertModel): Mono<AlertModel>
    fun updateAlertStatusById(currentUser: CurrentUserModel, projectId: UUID, id: UUID, status: AlertStatusEnum): Mono<AlertModel>
    fun disableAlertById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<AlertModel>
    fun enableAlertById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<AlertModel>
    fun deleteAlertById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Void>
    fun purgeAlertsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID>
}
