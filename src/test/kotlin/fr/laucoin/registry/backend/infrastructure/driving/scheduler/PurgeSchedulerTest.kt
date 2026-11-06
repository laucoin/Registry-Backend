package fr.laucoin.registry.backend.infrastructure.driving.scheduler

import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import fr.laucoin.registry.backend.domain.service.usecase.IPlatformPurgeUseCase
import fr.laucoin.registry.backend.domain.service.usecase.IProjectConfigurationPurgeUseCase
import fr.laucoin.registry.backend.domain.service.usecase.IProjectContentPurgeUseCase
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class PurgeSchedulerTest {
	private val platform: IPlatformPurgeUseCase = mock()
	private val content: IProjectContentPurgeUseCase = mock()
	private val configuration: IProjectConfigurationPurgeUseCase = mock()
	private val scheduler = PurgeScheduler(platform, content, configuration)

	@Test
	fun `Should purgeUsers consume every purged user with the configured threshold`() {
		// Arrange
		val consumed = AtomicInteger(0)
		val purged = Flux.just(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()).doOnNext { consumed.incrementAndGet() }
		whenever(platform.purgeUsers(null, false)).thenReturn(purged)

		// Act
		scheduler.purgeUsers()

		// Assert
		assertEquals(3, consumed.get())
	}

	@Test
	fun `Should purgeUsers not propagate a purge failure`() {
		// Arrange
		whenever(platform.purgeUsers(null, false)).thenReturn(Flux.error(IllegalStateException("boom")))

		// Act
		scheduler.purgeUsers()

		// Assert
		verify(platform).purgeUsers(null, false)
	}

	@Test
	fun `Should purgeProjects consume every purged project with the configured threshold`() {
		// Arrange
		val consumed = AtomicInteger(0)
		val purged = Flux.just(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()).doOnNext { consumed.incrementAndGet() }
		whenever(platform.purgeProjects(null, false)).thenReturn(purged)

		// Act
		scheduler.purgeProjects()

		// Assert
		assertEquals(3, consumed.get())
	}

	@Test
	fun `Should purgeProjectsContents run a real purge with the configured threshold`() {
		// Arrange
		whenever(content.purgeProjectsContents(null, false))
			.thenReturn(Mono.just(ProjectContentPurgeModel(emptyList(), emptyList(), emptyList())))

		// Act
		scheduler.purgeProjectsContents()

		// Assert
		verify(content).purgeProjectsContents(null, false)
	}

	@Test
	fun `Should purgeProjectsConfigurations run a real purge with the configured threshold`() {
		// Arrange
		whenever(configuration.purgeProjectsConfigurations(null, false))
			.thenReturn(Mono.just(ProjectConfigurationPurgeModel(emptyList(), emptyList(), emptyList(), emptyList())))

		// Act
		scheduler.purgeProjectsConfigurations()

		// Assert
		verify(configuration).purgeProjectsConfigurations(null, false)
	}

	@Test
	fun `Should purgeAudits run a real purge with the configured threshold`() {
		// Arrange
		whenever(platform.purgeAudits(null, false)).thenReturn(Mono.just(3L))

		// Act
		scheduler.purgeAudits()

		// Assert
		verify(platform).purgeAudits(null, false)
	}

	@Test
	fun `Should purgeAudits not propagate a purge failure`() {
		// Arrange
		whenever(platform.purgeAudits(null, false)).thenReturn(Mono.error(IllegalStateException("boom")))

		// Act
		scheduler.purgeAudits()

		// Assert
		verify(platform).purgeAudits(null, false)
	}
}
