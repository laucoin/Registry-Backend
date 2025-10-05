package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IVehicleV1Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.VehicleWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.VehicleWriterDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class VehicleV1Controller(
	private val service: IVehicleService,
	private val readerMapper: VehicleReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val writerMapper: VehicleWriterDtoMapper,
): IVehicleV1Controller {
	override fun findVehicles(
		projectId: UUID,
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		visibilitySearched: Boolean?,
		statusSearched: PresenceStatusEnum?,
		dateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<VehicleReaderDto>> {
		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = VehicleSearchParamModel(
			textSearched, visibilitySearched, statusSearched, dateTimeSearched
		)

		return service.findVehiclesPage(projectId, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findVehicleById(projectId: UUID, id: UUID): Mono<VehicleReaderDto> {
		return service.findVehicleById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun findVehicleMovements(
		currentUser: CurrentUserModel,
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
		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = MovementSearchParamModel(
			visibilitySearched,
			linkedToActivity,
			typeSearched,
			startDateTimeSearched,
			endDateTimeSearched
		)

		return service.findVehicleMovementsPage(projectId, id, pageable, searchParams)
			.map(movementReaderMapper::toDtoPage)
	}

	override fun createVehicle(
		currentUser: CurrentUserModel,
		projectId: UUID,
		vehicle: VehicleWriterDto
	): Mono<VehicleReaderDto> {
		val vehicleModel = writerMapper.toModel(vehicle, projectId)
		return service.createVehicle(currentUser, vehicleModel).map(readerMapper::toDto)
	}

	override fun updateVehicleById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		vehicle: VehicleWriterDto,
	): Mono<VehicleReaderDto> {
		val vehicleModel = writerMapper.toModel(vehicle, projectId)
		return service.updateVehicleById(currentUser, projectId, id, vehicleModel).map(readerMapper::toDto)
	}

	override fun disableVehicleById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<VehicleReaderDto> {
		return service.disableVehicleById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun enableVehicleById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<VehicleReaderDto> {
		return service.enableVehicleById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun deleteVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteVehicleById(currentUser, projectId, id)
	}
}
