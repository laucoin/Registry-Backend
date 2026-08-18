package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.out.api.controller.ISecurityV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CurrentUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CurrentUserReaderDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class SecurityV2Controller(
	private val mapper: CurrentUserReaderDtoMapper,
) : ISecurityV2Controller {
	override fun findCurrentUser(currentUser: CurrentUserModel): Mono<CurrentUserReaderDto> {
		return Mono.fromCallable { mapper.toDto(currentUser) }
	}
}
