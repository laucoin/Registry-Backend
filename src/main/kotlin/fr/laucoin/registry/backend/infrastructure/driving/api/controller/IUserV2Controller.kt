package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_D
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_METADATA_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_RESTORE
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_SOFT_DELETE
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_U
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.UserQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserExportReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.UserRoleWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat REST des endpoints de l'utilisateur sous /api/v2.
 * Périmètre : Porte les routes, les règles d'autorisation, les bornes de validation et les métadonnées OpenAPI.
 * Limites : Ne déclare aucun comportement ; l'implémentation du contrôleur délègue aux services.
 */
@Tag(name = "Users management", description = "API for Users-related operations")
@RequestMapping("/api/v2/users")
interface IUserV2Controller {
	@Operation(
		summary = "Find Users",
		description = "Find or get paginated Users",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
	@GetMapping
	fun findUsers(@Valid @ParameterObject search: UserQueryDto): Mono<PageModel<UserReaderDto>>

	@Operation(
		summary = "Find User",
		description = "Find User by ID",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
	@GetMapping("/{id}")
	fun findUserById(@PathVariable id: UUID): Mono<UserReaderDto>

	@Operation(
		summary = "Export Current User data",
		description = "Export the personal data of the Current User, including every Participant linked to them",
	)
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/current/export")
	fun exportCurrentUser(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		request: ServerHttpRequest,
	): Mono<ResponseEntity<UserExportReaderDto>>

	@Operation(
		summary = "Export User data",
		description = "Export the personal data of a User, including every Participant linked to them",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_R')")
	@GetMapping("/{id}/export")
	fun exportUserById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
		request: ServerHttpRequest,
	): Mono<ResponseEntity<UserExportReaderDto>>

	@Operation(
		summary = "Get assignable Roles",
		description = "Get all the roles you are allowed to assign",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_METADATA_R')")
	@GetMapping("/roles")
	fun getAssignableUserRoles(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
	): Flux<LabelDto>

	@Operation(
		summary = "Update User's role",
		description = "Update a User's role",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_U')")
	@PatchMapping("/{id}/role")
	fun updateUserRole(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
		@RequestBody @Valid role: UserRoleWriterDto,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Block User",
		description = "Prevent a User from logging in",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_SOFT_DELETE')")
	@PostMapping("/{id}/block")
	fun blockUserById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Unblock User",
		description = "Re-authorize a User to log in",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_RESTORE')")
	@PostMapping("/{id}/unblock")
	fun unblockUserById(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable id: UUID,
	): Mono<UserReaderDto>

	@Operation(
		summary = "Delete User",
		description = "Delete all User data",
	)
	@PreAuthorize("hasAuthority('$REGISTRY_USER_D')")
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	fun deleteUserById(@AuthenticationPrincipal currentUser: CurrentUserModel, @PathVariable id: UUID): Mono<Unit>

	@Operation(
		summary = "Delete Current User",
		description = "Delete all Current User data",
	)
	@PreAuthorize("isAuthenticated()")
	@DeleteMapping("/current")
	@ResponseStatus(NO_CONTENT)
	fun deleteCurrentUser(@AuthenticationPrincipal currentUser: CurrentUserModel): Mono<Unit>
}
