package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
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
        projectId: UUID,
        pageNumber: Int,
        pageSize: Int,
        textSearched: String?,
        visibilitySearched: Boolean?,
        statusSearched: PresenceStatusEnum?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<VehicleReaderDto>> {
        return service.findVehiclesPage(
            projectId,
            PageableModel(pageNumber * pageSize, pageSize),
            VehicleSearchParamModel(textSearched, visibilitySearched, statusSearched, dateTimeSearched),
        ).map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findVehicleById(locale: Locale, projectId: UUID, id: UUID): Mono<VehicleReaderDto> {
        return service.findVehicleById(projectId, id, visibilitySearched = null)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun findVehicleMovements(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
        pageNumber: Int,
        pageSize: Int,
        visibilitySearched: Boolean?,
        linkedToActivity: Boolean?,
        typeSearched: MovementTypeEnum?,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?
    ): Mono<PageModel<MovementReaderDto>> {
        return service.findVehicleMovementsPage(
            projectId,
            id,
            PageableModel(pageNumber * pageSize, pageSize),
            MovementSearchParamModel(visibilitySearched, linkedToActivity, typeSearched, startDateTimeSearched, endDateTimeSearched),
        ).map { movementReaderMapper.toDtoPage(it, locale) }
    }

    override fun createVehicle(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        vehicle: VehicleWriterDto
    ): Mono<VehicleReaderDto> {
        return service.createVehicle(currentUser, writerMapper.toModel(vehicle, projectId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateVehicleById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
        vehicle: VehicleWriterDto,
    ): Mono<VehicleReaderDto> {
        return service.updateVehicleById(currentUser, projectId, id, writerMapper.toModel(vehicle, projectId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun disableVehicleById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
    ): Mono<VehicleReaderDto> {
        return service.disableVehicleById(currentUser, projectId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun enableVehicleById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
    ): Mono<VehicleReaderDto> {
        return service.enableVehicleById(currentUser, projectId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Void> {
        return service.deleteVehicleById(currentUser, projectId, id)
    }
}
