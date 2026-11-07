package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_RESTORE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.service.usecase.IGroupUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IGroupMemberV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ParticipantQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query.ParticipantQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AddedGroupMembersReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ParticipantReaderDtoMapper
import java.util.UUID
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur REST des membres d'un groupe.
 * Périmètre : Mappe les paramètres de requête et les DTO, et délègue au cas d'usage du groupe pour lister, rechercher, ajouter et retirer des membres.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class GroupMemberV2Controller(
	private val service: IGroupUseCase,
	private val participantReaderMapper: ParticipantReaderDtoMapper,
	private val addedGroupMembersReaderMapper: AddedGroupMembersReaderDtoMapper,
	private val queryMapper: ParticipantQueryDtoMapper,
) : IGroupMemberV2Controller {
	override fun findGroupMembersByGroupId(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		search: ParticipantQueryDto,
	): Mono<PageModel<ParticipantReaderDto>> {
		val visibility = currentUser.visibilityFor(search.isVisible, projectId, REGISTRY_PROJECT_PARTICIPANT_RESTORE)

		return service.findGroupMembersPageByGroupId(projectId, id, queryMapper.toPageable(search), queryMapper.toSearchParams(search, visibility))
			.map(participantReaderMapper::toDtoPage)
	}

	override fun searchParticipants(projectId: UUID, query: String?): Flux<ParticipantReaderDto> {
		return service.searchParticipantsByText(projectId, query).map(participantReaderMapper::toDto)
	}

	override fun addMembersToGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		memberIds: List<UUID>,
	): Mono<ResponseEntity<AddedGroupMembersReaderDto>> {
		return service.addMembersToGroupById(currentUser, projectId, id, memberIds)
			.map(addedGroupMembersReaderMapper::toDto)
			.map { responseOf(it) }
	}

	override fun removeMemberFromGroupById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		memberId: UUID
	): Mono<Unit> {
		return service.removeMemberFromGroupById(currentUser, projectId, id, memberId).thenReturn(Unit)
	}

	private fun responseOf(dto: AddedGroupMembersReaderDto): ResponseEntity<AddedGroupMembersReaderDto> {
		val status = if (dto.notAddedMemberIds.isEmpty()) OK else MULTI_STATUS
		return ResponseEntity.status(status).body(dto)
	}
}
