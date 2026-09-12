package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_TOKEN_OUTDATED
import fr.laucoin.registry.backend.domain.model.AuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.domain.service.impl.AuthenticationCookieService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.ISecurityV1Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CurrentUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CurrentUserReaderDtoMapper
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class SecurityV1Controller(
	private val authenticationPort: IAuthenticationPort,
	private val cookieService: AuthenticationCookieService,
	private val mapper: CurrentUserReaderDtoMapper,
): ISecurityV1Controller {
	override fun getLoginUri(redirectUri: String?): AuthenticationUriModel {
		return authenticationPort.getLoginUri(redirectUri!!)
	}

	override fun getLogoutUri(redirectUri: String?, exchange: ServerWebExchange): AuthenticationUriModel {
		cookieService.clearAuthCookies(exchange.response)
		return authenticationPort.getLogoutUri(redirectUri!!)
	}

	override fun fetchToken(authenticationInfo: AuthenticationInfoModel, exchange: ServerWebExchange): Mono<Void> {
		return authenticationPort.getAuthenticationToken(
			authenticationInfo.authorizationCode!!,
			authenticationInfo.redirectUri!!
		)
			.doOnNext { cookieService.setAuthCookies(exchange.response, it) }
			.then()
	}

	override fun refreshToken(exchange: ServerWebExchange): Mono<Void> {
		val refreshToken = cookieService.extractRefreshToken(exchange.request)
			?: return Mono.error(RegistryException(UNAUTHORIZED, REFRESH_TOKEN_OUTDATED))

		return authenticationPort.refreshAuthenticationToken(refreshToken)
			.doOnNext { cookieService.setAuthCookies(exchange.response, it) }
			.then()
	}

	override fun findCurrentUser(currentUser: CurrentUserModel): CurrentUserReaderDto {
		return mapper.toDto(currentUser)
	}
}
