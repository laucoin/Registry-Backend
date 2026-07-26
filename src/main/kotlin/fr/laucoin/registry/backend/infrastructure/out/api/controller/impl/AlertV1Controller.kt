package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IAlertV1Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.AlertWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.AlertCreationWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.AlertWriterDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@RestController
class AlertV1Controller(
	private val service: IAlertService,
	private val readerMapper: AlertReaderDtoMapper,
	private val communicationReaderMapper: CommunicationReaderDtoMapper,
	private val writerMapper: AlertWriterDtoMapper,
	private val creationWriterMapper: AlertCreationWriterDtoMapper,
) : IAlertV1Controller {
	override fun findAlerts(
		projectId: UUID,
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		visibilitySearched: Boolean?,
		statusSearched: AlertStatusEnum?,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?
	): Mono<PageModel<AlertReaderDto>> {
		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = AlertSearchParamModel(
			textSearched,
			visibilitySearched,
			statusSearched,
			startDateTimeSearched,
			endDateTimeSearched
		)

		return service.findAlertsPage(projectId, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findAlertById(projectId: UUID, id: UUID): Mono<AlertReaderDto> {
		return service.findAlertById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun findAlertCommunications(
		projectId: UUID,
		id: UUID,
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

		return service.findAlertCommunicationsPage(projectId, id, pageable, searchParams)
			.map(communicationReaderMapper::toDtoPage)
	}

	override fun createAlert(
		currentUser: CurrentUserModel,
		projectId: UUID,
		alert: AlertCreationWriterDto
	): Mono<AlertReaderDto> {
		val alertModel = creationWriterMapper.toModel(alert, projectId)
		return service.createAlert(currentUser, alertModel).map(readerMapper::toDto)
	}

	override fun updateAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		alert: AlertWriterDto
	): Mono<AlertReaderDto> {
		val alertModel = writerMapper.toModel(alert, projectId)
		return service.updateAlertById(currentUser, projectId, id, alertModel).map(readerMapper::toDto)
	}

	override fun updateAlertStatusById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		status: AlertStatusEnum
	): Mono<AlertReaderDto> {
		return service.updateAlertStatusById(currentUser, projectId, id, status).map(readerMapper::toDto)
	}

	override fun disableAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<AlertReaderDto> {
		return service.disableAlertById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun enableAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<AlertReaderDto> {
		return service.enableAlertById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun deleteAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<Unit> {
		return service.deleteAlertById(currentUser, projectId, id)
	}
}
