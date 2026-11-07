package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.core.IProjectProfileSearchService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux

class ProjectProfileSearchServiceTest {
	private val userPort: IUserPort = mock()
	private val service: IProjectProfileSearchService = ProjectProfileSearchService(userPort)

	@Test
	fun `Should searchUsers call port findByEmails with the trimmed exact email`() {
		// Arrange
		val user = UserModel()
		whenever(userPort.findByEmails(any(), any())).thenReturn(Flux.just(user))

		// Act
		val result = service.searchUsers(" ada@example.com ").collectList().block()

		// Assert
		assertEquals(listOf(user), result)
		verify(userPort).findByEmails(listOf("ada@example.com"), isVisible = true)
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = ["  "])
	fun `Should searchUsers return nothing without querying the port when the query is blank`(query: String?) {
		// Act
		val result = service.searchUsers(query).collectList().block()

		// Assert
		assertEquals(emptyList<UserModel>(), result)
		verify(userPort, never()).findByEmails(any(), any())
	}
}
