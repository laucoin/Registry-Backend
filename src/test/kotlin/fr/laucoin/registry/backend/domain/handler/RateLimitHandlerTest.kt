package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.constant.AuditConst.CORRELATION_ID_HEADER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.RATE_LIMIT_EXCEEDED
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_METADATA_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_U
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.test.ModelExt.commonUser
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * ADR 019 §1 — per-user limits on sensitive operations. Tiny capacities via
 * test properties: this also forces a dedicated Spring context, so exhausted
 * buckets can never leak into the shared cached context of other tests.
 */
@TestPropertySource(
	properties = [
		"registry.security.rate-limit.sensitive.capacity=3",
		"registry.security.rate-limit.sensitive.period-seconds=60",
		"registry.security.rate-limit.search.capacity=3",
		"registry.security.rate-limit.search.period-seconds=60",
	]
)
class RateLimitHandlerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IUserService

	@Autowired
	private lateinit var webClient: WebTestClient

	@Test
	fun `Should throttle sensitive operations per user with 429 and Retry-After`() {
		// Arrange
		whenever(service.blockUserById(any(), any())).thenReturn(Mono.just(commonUser()))
		val id = UUID.randomUUID()

		// Act
		repeat(3) {
			webClient
				.authenticate(REGISTRY_USER_U)
				.post()
				.uri(uriBuilder("/api/v2/users/{id}/block", listOf(id), emptyList()))
				.exchange()
				.expectStatus().isOk
		}

		// Assert
		val result = webClient
			.authenticate(REGISTRY_USER_U)
			.post()
			.uri(uriBuilder("/api/v2/users/{id}/block", listOf(id), emptyList()))
			.exchange()
			.expectHeader().exists("Retry-After")
			.expectHeader().exists(CORRELATION_ID_HEADER)
		result.assertError(TOO_MANY_REQUESTS, RATE_LIMIT_EXCEEDED)
	}

	@Test
	fun `Should throttle search operations per user with 429 and Retry-After`() {
		// Arrange
		whenever(service.assignableUserRoles(any())).thenReturn(Flux.just("USER"))

		// Act
		repeat(3) {
			webClient
				.authenticate(REGISTRY_USER_METADATA_R)
				.get()
				.uri(uriBuilder("/api/v2/users/assignable-roles", emptyList(), emptyList()))
				.exchange()
				.expectStatus().isOk
		}

		// Assert
		val result = webClient
			.authenticate(REGISTRY_USER_METADATA_R)
			.get()
			.uri(uriBuilder("/api/v2/users/assignable-roles", emptyList(), emptyList()))
			.exchange()
			.expectHeader().exists("Retry-After")
		result.assertError(TOO_MANY_REQUESTS, RATE_LIMIT_EXCEEDED)
	}

	/**
	 * Also checks that a well-formed caller id is echoed back (ADR 019 §5 / ADR 020).
	 */
	@Test
	fun `Should leave plain reads unlimited and carry the API headers`() {
		// Arrange
		whenever(service.findUserById(any(), anyOrNull())).thenReturn(Mono.just(commonUser()))
		val id = UUID.randomUUID()

		// Act + Assert
		webClient
			.authenticate(REGISTRY_USER_R)
			.get()
			.uri(uriBuilder("/api/v2/users/{id}", listOf(id), emptyList()))
			.header(CORRELATION_ID_HEADER, "e2e-correlation-000001")
			.exchange()
			.expectStatus().isOk
			.expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
			.expectHeader().value("Cache-Control") { assert(it.contains("no-store")) }
			.expectHeader().valueEquals(CORRELATION_ID_HEADER, "e2e-correlation-000001")
	}
}

/**
 * ADR 019 §1 — capacity 0 is the deploy-time kill switch: the category is
 * disabled entirely and requests are never rejected.
 */
@TestPropertySource(
	properties = [
		"registry.security.rate-limit.sensitive.capacity=0",
		"registry.security.rate-limit.sensitive.period-seconds=60",
	]
)
class RateLimitDisabledHandlerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IUserService

	@Autowired
	private lateinit var webClient: WebTestClient

	@Test
	fun `Should disable a category entirely when its capacity is zero`() {
		// Arrange
		whenever(service.blockUserById(any(), any())).thenReturn(Mono.just(commonUser()))
		val id = UUID.randomUUID()

		// Act + Assert
		repeat(5) {
			webClient
				.authenticate(REGISTRY_USER_U)
				.post()
				.uri(uriBuilder("/api/v2/users/{id}/block", listOf(id), emptyList()))
				.exchange()
				.expectStatus().isOk
		}
	}
}
