package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.service.usecase.IParticipantUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IParticipantSearchV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PartialUserReaderDtoMapper
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

/**
 * Objectif : Adaptateur REST de la recherche des utilisateurs et des groupes d'un participant.
 * Périmètre : Mappe les modèles en DTO et délègue au cas d'usage des participants.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class ParticipantSearchV2Controller(
	private val service: IParticipantUseCase,
	private val partialUserReaderMapper: PartialUserReaderDtoMapper,
	private val groupReaderMapper: GroupWithoutMemberReaderDtoMapper,
): IParticipantSearchV2Controller {
	override fun searchUsers(projectId: UUID, query: String?): Flux<PartialUserReaderDto> {
		return service.searchUsersByText(projectId, query).map(partialUserReaderMapper::toDto)
	}

	override fun searchGroups(
		projectId: UUID,
		query: String?
	): Flux<GroupWithoutMemberReaderDto> {
		return service.searchGroupsByText(projectId, query).map(groupReaderMapper::toDto)
	}
}
