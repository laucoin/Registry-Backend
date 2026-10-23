package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PurgeError.PURGE_DATE_THRESHOLD_IN_FUTURE
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_JOB_C
import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import fr.laucoin.registry.backend.domain.service.IPurgeService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectConfigurationPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectContentPurgeReaderDto
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class PurgeControllerTest: TestContext() {
	@MockitoBean
	private lateinit var purgeService: IPurgeService

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/purge"
	}

	@Test
	fun `Should purgeUsersIfNecessary should return 200`() {
		// Arrange
		whenever(purgeService.purgeUsers(any(), any())).thenReturn(Flux.empty())

		// Act
		val result = webClient
			.authenticate(REGISTRY_JOB_C)
			.post()
			.uri(
				uriBuilder(
					"$BASE_URL/users",
					listOf(projectId),
					listOf(
						Pair("dateThreshold", "2023-01-01"),
						Pair("dryRun", true),
					),
				)
			)
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(purgeService).purgeUsers(any(), any())
	}

	@Test
	fun `Should purgeProjectsIfNecessary should return 200`() {
		// Arrange
		whenever(purgeService.purgeProjects(any(), any())).thenReturn(Flux.empty())

		// Act
		val result = webClient
			.authenticate(REGISTRY_JOB_C)
			.post()
			.uri(
				uriBuilder(
					"$BASE_URL/projects",
					listOf(projectId),
					listOf(
						Pair("dateThreshold", "2023-01-01"),
						Pair("dryRun", true),
					),
				)
			)
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(purgeService).purgeProjects(any(), any())
	}

	@Test
	fun `Should purgeProjectsContentsIfNecessary should return 200`() {
		// Arrange
		whenever(purgeService.purgeProjectsContents(any(), any()))
			.thenReturn(Mono.just(ProjectContentPurgeModel(listOf(UUID.randomUUID()), emptyList(), emptyList())))

		// Act
		val result = webClient
			.authenticate(REGISTRY_JOB_C)
			.post()
			.uri(
				uriBuilder(
					"$BASE_URL/projects/contents",
					listOf(projectId),
					listOf(
						Pair("dateThreshold", "2023-01-01"),
						Pair("dryRun", true),
					),
				)
			)
			.exchange()

		// Assert
		result.body<ProjectContentPurgeReaderDto>(OK)

		verify(purgeService).purgeProjectsContents(any(), any())
	}

	@Test
	fun `Should purgeProjectsConfigurationsIfNecessary should return 200`() {
		// Arrange
		whenever(purgeService.purgeProjectsConfigurations(any(), any()))
			.thenReturn(Mono.just(ProjectConfigurationPurgeModel(emptyList(), emptyList(), emptyList(), emptyList())))

		// Act
		val result = webClient
			.authenticate(REGISTRY_JOB_C)
			.post()
			.uri(
				uriBuilder(
					"$BASE_URL/projects/configurations",
					listOf(projectId),
					listOf(
						Pair("dateThreshold", "2023-01-01"),
						Pair("dryRun", true),
					),
				)
			)
			.exchange()

		// Assert
		result.body<ProjectConfigurationPurgeReaderDto>(OK)

		verify(purgeService).purgeProjectsConfigurations(any(), any())
	}

	@ParameterizedTest
	@ValueSource(strings = ["/users", "/projects", "/projects/contents", "/projects/configurations"])
	fun `Should reject a purge threshold in the future`(endpoint: String) {
		// Arrange
		val tomorrow = LocalDate.now().plusDays(1)

		// Act
		val result = webClient
			.authenticate(REGISTRY_JOB_C)
			.post()
			.uri(
				uriBuilder(
					"$BASE_URL$endpoint",
					listOf(projectId),
					listOf(
						Pair("dateThreshold", tomorrow.toString()),
						Pair("dryRun", false),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PURGE_DATE_THRESHOLD_IN_FUTURE)

		verifyNoInteractions(purgeService)

	}

	/**
	 * Purging more aggressively than the configured retention window is a legitimate operation, so
	 * the constraint must reject future dates only — never dates that are merely more recent than
	 * the threshold in configuration.
	 */
	@ParameterizedTest
	@ValueSource(ints = [0, -1])
	fun `Should accept a purge threshold more recent than the configured retention window`(daysFromToday: Long) {
		// Arrange
		val threshold = LocalDate.now().plusDays(daysFromToday)

		whenever(purgeService.purgeUsers(any(), any())).thenReturn(Flux.empty())

		// Act
		val result = webClient
			.authenticate(REGISTRY_JOB_C)
			.post()
			.uri(
				uriBuilder(
					"$BASE_URL/users",
					listOf(projectId),
					listOf(
						Pair("dateThreshold", threshold.toString()),
						Pair("dryRun", true),
					),
				)
			)
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(purgeService).purgeUsers(threshold, true)
	}
}
