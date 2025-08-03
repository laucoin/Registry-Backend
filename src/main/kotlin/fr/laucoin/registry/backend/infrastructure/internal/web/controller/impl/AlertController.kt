package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IAlertController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.AlertWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.AlertCreationWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.AlertWriterDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class AlertController(
    private val service: IAlertService,
    private val readerMapper: AlertReaderDtoMapper,
    private val communicationReaderMapper: CommunicationReaderDtoMapper,
    private val writerMapper: AlertWriterDtoMapper,
    private val creationWriterMapper: AlertCreationWriterDtoMapper,
): IAlertController {
    override fun findAlerts(
        locale: Locale,
        projectId: UUID,
        pageNumber: Int,
        pageSize: Int,
        textSearched: String?,
        visibilitySearched: Boolean?,
        statusSearched: AlertStatusEnum?,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?
    ): Mono<PageModel<AlertReaderDto>> {
        return service.findAlertsPage(
            projectId,
            PageableModel(pageNumber * pageSize, pageSize),
            AlertSearchParamModel(
                textSearched,
                visibilitySearched,
                statusSearched,
                startDateTimeSearched,
                endDateTimeSearched
            )
        ).map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findAlertById(
        locale: Locale,
        projectId: UUID,
        id: UUID
    ): Mono<AlertReaderDto> {
        return service.findAlertById(projectId, id, visibilitySearched = null)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun findAlertCommunications(
        locale: Locale,
        projectId: UUID,
        id: UUID,
        pageNumber: Int,
        pageSize: Int,
        textSearched: String?,
        visibilitySearched: Boolean?,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?
    ): Mono<PageModel<CommunicationReaderDto>> {
        return service.findAlertCommunicationsPage(
            projectId,
            id,
            PageableModel(pageNumber * pageSize, pageSize),
            CommunicationSearchParamModel(textSearched, visibilitySearched, startDateTimeSearched, endDateTimeSearched),
        ).map { communicationReaderMapper.toDtoPage(it, locale) }
    }

    override fun createAlert(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        alert: AlertCreationWriterDto
    ): Mono<AlertReaderDto> {
        return service.createAlert(currentUser, creationWriterMapper.toModel(alert, projectId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateAlertById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
        alert: AlertWriterDto
    ): Mono<AlertReaderDto> {
        return service.updateAlertById(currentUser, projectId, id, writerMapper.toModel(alert, projectId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateAlertStatusById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
        status: AlertStatusEnum
    ): Mono<AlertReaderDto> {
        return service.updateAlertStatusById(currentUser, projectId, id, status)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun disableAlertById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID
    ): Mono<AlertReaderDto> {
        return service.disableAlertById(currentUser, projectId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun enableAlertById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID
    ): Mono<AlertReaderDto> {
        return service.enableAlertById(currentUser, projectId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteAlertById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID
    ): Mono<Void> {
        return service.deleteAlertById(currentUser, projectId, id)
    }
}
