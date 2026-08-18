package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CurrentUserReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

/**
 * The OAuth2 dance itself is not brokered here in v2: the Nuxt BFF is the OIDC
 * client (discovery, PKCE authorize URL, code exchange, refresh and end-session
 * all run there against the provider). The backend only reads the resulting
 * JWT, so the only endpoint left is the caller's own identity — and v2 exposes
 * no unauthenticated route at all.
 */
@Tag(name = "Security management (v2)", description = "API for security operations")
@RequestMapping("/api/v2/authentication")
interface ISecurityV2Controller {
	@Operation(
		summary = "Get Current User",
		description = "Get the logged in User with authorities and preferences",
	)
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/user/current")
	fun findCurrentUser(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
	): Mono<CurrentUserReaderDto>
}
