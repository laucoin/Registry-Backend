package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_COOKIE_MISSING
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.STATE_MISMATCH
import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler
import fr.laucoin.registry.backend.domain.model.AuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.AuthorizationChallengeModel
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
import java.security.MessageDigest

@RestController
class SecurityV1Controller(
	private val authenticationPort: IAuthenticationPort,
	private val mapper: CurrentUserReaderDtoMapper,
	private val sessionMapper: SessionReaderDtoMapper,
	private val cookieHandler: AuthenticationCookieHandler,
) : ISecurityV1Controller {
	override fun getLoginUri(exchange: ServerWebExchange, redirectUri: String?): AuthenticationUriModel {
		val challenge = AuthorizationChallengeModel.generate()
		val uri = authenticationPort.getLoginUri(redirectUri!!, challenge)
		cookieHandler.writeChallenge(exchange, challenge)
		return uri
	}

	override fun getLogoutUri(exchange: ServerWebExchange, redirectUri: String?): AuthenticationUriModel {
		cookieHandler.clear(exchange)
		return authenticationPort.getLogoutUri(redirectUri!!)
	}

	override fun fetchToken(
		exchange: ServerWebExchange,
		authenticationInfo: AuthenticationInfoModel,
	): Mono<SessionReaderDto> {
		val codeVerifier = verifyState(exchange, authenticationInfo.state!!)

		return authenticationPort.getAuthenticationToken(
			authenticationInfo.authorizationCode!!,
			authenticationInfo.redirectUri!!,
			codeVerifier,
		).openSession(exchange)
	}

	private fun verifyState(exchange: ServerWebExchange, presented: String): String {
		val expected = cookieHandler.readState(exchange)
		val codeVerifier = cookieHandler.readCodeVerifier(exchange)
		cookieHandler.clearChallenge(exchange)

		val matches = expected != null && MessageDigest.isEqual(
			expected.toByteArray(Charsets.UTF_8),
			presented.toByteArray(Charsets.UTF_8),
		)
		if (!matches || codeVerifier == null) {
			throw RegistryException(UNAUTHORIZED, STATE_MISMATCH)
		}
		return codeVerifier
	}

	override fun refreshToken(exchange: ServerWebExchange): Mono<SessionReaderDto> {
		val refreshToken = cookieHandler.readRefreshToken(exchange)
			?: return Mono.error(RegistryException(UNAUTHORIZED, REFRESH_COOKIE_MISSING))

		return authenticationPort.refreshAuthenticationToken(refreshToken).openSession(exchange)
	}

	private fun Mono<TokenModel>.openSession(exchange: ServerWebExchange): Mono<SessionReaderDto> = map {
		cookieHandler.write(exchange, it)
		sessionMapper.toDto(it)
	}

	override fun findCurrentUser(currentUser: CurrentUserModel): CurrentUserReaderDto {
		return mapper.toDto(currentUser)
	}
}
