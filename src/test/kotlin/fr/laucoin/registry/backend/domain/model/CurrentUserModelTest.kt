package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class CurrentUserModelTest {
	private val projectId = UUID.randomUUID()
	private val otherProjectId = UUID.randomUUID()

	@Test
	fun `Should visibilityFor keep the requested value when the permission is held`() {
		// Arrange
		val user = currentUser("${projectId}_REGISTRY_PROJECT_GROUP_D")

		// Act & Assert
		assertEquals(false, user.visibilityFor(false, projectId, "REGISTRY_PROJECT_GROUP_D"))
		assertEquals(null, user.visibilityFor(null, projectId, "REGISTRY_PROJECT_GROUP_D"))
	}

	@Test
	fun `Should visibilityFor only return visible elements without the permission`() {
		// Arrange
		val user = currentUser("${projectId}_REGISTRY_PROJECT_GROUP_U", "${otherProjectId}_REGISTRY_PROJECT_GROUP_D")

		// Act & Assert
		assertEquals(true, user.visibilityFor(false, projectId, "REGISTRY_PROJECT_GROUP_D"))
		assertEquals(true, user.visibilityFor(null, projectId, "REGISTRY_PROJECT_GROUP_D"))
		assertEquals(true, user.visibilityFor(true, projectId, "REGISTRY_PROJECT_GROUP_D"))
	}

	@Test
	fun `Should projectIdsWith return the projects holding exactly the permission`() {
		// Arrange
		val user = currentUser(
			"${projectId}_REGISTRY_PROJECT_D",
			"${otherProjectId}_REGISTRY_PROJECT_PARTICIPANT_D",
			"REGISTRY_PROJECT_D",
		)

		// Act & Assert
		assertEquals(listOf(projectId), user.projectIdsWith("REGISTRY_PROJECT_D"))
	}
}
