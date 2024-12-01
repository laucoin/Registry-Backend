package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.CurrentUserDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

@Tag(name = "Security management", description = "API for security operations")
@RequestMapping("/auth")
interface ISecurityController {
    @Operation(
        summary = "Get Current User Token",
        description = "Get the logged in User's token"
    )
    @GetMapping("/token")
    fun findToken(): Mono<TokenModel>

    @Operation(
        summary = "Get Current User",
        description = "Get the logged in User"
    )
    @GetMapping("/profile")
    fun findCurrentUser(): Mono<CurrentUserDto>
}
