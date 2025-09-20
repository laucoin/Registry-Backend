package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.model.RegistryException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

object ReactiveExt {
	fun <T> Mono<T>.notFoundIfEmpty(identifier: Any): Mono<T> {
		return switchIfEmpty {
			LoggerFactory.getLogger(this::class.java).warn(
				"Not data found with the given identifier ({}) and the current user permissions",
				identifier
			)
			throw RegistryException(
				status = NOT_FOUND,
				code = NOT_FOUND_WITH_GIVEN_IDENTIFIER,
				args = arrayListOf(identifier.toString()),
			)
		}
	}
}
