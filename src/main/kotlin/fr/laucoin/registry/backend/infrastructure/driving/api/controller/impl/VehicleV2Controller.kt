package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.service.IConcurrencyService
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IVehicleV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.VehicleWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.expectedLastEditions
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocationExt.created
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.VehicleWriterDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Purpose: REST adapter of the vehicle endpoints.
 * Scope: Maps request parameters and DTOs, and delegates to the vehicle use cases.
 * Limits: Holds no business rule; authorization is declared on the contract.
 */
@RestController
class VehicleV2Controller(
	private val service: IVehicleService,
	private val concurrencyService: IConcurrencyService,
	private val readerMapper: VehicleReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val writerMapper: VehicleWriterDtoMapper,
): IVehicleV2Controller {
	override fun findVehicles(
		projectId: UUID,
		page: Int,
		size: Int,
		query: String?,
		isVisible: Boolean?,
		status: PresenceStatusEnum?,
		at: ZonedDateTime?,
		brand: String?,
		model: String?,
		sort: String?,
	): Mono<PageModel<VehicleReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = VehicleSearchParamModel(
			query, isVisible, status, at, brand, model,
			sort = sort.toSortModel(VehicleSearchParamModel.SORT_FIELDS),
		)

		return service.findVehiclesPage(projectId, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findVehicleById(projectId: UUID, id: UUID): Mono<VehicleReaderDto> {
		return service.findVehicleById(projectId, id, isVisible = null).map(readerMapper::toDto)
	}

	override fun findVehicleMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		page: Int,
		size: Int,
		isVisible: Boolean?,
		hasActivity: Boolean?,
		type: MovementTypeEnum?,
		from: ZonedDateTime?,
		to: ZonedDateTime?,
		sort: String?,
	): Mono<PageModel<MovementReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = MovementSearchParamModel(
			isVisible,
			hasActivity,
			type,
			from,
			to,
			sort = sort.toSortModel(MovementSearchParamModel.SORT_FIELDS),
		)

		return service.findVehicleMovementsPage(projectId, id, pageable, searchParams)
			.map(movementReaderMapper::toDtoPage)
	}

	override fun createVehicle(
		currentUser: CurrentUserModel,
		projectId: UUID,
		vehicle: VehicleWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<VehicleReaderDto>> {
		val vehicleModel = writerMapper.toModel(vehicle, projectId)
		return service.createVehicle(currentUser, vehicleModel)
			.map(readerMapper::toDto)
			.map { exchange.created(it) }
	}

	override fun updateVehicleById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		vehicle: VehicleWriterDto,
		exchange: ServerWebExchange,
	): Mono<VehicleReaderDto> {
		val vehicleModel = writerMapper.toModel(vehicle, projectId)
		return concurrencyService.updateIfUnchanged(
			LockableResourceEnum.VEHICLE,
			id,
			exchange.expectedLastEditions(),
			current = { service.findVehicleById(projectId, id, isVisible = null) },
			action = { service.updateVehicleById(currentUser, projectId, id, vehicleModel) },
		).map(readerMapper::toDto)
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
