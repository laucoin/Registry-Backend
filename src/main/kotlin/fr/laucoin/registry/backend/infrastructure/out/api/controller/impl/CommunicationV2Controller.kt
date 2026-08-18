package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.CommunicationSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.ICommunicationV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CommunicationWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.PageQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AlertReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.CommunicationWriterDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@RestController
class CommunicationV2Controller(
	private val service: ICommunicationService,
	private val readerMapper: CommunicationReaderDtoMapper,
	private val readerAlertMapper: AlertReaderDtoMapper,
	private val readerMovementMapper: MovementReaderDtoMapper,
	private val writerMapper: CommunicationWriterDtoMapper,
) : ICommunicationV2Controller {
	override fun findCommunications(
		projectId: UUID,
		pageQuery: SortedPageQueryDto,
		q: String?,
		visible: Boolean?,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Mono<PageModel<CommunicationReaderDto>> {
		val pageable = PageQueryDtoMapper.toPageable(pageQuery)
		val searchParams = CommunicationSearchParamModel(q, visible, startDateTime, endDateTime)
		val sortModels = PageQueryDtoMapper.toSortModels(pageQuery, CommunicationSortFieldEnum::fromParamName)

		return service.findCommunicationPage(projectId, pageable, searchParams, sortModels)
			.map(readerMapper::toDtoPage)
	}

	override fun findCommunicationById(projectId: UUID, id: UUID): Mono<CommunicationReaderDto> {
		return service.findCommunicationById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun getAttachableMovements(projectId: UUID, q: String?): Flux<MovementReaderDto> {
		return service.searchOutMovementWithActivityByText(projectId, q).map(readerMovementMapper::toDto)
	}

	override fun getAttachableAlerts(projectId: UUID, q: String?): Flux<AlertReaderDto> {
		return service.searchAlertByText(projectId, q).map(readerAlertMapper::toDto)
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
