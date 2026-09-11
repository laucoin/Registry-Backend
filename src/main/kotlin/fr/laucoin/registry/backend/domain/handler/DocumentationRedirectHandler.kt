package fr.laucoin.registry.backend.domain.handler

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.FOUND
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.net.URI

@Component
class DocumentationRedirectHandler(
	@param:Value($$"${management.server.port}")
	private val managementPort: Int,
	@param:Value($$"${registry.feature.documentation.enabled:false}")
	private val documentationEnabled: Boolean,
) : WebFilter {

	private companion object {
		private const val ROOT = "/"
		private const val SWAGGER_UI = "/swagger-ui/index.html"
	}

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		if (!documentationEnabled || !exchange.isManagementRoot()) return chain.filter(exchange)

		exchange.response.statusCode = FOUND
		exchange.response.headers.location = URI.create(SWAGGER_UI)
		return exchange.response.setComplete()
	}

	private fun ServerWebExchange.isManagementRoot() =
		request.localAddress?.port == managementPort && request.path.pathWithinApplication().value() == ROOT
}
