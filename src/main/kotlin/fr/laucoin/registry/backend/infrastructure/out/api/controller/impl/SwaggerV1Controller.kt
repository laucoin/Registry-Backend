package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.infrastructure.out.api.controller.ISwaggerV1Controller
import java.net.URI
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus.FOUND
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@ConditionalOnProperty(value = ["registry.feature.documentation.enabled"], havingValue = "true", matchIfMissing = false)
class SwaggerV1Controller(
	@param:Value($$"${springdoc.swagger-ui.path}")
	private val swaggerPath: String
): ISwaggerV1Controller {
	override fun redirect(response: ServerHttpResponse): Mono<Unit> {
		return Mono.fromRunnable {
			response.statusCode = FOUND
			response.headers.location = URI.create(swaggerPath)
		}
	}
}
