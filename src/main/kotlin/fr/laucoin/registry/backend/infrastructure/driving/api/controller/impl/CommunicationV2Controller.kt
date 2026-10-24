package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.service.IConcurrencyService
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.ICommunicationV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CommunicationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.expectedLastEditions
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocationExt.created
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.CommunicationWriterDtoMapper
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: REST adapter of the alert communication endpoints.
 * Scope: Maps communication models and DTOs and delegates to ICommunicationService.
 * Limits: Holds no business rule.
 */
@RestController
class CommunicationV2Controller(
	private val service: ICommunicationService,
	private val concurrencyService: IConcurrencyService,
	private val readerMapper: CommunicationReaderDtoMapper,
	private val readerAlertMapper: AlertReaderDtoMapper,
	private val readerMovementMapper: MovementReaderDtoMapper,
	private val writerMapper: CommunicationWriterDtoMapper,
): ICommunicationV2Controller {
	override fun findCommunicationById(projectId: UUID, id: UUID): Mono<CommunicationReaderDto> {
		return service.findCommunicationById(projectId, id, isVisible = null).map(readerMapper::toDto)
	}

	override fun searchMovements(projectId: UUID, query: String?): Flux<MovementReaderDto> {
		return service.searchOutMovementWithActivityByText(projectId, query).map(readerMovementMapper::toDto)
	}

	override fun searchAlerts(projectId: UUID, query: String?): Flux<AlertReaderDto> {
		return service.searchAlertByText(projectId, query).map(readerAlertMapper::toDto)
	}

	override fun createCommunication(
		currentUser: CurrentUserModel,
		projectId: UUID,
		communication: CommunicationWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<CommunicationReaderDto>> {
		val communicationModel = writerMapper.toModel(communication, projectId)
		return service.createCommunication(currentUser, communicationModel)
			.map(readerMapper::toDto)
			.map { exchange.created(it) }
	}

	override fun updateCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		communication: CommunicationWriterDto,
		exchange: ServerWebExchange,
	): Mono<CommunicationReaderDto> {
		val communicationModel = writerMapper.toModel(communication, projectId)
		return concurrencyService.updateIfUnchanged(
			LockableResourceEnum.COMMUNICATION,
			id,
			exchange.expectedLastEditions(),
			current = { service.findCommunicationById(projectId, id, isVisible = null) },
			action = { service.updateCommunicationById(currentUser, projectId, id, communicationModel) },
		).map(readerMapper::toDto)
	}

	override fun disableCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<CommunicationReaderDto> {
		return service.disableCommunicationById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun enableCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<CommunicationReaderDto> {
		return service.enableCommunicationById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun deleteCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<Unit> {
		return service.deleteCommunicationById(currentUser, projectId, id)
	}
}
