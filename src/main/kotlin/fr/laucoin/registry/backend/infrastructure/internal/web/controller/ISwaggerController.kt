package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono

interface ISwaggerController {
    @GetMapping
    fun redirect(response: ServerHttpResponse): Mono<Void>
}
