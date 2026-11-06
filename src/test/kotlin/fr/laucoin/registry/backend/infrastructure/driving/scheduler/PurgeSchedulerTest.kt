package fr.laucoin.registry.backend.infrastructure.driving.scheduler

import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import fr.laucoin.registry.backend.domain.service.usecase.IPurgeUseCase
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class PurgeSchedulerTest {
	private val purgeUseCase: IPurgeUseCase = mock()
	private val scheduler = PurgeScheduler(purgeUseCase)

	@Test
	fun `Should purgeUsers run a real purge with the configured threshold`() {
		// Arrange
		whenever(purgeUseCase.purgeUsers(null, false)).thenReturn(Flux.just(UUID.randomUUID()))

		// Act
		scheduler.purgeUsers()

		// Assert
		verify(purgeUseCase).purgeUsers(null, false)
	}

	@Test
	fun `Should purgeUsers not propagate a purge failure`() {
		// Arrange
		whenever(purgeUseCase.purgeUsers(null, false)).thenReturn(Flux.error(IllegalStateException("boom")))

		// Act
		scheduler.purgeUsers()

		// Assert
		verify(purgeUseCase).purgeUsers(null, false)
	}

	@Test
	fun `Should purgeProjects run a real purge with the configured threshold`() {
		// Arrange
		whenever(purgeUseCase.purgeProjects(null, false)).thenReturn(Flux.empty())

		// Act
		scheduler.purgeProjects()

		// Assert
		verify(purgeUseCase).purgeProjects(null, false)
	}

	@Test
	fun `Should purgeProjectsContents run a real purge with the configured threshold`() {
		// Arrange
		whenever(purgeUseCase.purgeProjectsContents(null, false))
			.thenReturn(Mono.just(ProjectContentPurgeModel(emptyList(), emptyList(), emptyList())))

		// Act
		scheduler.purgeProjectsContents()

		// Assert
		verify(purgeUseCase).purgeProjectsContents(null, false)
	}

	@Test
	fun `Should purgeProjectsConfigurations run a real purge with the configured threshold`() {
		// Arrange
		whenever(purgeUseCase.purgeProjectsConfigurations(null, false))
			.thenReturn(Mono.just(ProjectConfigurationPurgeModel(emptyList(), emptyList(), emptyList(), emptyList())))

		// Act
		scheduler.purgeProjectsConfigurations()

		// Assert
		verify(purgeUseCase).purgeProjectsConfigurations(null, false)
	}

	@Test
	fun `Should purgeAudits run a real purge with the configured threshold`() {
		// Arrange
		whenever(purgeUseCase.purgeAudits(null, false)).thenReturn(Mono.just(3L))

		// Act
		scheduler.purgeAudits()

		// Assert
		verify(purgeUseCase).purgeAudits(null, false)
	}

	@Test
	fun `Should purgeAudits not propagate a purge failure`() {
		// Arrange
		whenever(purgeUseCase.purgeAudits(null, false)).thenReturn(Mono.error(IllegalStateException("boom")))

		// Act
		scheduler.purgeAudits()

		// Assert
		verify(purgeUseCase).purgeAudits(null, false)
	}
}
