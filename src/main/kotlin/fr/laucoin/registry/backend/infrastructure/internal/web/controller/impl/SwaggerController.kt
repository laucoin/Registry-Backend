package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.infrastructure.internal.web.controller.ISwaggerController
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus.FOUND
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.net.URI

@RestController
@ConditionalOnProperty(value = ["registry.feature.documentation.enabled"], havingValue = "true", matchIfMissing = false)
class SwaggerController(
    @param:Value("\${springdoc.swagger-ui.path}")
    private val swaggerPath: String
) : ISwaggerController {
    override fun redirect(response: ServerHttpResponse): Mono<Void> {
        return Mono.fromRunnable {
            response.setStatusCode(FOUND)
            response.headers.location = URI.create(swaggerPath)
        }
    }
}
