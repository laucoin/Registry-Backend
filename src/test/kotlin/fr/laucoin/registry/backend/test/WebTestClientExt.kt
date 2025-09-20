package fr.laucoin.registry.backend.test

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.ErrorDto
import fr.laucoin.registry.backend.test.ModelExt.projectId
import java.net.URI
import java.util.Objects
import java.util.UUID
import java.util.function.Function
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono

object WebTestClientExt {
	private const val FIRST_NAME = "John"
	private const val LAST_NAME = "DOE"


	private val user = UserModel(
		oidcId = UUID.fromString("08513d7b-cce1-4efe-bb27-8d936c4a12b4"),
		firstName = FIRST_NAME,
		lastName = LAST_NAME,
		email = "$FIRST_NAME.$LAST_NAME@test.com"
	).apply {
		id = UUID.fromString("9cd10ea7-96c1-4f82-8366-d11d2e3ec300")
		oidcId = UUID.fromString("08513d7b-cce1-4efe-bb27-8d936c4a12b4")
	}

	fun buildAuthority(authority: String): String = "${projectId}_$authority"

	fun WebTestClient.authenticate(vararg authorities: String): WebTestClient {
		val currentUser = currentUser(*authorities)
		authentication(currentUser)
		return mutateWith(mockUser(currentUser))
	}

	fun authenticate(vararg authorities: String): Authentication {
		val currentUser = currentUser(*authorities)
		return authentication(currentUser)
	}

	fun currentUser(vararg authorities: String): CurrentUserModel {
		val currentUser = CurrentUserModel(user)
		currentUser.promote(authorities.asList())
		return currentUser
	}

	private fun authentication(currentUser: CurrentUserModel): Authentication {
		val authentication = UsernamePasswordAuthenticationToken(currentUser, null, currentUser.authorities)
		ReactiveSecurityContextHolder.withSecurityContext(Mono.just(SecurityContextImpl(authentication)))
		return authentication
	}

	fun uriBuilder(uri: String, params: List<Any>, queryParams: List<Pair<String, Any?>>): Function<UriBuilder, URI> {
		return Function {
			it.path(uri)
				.queryParams(buildQueryParams(queryParams))
				.build(*params.toTypedArray())
		}
	}

	private fun buildQueryParams(queryParams: List<Pair<String, Any?>>): MultiValueMap<String, String> {
		val formattedQueryParams = LinkedMultiValueMap<String, String>()
		queryParams
			.filter { (_, value) -> Objects.nonNull(value) }
			.forEach { (key, value) -> formattedQueryParams.add(key, value.toString()) }
		return formattedQueryParams
	}

	inline fun <reified T> ResponseSpec.body(status: HttpStatus): T? {
		val responseType = object: ParameterizedTypeReference<T>() {}

		return expectStatus().isEqualTo(status.value())
			.expectBody(responseType)
			.returnResult()
			.responseBody
	}

	fun ResponseSpec.assertError(expectedStatus: HttpStatus, expectedCode: String): ResponseSpec {
		val error = body<Map<*, *>>(expectedStatus)
		assertEquals(expectedStatus.value(), error?.get(ErrorDto::statusCode.name))
		assertEquals(expectedStatus.name, error?.get(ErrorDto::statusName.name))
		assertEquals(expectedCode, error?.get(ErrorDto::code.name))
		assertNotNull(error?.get(ErrorDto::title.name))
		assertNotNull(error?.get(ErrorDto::message.name))
		return this
	}
}
