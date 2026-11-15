package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IMovementGuestV2Controller
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.service.support.IConcurrencyService
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.usecase.IMovementUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.expectedLastEditions
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocationExt.created
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.GuestMovementWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.GuestWriterDtoMapper
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur REST des mouvements d'invités.
 * Périmètre : Mappe le mouvement et ses nouveaux invités, protège la mise à jour par If-Match et délègue au cas d'usage du mouvement.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class MovementGuestV2Controller(
	private val service: IMovementUseCase,
	private val concurrencyService: IConcurrencyService,
	private val readerMapper: MovementReaderDtoMapper,
	private val guestMovementWriterMapper: GuestMovementWriterDtoMapper,
	private val guestWriterMapper: GuestWriterDtoMapper,
) : IMovementGuestV2Controller {
	override fun createGuestsMovement(
		currentUser: CurrentUserModel,
		projectId: UUID,
		movement: GuestMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<ResponseEntity<MovementReaderDto>> {
		val movementModel = guestMovementWriterMapper.toModel(movement, projectId)
		val newGuestModels = guestWriterMapper.toModels(movement.guests ?: emptyList(), projectId)
		return service.createMovement(currentUser, movementModel, newGuestModels)
			.map(readerMapper::toDto)
			.map { exchange.created(it, "/guests") }
	}

	override fun updateGuestsMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		movement: GuestMovementWriterDto,
		exchange: ServerWebExchange,
	): Mono<MovementReaderDto> {
		val movementModel = guestMovementWriterMapper.toModel(movement, projectId)
		val newGuestModels = guestWriterMapper.toModels(movement.guests ?: emptyList(), projectId)

		return concurrencyService.updateIfUnchanged(
			LockableResourceEnum.MOVEMENT,
			id,
			exchange.expectedLastEditions(),
			current = { service.findMovementById(projectId, id, isVisible = null) },
			action = { service.updateMovementById(currentUser, projectId, id, movementModel, newGuestModels) },
		).map(readerMapper::toDto)
	}
}
