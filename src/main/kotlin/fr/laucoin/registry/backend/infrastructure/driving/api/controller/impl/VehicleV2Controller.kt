package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.VehicleSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IVehicleV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.VehicleWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.PageQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.SortParamDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PageReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.VehicleWriterDtoMapper
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class VehicleV2Controller(
	private val service: IVehicleService,
	private val readerMapper: VehicleReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val writerMapper: VehicleWriterDtoMapper,
	private val pageQueryMapper: PageQueryDtoMapper,
	private val sortParamMapper: SortParamDtoMapper,
	private val pageReaderMapper: PageReaderDtoMapper,
): IVehicleV2Controller {
	override fun findVehicles(
		projectId: UUID,
		page: SortedPageQueryDto,
		q: String?,
		visible: Boolean?,
		status: PresenceStatusEnum?,
		dateTime: ZonedDateTime?,
	): Mono<PageReaderDto<VehicleReaderDto>> {
		val pageable = pageQueryMapper.toPageable(page)
		val sortFields = sortParamMapper.toSortModels(page.sort, page.direction) { key ->
			VehicleSortFieldEnum.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
		}
		val searchParams = VehicleSearchParamModel(q, visible, status, dateTime)

		return service.findVehiclesPage(projectId, pageable, searchParams, sortFields)
			.map { pageReaderMapper.toDto(it, readerMapper::toDto) }
	}

	override fun findVehicleById(projectId: UUID, id: UUID): Mono<VehicleReaderDto> {
		return service.findVehicleById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun findVehicleMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		page: PageQueryDto,
		visible: Boolean?,
		linkedToActivity: Boolean?,
		type: MovementTypeEnum?,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Mono<PageReaderDto<MovementReaderDto>> {
		val pageable = pageQueryMapper.toPageable(page)
		val searchParams = MovementSearchParamModel(visible, linkedToActivity, type, startDateTime, endDateTime)

		return service.findVehicleMovementsPage(projectId, id, pageable, searchParams)
			.map { pageReaderMapper.toDto(it, movementReaderMapper::toDto) }
	}

	override fun createVehicle(
		currentUser: CurrentUserModel,
		projectId: UUID,
		vehicle: VehicleWriterDto,
	): Mono<ResponseEntity<VehicleReaderDto>> {
		val vehicleModel = writerMapper.toModel(vehicle, projectId)
		return service.createVehicle(currentUser, vehicleModel).map(readerMapper::toDto).map {
			ResponseEntity.created(URI.create("$API_V2/projects/$projectId/vehicles/${it.id}")).body(it)
		}
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
