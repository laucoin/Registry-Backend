package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.infrastructure.out.api.controller.ISecurityV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AuthenticationUriReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CurrentUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.TokenReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.AuthenticationInfoWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.RefreshAuthenticationInfoWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AuthenticationUriReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CurrentUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.TokenReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.AuthenticationInfoWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.RefreshAuthenticationInfoWriterDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class SecurityV2Controller(
	private val authenticationPort: IAuthenticationPort,
	private val mapper: CurrentUserReaderDtoMapper,
	private val authenticationUriReaderMapper: AuthenticationUriReaderDtoMapper,
	private val tokenReaderMapper: TokenReaderDtoMapper,
	private val authenticationInfoWriterMapper: AuthenticationInfoWriterDtoMapper,
	private val refreshAuthenticationInfoWriterMapper: RefreshAuthenticationInfoWriterDtoMapper,
) : ISecurityV2Controller {
	override fun getLoginUri(redirectUri: String?): AuthenticationUriReaderDto {
		return authenticationUriReaderMapper.toDto(authenticationPort.getLoginUri(redirectUri!!))
	}

	override fun getLogoutUri(redirectUri: String?): AuthenticationUriReaderDto {
		return authenticationUriReaderMapper.toDto(authenticationPort.getLogoutUri(redirectUri!!))
	}

	override fun fetchToken(authenticationInfo: AuthenticationInfoWriterDto): Mono<TokenReaderDto> {
		val model = authenticationInfoWriterMapper.toModel(authenticationInfo)
		return authenticationPort.getAuthenticationToken(model.authorizationCode!!, model.redirectUri!!)
			.map(tokenReaderMapper::toDto)
	}

	override fun refreshToken(refreshAuthenticationInfo: RefreshAuthenticationInfoWriterDto): Mono<TokenReaderDto> {
		val model = refreshAuthenticationInfoWriterMapper.toModel(refreshAuthenticationInfo)
		return authenticationPort.refreshAuthenticationToken(model.refreshToken!!)
			.map(tokenReaderMapper::toDto)
	}

	override fun findCurrentUser(currentUser: CurrentUserModel): CurrentUserReaderDto {
		return mapper.toDto(currentUser)
	}
}
