package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.ICommunicationV1Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CommunicationWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.CommunicationWriterDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class CommunicationV1Controller(
	private val service: ICommunicationService,
	private val readerMapper: CommunicationReaderDtoMapper,
	private val readerAlertMapper: AlertReaderDtoMapper,
	private val readerMovementMapper: MovementReaderDtoMapper,
	private val writerMapper: CommunicationWriterDtoMapper,
): ICommunicationV1Controller {
	override fun findCommunications(
		projectId: UUID,
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		visibilitySearched: Boolean?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?
	): Mono<PageModel<CommunicationReaderDto>> {
		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = CommunicationSearchParamModel(
			textSearched, visibilitySearched, startDateTimeSearched, endDateTimeSearched
		)

		return service.findCommunicationPage(projectId, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findCommunicationById(projectId: UUID, id: UUID): Mono<CommunicationReaderDto> {
		return service.findCommunicationById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun searchActivities(projectId: UUID, textSearched: String?): Flux<MovementReaderDto> {
		return service.searchOutMovementWithActivityByText(projectId, textSearched).map(readerMovementMapper::toDto)
	}

	override fun searchAlerts(projectId: UUID, textSearched: String?): Flux<AlertReaderDto> {
		return service.searchAlertByText(projectId, textSearched).map(readerAlertMapper::toDto)
	}

	override fun createCommunication(
		currentUser: CurrentUserModel,
		projectId: UUID,
		communication: CommunicationWriterDto,
	): Mono<CommunicationReaderDto> {
		val communicationModel = writerMapper.toModel(communication, projectId)
		return service.createCommunication(currentUser, communicationModel).map(readerMapper::toDto)
	}

	override fun updateCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		communication: CommunicationWriterDto,
	): Mono<CommunicationReaderDto> {
		val communicationModel = writerMapper.toModel(communication, projectId)
		return service.updateCommunicationById(currentUser, projectId, id, communicationModel).map(readerMapper::toDto)
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
