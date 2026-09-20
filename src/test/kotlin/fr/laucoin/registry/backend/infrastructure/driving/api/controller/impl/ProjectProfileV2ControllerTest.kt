package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ROLE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_U
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IProjectProfileService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CreatedProjectProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectProfilesWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CreatedProjectProfilesReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectProfileReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectProfileRoleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectProfileStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ProjectProfileWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ProjectProfilesWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ProjectProfileV2ControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IProjectProfileService

	@MockitoBean
	private lateinit var readerMapper: ProjectProfileReaderDtoMapper

	@MockitoBean
	private lateinit var createdProjectProfilesReaderMapper: CreatedProjectProfilesReaderDtoMapper

	@MockitoBean
	private lateinit var partialUserReaderMapper: PartialUserReaderDtoMapper

	@MockitoBean
	private lateinit var projectProfileRoleReaderMapper: ProjectProfileRoleReaderDtoMapper

	@MockitoBean
	private lateinit var projectProfileStatusReaderMapper: ProjectProfileStatusReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: ProjectProfileWriterDtoMapper

	@MockitoBean
	private lateinit var profilesWriterMapper: ProjectProfilesWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/profiles"
	}

	@Test
	fun `Should findProjectProfiles call service with page, size and empty sort by default`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 1, listOf(ProjectProfileModel()))
		whenever(service.findProjectProfilesPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findProjectProfilesPage(
			projectId,
			pageable,
			ProjectProfileSearchParamModel(textSearched = null, availabilitySearched = null, statusSearched = null, dateTimeSearched = null),
			emptyList(),
		)
		verify(readerMapper, atLeastOnce()).toDto(any())
	}

	@Test
	fun `Should findProjectProfiles return 400 when a query param is invalid`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("page", -1))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PAGE_NUMBER_IS_LOWER_THAN_ZERO)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findProjectProfiles return 400 on an unknown sort field`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "unknownField"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findProjectProfileById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findProjectProfileById(any(), any(), anyOrNull())).thenReturn(Mono.just(ProjectProfileModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).findProjectProfileById(projectId, uuid, visibilitySearched = null)
	}

	@Test
	fun `Should searchUsers rename textSearched to q`() {
		// Arrange
		val searched = "John"
		whenever(service.searchUsers(anyOrNull())).thenReturn(Flux.just(UserModel()))
		whenever(partialUserReaderMapper.toDto(any())).thenReturn(PartialUserReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/search/users", listOf(projectId), listOf(Pair("q", searched))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).searchUsers(searched)
	}

	@Test
	fun `Should getAssignableProjectProfileRoles return 200`() {
		// Arrange
		whenever(service.getAssignableProjectRoles(any(), any())).thenReturn(Flux.just("role"))
		whenever(projectProfileRoleReaderMapper.toDto(any())).thenReturn(LabelDto("value", "label"))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/roles", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).getAssignableProjectRoles(any(), eq(projectId))
	}

	@Test
	fun `Should createProjectProfiles return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val profiles = ProjectProfilesWriterDto(userIds = listOf(uuid), role = "ROLE")
		whenever(service.createProjectProfiles(any(), any(), any(), any()))
			.thenReturn(Mono.just(Pair(listOf(UUID.randomUUID()), emptyList())))
		whenever(profilesWriterMapper.toModels(any(), any())).thenReturn(emptyList())
		whenever(createdProjectProfilesReaderMapper.toDto(any())).thenReturn(CreatedProjectProfilesReaderDto(emptyList(), emptyList()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(profiles)
			.exchange()

		// Assert
		result.body<CreatedProjectProfilesReaderDto>(OK)
		verify(service).createProjectProfiles(any(), eq(projectId), eq(profiles.userIds!!), any())
	}

	@Test
	fun `Should createProjectProfiles return 207`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()
		val profiles = ProjectProfilesWriterDto(userIds = listOf(uuid, uuid2), role = "ROLE")
		whenever(service.createProjectProfiles(any(), any(), any(), any())).thenReturn(Mono.just(Pair(listOf(uuid2), listOf(uuid))))
		whenever(profilesWriterMapper.toModels(any(), any())).thenReturn(emptyList())
		whenever(createdProjectProfilesReaderMapper.toDto(any())).thenReturn(CreatedProjectProfilesReaderDto(listOf(uuid2), listOf(uuid)))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(profiles)
			.exchange()

		// Assert
		val body = result.body<CreatedProjectProfilesReaderDto>(MULTI_STATUS)
		assertEquals(uuid2, body?.createdUserIds?.first())
		assertEquals(uuid, body?.notCreatedUserIds?.first())
	}

	@Test
	fun `Should createProjectProfiles return 400`() {
		// Arrange
		val profiles = ProjectProfilesWriterDto(userIds = null, role = "ROLE")

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(profiles)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, "PROJECT_PROFILE_USERS_EMPTY")
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateProjectProfile return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val profile = ProjectProfileWriterDto(role = "ROLE")
		whenever(service.updateProjectProfileById(any(), any(), any(), any())).thenReturn(Mono.just(ProjectProfileModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(ProjectProfileModel())
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.bodyValue(profile)
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).updateProjectProfileById(any(), eq(projectId), eq(uuid), any())
	}

	@Test
	fun `Should updateProjectProfile return 400`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val profile = ProjectProfileWriterDto(role = "")

		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.bodyValue(profile)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PROJECT_PROFILE_ROLE_BLANK)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should blockProjectProfileById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.blockProjectProfileById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(ProjectProfileModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/block", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).blockProjectProfileById(any(), eq(projectId), eq(uuid))
	}

	@Test
	fun `Should unblockProjectProfileById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.unblockProjectProfileById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(ProjectProfileModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/unblock", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).unblockProjectProfileById(any(), eq(projectId), eq(uuid))
	}

	@Test
	fun `Should deleteProjectProfileById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.deleteProjectProfileById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)
		verify(service).deleteProjectProfileById(any(), eq(projectId), eq(uuid))
	}
}
