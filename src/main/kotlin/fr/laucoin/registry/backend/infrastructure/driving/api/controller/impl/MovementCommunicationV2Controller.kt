package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_RESTORE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.service.usecase.IMovementUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IMovementCommunicationV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.CommunicationQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query.CommunicationQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CommunicationReaderDtoMapper
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur REST des communications d'un mouvement.
 * Périmètre : Mappe la requête de recherche et délègue au cas d'usage du mouvement la lecture paginée de ses communications.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class MovementCommunicationV2Controller(
	private val service: IMovementUseCase,
	private val communicationReaderMapper: CommunicationReaderDtoMapper,
	private val queryMapper: CommunicationQueryDtoMapper,
) : IMovementCommunicationV2Controller {
	override fun findMovementCommunications(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		search: CommunicationQueryDto,
	): Mono<PageModel<CommunicationReaderDto>> {
		val visibility = currentUser.visibilityFor(search.isVisible, projectId, REGISTRY_PROJECT_COMMUNICATION_RESTORE)
		return service.findMovementCommunicationsPage(projectId, id, queryMapper.toPageable(search), queryMapper.toSearchParams(search, visibility))
			.map(communicationReaderMapper::toDtoPage)
	}
}
