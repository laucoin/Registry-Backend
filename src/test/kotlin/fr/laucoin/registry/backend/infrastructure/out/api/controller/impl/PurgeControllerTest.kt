package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_JOB_C
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectConfigurationPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectContentPurgeReaderDto
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux

class PurgeControllerTest: TestContext() {
	@MockitoBean
	private lateinit var userService: IUserService

	@MockitoBean
	private lateinit var projectService: IProjectService

	@MockitoBean
	private lateinit var movementService: IMovementService

	@MockitoBean
	private lateinit var alertService: IAlertService

	@MockitoBean
	private lateinit var communicationService: ICommunicationService

	@MockitoBean
	private lateinit var activityService: IActivityService

	@MockitoBean
	private lateinit var vehicleService: IVehicleService

	@MockitoBean
	private lateinit var participantService: IParticipantService

	@MockitoBean
	private lateinit var groupService: IGroupService

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/purge"
	}

	@Test
	fun `Should purgeUsersIfNecessary should return 200`() {
		// Arrange
		whenever(userService.purgeUsersIfNecessary(any(), any())).thenReturn(Flux.empty())

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

		verify(userService).purgeUsersIfNecessary(any(), any())
		verifyNoInteractions(projectService)
		verifyNoInteractions(movementService)
		verifyNoInteractions(alertService)
		verifyNoInteractions(communicationService)
		verifyNoInteractions(activityService)
		verifyNoInteractions(vehicleService)
		verifyNoInteractions(participantService)
		verifyNoInteractions(groupService)
	}

	@Test
	fun `Should purgeProjectsIfNecessary should return 200`() {
		// Arrange
		whenever(projectService.purgeProjectsIfNecessary(any(), any())).thenReturn(Flux.empty())

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

		verifyNoInteractions(userService)
		verify(projectService).purgeProjectsIfNecessary(any(), any())
		verifyNoInteractions(movementService)
		verifyNoInteractions(alertService)
		verifyNoInteractions(communicationService)
		verifyNoInteractions(activityService)
		verifyNoInteractions(vehicleService)
		verifyNoInteractions(participantService)
		verifyNoInteractions(groupService)
	}

	@Test
	fun `Should purgeProjectsContentsIfNecessary should return 200`() {
		// Arrange
		whenever(movementService.purgeMovementsIfNecessary(any(), any())).thenReturn(Flux.just(UUID.randomUUID()))
		whenever(alertService.purgeAlertsIfNecessary(any(), any())).thenReturn(Flux.just(UUID.randomUUID()))
		whenever(
			communicationService.purgeOrphanCommunications(
				any(),
				any(),
				any()
			)
		).thenReturn(Flux.just(UUID.randomUUID()))

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

		verifyNoInteractions(userService)
		verifyNoInteractions(projectService)
		verify(movementService).purgeMovementsIfNecessary(any(), any())
		verify(alertService).purgeAlertsIfNecessary(any(), any())
		verify(communicationService).purgeOrphanCommunications(any(), any(), any())
		verifyNoInteractions(activityService)
		verifyNoInteractions(vehicleService)
		verifyNoInteractions(participantService)
		verifyNoInteractions(groupService)
	}

	@Test
	fun `Should purgeProjectsConfigurationsIfNecessary should return 200`() {
		// Arrange
		whenever(activityService.purgeActivitiesIfNecessary(any(), any())).thenReturn(Flux.just(UUID.randomUUID()))
		whenever(vehicleService.purgeVehiclesIfNecessary(any(), any())).thenReturn(Flux.just(UUID.randomUUID()))
		whenever(participantService.purgeParticipantsIfNecessary(any(), any())).thenReturn(Flux.just(UUID.randomUUID()))
		whenever(groupService.purgeEmptyGroups(any(), any())).thenReturn(Flux.just(UUID.randomUUID()))

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

		verifyNoInteractions(userService)
		verifyNoInteractions(projectService)
		verifyNoInteractions(movementService)
		verifyNoInteractions(alertService)
		verifyNoInteractions(communicationService)
		verify(activityService).purgeActivitiesIfNecessary(any(), any())
		verify(vehicleService).purgeVehiclesIfNecessary(any(), any())
		verify(participantService).purgeParticipantsIfNecessary(any(), any())
		verify(groupService).purgeEmptyGroups(any(), any())
	}
}
