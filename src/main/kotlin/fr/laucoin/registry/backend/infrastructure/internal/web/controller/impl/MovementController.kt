package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IMovementController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ParticipantMovementWriterDto
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
    private val movementParticipantsAndGroupsMapper: MovementParticipantsAndGroupsReaderDtoMapper,
    private val vehiclesMapper: VehicleReaderDtoMapper,
    private val writerMapper: ParticipantMovementWriterDtoMapper,
    private val guestMovementWriterMapper: GuestMovementWriterDtoMapper,
    private val guestWriterMapper: GuestWriterDtoMapper,
): IMovementController {
    private val similarity: JaroWinklerSimilarity = JaroWinklerSimilarity()

    override fun findMovements(
        locale: Locale,
        eventId: UUID,
        pageNumber: Int,
        pageSize: Int,
        visibilitySearched: Boolean?,
        typeSearched: MovementTypeEnum?,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?
    ): Mono<PageModel<MovementReaderDto>> {
        return service.findMovementsPage(
            eventId,
            PageableModel(pageNumber * pageSize, pageSize),
            MovementSearchParamModel(visibilitySearched, typeSearched, startDateTimeSearched, endDateTimeSearched)
        ).map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findMovementsContents(
        locale: Locale,
        eventId: UUID,
        movementIds: List<UUID>
    ): Flux<Pair<UUID, List<MovementContentReaderDto>>> {
        return service.findMovementsContent(eventId, movementIds)
            .map { Pair(it.first, it.second.map { content -> readerContentMapper.toDto(content, locale) }) }
    }

    override fun findMovementById(locale: Locale, eventId: UUID, id: UUID): Mono<MovementReaderDto> {
        return service.findMovementById(eventId, id, visibilitySearched = null)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun searchReasonsAndActivities(
        locale: Locale,
        eventId: UUID,
        typeSearched: MovementTypeEnum,
        contentTypeSearched: ParticipantTypeEnum,
        textSearched: String?,
    ): Flux<MovementReasonsReaderDto> {
        return service.searchActivities(eventId, contentTypeSearched, textSearched)
            .map { activityReasonReaderMapper.toDto(it, locale) }
            .mergeWith(searchReasons(locale, textSearched, typeSearched, contentTypeSearched))
    }

    private fun searchReasons(
        locale: Locale,
        textSearched: String?,
        typeSearched: MovementTypeEnum,
        contentTypeSearched: ParticipantTypeEnum,
    ): Flux<MovementReasonsReaderDto> {
        return service.searchReasons(contentTypeSearched, typeSearched)
            .map { reasonReaderMapper.toDto(it, locale) }
            .map { Pair(it, similarity.apply(it.label, textSearched ?: it.label)) }
            .filter { it.second > 0 }
            .map { it.first }
    }

    override fun searchParticipantsAndGroups(
        locale: Locale,
        eventId: UUID,
        contentTypeSearched: ParticipantTypeEnum,
        textSearched: String?
    ): Mono<MovementParticipantsAndGroupsReaderDto> {
        return service.searchParticipantsAndGroups(eventId, contentTypeSearched, textSearched)
            .map { Pair(it.t1, it.t2) }
            .map { movementParticipantsAndGroupsMapper.toDto(it, locale) }
    }

    override fun searchVehicles(
        locale: Locale,
        eventId: UUID,
        textSearched: String?
    ): Flux<VehicleReaderDto> {
        return service.searchVehicles(eventId, textSearched)
            .map { vehiclesMapper.toDto(it, locale) }
    }

    override fun createMovement(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        movement: ParticipantMovementWriterDto,
    ): Mono<MovementReaderDto> {
        return service.createMovement(currentUser, writerMapper.toModel(movement, eventId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateMovementById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
        movement: ParticipantMovementWriterDto
    ): Mono<MovementReaderDto> {
        return service.updateMovementById(currentUser, eventId, id, writerMapper.toModel(movement, eventId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun createGuestsMovement(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        movement: GuestMovementWriterDto
    ): Mono<MovementReaderDto> {
        return service.createMovement(
            currentUser,
            guestMovementWriterMapper.toModel(movement, eventId),
            guestWriterMapper.toModels(movement.guests ?: emptyList(), eventId),
        ).map { readerMapper.toDto(it, locale) }
    }

    override fun updateGuestsMovementById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
        movement: GuestMovementWriterDto
    ): Mono<MovementReaderDto> {
        return service.updateMovementById(
            currentUser,
            eventId,
            id,
            guestMovementWriterMapper.toModel(movement, eventId),
            guestWriterMapper.toModels(movement.guests ?: emptyList(), eventId),
        ).map { readerMapper.toDto(it, locale) }
    }

    override fun disableMovementById(currentUser: CurrentUserModel, locale: Locale, eventId: UUID, id: UUID): Mono<MovementReaderDto> {
        return service.disableMovementById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun enableMovementById(currentUser: CurrentUserModel, locale: Locale, eventId: UUID, id: UUID): Mono<MovementReaderDto> {
        return service.enableMovementById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteMovementById(eventId: UUID, id: UUID): Mono<Void> {
        return service.deleteMovementById(eventId, id)
    }
}
