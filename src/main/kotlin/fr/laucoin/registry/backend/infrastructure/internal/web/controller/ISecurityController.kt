package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_BLANK
import fr.laucoin.registry.backend.domain.model.AuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RefreshAuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CurrentUserReaderDto
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

@Tag(name = "Security management", description = "API for security operations")
@RequestMapping("/api/authentication")
interface ISecurityController {
    @Operation(
        summary = "OAuth2 auth URI",
        description = "Build and return the OAuth2 provider authentication URI"
    )
    @GetMapping("/login/uri")
    fun getLoginUri(@RequestParam @Valid @NotBlank(message = REDIRECT_URI_BLANK) redirectUri: String?): AuthenticationUriModel

    @Operation(
        summary = "OAuth2 logout URI",
        description = "Build and return the OAuth2 provider logout URI"
    )
    @GetMapping("/logout/uri")
    fun getLogoutUri(@RequestParam @Valid @NotBlank(message = REDIRECT_URI_BLANK) redirectUri: String?): AuthenticationUriModel

    @Operation(
        summary = "Fetch token from code",
        description = "Return OAuth2 provider token from authorization code"
    )
    @PostMapping("/token")
    fun fetchToken(@RequestBody @Valid authenticationInfo: AuthenticationInfoModel): Mono<TokenModel>

    @Operation(
        summary = "Fetch token from refresh token",
        description = "Return OAuth2 provider token from refresh token"
    )
    @PostMapping("/token/refresh")
    fun refreshToken(@RequestBody @Valid refreshAuthenticationInfo: RefreshAuthenticationInfoModel): Mono<TokenModel>

    @Operation(
        summary = "Get Current User",
        description = "Get the logged in User"
    )
    @GetMapping("/user/current")
    fun findCurrentUser(@AuthenticationPrincipal currentUser: CurrentUserModel): CurrentUserReaderDto
}
