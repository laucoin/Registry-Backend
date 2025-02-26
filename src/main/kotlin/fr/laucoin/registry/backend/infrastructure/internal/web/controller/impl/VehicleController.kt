package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.UsableElementStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IVehicleController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.VehicleWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.VehicleWriterDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class VehicleController(
    private val service: IVehicleService,
    private val readerMapper: VehicleReaderDtoMapper,
    private val movementReaderMapper: MovementReaderDtoMapper,
    private val writerMapper: VehicleWriterDtoMapper,
): IVehicleController {
    override fun findVehicles(
        locale: Locale,
        eventId: UUID,
        pageNumber: Int,
        pageSize: Int,
        textSearched: String?,
        visibilitySearched: Boolean?,
        statusSearched: UsableElementStatusEnum?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<VehicleReaderDto>> {
        return service.findVehiclesPage(
            eventId,
            PageableModel(pageNumber * pageSize, pageSize),
            VehicleSearchParamModel(textSearched, visibilitySearched, statusSearched, dateTimeSearched),
        ).map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findVehicleById(locale: Locale, eventId: UUID, id: UUID): Mono<VehicleReaderDto> {
        return service.findVehicleById(eventId, id, visibilitySearched = null)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun findVehicleMovements(
        locale: Locale,
        eventId: UUID,
        id: UUID,
        pageNumber: Int,
        pageSize: Int,
        visibilitySearched: Boolean?,
        typeSearched: MovementTypeEnum?,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?
    ): Mono<PageModel<MovementReaderDto>> {
        return service.findVehicleMovementsPage(
            eventId,
            id,
            PageableModel(pageNumber * pageSize, pageSize),
            MovementSearchParamModel(visibilitySearched, typeSearched, startDateTimeSearched, endDateTimeSearched),
        ).map { movementReaderMapper.toDtoPage(it, locale) }
    }

    override fun createVehicle(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        vehicle: VehicleWriterDto
    ): Mono<VehicleReaderDto> {
        return service.createVehicle(currentUser, writerMapper.toModel(vehicle, eventId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateVehicleById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
        vehicle: VehicleWriterDto,
    ): Mono<VehicleReaderDto> {
        return service.updateVehicleById(currentUser, eventId, id, writerMapper.toModel(vehicle, eventId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun disableVehicleById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
    ): Mono<VehicleReaderDto> {
        return service.disableVehicleById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun enableVehicleById(
        currentUser: CurrentUserModel,
        locale: Locale,
        eventId: UUID,
        id: UUID,
    ): Mono<VehicleReaderDto> {
        return service.enableVehicleById(currentUser, eventId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteVehicleById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<Void> {
        return service.deleteVehicleById(currentUser, eventId, id)
    }
}
