package fr.laucoin.registry.backend.domain.handler

import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SEARCH
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.net.InetSocketAddress
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.context.i18n.SimpleLocaleContext
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.method.HandlerMethod
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono

private interface AnnotatedResource {
	@RateLimited(SENSITIVE)
	fun sensitiveOp(): String

	@RateLimited(SEARCH, whenParamPresent = ["q"])
	fun searchOp(): String

	fun unannotatedOp(): String
}

private class AnnotatedResourceImpl : AnnotatedResource {
	override fun sensitiveOp() = "ok"
	override fun searchOp() = "ok"
	override fun unannotatedOp() = "ok"
}

class RateLimitHandlerTest {
	private val handlerMapping: RequestMappingHandlerMapping = mock()
	private val translateService: ITranslateService = mock()
	private val localeContextResolver: LocaleContextResolver = mock()
	private val impl = AnnotatedResourceImpl()
	private val chainCalls = AtomicInteger(0)
	private val chain = WebFilterChain {
		chainCalls.incrementAndGet()
		it.response.statusCode = OK
		Mono.empty()
	}

	init {
		whenever(translateService.getError(any(), anyOrNull(), anyOrNull(), any())).thenReturn("")
		whenever(localeContextResolver.resolveLocaleContext(any())).thenReturn(SimpleLocaleContext(Locale.ENGLISH))
	}

	private fun handler(
		capacities: Map<RateLimitCategoryEnum, Int> = mapOf(SENSITIVE to 2, SEARCH to 2),
		windowSeconds: Map<RateLimitCategoryEnum, Long> = mapOf(SENSITIVE to 60L, SEARCH to 60L),
	): RateLimitHandler = RateLimitHandler(
		handlerMapping,
		translateService,
		localeContextResolver,
		Gson(),
		capacities,
		windowSeconds,
	)

	// AnnotationUtils.findAnnotation is expected to walk up to the interface method, since the impl
	// class itself never redeclares @RateLimited — mirroring every real IXxxV2Controller/impl pair.
	private fun handlerMethod(methodName: String): HandlerMethod =
		HandlerMethod(impl, AnnotatedResourceImpl::class.java.getMethod(methodName))

	private fun exchange(path: String = "/api/v2/anything", queryParam: String? = null, remoteHost: String = "10.0.0.1"): ServerWebExchange {
		val uri = if (queryParam != null) "$path?$queryParam" else path
		val request = MockServerHttpRequest.method(GET, uri)
			.remoteAddress(InetSocketAddress(remoteHost, 12345))
			.build()
		return MockServerWebExchange.from(request)
	}

	@Test
	fun `Should filter let the request through when no handler method is resolved`() {
		whenever(handlerMapping.getHandler(any())).thenReturn(Mono.empty())

		handler().filter(exchange(), chain).block()

		assertEquals(1, chainCalls.get())
	}

	@Test
	fun `Should filter let the request through when the handler method has no RateLimited annotation`() {
		whenever(handlerMapping.getHandler(any())).thenReturn(Mono.just(handlerMethod("unannotatedOp")))

		repeat(5) { handler(capacities = mapOf(SENSITIVE to 1, SEARCH to 1)).filter(exchange(), chain).block() }

		assertEquals(5, chainCalls.get())
	}

	@Test
	fun `Should filter allow up to capacity requests then return 429`() {
		whenever(handlerMapping.getHandler(any())).thenReturn(Mono.just(handlerMethod("sensitiveOp")))
		val limiter = handler(capacities = mapOf(SENSITIVE to 2, SEARCH to 2))
		val ex = exchange()

		limiter.filter(ex, chain).block()
		limiter.filter(ex, chain).block()
		limiter.filter(ex, chain).block()

		assertEquals(2, chainCalls.get())
		assertEquals(TOO_MANY_REQUESTS, ex.response.statusCode)
	}

	@Test
	fun `Should filter track requests per client independently`() {
		whenever(handlerMapping.getHandler(any())).thenReturn(Mono.just(handlerMethod("sensitiveOp")))
		val limiter = handler(capacities = mapOf(SENSITIVE to 1, SEARCH to 1))
		val clientA = exchange(remoteHost = "10.0.0.1")
		val clientB = exchange(remoteHost = "10.0.0.2")

		limiter.filter(clientA, chain).block()
		limiter.filter(clientB, chain).block()
		limiter.filter(clientA, chain).block()

		assertEquals(2, chainCalls.get())
		assertEquals(TOO_MANY_REQUESTS, clientA.response.statusCode)
		assertEquals(OK, clientB.response.statusCode)
	}

	@Test
	fun `Should filter only enforce whenParamPresent when the listed query param is present`() {
		whenever(handlerMapping.getHandler(any())).thenReturn(Mono.just(handlerMethod("searchOp")))
		val limiter = handler(capacities = mapOf(SENSITIVE to 1, SEARCH to 1))

		repeat(5) { limiter.filter(exchange(), chain).block() }
		assertEquals(5, chainCalls.get())

		val withQuery = exchange(queryParam = "q=hello")
		limiter.filter(withQuery, chain).block()
		limiter.filter(withQuery, chain).block()

		assertEquals(6, chainCalls.get())
		assertEquals(TOO_MANY_REQUESTS, withQuery.response.statusCode)
	}

	@Test
	fun `Should filter treat a zero capacity as a disabled category`() {
		whenever(handlerMapping.getHandler(any())).thenReturn(Mono.just(handlerMethod("sensitiveOp")))
		val limiter = handler(capacities = mapOf(SENSITIVE to 0, SEARCH to 1))

		repeat(5) { limiter.filter(exchange(), chain).block() }

		assertEquals(5, chainCalls.get())
	}
}
