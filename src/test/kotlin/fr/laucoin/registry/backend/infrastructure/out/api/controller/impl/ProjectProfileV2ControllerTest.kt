package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_EMAIL_INVALID
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_USERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_U
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.service.IProjectProfileService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CreatedProjectProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectProfilesWriterDto
import fr.laucoin.registry.backend.test.ModelExt.commonProjectProfile
import fr.laucoin.registry.backend.test.ModelExt.commonUser
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.ModelExt.userId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.test.assertEquals

class ProjectProfileV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IProjectProfileService

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/profiles"
	}

	private fun profilePage(): Mono<PageModel<ProjectProfileModel>> =
		Mono.just(PageModel(PageableModel(0, 20), 1, listOf(commonProjectProfile())))

	@Test
	fun `Should findProjectProfiles return 200 with the v2 list grammar`() {
		// Arrange
		whenever(service.findProjectProfilesPage(any(), any(), any())).thenReturn(profilePage())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(projectId),
					listOf(
						Pair("page", 2),
						Pair("size", 10),
						Pair("q", "john"),
						Pair("available", true),
						Pair("status", INVITED),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<ProjectProfileReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		val searchParamsCaptor = argumentCaptor<ProjectProfileSearchParamModel>()
		verify(service).findProjectProfilesPage(eq(projectId), pageableCaptor.capture(), searchParamsCaptor.capture())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
		assertEquals("john", searchParamsCaptor.firstValue.textSearched)
		assertEquals(true, searchParamsCaptor.firstValue.availabilitySearched)
		assertEquals(listOf(INVITED), searchParamsCaptor.firstValue.statusSearched)
	}

	@Test
	fun `Should findProjectProfiles return 403 without the read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should searchAssignableUsers return 200 as an eligibility sub-collection`() {
		// Arrange
		whenever(service.searchUsers(eq("john"))).thenReturn(Flux.just(commonUser()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/assignable-users", listOf(projectId), listOf(Pair("q", "john"))))
			.exchange()

		// Assert
		result.body<List<PartialUserReaderDto>>(OK)
		verify(service).searchUsers("john")
	}

	@Test
	fun `Should findProjectProfileById return 200 with the read permission`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.findProjectProfileById(any(), any(), anyOrNull()))
			.thenReturn(Mono.just(commonProjectProfile()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).findProjectProfileById(projectId, id, visibilitySearched = null)
	}

	@Test
	fun `Should findProjectProfileById return 403 without the read permission`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should getAssignableProjectProfileRoles return 200 with the metadata read permission`() {
		// Arrange
		whenever(service.getAssignableProjectRoles(any(), any())).thenReturn(Flux.just("PROJECT_PARTICIPANT"))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/roles", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		val roles = result.body<List<LabelDto>>(OK)
		assertEquals("PROJECT_PARTICIPANT", roles?.first()?.value)
		verify(service).getAssignableProjectRoles(any(), eq(projectId))
	}

	@Test
	fun `Should getAssignableProjectProfileRoles return 403 without the metadata read permission`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/roles", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createProjectProfiles return 200 with the create permission`() {
		// Arrange
		val profiles = ProjectProfilesWriterDto(userIds = listOf(userId), role = "PROJECT_PARTICIPANT")
		whenever(service.createProjectProfiles(any(), any(), any(), any(), any()))
			.thenReturn(Mono.just(Pair(listOf(userId), emptyList())))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(profiles)
			.exchange()

		// Assert
		val created = result.body<CreatedProjectProfilesReaderDto>(OK)
		assertEquals(listOf(userId), created?.createdUserIds)
		verify(service).createProjectProfiles(
			any(), eq(projectId), eq(listOf(userId)), eq(emptyList()), any()
		)
	}

	@Test
	fun `Should createProjectProfiles invite by email with the create permission`() {
		// Arrange
		val invitedUserId = UUID.randomUUID()
		val profiles = ProjectProfilesWriterDto(emails = listOf("invited@test.com"), role = "PROJECT_PARTICIPANT")
		whenever(service.createProjectProfiles(any(), any(), any(), any(), any()))
			.thenReturn(Mono.just(Pair(listOf(invitedUserId), emptyList())))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(profiles)
			.exchange()

		// Assert
		val created = result.body<CreatedProjectProfilesReaderDto>(OK)
		assertEquals(listOf(invitedUserId), created?.createdUserIds)
		verify(service).createProjectProfiles(
			any(), eq(projectId), eq(emptyList()), eq(listOf("invited@test.com")), any()
		)
	}

	@Test
	fun `Should createProjectProfiles return 400 when neither userIds nor emails are provided`() {
		// Arrange
		val profiles = ProjectProfilesWriterDto(role = "PROJECT_PARTICIPANT")

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(profiles)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PROJECT_PROFILE_USERS_EMPTY)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createProjectProfiles return 400 when an email is invalid`() {
		// Arrange
		val profiles = ProjectProfilesWriterDto(emails = listOf("not-an-email"), role = "PROJECT_PARTICIPANT")

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(profiles)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PROJECT_PROFILE_EMAIL_INVALID)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createProjectProfiles return 403 without the create permission`() {
		// Arrange
		val profiles = ProjectProfilesWriterDto(userIds = listOf(userId), role = "PROJECT_PARTICIPANT")

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(profiles)
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should deleteProjectProfileById return 200 with the delete permission`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.deleteProjectProfileById(any(), any(), any())).thenReturn(Mono.just(Unit))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<Unit>(OK)
		verify(service).deleteProjectProfileById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should deleteProjectProfileById return 403 without the delete permission`() {
		// Act
		val result = webClient
			.authenticate()
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should blockProjectProfileById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.blockProjectProfileById(any(), any(), any())).thenReturn(Mono.just(commonProjectProfile()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/block", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).blockProjectProfileById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should unblockProjectProfileById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.unblockProjectProfileById(any(), any(), any())).thenReturn(Mono.just(commonProjectProfile()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/unblock", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).unblockProjectProfileById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should updateProjectProfile patch changed fields from the body`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.updateProjectProfileById(any(), any(), any(), any()))
			.thenReturn(Mono.just(commonProjectProfile()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.bodyValue(ProjectProfileWriterDto(role = "ROLE_2"))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).updateProjectProfileById(any(), eq(projectId), eq(id), any())
	}
}
