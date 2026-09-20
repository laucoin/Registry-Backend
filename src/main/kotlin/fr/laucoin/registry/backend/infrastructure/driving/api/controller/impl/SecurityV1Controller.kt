package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_TOKEN_OUTDATED
import fr.laucoin.registry.backend.domain.extension.UserExt.getClaimAsUUID
import fr.laucoin.registry.backend.domain.model.AuthenticationInfoModel
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.domain.service.impl.AuthenticationCookieService
import fr.laucoin.registry.backend.domain.service.impl.LoggerService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.ISecurityV1Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CurrentUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CurrentUserReaderDtoMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class SecurityV1Controller(
	private val authenticationPort: IAuthenticationPort,
	private val cookieService: AuthenticationCookieService,
	private val mapper: CurrentUserReaderDtoMapper,
	private val userService: IUserService,
	private val jwtDecoder: ReactiveJwtDecoder,
	@param:Value($$"${registry.security.oauth2.claims.user-id}")
	private val userIdKey: String,
) : ISecurityV1Controller, LoggerService() {
	override fun getLoginUri(redirectUri: String?): AuthenticationUriModel {
		return authenticationPort.getLoginUri(redirectUri!!)
	}

	override fun getLogoutUri(redirectUri: String?, exchange: ServerWebExchange): Mono<AuthenticationUriModel> {
		val accessToken = cookieService.extractAccessToken(exchange.request)
		val refreshToken = cookieService.extractRefreshToken(exchange.request)
		cookieService.clearAuthCookies(exchange.response)
		return authenticationPort.getLogoutUri(redirectUri!!, accessToken, refreshToken)
	}

	override fun fetchToken(authenticationInfo: AuthenticationInfoModel, exchange: ServerWebExchange): Mono<Void> {
		return authenticationPort.getAuthenticationToken(
			authenticationInfo.authorizationCode!!,
			authenticationInfo.redirectUri!!
		)
			.doOnNext { cookieService.setAuthCookies(exchange.response, it) }
			.flatMap { recordLogin(it) }
	}

	override fun refreshToken(exchange: ServerWebExchange): Mono<Void> {
		val refreshToken = cookieService.extractRefreshToken(exchange.request)
			?: return Mono.error(RegistryException(UNAUTHORIZED, REFRESH_TOKEN_OUTDATED))

		return authenticationPort.refreshAuthenticationToken(refreshToken)
			.doOnNext { cookieService.setAuthCookies(exchange.response, it) }
			.flatMap { recordLogin(it) }
	}

	private fun recordLogin(token: TokenModel): Mono<Void> {
		return jwtDecoder.decode(token.accessToken)
			.flatMap { jwt ->
				val oidcId = jwt.getClaimAsUUID(userIdKey)
				if (oidcId != null) userService.recordLoginByOidcId(oidcId) else Mono.empty()
			}
			.onErrorResume {
				log.warn("Failed to record last login after token issuance", it)
				Mono.empty()
			}
			.then()
	}

	override fun findCurrentUser(currentUser: CurrentUserModel): CurrentUserReaderDto {
		return mapper.toDto(currentUser)
	}
}
