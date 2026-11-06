package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.ParticipantFeatureModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.core.IParticipantSearchService
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux

class ParticipantSearchServiceTest {
	private val userPort: IUserPort = mock()
	private val groupPort: IGroupPort = mock()
	private val service: IParticipantSearchService = ParticipantSearchService(
		userPort,
		groupPort,
		ParticipantFeatureModel(ParticipantFeatureModel.Searched(maxUserResult = MAX_USERS, maxGroupResult = MAX_GROUPS)),
	)

	@Test
	fun `Should searchUsers call userPort findWithLimit`() {
		// Arrange
		val query = "text"
		val expectedSearch = UserSearchParamModel(query, isVisible = true)

		whenever(userPort.findWithLimit(any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchUsersByText(defaultProjectId, query).blockFirst()

		// Assert
		verify(userPort).findWithLimit(eq(MAX_USERS), argThat { this == expectedSearch && memberOfProjectId == defaultProjectId })
	}

	@Test
	fun `Should searchGroups call groupPort findWithLimit`() {
		// Arrange
		val query = "text"
		val expectedSearch = GroupSearchParamModel(query, isVisible = true)

		whenever(groupPort.findWithLimit(any(), any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchGroupsByText(defaultProjectId, query).blockFirst()

		// Assert
		verify(groupPort).findWithLimit(MAX_GROUPS, defaultProjectId, expectedSearch)
	}

	private companion object {
		private const val MAX_USERS = 1
		private const val MAX_GROUPS = 2
	}
}
