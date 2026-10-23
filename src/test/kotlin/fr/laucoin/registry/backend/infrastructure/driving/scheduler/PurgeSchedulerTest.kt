package fr.laucoin.registry.backend.infrastructure.driving.scheduler

import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import fr.laucoin.registry.backend.domain.service.IPurgeService
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class PurgeSchedulerTest {
	private val purgeService: IPurgeService = mock()
	private val scheduler = PurgeScheduler(purgeService)

	@Test
	fun `Should purgeUsers run a real purge with the configured threshold`() {
		// Arrange
		whenever(purgeService.purgeUsers(null, false)).thenReturn(Flux.just(UUID.randomUUID()))

		// Act
		scheduler.purgeUsers()

		// Assert
		verify(purgeService).purgeUsers(null, false)
	}

	@Test
	fun `Should purgeUsers not propagate a purge failure`() {
		// Arrange
		whenever(purgeService.purgeUsers(null, false)).thenReturn(Flux.error(IllegalStateException("boom")))

		// Act
		scheduler.purgeUsers()

		// Assert
		verify(purgeService).purgeUsers(null, false)
	}

	@Test
	fun `Should purgeProjects run a real purge with the configured threshold`() {
		// Arrange
		whenever(purgeService.purgeProjects(null, false)).thenReturn(Flux.empty())

		// Act
		scheduler.purgeProjects()

		// Assert
		verify(purgeService).purgeProjects(null, false)
	}

	@Test
	fun `Should purgeProjectsContents run a real purge with the configured threshold`() {
		// Arrange
		whenever(purgeService.purgeProjectsContents(null, false))
			.thenReturn(Mono.just(ProjectContentPurgeModel(emptyList(), emptyList(), emptyList())))

		// Act
		scheduler.purgeProjectsContents()

		// Assert
		verify(purgeService).purgeProjectsContents(null, false)
	}

	@Test
	fun `Should purgeProjectsConfigurations run a real purge with the configured threshold`() {
		// Arrange
		whenever(purgeService.purgeProjectsConfigurations(null, false))
			.thenReturn(Mono.just(ProjectConfigurationPurgeModel(emptyList(), emptyList(), emptyList(), emptyList())))

		// Act
		scheduler.purgeProjectsConfigurations()

		// Assert
		verify(purgeService).purgeProjectsConfigurations(null, false)
	}
}
