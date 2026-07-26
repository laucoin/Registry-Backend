package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.model.AuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RefreshAuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.infrastructure.out.api.controller.ISecurityV1Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CurrentUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CurrentUserReaderDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class SecurityV1Controller(
	private val authenticationPort: IAuthenticationPort,
	private val mapper: CurrentUserReaderDtoMapper,
) : ISecurityV1Controller {
	override fun getLoginUri(redirectUri: String?): AuthenticationUriModel {
		return authenticationPort.getLoginUri(redirectUri!!)
	}

	override fun getLogoutUri(redirectUri: String?): AuthenticationUriModel {
		return authenticationPort.getLogoutUri(redirectUri!!)
	}

	override fun fetchToken(authenticationInfo: AuthenticationInfoModel): Mono<TokenModel> {
		return authenticationPort.getAuthenticationToken(
			authenticationInfo.authorizationCode!!,
			authenticationInfo.redirectUri!!
		)
	}

	override fun refreshToken(refreshAuthenticationInfo: RefreshAuthenticationInfoModel): Mono<TokenModel> {
		return authenticationPort.refreshAuthenticationToken(refreshAuthenticationInfo.refreshToken!!)
	}

	override fun findCurrentUser(currentUser: CurrentUserModel): CurrentUserReaderDto {
		return mapper.toDto(currentUser)
	}
}
