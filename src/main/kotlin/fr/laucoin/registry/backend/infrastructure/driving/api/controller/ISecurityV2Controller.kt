package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_BLANK
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AuthenticationUriReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CurrentUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AuthenticationInfoWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
@RequestMapping("$API_V2/authentication")
interface ISecurityV2Controller {
	@Operation(
		summary = "OAuth2 auth URI",
		description = """
			Build the OAuth2 provider's authentication (login) URI the frontend must redirect the browser to, so the
			provider can send the User back to `redirectUri` once authenticated.
		""",
	)
	@GetMapping("/login/uri")
	fun getLoginUri(@RequestParam @Valid @NotBlank(message = REDIRECT_URI_BLANK) redirectUri: String?): Mono<AuthenticationUriReaderDto>

	@Operation(
		summary = "OAuth2 logout URI",
		description = """
			Build the OAuth2 provider's logout URI, and clear this session's authentication cookies (access and refresh
			token) so the browser can then be redirected to `redirectUri`.
		""",
	)
	@GetMapping("/logout/uri")
	fun getLogoutUri(
		@RequestParam @Valid @NotBlank(message = REDIRECT_URI_BLANK) redirectUri: String?,
		@Parameter(hidden = true) exchange: ServerWebExchange,
	): Mono<AuthenticationUriReaderDto>

	@Operation(
		summary = "Fetch token from code",
		description = """
			Exchange the OAuth2 authorization code obtained after login for the provider's access/refresh tokens, and
			set them as HttpOnly cookies on the response so the browser doesn't need to handle them directly.
		""",
	)
	@PostMapping("/token")
	fun fetchToken(
		@RequestBody @Valid authenticationInfo: AuthenticationInfoWriterDto,
		@Parameter(hidden = true) exchange: ServerWebExchange,
	): Mono<Void>

	@Operation(
		summary = "Fetch token from refresh token",
		description = """
			Use the refresh token cookie to obtain a new access token from the OAuth2 provider without a full login,
			and set the renewed tokens as HttpOnly cookies on the response.
		""",
	)
	@PostMapping("/token/refresh")
	fun refreshToken(@Parameter(hidden = true) exchange: ServerWebExchange): Mono<Void>

	@Operation(
		summary = "Get Current User",
		description = "Get the account of the currently logged in User, as resolved from the authentication cookies.",
	)
	@GetMapping("/user/current")
	fun findCurrentUser(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
	): Mono<CurrentUserReaderDto>
}
