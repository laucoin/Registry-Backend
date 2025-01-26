package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.AuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RefreshAuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.ISecurityController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CurrentUserReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.CurrentUserReaderDtoMapper
import java.util.Locale
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class SecurityController(
    private val authenticationPort: IAuthenticationPort,
    private val mapper: CurrentUserReaderDtoMapper,
): ISecurityController {
    override fun getLoginUri(redirectUri: String?): AuthenticationUriModel {
        return authenticationPort.getLoginUri(redirectUri !!)
    }

    override fun getLogoutUri(redirectUri: String?): AuthenticationUriModel {
        return authenticationPort.getLogoutUri(redirectUri !!)
    }

    override fun fetchToken(authenticationInfo: AuthenticationInfoModel): Mono<TokenModel> {
        return authenticationPort.getAuthenticationToken(authenticationInfo.authorizationCode !!, authenticationInfo.redirectUri !!)
    }

    override fun refreshToken(refreshAuthenticationInfo: RefreshAuthenticationInfoModel): Mono<TokenModel> {
        return authenticationPort.refreshAuthenticationToken(refreshAuthenticationInfo.refreshToken !!)
    }

    override fun findCurrentUser(currentUser: CurrentUserModel, locale: Locale): CurrentUserReaderDto {
        return mapper.toDto(currentUser, locale)
    }
}
