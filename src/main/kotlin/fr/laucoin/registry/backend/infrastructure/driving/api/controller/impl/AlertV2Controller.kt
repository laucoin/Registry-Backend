package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.enumeration.AlertSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IAlertV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.OngoingAlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertStatusWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.PageQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.SortParamDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.OngoingAlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PageReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.AlertCreationWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.AlertWriterDtoMapper
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class AlertV2Controller(
	private val service: IAlertService,
	private val readerMapper: AlertReaderDtoMapper,
	private val communicationReaderMapper: CommunicationReaderDtoMapper,
	private val ongoingAlertReaderMapper: OngoingAlertReaderDtoMapper,
	private val writerMapper: AlertWriterDtoMapper,
	private val creationWriterMapper: AlertCreationWriterDtoMapper,
	private val pageQueryMapper: PageQueryDtoMapper,
	private val sortParamMapper: SortParamDtoMapper,
	private val pageReaderMapper: PageReaderDtoMapper,
): IAlertV2Controller {
	override fun findAlerts(
		projectId: UUID,
		page: SortedPageQueryDto,
		q: String?,
		visible: Boolean?,
		status: AlertStatusEnum?,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Mono<PageReaderDto<AlertReaderDto>> {
		val pageable = pageQueryMapper.toPageable(page)
		val sortFields = sortParamMapper.toSortModels(page.sort, page.direction) { key ->
			AlertSortFieldEnum.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
		}
		val searchParams = AlertSearchParamModel(q, visible, status, startDateTime, endDateTime)

		return service.findAlertsPage(projectId, pageable, searchParams, sortFields)
			.map { pageReaderMapper.toDto(it, readerMapper::toDto) }
	}

	override fun findAlertById(projectId: UUID, id: UUID): Mono<AlertReaderDto> {
		return service.findAlertById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun findAlertCommunications(
		projectId: UUID,
		id: UUID,
		page: PageQueryDto,
		q: String?,
		visible: Boolean?,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Mono<PageReaderDto<CommunicationReaderDto>> {
		val pageable = pageQueryMapper.toPageable(page)
		val searchParams = CommunicationSearchParamModel(q, visible, startDateTime, endDateTime)

		return service.findAlertCommunicationsPage(projectId, id, pageable, searchParams)
			.map { pageReaderMapper.toDto(it, communicationReaderMapper::toDto) }
	}

	override fun findOngoingAlerts(projectId: UUID, limit: Int): Flux<OngoingAlertReaderDto> {
		return service.findOngoingAlerts(projectId, limit).map(ongoingAlertReaderMapper::toDto)
	}

	override fun createAlert(
		currentUser: CurrentUserModel,
		projectId: UUID,
		alert: AlertCreationWriterDto,
	): Mono<ResponseEntity<AlertReaderDto>> {
		val alertModel = creationWriterMapper.toModel(alert, projectId)
		return service.createAlert(currentUser, alertModel).map(readerMapper::toDto).map {
			ResponseEntity.created(URI.create("$API_V2/projects/$projectId/alerts/${it.id}")).body(it)
		}
	}

	override fun updateAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		alert: AlertWriterDto,
	): Mono<AlertReaderDto> {
		val alertModel = writerMapper.toModel(alert, projectId)
		return service.updateAlertById(currentUser, projectId, id, alertModel).map(readerMapper::toDto)
	}

	override fun updateAlertStatusById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		status: AlertStatusWriterDto,
	): Mono<AlertReaderDto> {
		return service.updateAlertStatusById(currentUser, projectId, id, status.status!!).map(readerMapper::toDto)
	}

	override fun disableAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<AlertReaderDto> {
		return service.disableAlertById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun enableAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<AlertReaderDto> {
		return service.enableAlertById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun deleteAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<Unit> {
		return service.deleteAlertById(currentUser, projectId, id)
	}
}
