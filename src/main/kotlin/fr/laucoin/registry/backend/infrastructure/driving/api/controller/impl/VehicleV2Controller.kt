package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.VehicleQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ElementMovementQueryDto
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.service.support.IConcurrencyService
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.service.usecase.IVehicleUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IVehicleV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleTimelineReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.VehicleWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.expectedLastEditions
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocationExt.created
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query.VehicleQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.VehicleTimelineReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.VehicleWriterDtoMapper
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_RESTORE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_RESTORE

/**
 * Objectif : Adaptateur REST des endpoints du véhicule.
 * Périmètre : Mappe les paramètres de requête et les DTO, et délègue aux cas d'usage du véhicule.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class VehicleV2Controller(
	private val service: IVehicleUseCase,
	private val concurrencyService: IConcurrencyService,
	private val readerMapper: VehicleReaderDtoMapper,
	private val timelineReaderMapper: VehicleTimelineReaderDtoMapper,
	private val movementReaderMapper: MovementReaderDtoMapper,
	private val writerMapper: VehicleWriterDtoMapper,
	private val queryMapper: VehicleQueryDtoMapper,
): IVehicleV2Controller {
	override fun findVehicles(
		currentUser: CurrentUserModel,
		projectId: UUID,
		search: VehicleQueryDto,
	): Mono<PageModel<VehicleReaderDto>> {
		val visibility = currentUser.visibilityFor(search.isVisible, projectId, REGISTRY_PROJECT_VEHICLE_RESTORE)
		return service.findVehiclesPage(projectId, queryMapper.toPageable(search), queryMapper.toSearchParams(search, visibility))
			.map(readerMapper::toDtoPage)
	}

	override fun findVehicleById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<VehicleReaderDto> {
		return service.findVehicleById(projectId, id, isVisible = currentUser.visibilityFor(null, projectId, REGISTRY_PROJECT_VEHICLE_RESTORE)).map(readerMapper::toDto)
	}

	override fun findVehicleTimeline(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<VehicleTimelineReaderDto> {
		return service.findVehicleTimeline(projectId, id).map(timelineReaderMapper::toDto)
	}

	override fun findVehicleMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		search: ElementMovementQueryDto,
	): Mono<PageModel<MovementReaderDto>> {
		val visibility = currentUser.visibilityFor(search.isVisible, projectId, REGISTRY_PROJECT_MOVEMENT_RESTORE)
		val searchParams = queryMapper.toMovementSearchParams(search, visibility)

		return service.findVehicleMovementsPage(projectId, id, queryMapper.toPageable(search), searchParams)
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
