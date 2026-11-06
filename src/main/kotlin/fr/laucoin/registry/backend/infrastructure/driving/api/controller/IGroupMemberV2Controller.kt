package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import org.springdoc.core.annotations.ParameterObject
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ParticipantQueryDto
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_TOO_MANY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_U
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.GROUP_MEMBER_IDS_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.SEARCH_QUERY_MAX
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.util.UUID
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST des membres d'un groupe sous /api/v2 : liste, recherche, ajout et retrait de participants.
 * Périmètre : Porte les routes des membres, leurs règles d'autorisation, leurs bornes de validation et leurs métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue au cas d'usage du groupe.
 */
@Tag(name = "Participant's groups management", description = "API for Group-related operations")
@RequestMapping("/api/v2/projects/{projectId}/groups")
interface IGroupMemberV2Controller {
	@Operation(
		summary = "Find Group Members",
		description = "Find or get paginated Group Members by Group ID",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_R')")
	@GetMapping("/{id}/members")
	fun findGroupMembersByGroupId(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@Valid @ParameterObject search: ParticipantQueryDto,
	): Mono<PageModel<ParticipantReaderDto>>

	@Operation(
		summary = "Search Participants",
		description = "Search Participants to add in a Group",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_METADATA_R')")
	@GetMapping("/search/participants")
	fun searchParticipants(
		@PathVariable projectId: UUID,
		@Parameter(description = "Text matched against the participant first name and last name")
		@RequestParam("q") @Valid @Size(max = SEARCH_QUERY_MAX, message = PARAMETER_TOO_LONG) query: String?
	): Flux<ParticipantReaderDto>

	@Operation(
		summary = "Add members in Group",
		description = "Add members in an existing Group",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_U')")
	@PostMapping("/{id}/members")
	fun addMembersToGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@RequestBody @Valid @NotEmpty(message = GROUP_MEMBERS_EMPTY) @Size(max = GROUP_MEMBER_IDS_MAX, message = GROUP_MEMBERS_TOO_MANY) memberIds: List<UUID>,
	): Mono<ResponseEntity<AddedGroupMembersReaderDto>>

	@Operation(
		summary = "Remove member from Group",
		description = "Remove member from an existing Group",
	)
	@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_GROUP_U')")
	@DeleteMapping("/{id}/members/{memberId}")
	@ResponseStatus(NO_CONTENT)
	fun removeMemberFromGroupById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
		@PathVariable id: UUID,
		@PathVariable memberId: UUID,
	): Mono<Unit>
}
