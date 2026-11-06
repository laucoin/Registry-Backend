package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.model.ProfileFeatureModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.core.IProjectProfileSearchService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux

class ProjectProfileSearchServiceTest {
	private val userPort: IUserPort = mock()
	private val service: IProjectProfileSearchService =
		ProjectProfileSearchService(userPort, ProfileFeatureModel(ProfileFeatureModel.Searched(maxUserResult = MAX_USERS)))

	@Test
	fun `Should searchUsers call port findWithLimit`() {
		// Arrange
		val text = "text"
		whenever(userPort.findWithLimit(any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchUsers(text).blockFirst()

		// Assert
		verify(userPort).findWithLimit(eq(MAX_USERS), eq(UserSearchParamModel(text, isVisible = true)))
	}

	private companion object {
		private const val MAX_USERS = 1
	}
}
