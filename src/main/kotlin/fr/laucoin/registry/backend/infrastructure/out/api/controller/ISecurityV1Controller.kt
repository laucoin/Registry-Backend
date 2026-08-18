package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_BLANK
import fr.laucoin.registry.backend.domain.model.AuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RefreshAuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CurrentUserReaderDto
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
import reactor.core.publisher.Mono

@Deprecated(
	"API v1 has no remaining Registry-Frontend consumer and is scheduled for removal; use the /api/v2 contract.",
	level = DeprecationLevel.WARNING,
)
@Tag(name = "Security management (v1, deprecated)", description = "API for security operations — deprecated, scheduled for removal; use /api/v2")
@RequestMapping("/api/v1/authentication")
interface ISecurityV1Controller {
	@Operation(
		summary = "OAuth2 auth URI",
		description = "Build and return the OAuth2 provider authentication URI",
		deprecated = true,
	)
	@GetMapping("/login/uri")
	fun getLoginUri(@RequestParam @Valid @NotBlank(message = REDIRECT_URI_BLANK) redirectUri: String?): AuthenticationUriModel

	@Operation(
		summary = "OAuth2 logout URI",
		description = "Build and return the OAuth2 provider logout URI",
		deprecated = true,
	)
	@GetMapping("/logout/uri")
	fun getLogoutUri(@RequestParam @Valid @NotBlank(message = REDIRECT_URI_BLANK) redirectUri: String?): AuthenticationUriModel

	@Operation(
		summary = "Fetch token from code",
		description = "Return OAuth2 provider token from authorization code",
		deprecated = true,
	)
	@PostMapping("/token")
	fun fetchToken(@RequestBody @Valid authenticationInfo: AuthenticationInfoModel): Mono<TokenModel>

	@Operation(
		summary = "Fetch token from refresh token",
		description = "Return OAuth2 provider token from refresh token",
		deprecated = true,
	)
	@PostMapping("/token/refresh")
	fun refreshToken(@RequestBody @Valid refreshAuthenticationInfo: RefreshAuthenticationInfoModel): Mono<TokenModel>

	@Operation(
		summary = "Get Current User",
		description = "Get the logged in User",
		deprecated = true,
	)
	@GetMapping("/user/current")
	fun findCurrentUser(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
	): CurrentUserReaderDto
}
