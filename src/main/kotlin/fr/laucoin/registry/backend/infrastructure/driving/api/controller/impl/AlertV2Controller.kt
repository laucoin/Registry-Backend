package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IAlertV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.ifUnchanged
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocationExt.created
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.AlertCreationWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.AlertWriterDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class AlertV2Controller(
	private val service: IAlertService,
	private val readerMapper: AlertReaderDtoMapper,
	private val communicationReaderMapper: CommunicationReaderDtoMapper,
	private val writerMapper: AlertWriterDtoMapper,
	private val creationWriterMapper: AlertCreationWriterDtoMapper,
): IAlertV2Controller {
	override fun findAlerts(
		currentUser: CurrentUserModel,
		projectId: UUID,
		page: Int,
		size: Int,
		query: String?,
		isVisible: Boolean?,
		status: AlertStatusEnum?,
		criticality: AlertCriticalityEnum?,
		from: ZonedDateTime?,
		to: ZonedDateTime?,
		sort: String?,
	): Mono<PageModel<AlertReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = AlertSearchParamModel(
			query,
			isVisible,
			status,
			from,
			to,
			criticality,
			sort = sort.toSortModel(AlertSearchParamModel.SORT_FIELDS),
		)

		return service.findAlertsPage(projectId, pageable, searchParams)
			.map { it.apply { content.forEach { alert -> alert.restrictTo(currentUser, projectId) } } }
			.map(readerMapper::toDtoPage)
	}

	override fun findAlertById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<AlertReaderDto> {
		return service.findAlertById(projectId, id, isVisible = null).map { toReaderDto(currentUser, projectId, it) }
	}

	override fun findAlertCommunications(
		projectId: UUID,
		id: UUID,
		page: Int,
		size: Int,
		query: String?,
		isVisible: Boolean?,
		from: ZonedDateTime?,
		to: ZonedDateTime?,
		sort: String?,
	): Mono<PageModel<CommunicationReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = CommunicationSearchParamModel(
			query, isVisible, from, to,
			sort = sort.toSortModel(CommunicationSearchParamModel.SORT_FIELDS),
		)

		return service.findAlertCommunicationsPage(projectId, id, pageable, searchParams)
			.map(communicationReaderMapper::toDtoPage)
	}

	override fun createAlert(
		currentUser: CurrentUserModel,
		projectId: UUID,
		alert: AlertCreationWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<AlertReaderDto>> {
		val alertModel = creationWriterMapper.toModel(alert, projectId)
		return service.createAlert(currentUser, alertModel)
			.map { toReaderDto(currentUser, projectId, it) }
			.map { exchange.created(it) }
	}

	override fun updateAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		alert: AlertWriterDto,
		exchange: ServerWebExchange,
	): Mono<AlertReaderDto> {
		val alertModel = writerMapper.toModel(alert, projectId)
		return exchange.ifUnchanged(
			current = { service.findAlertById(projectId, id, isVisible = null) },
			action = { service.updateAlertById(currentUser, projectId, id, alertModel) },
		).map { toReaderDto(currentUser, projectId, it) }
	}

	override fun updateAlertStatusById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		status: AlertStatusEnum
	): Mono<AlertReaderDto> {
		return service.updateAlertStatusById(currentUser, projectId, id, status).map { toReaderDto(currentUser, projectId, it) }
	}

	override fun disableAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<AlertReaderDto> {
		return service.disableAlertById(currentUser, projectId, id).map { toReaderDto(currentUser, projectId, it) }
	}

	override fun enableAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<AlertReaderDto> {
		return service.enableAlertById(currentUser, projectId, id).map { toReaderDto(currentUser, projectId, it) }
	}

	override fun deleteAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<Unit> {
		return service.deleteAlertById(currentUser, projectId, id)
	}

	private fun toReaderDto(currentUser: CurrentUserModel, projectId: UUID, alert: AlertModel): AlertReaderDto {
		return readerMapper.toDto(alert.restrictTo(currentUser, projectId))
	}

	private fun AlertModel.restrictTo(currentUser: CurrentUserModel, projectId: UUID): AlertModel {
		if (!currentUser.hasAuthority(projectId, REGISTRY_PROJECT_ALERT_COMMUNICATION_R)) lastCommunication = null
		return this
	}
}
