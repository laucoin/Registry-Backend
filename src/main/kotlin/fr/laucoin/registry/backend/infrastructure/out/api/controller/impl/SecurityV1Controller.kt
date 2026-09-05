package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_COOKIE_MISSING
import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler
import fr.laucoin.registry.backend.domain.model.AuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.infrastructure.out.api.controller.ISecurityV1Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CurrentUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.SessionReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CurrentUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.SessionReaderDtoMapper
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class SecurityV1Controller(
	private val authenticationPort: IAuthenticationPort,
	private val mapper: CurrentUserReaderDtoMapper,
	private val sessionMapper: SessionReaderDtoMapper,
	private val cookieHandler: AuthenticationCookieHandler,
): ISecurityV1Controller {
	override fun getLoginUri(redirectUri: String?): AuthenticationUriModel {
		return authenticationPort.getLoginUri(redirectUri!!)
	}

	override fun getLogoutUri(redirectUri: String?): AuthenticationUriModel {
		return authenticationPort.getLogoutUri(redirectUri!!)
	}

	override fun fetchToken(
		exchange: ServerWebExchange,
		authenticationInfo: AuthenticationInfoModel,
	): Mono<SessionReaderDto> {
		return authenticationPort.getAuthenticationToken(
			authenticationInfo.authorizationCode!!,
			authenticationInfo.redirectUri!!
		).openSession(exchange)
	}

	override fun refreshToken(exchange: ServerWebExchange): Mono<SessionReaderDto> {
		val refreshToken = cookieHandler.readRefreshToken(exchange)
			?: return Mono.error(RegistryException(UNAUTHORIZED, REFRESH_COOKIE_MISSING))

		return authenticationPort.refreshAuthenticationToken(refreshToken).openSession(exchange)
	}

	/**
	 * Writes the tokens to their cookies and answers with the lifetimes alone. The cookies have to be
	 * added before the body is written, which is why this maps rather than peeks at the result.
	 */
	private fun Mono<TokenModel>.openSession(exchange: ServerWebExchange): Mono<SessionReaderDto> = map {
		cookieHandler.write(exchange, it)
		sessionMapper.toDto(it)
	}

	override fun findCurrentUser(currentUser: CurrentUserModel): CurrentUserReaderDto {
		return mapper.toDto(currentUser)
	}
}
