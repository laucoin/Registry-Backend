package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.port.IProjectFavoritePort
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class ProjectFavoriteHelperTest {
	private val port: IProjectFavoritePort = mock()
	private val helper = ProjectFavoriteHelper(port)
	private val userId = UUID.randomUUID()

	@Test
	fun `Should markFavorites flag only the projects the user marked as favorite`() {
		// Arrange
		val favorite = ProjectModel().apply { id = UUID.randomUUID() }
		val other = ProjectModel().apply { id = UUID.randomUUID() }
		whenever(port.findFavoriteProjectIds(userId, listOf(favorite.id!!, other.id!!))).thenReturn(Flux.just(favorite.id!!))

		// Act
		val result = helper.markFavorites(userId, listOf(favorite, other))

		// Assert
		StepVerifier.create(result).expectNext(Unit).verifyComplete()
		assertEquals(true, favorite.favorite)
		assertEquals(false, other.favorite)
	}

	@Test
	fun `Should markFavorites query each project once`() {
		// Arrange
		val project = ProjectModel().apply { id = UUID.randomUUID() }
		val sameProject = ProjectModel().apply { id = project.id }
		whenever(port.findFavoriteProjectIds(userId, listOf(project.id!!))).thenReturn(Flux.empty())

		// Act
		val result = helper.markFavorites(userId, listOf(project, sameProject))

		// Assert
		StepVerifier.create(result).expectNext(Unit).verifyComplete()
		assertEquals(false, project.favorite)
		assertEquals(false, sameProject.favorite)
	}

	@Test
	fun `Should markFavorites skip the query when there is no project`() {
		// Act
		val result = helper.markFavorites(userId, emptyList())

		// Assert
		StepVerifier.create(result).expectNext(Unit).verifyComplete()
		verify(port, never()).findFavoriteProjectIds(any(), any())
	}
}
