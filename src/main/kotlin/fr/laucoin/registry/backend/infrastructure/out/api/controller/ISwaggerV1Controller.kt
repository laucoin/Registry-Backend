package fr.laucoin.registry.backend.infrastructure.out.api.controller

import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono

fun interface ISwaggerV1Controller {
	@GetMapping
	fun redirect(response: ServerHttpResponse): Mono<Unit>
}
