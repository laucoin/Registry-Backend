package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.enumeration.CommunicationSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.ICommunicationV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CommunicationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.PageQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.SortParamDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PageReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.CommunicationWriterDtoMapper
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class CommunicationV2Controller(
	private val service: ICommunicationService,
	private val readerMapper: CommunicationReaderDtoMapper,
	private val readerAlertMapper: AlertReaderDtoMapper,
	private val readerMovementMapper: MovementReaderDtoMapper,
	private val writerMapper: CommunicationWriterDtoMapper,
	private val pageQueryMapper: PageQueryDtoMapper,
	private val sortParamMapper: SortParamDtoMapper,
	private val pageReaderMapper: PageReaderDtoMapper,
): ICommunicationV2Controller {
	override fun findCommunications(
		projectId: UUID,
		page: SortedPageQueryDto,
		q: String?,
		visible: Boolean?,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Mono<PageReaderDto<CommunicationReaderDto>> {
		val pageable = pageQueryMapper.toPageable(page)
		val sortFields = sortParamMapper.toSortModels(page.sort, page.direction) { key ->
			CommunicationSortFieldEnum.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
		}
		val searchParams = CommunicationSearchParamModel(q, visible, startDateTime, endDateTime)

		return service.findCommunicationPage(projectId, pageable, searchParams, sortFields)
			.map { pageReaderMapper.toDto(it, readerMapper::toDto) }
	}

	override fun findCommunicationById(projectId: UUID, id: UUID): Mono<CommunicationReaderDto> {
		return service.findCommunicationById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun searchActivities(projectId: UUID, q: String?): Flux<MovementReaderDto> {
		return service.searchOutMovementWithActivityByText(projectId, q).map(readerMovementMapper::toDto)
	}

	override fun searchAlerts(projectId: UUID, q: String?): Flux<AlertReaderDto> {
		return service.searchAlertByText(projectId, q).map(readerAlertMapper::toDto)
	}

	override fun createCommunication(
		currentUser: CurrentUserModel,
		projectId: UUID,
		communication: CommunicationWriterDto,
	): Mono<ResponseEntity<CommunicationReaderDto>> {
		val communicationModel = writerMapper.toModel(communication, projectId)
		return service.createCommunication(currentUser, communicationModel).map(readerMapper::toDto).map {
			ResponseEntity.created(URI.create("$API_V2/projects/$projectId/communications/${it.id}")).body(it)
		}
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
		id: UUID,
	): Mono<CommunicationReaderDto> {
		return service.enableCommunicationById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun deleteCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<Unit> {
		return service.deleteCommunicationById(currentUser, projectId, id)
	}
}
