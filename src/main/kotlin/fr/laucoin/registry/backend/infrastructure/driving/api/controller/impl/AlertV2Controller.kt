package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.AlertQueryDto
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.service.support.IConcurrencyService
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.service.usecase.IAlertUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IAlertV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertStatisticsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.expectedLastEditions
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocationExt.created
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query.AlertQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AlertStatisticsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.AlertCreationWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.AlertWriterDtoMapper
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_RESTORE

/**
 * Objectif : Adaptateur REST des endpoints de l'alerte.
 * Périmètre : Mappe les paramètres de requête et les DTO, et délègue aux cas d'usage de l'alerte.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class AlertV2Controller(
	private val service: IAlertUseCase,
	private val concurrencyService: IConcurrencyService,
	private val readerMapper: AlertReaderDtoMapper,
	private val statisticsReaderMapper: AlertStatisticsReaderDtoMapper,
	private val writerMapper: AlertWriterDtoMapper,
	private val creationWriterMapper: AlertCreationWriterDtoMapper,
	private val queryMapper: AlertQueryDtoMapper,
): IAlertV2Controller {
	override fun findAlerts(
		currentUser: CurrentUserModel,
		projectId: UUID,
		search: AlertQueryDto,
	): Mono<PageModel<AlertReaderDto>> {
		val visibility = currentUser.visibilityFor(search.isVisible, projectId, REGISTRY_PROJECT_ALERT_RESTORE)
		val searchParams = queryMapper.toSearchParams(search, visibility)

		return service.findAlertsPage(currentUser, projectId, queryMapper.toPageable(search), searchParams).map(readerMapper::toDtoPage)
	}

	override fun findAlertStatistics(currentUser: CurrentUserModel, projectId: UUID): Mono<AlertStatisticsReaderDto> {
		return service.findAlertStatistics(projectId).map(statisticsReaderMapper::toDto)
	}

	override fun findAlertById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<AlertReaderDto> {
		return service.findAlertForUser(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun createAlert(
		currentUser: CurrentUserModel,
		projectId: UUID,
		alert: AlertCreationWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<AlertReaderDto>> {
		val alertModel = creationWriterMapper.toModel(alert, projectId)
		return service.createAlert(currentUser, alertModel)
			.map(readerMapper::toDto)
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
		return concurrencyService.updateIfUnchanged(
			LockableResourceEnum.ALERT,
			id,
			exchange.expectedLastEditions(),
			current = { service.findAlertById(projectId, id, isVisible = null) },
			action = { service.updateAlertById(currentUser, projectId, id, alertModel) },
		).map(readerMapper::toDto)
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
