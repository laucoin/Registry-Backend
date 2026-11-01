package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PurgeError.PURGE_DATE_THRESHOLD_IN_FUTURE
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_JOB_C
import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import fr.laucoin.registry.backend.domain.service.IPurgeService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectConfigurationPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectContentPurgeReaderDto
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PURGE_REPORTED_IDS_MAX
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
import java.time.ZoneOffset
import java.time.LocalDate
import java.util.UUID

class PurgeControllerTest : TestContext() {
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
					listOf(defaultProjectId),
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
	fun `Should purgeUsersIfNecessary report at most 1000 identifiers`() {
		// Arrange
		val ids = List(PURGE_REPORTED_IDS_MAX + 1) { UUID.randomUUID() }
		whenever(purgeService.purgeUsers(anyOrNull(), any())).thenReturn(Flux.fromIterable(ids))

		// Act
		val result = webClient
			.authenticate(REGISTRY_JOB_C)
			.post()
			.uri(uriBuilder("$BASE_URL/users", listOf(defaultProjectId), listOf(Pair("dryRun", true))))
			.exchange()

		// Assert
		val body = result.body<List<*>>(OK)
		assertEquals(PURGE_REPORTED_IDS_MAX, body!!.size)
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
					listOf(defaultProjectId),
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
					listOf(defaultProjectId),
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
					listOf(defaultProjectId),
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
		val tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1)

		// Act
		val result = webClient
			.authenticate(REGISTRY_JOB_C)
			.post()
			.uri(
				uriBuilder(
					"$BASE_URL$endpoint",
					listOf(defaultProjectId),
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
	 * Purger plus agressivement que la fenêtre de rétention configurée est une opération légitime ;
	 * la contrainte ne doit donc rejeter que les dates futures — jamais des dates simplement plus récentes que
	 * le seuil de configuration.
	 */
	@ParameterizedTest
	@ValueSource(ints = [0, -1])
	fun `Should accept a purge threshold more recent than the configured retention window`(daysFromToday: Long) {
		// Arrange
		val threshold = LocalDate.now(ZoneOffset.UTC).plusDays(daysFromToday)

		whenever(purgeService.purgeUsers(any(), any())).thenReturn(Flux.empty())

		// Act
		val result = webClient
			.authenticate(REGISTRY_JOB_C)
			.post()
			.uri(
				uriBuilder(
					"$BASE_URL/users",
					listOf(defaultProjectId),
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
