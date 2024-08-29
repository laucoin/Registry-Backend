package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.currentUser
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.currentUserToken
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.ISecurityController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.CurrentUserDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.CurrentUserDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class SecurityController(private val mapper: CurrentUserDtoMapper): ISecurityController {
    override fun findToken(): Mono<TokenModel> = currentUserToken()

    override fun findCurrentUser(): Mono<CurrentUserDto> = currentUser().map(mapper::toDto)
}
