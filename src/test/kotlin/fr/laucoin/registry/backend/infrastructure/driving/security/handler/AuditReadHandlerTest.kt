package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import fr.laucoin.registry.backend.domain.enumeration.AuditOutcomeEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditResourceTypeEnum
import fr.laucoin.registry.backend.domain.model.ClientContextModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.support.IAuditReadService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.ClientContextService
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.HEAD
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class AuditReadHandlerTest {
	private val auditReadService: IAuditReadService = mock()
	private val handler = AuditReadHandler(auditReadService, ClientContextService(emptyList()))
	private val chainCalls = AtomicInteger(0)
	private val client = ClientContextModel("203.0.113.7", "JUnit/1.0")
	private val user = CurrentUserModel().apply { id = UUID.randomUUID() }
	private val projectId = UUID.randomUUID()
	private val resourceId = UUID.randomUUID()

	private fun chain(status: HttpStatus) = WebFilterChain {
		chainCalls.incrementAndGet()
		it.response.statusCode = status
		Mono.empty()
	}

	private fun exchange(path: String, method: HttpMethod = GET): ServerWebExchange {
		val request = MockServerHttpRequest.method(method, path)
			.remoteAddress(InetSocketAddress("203.0.113.7", 12345))
			.header("User-Agent", "JUnit/1.0")
			.build()
		return MockServerWebExchange.from(request)
	}

	private fun call(path: String, status: HttpStatus = OK, method: HttpMethod = GET, authenticated: Boolean = true) {
		val flow = handler.filter(exchange(path, method), chain(status))
		val context = UsernamePasswordAuthenticationToken(user, null)
		(if (authenticated) flow.contextWrite(ReactiveSecurityContextHolder.withAuthentication(context)) else flow).block()
	}

	@ParameterizedTest
	@MethodSource("auditedPaths")
	fun `Should filter record the read of an audited path`(
		path: String,
		type: AuditResourceTypeEnum,
		withResource: Boolean,
		withProject: Boolean,
	) {
		// Arrange
		val expectedResource = if (withResource) resourceId else null
		val expectedProject = if (withProject) projectId else null

		// Act
		call(path.replace("{p}", "$projectId").replace("{id}", "$resourceId"))

		// Assert
		verify(auditReadService).recordRead(user, type, expectedResource, expectedProject, client, AuditOutcomeEnum.SUCCESS)
	}

	@Test
	fun `Should filter record a refused read as FORBIDDEN`() {
		// Arrange
		val path = "/api/v2/projects/$projectId/participants/$resourceId"

		// Act
		call(path, FORBIDDEN)

		// Assert
		verify(auditReadService)
			.recordRead(user, AuditResourceTypeEnum.PARTICIPANT, resourceId, projectId, client, AuditOutcomeEnum.FORBIDDEN)
	}

	@ParameterizedTest
	@MethodSource("ignoredReads")
	fun `Should filter ignore a read that is not an audited success or refusal`(path: String, status: HttpStatus) {
		// Arrange

		// Act
		call(path.replace("{p}", "$projectId").replace("{id}", "$resourceId"), status)

		// Assert
		verify(auditReadService, never()).recordRead(any(), any(), any(), any(), any(), any())
	}

	@Test
	fun `Should filter ignore a write and an unauthenticated read`() {
		// Arrange
		val path = "/api/v2/users/$resourceId"

		// Act
		call(path, method = POST)
		call(path, authenticated = false)

		// Assert
		verify(auditReadService, never()).recordRead(any(), any(), any(), any(), any(), any())
		assertEquals(2, chainCalls.get())
	}

	@Test
	fun `Should filter record a read as a success when the response keeps its implicit status`() {
		// Arrange
		val path = "/api/v2/users/$resourceId"
		val untouched = WebFilterChain { Mono.empty() }
		val context = UsernamePasswordAuthenticationToken(user, null)

		// Act
		handler.filter(exchange(path), untouched).contextWrite(ReactiveSecurityContextHolder.withAuthentication(context)).block()

		// Assert
		verify(auditReadService).recordRead(user, AuditResourceTypeEnum.USER, resourceId, null, client, AuditOutcomeEnum.SUCCESS)
	}

	@Test
	fun `Should filter record the HEAD read like the GET one`() {
		// Arrange
		val path = "/api/v2/users/$resourceId"

		// Act
		call(path, method = HEAD)

		// Assert
		verify(auditReadService).recordRead(user, AuditResourceTypeEnum.USER, resourceId, null, client, AuditOutcomeEnum.SUCCESS)
	}

	companion object {
		@JvmStatic
		fun auditedPaths(): Stream<Arguments> = Stream.of(
			Arguments.of("/api/v2/users", AuditResourceTypeEnum.USER, false, false),
			Arguments.of("/api/v2/users/{id}", AuditResourceTypeEnum.USER, true, false),
			Arguments.of("/api/v2/projects/{p}/participants", AuditResourceTypeEnum.PARTICIPANT, false, true),
			Arguments.of("/api/v2/projects/{p}/participants/birthday", AuditResourceTypeEnum.PARTICIPANT, false, true),
			Arguments.of("/api/v2/projects/{p}/participants/search/users", AuditResourceTypeEnum.PARTICIPANT, false, true),
			Arguments.of("/api/v2/projects/{p}/participants/{id}", AuditResourceTypeEnum.PARTICIPANT, true, true),
			Arguments.of("/api/v2/projects/{p}/participants/{id}/timeline", AuditResourceTypeEnum.PARTICIPANT, true, true),
			Arguments.of("/api/v2/projects/{p}/participants/{id}/movements", AuditResourceTypeEnum.PARTICIPANT, true, true),
			Arguments.of("/api/v2/projects/{p}/movements", AuditResourceTypeEnum.MOVEMENT, false, true),
			Arguments.of("/api/v2/projects/{p}/movements/contents", AuditResourceTypeEnum.MOVEMENT, false, true),
			Arguments.of("/api/v2/projects/{p}/movements/search/participants-and-groups", AuditResourceTypeEnum.MOVEMENT, false, true),
			Arguments.of("/api/v2/projects/{p}/movements/{id}", AuditResourceTypeEnum.MOVEMENT, true, true),
		)

		@JvmStatic
		fun ignoredReads(): Stream<Arguments> = Stream.of(
			Arguments.of("/api/v2/users/roles", OK),
			Arguments.of("/api/v2/users/current/export", OK),
			Arguments.of("/api/v2/users/{id}/export", OK),
			Arguments.of("/api/v2/projects/{p}/participants/{id}/export", OK),
			Arguments.of("/api/v2/projects/{p}/movements/planned/arrivals", OK),
			Arguments.of("/api/v2/projects/{p}/groups", OK),
			Arguments.of("/api/v2/projects/not-a-uuid/participants", OK),
			Arguments.of("/api/v2/projects/{p}/participants/{id}", NOT_FOUND),
			Arguments.of("/api/v2/users", INTERNAL_SERVER_ERROR),
		)
	}
}
