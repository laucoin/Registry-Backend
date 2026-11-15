package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.MovementQueryDto
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.service.support.IConcurrencyService
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.usecase.IMovementUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IMovementV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.expectedLastEditions
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocationExt.created
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query.MovementQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementContentReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ParticipantMovementWriterDtoMapper
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_RESTORE

/**
 * Objectif : Adaptateur REST des endpoints du mouvement.
 * Périmètre : Mappe les paramètres de requête et les DTO, et délègue aux cas d'usage du mouvement.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class MovementV2Controller(
	private val service: IMovementUseCase,
	private val concurrencyService: IConcurrencyService,
	private val readerMapper: MovementReaderDtoMapper,
	private val readerContentMapper: MovementContentReaderDtoMapper,
	private val writerMapper: ParticipantMovementWriterDtoMapper,
	private val queryMapper: MovementQueryDtoMapper,
): IMovementV2Controller {
	override fun findMovements(
		currentUser: CurrentUserModel,
		projectId: UUID,
		search: MovementQueryDto,
	): Mono<PageModel<MovementReaderDto>> {
		val visibility = currentUser.visibilityFor(search.isVisible, projectId, REGISTRY_PROJECT_MOVEMENT_RESTORE)
		val searchParams = queryMapper.toSearchParams(search, visibility)

		return movementsPage(search.isCurrent, currentUser, projectId, queryMapper.toPageable(search), searchParams).map(readerMapper::toDtoPage)
	}

	private fun movementsPage(
		isCurrent: Boolean,
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>> {
		return if (isCurrent) {
			service.findCurrentMovementsPage(currentUser, projectId, pageable, searchParams)
		} else {
			service.findMovementsPage(currentUser, projectId, pageable, searchParams)
		}
	}

	override fun findMovementsContents(
		projectId: UUID,
		movementIds: List<UUID>,
		isCurrent: Boolean,
	): Flux<Pair<UUID, List<MovementContentReaderDto>>> {
		return movementsContent(isCurrent, projectId, movementIds).map(::toContentPair)
	}

	private fun movementsContent(
		isCurrent: Boolean,
		projectId: UUID,
		movementIds: List<UUID>,
	): Flux<Pair<UUID, List<MovementModel.MovementContentModel>>> {
		return if (isCurrent) {
			service.findCurrentMovementsContent(projectId, movementIds)
		} else {
			service.findMovementsContent(projectId, movementIds)
		}
	}

	private fun toContentPair(pair: Pair<UUID, List<MovementModel.MovementContentModel>>): Pair<UUID, List<MovementContentReaderDto>> {
		return Pair(pair.first, pair.second.map(readerContentMapper::toDto))
	}

	override fun findMovementById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<MovementReaderDto> {
		return service.findMovementById(projectId, id, isVisible = currentUser.visibilityFor(null, projectId, REGISTRY_PROJECT_MOVEMENT_RESTORE)).map(readerMapper::toDto)
	}

	override fun createMovement(
		currentUser: CurrentUserModel,
		projectId: UUID,
		movement: ParticipantMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<MovementReaderDto>> {
		return service.createMovement(currentUser, writerMapper.toModel(movement, projectId))
			.map(readerMapper::toDto)
			.map { exchange.created(it) }
	}

	override fun updateMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		movement: ParticipantMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<MovementReaderDto> {
		val movementModel = writerMapper.toModel(movement, projectId)
		return concurrencyService.updateIfUnchanged(
			LockableResourceEnum.MOVEMENT,
			id,
			exchange.expectedLastEditions(),
			current = { service.findMovementById(projectId, id, isVisible = null) },
			action = { service.updateMovementById(currentUser, projectId, id, movementModel) },
		).map(readerMapper::toDto)
	}

	override fun disableMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<MovementReaderDto> {
		return service.disableMovementById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun enableMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<MovementReaderDto> {
		return service.enableMovementById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun deleteMovementById(projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteMovementById(projectId, id)
	}
}
