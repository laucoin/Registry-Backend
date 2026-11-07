package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.usecase.IExportUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IParticipantExportV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantExportReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ExportExt.asAttachment
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ExportReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.security.service.ClientContextService
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur REST de l'export des données personnelles d'un participant.
 * Périmètre : Résout le contexte client, mappe l'export en DTO et délègue au cas d'usage d'export.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class ParticipantExportV2Controller(
	private val exportUseCase: IExportUseCase,
	private val exportMapper: ExportReaderDtoMapper,
	private val clientContextService: ClientContextService,
): IParticipantExportV2Controller {
	override fun exportParticipantById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		request: ServerHttpRequest,
	): Mono<ResponseEntity<ParticipantExportReaderDto>> {
		return exportUseCase.exportParticipant(currentUser, projectId, id, clientContextService.resolve(request))
			.map(exportMapper::toDto)
			.map { it.asAttachment("participant-$id.json") }
	}
}
