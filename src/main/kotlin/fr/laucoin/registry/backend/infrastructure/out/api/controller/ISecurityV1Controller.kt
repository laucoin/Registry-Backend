package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_BLANK
import fr.laucoin.registry.backend.domain.model.AuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CurrentUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.SessionReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Tag(name = "Security management", description = "API for security operations")
@RequestMapping("/api/v1/authentication")
interface ISecurityV1Controller {
	@Operation(
		summary = "OAuth2 auth URI",
		description = "Build and return the OAuth2 provider authentication URI",
	)
	@GetMapping("/login/uri")
	fun getLoginUri(@RequestParam @Valid @NotBlank(message = REDIRECT_URI_BLANK) redirectUri: String?): AuthenticationUriModel

	@Operation(
		summary = "Close the session and return the provider logout URI",
		description = "Expires the session cookies and returns the URL to send the browser to, so the "
			+ "provider ends its own session too.",
	)
	@GetMapping("/logout/uri")
	fun getLogoutUri(
		exchange: ServerWebExchange,
		@RequestParam @Valid @NotBlank(message = REDIRECT_URI_BLANK) redirectUri: String?,
	): AuthenticationUriModel

	@Operation(
		summary = "Open a session from an authorization code",
		description = "Exchange the code with the provider and set the session cookies. The tokens are "
			+ "not returned in the body: they are written as HttpOnly cookies, out of reach of any script.",
	)
	@PostMapping("/token")
	fun fetchToken(
		exchange: ServerWebExchange,
		@RequestBody @Valid authenticationInfo: AuthenticationInfoModel,
	): Mono<SessionReaderDto>

	@Operation(
		summary = "Renew the session",
		description = "Read the refresh token from its cookie, renew it with the provider and set the "
			+ "session cookies again. Takes no request body.",
	)
	@PostMapping("/token/refresh")
	fun refreshToken(exchange: ServerWebExchange): Mono<SessionReaderDto>

	@Operation(
		summary = "Get Current User",
		description = "Get the logged in User",
	)
	@GetMapping("/user/current")
	fun findCurrentUser(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
	): CurrentUserReaderDto
}
