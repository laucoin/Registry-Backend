package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.infrastructure.internal.web.controller.ISwaggerController
import java.net.URI
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.FOUND
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class SwaggerController(
    @Value("\${springdoc.swagger-ui.path}")
    private val swaggerPath: String
): ISwaggerController {
    override fun redirect(response: ServerHttpResponse): Mono<Void> {
        return Mono.fromRunnable {
            response.setStatusCode(FOUND)
            response.headers.location = URI.create(swaggerPath)
        }
    }
}
