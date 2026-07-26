package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.VehicleSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IVehicleV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.VehicleWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.SortParamDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.VehicleWriterDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@RestController
class VehicleV2Controller(
	private val service: IVehicleService,
	private val readerMapper: VehicleReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val writerMapper: VehicleWriterDtoMapper,
) : IVehicleV2Controller {
	override fun findVehicles(
		projectId: UUID,
		page: Int,
		size: Int,
		sort: List<String>?,
		q: String?,
		visible: Boolean?,
		status: PresenceStatusEnum?,
		dateTime: ZonedDateTime?,
	): Mono<PageModel<VehicleReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = VehicleSearchParamModel(q, visible, status, dateTime)
		val sortModels = SortParamDtoMapper.toSortModels(sort, VehicleSortFieldEnum::fromParamName)

		return service.findVehiclesPage(projectId, pageable, searchParams, sortModels).map(readerMapper::toDtoPage)
	}

	override fun findVehicleById(projectId: UUID, id: UUID): Mono<VehicleReaderDto> {
		return service.findVehicleById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun findVehicleMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		page: Int,
		size: Int,
		visible: Boolean?,
		linkedToActivity: Boolean?,
		type: MovementTypeEnum?,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Mono<PageModel<MovementReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = MovementSearchParamModel(
			visible,
			linkedToActivity,
			type,
			startDateTime,
			endDateTime
		)

		return service.findVehicleMovementsPage(projectId, id, pageable, searchParams)
			.map(movementReaderMapper::toDtoPage)
	}

	override fun createVehicle(
		currentUser: CurrentUserModel,
		projectId: UUID,
		vehicle: VehicleWriterDto,
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
