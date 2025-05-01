package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IMovementController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ParticipantMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementActivityReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementContentReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementParticipantsAndGroupsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.GuestMovementWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.GuestWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.ParticipantMovementWriterDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class MovementController(
    private val service: IMovementService,
    private val readerMapper: MovementReaderDtoMapper,
    private val readerContentMapper: MovementContentReaderDtoMapper,
    private val reasonReaderMapper: MovementReasonReaderDtoMapper,
    private val activityReasonReaderMapper: MovementActivityReasonReaderDtoMapper,
    private val communicationReaderMapper: CommunicationReaderDtoMapper,
    private val movementParticipantsAndGroupsMapper: MovementParticipantsAndGroupsReaderDtoMapper,
    private val vehiclesMapper: VehicleReaderDtoMapper,
    private val writerMapper: ParticipantMovementWriterDtoMapper,
    private val guestMovementWriterMapper: GuestMovementWriterDtoMapper,
    private val guestWriterMapper: GuestWriterDtoMapper,
): IMovementController {
    private val similarity: JaroWinklerSimilarity = JaroWinklerSimilarity()

    override fun findMovements(
        locale: Locale,
        projectId: UUID,
        pageNumber: Int,
        pageSize: Int,
        visibilitySearched: Boolean?,
        typeSearched: MovementTypeEnum?,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?
    ): Mono<PageModel<MovementReaderDto>> {
        return service.findMovementsPage(
            projectId,
            PageableModel(pageNumber * pageSize, pageSize),
            MovementSearchParamModel(visibilitySearched, typeSearched, startDateTimeSearched, endDateTimeSearched)
        ).map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findMovementsContents(
        locale: Locale,
        projectId: UUID,
        movementIds: List<UUID>
    ): Flux<Pair<UUID, List<MovementContentReaderDto>>> {
        return service.findMovementsContent(projectId, movementIds)
            .map { Pair(it.first, it.second.map { content -> readerContentMapper.toDto(content, locale) }) }
    }

    override fun findMovementById(locale: Locale, projectId: UUID, id: UUID): Mono<MovementReaderDto> {
        return service.findMovementById(projectId, id, visibilitySearched = null)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun searchReasonsAndActivities(
        locale: Locale,
        projectId: UUID,
        typeSearched: MovementTypeEnum,
        contentTypeSearched: ParticipantTypeEnum,
        textSearched: String?,
    ): Flux<MovementReasonsReaderDto> {
        return service.searchActivitiesByText(projectId, contentTypeSearched, textSearched)
            .map { activityReasonReaderMapper.toDto(it, locale) }
            .mergeWith(searchReasons(locale, textSearched, typeSearched, contentTypeSearched))
    }

    private fun searchReasons(
        locale: Locale,
        textSearched: String?,
        typeSearched: MovementTypeEnum,
        contentTypeSearched: ParticipantTypeEnum,
    ): Flux<MovementReasonsReaderDto> {
        return service.searchReasonsByText(contentTypeSearched, typeSearched)
            .map { reasonReaderMapper.toDto(it, locale) }
            .map { Pair(it, similarity.apply(it.label, textSearched ?: it.label)) }
            .filter { it.second > 0 }
            .map { it.first }
    }

    override fun searchParticipantsAndGroups(
        locale: Locale,
        projectId: UUID,
        contentTypeSearched: ParticipantTypeEnum,
        textSearched: String?
    ): Mono<MovementParticipantsAndGroupsReaderDto> {
        return service.searchParticipantsAndGroupsByText(projectId, contentTypeSearched, textSearched)
            .map { Pair(it.t1, it.t2) }
            .map { movementParticipantsAndGroupsMapper.toDto(it, locale) }
    }

    override fun searchVehicles(
        locale: Locale,
        projectId: UUID,
        textSearched: String?
    ): Flux<VehicleReaderDto> {
        return service.searchVehiclesByText(projectId, textSearched)
            .map { vehiclesMapper.toDto(it, locale) }
    }

    override fun findParticipantMovements(
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
        return service.findMovementCommunicationsPage(
            projectId,
            id,
            PageableModel(pageNumber * pageSize, pageSize),
            CommunicationSearchParamModel(
                textSearched,
                visibilitySearched,
                startDateTimeSearched,
                endDateTimeSearched
            )
        ).map { communicationReaderMapper.toDtoPage(it, locale) }
    }

    override fun createMovement(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        movement: ParticipantMovementWriterDto,
    ): Mono<MovementReaderDto> {
        return service.createMovement(currentUser, writerMapper.toModel(movement, projectId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateMovementById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
        movement: ParticipantMovementWriterDto
    ): Mono<MovementReaderDto> {
        return service.updateMovementById(currentUser, projectId, id, writerMapper.toModel(movement, projectId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun createGuestsMovement(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        movement: GuestMovementWriterDto
    ): Mono<MovementReaderDto> {
        return service.createMovement(
            currentUser,
            guestMovementWriterMapper.toModel(movement, projectId),
            guestWriterMapper.toModels(movement.guests ?: emptyList(), projectId),
        ).map { readerMapper.toDto(it, locale) }
    }

    override fun updateGuestsMovementById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
        movement: GuestMovementWriterDto
    ): Mono<MovementReaderDto> {
        return service.updateMovementById(
            currentUser,
            projectId,
            id,
            guestMovementWriterMapper.toModel(movement, projectId),
            guestWriterMapper.toModels(movement.guests ?: emptyList(), projectId),
        ).map { readerMapper.toDto(it, locale) }
    }

    override fun disableMovementById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID
    ): Mono<MovementReaderDto> {
        return service.disableMovementById(currentUser, projectId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun enableMovementById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID
    ): Mono<MovementReaderDto> {
        return service.enableMovementById(currentUser, projectId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteMovementById(projectId: UUID, id: UUID): Mono<Void> {
        return service.deleteMovementById(projectId, id)
    }
}
