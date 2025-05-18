package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.ICommunicationController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.CommunicationWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.CommunicationWriterDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class CommunicationController(
    private val service: ICommunicationService,
    private val readerMapper: CommunicationReaderDtoMapper,
    private val readerMovementMapper: MovementReaderDtoMapper,
    private val writerMapper: CommunicationWriterDtoMapper,
): ICommunicationController {
    override fun findCommunications(
        locale: Locale,
        projectId: UUID,
        pageNumber: Int,
        pageSize: Int,
        textSearched: String?,
        visibilitySearched: Boolean?,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?
    ): Mono<PageModel<CommunicationReaderDto>> {
        return service.findCommunicationPage(
            projectId,
            PageableModel(pageNumber * pageSize, pageSize),
            CommunicationSearchParamModel(textSearched, visibilitySearched, startDateTimeSearched, endDateTimeSearched),
        ).map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findMovementsContents(
        locale: Locale,
        projectId: UUID,
        movementIds: List<UUID>
    ): Flux<Pair<UUID, List<CommunicationReaderDto>>> {
        return service.findCommunicationsByMovements(projectId, movementIds, null)
            .map { Pair(it.first, it.second.map { content -> readerMapper.toDto(content, locale) }) }
    }

    override fun findCommunicationById(
        locale: Locale,
        projectId: UUID,
        id: UUID
    ): Mono<CommunicationReaderDto> {
        return service.findCommunicationById(projectId, id, visibilitySearched = null)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun searchActivities(
        locale: Locale,
        projectId: UUID,
        textSearched: String?
    ): Flux<MovementReaderDto> {
        return service.searchOutMovementWithActivityByText(projectId, textSearched)
            .map { readerMovementMapper.toDto(it, locale) }
    }

    override fun createCommunication(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        communication: CommunicationWriterDto
    ): Mono<CommunicationReaderDto> {
        return service.createCommunication(currentUser, writerMapper.toModel(communication, projectId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateCommunicationById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
        communication: CommunicationWriterDto
    ): Mono<CommunicationReaderDto> {
        return service.updateCommunicationById(currentUser, projectId, id, writerMapper.toModel(communication, projectId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun disableCommunicationById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID
    ): Mono<CommunicationReaderDto> {
        return service.disableCommunicationById(currentUser, projectId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun enableCommunicationById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID
    ): Mono<CommunicationReaderDto> {
        return service.enableCommunicationById(currentUser, projectId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteCommunicationById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID
    ): Mono<Void> {
        return service.deleteCommunicationById(currentUser, projectId, id)
    }
}
