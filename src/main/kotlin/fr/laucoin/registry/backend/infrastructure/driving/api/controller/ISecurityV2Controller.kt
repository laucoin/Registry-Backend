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
		description = "Build and return the OAuth2 provider authentication URI",
	)
	@GetMapping("/login/uri")
	fun getLoginUri(@RequestParam @Valid @NotBlank(message = REDIRECT_URI_BLANK) redirectUri: String?): Mono<AuthenticationUriReaderDto>

	@Operation(
		summary = "OAuth2 logout URI",
		description = "Build and return the OAuth2 provider logout URI, clearing the authentication cookies",
	)
	@GetMapping("/logout/uri")
	fun getLogoutUri(
		@RequestParam @Valid @NotBlank(message = REDIRECT_URI_BLANK) redirectUri: String?,
		@Parameter(hidden = true) exchange: ServerWebExchange,
	): Mono<AuthenticationUriReaderDto>

	@Operation(
		summary = "Fetch token from code",
		description = "Exchange an authorization code for an OAuth2 provider token, set as HttpOnly cookies",
	)
	@PostMapping("/token")
	fun fetchToken(
		@RequestBody @Valid authenticationInfo: AuthenticationInfoWriterDto,
		@Parameter(hidden = true) exchange: ServerWebExchange,
	): Mono<Void>

	@Operation(
		summary = "Fetch token from refresh token",
		description = "Renew the OAuth2 provider token from the refresh token cookie, set as HttpOnly cookies",
	)
	@PostMapping("/token/refresh")
	fun refreshToken(@Parameter(hidden = true) exchange: ServerWebExchange): Mono<Void>

	@Operation(
		summary = "Get Current User",
		description = "Get the logged in User",
	)
	@GetMapping("/user/current")
	fun findCurrentUser(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
	): Mono<CurrentUserReaderDto>
}
