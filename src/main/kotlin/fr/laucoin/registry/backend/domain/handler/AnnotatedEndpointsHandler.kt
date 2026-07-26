package fr.laucoin.registry.backend.domain.handler

import org.springframework.beans.factory.ObjectProvider
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.util.pattern.PathPattern

/**
 * Resolves which annotated endpoint (if any) a request targets. Cross-cutting
 * HTTP behaviours (rate limiting, cache headers) are declared as annotations
 * ON the controller contract method — visible to whoever edits the endpoint —
 * and matched here against the routes Spring registered for those methods, so
 * no handler ever duplicates endpoint paths in hard-coded patterns.
 */
@Component
class AnnotatedEndpointsHandler(
	private val handlerMapping: ObjectProvider<RequestMappingHandlerMapping>,
) {
	class AnnotatedEndpoint<A : Annotation>(
		private val patterns: Set<PathPattern>,
		private val methods: Set<HttpMethod>,
		val annotation: A,
	) {
		fun matches(request: ServerHttpRequest): Boolean =
			(methods.isEmpty() || request.method in methods)
					&& patterns.any { it.matches(request.path.pathWithinApplication()) }
	}

	/**
	 * All registered endpoints whose (contract) method carries [annotationType].
	 * Call lazily: route registration completes during context refresh, so the
	 * lookup is only safe once the application serves traffic. Every
	 * [RequestMappingHandlerMapping] is scanned — actuator registers a second
	 * one (controllerEndpointHandlerMapping) beside the WebFlux default.
	 */
	fun <A : Annotation> endpoints(annotationType: Class<A>): List<AnnotatedEndpoint<A>> =
		handlerMapping.flatMap { mapping ->
			mapping.handlerMethods.mapNotNull { (info, handlerMethod) ->
				AnnotatedElementUtils.findMergedAnnotation(handlerMethod.method, annotationType)
					?.let { annotation ->
						AnnotatedEndpoint(
							info.patternsCondition.patterns,
							info.methodsCondition.methods.map { it.asHttpMethod() }.toSet(),
							annotation,
						)
					}
			}
		}
}
