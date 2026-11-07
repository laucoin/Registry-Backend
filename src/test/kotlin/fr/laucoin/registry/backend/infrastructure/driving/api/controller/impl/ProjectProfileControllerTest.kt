package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.LIST_TOO_LARGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_FORMAT_INVALID
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ROLE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_USERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.RESOURCE_MODIFIED_SINCE_READ
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_RESTORE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_SOFT_DELETE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_U
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.usecase.IProjectProfileUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CreatedProjectProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectProfilesWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CreatedProjectProfilesReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectProfileReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectProfileRoleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectProfileStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ProjectProfileWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ProjectProfilesWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals

class ProjectProfileControllerTest : TestContext() {
	@MockitoBean
	private lateinit var useCase: IProjectProfileUseCase

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

		@JvmStatic
		fun `Should findProjectProfiles return 200`(): Stream<Arguments> = Stream.of(
			Arguments.of("not locale", null, null, null, null, null, null),
			Arguments.of(null, 0, null, null, null, null, null),
			Arguments.of(null, null, 200, null, null, null, null),
			Arguments.of(null, null, null, null, null, null, null),
			Arguments.of(null, null, null, "text", null, null, null),
			Arguments.of(null, null, null, null, true, null, null),
			Arguments.of(null, null, null, null, null, ACCEPTED, null),
			Arguments.of(null, null, null, null, null, INVITED, null),
			Arguments.of(null, null, null, null, null, BLOCKED, null),
			Arguments.of(null, null, null, null, null, REJECTED, null),
			Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
		)

		@JvmStatic
		fun `Should findProjectProfiles throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(1_000_001, null, PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE),
				Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}

		@JvmStatic
		fun `Should createProjectProfiles return 400`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				ProjectProfilesWriterDto(emails = null, role = "ROLE"),
				PROJECT_PROFILE_USERS_EMPTY,
				PROJECT_PROFILE_USERS_EMPTY,
			),
			Arguments.of(
				ProjectProfilesWriterDto(emails = emptyList(), role = "ROLE"),
				PROJECT_PROFILE_USERS_EMPTY,
			),
			Arguments.of(
				ProjectProfilesWriterDto(role = "ROLE"),
				PROJECT_PROFILE_USERS_EMPTY,
			),
			Arguments.of(
				ProjectProfilesWriterDto(emails = listOf("not-an-email"), role = "ROLE"),
				PARAMETER_FORMAT_INVALID,
			),
			Arguments.of(
				ProjectProfilesWriterDto(emails = listOf("${"a".repeat(320)}@sgdf.fr"), role = "ROLE"),
				PARAMETER_FORMAT_INVALID,
			),
			Arguments.of(
				ProjectProfilesWriterDto(emails = listOf("invited@sgdf.fr"), role = ""),
				PROJECT_PROFILE_ROLE_BLANK,
			),
			Arguments.of(
				ProjectProfilesWriterDto(emails = listOf("invited@sgdf.fr")),
				PROJECT_PROFILE_ROLE_BLANK,
			),
			Arguments.of(
				ProjectProfilesWriterDto(
					emails = listOf("invited@sgdf.fr"),
					role = "ROLE",
					startAccess = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
					endAccess = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
				),
				PROJECT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS,
			),
		)

		@JvmStatic
		fun `Should updateProjectProfile return 400`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				ProjectProfileWriterDto(role = ""),
				PROJECT_PROFILE_ROLE_BLANK,
			),
			Arguments.of(
				ProjectProfileWriterDto(),
				PROJECT_PROFILE_ROLE_BLANK,
			),
			Arguments.of(
				ProjectProfileWriterDto(
					role = "ROLE",
					startAccess = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
					endAccess = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
				),
				PROJECT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS,
			),
		)
	}

	@Test
	fun `Should findProjectProfiles pass the visibility filter independently of the status`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val searchParams = ProjectProfileSearchParamModel(status = ProfileStatusEnum.ACCEPTED, isVisible = false)
		whenever(useCase.findProjectProfilesPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(pageable, totalElements = 1, listOf(ProjectProfileModel()))))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ProjectProfileReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R), buildAuthority(REGISTRY_PROJECT_PROFILE_RESTORE))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(Pair("status", ProfileStatusEnum.ACCEPTED), Pair("isVisible", false)),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)
		verify(useCase).findProjectProfilesPage(defaultProjectId, pageable, searchParams)
	}

	@Test
	fun `Should findProjectProfiles only return visible profiles without the restore permission`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val searchParams = ProjectProfileSearchParamModel(isVisible = true)
		whenever(useCase.findProjectProfilesPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(pageable, totalElements = 1, listOf(ProjectProfileModel()))))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ProjectProfileReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("isVisible", false))))
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)
		verify(useCase).findProjectProfilesPage(defaultProjectId, pageable, searchParams)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findProjectProfiles return 200`(
		requestedLocale: String?,
		page: Int?,
		size: Int?,
		query: String?,
		isAvailable: Boolean?,
		status: ProfileStatusEnum?,
		at: String?,
	) {
		// Arrange
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber.toLong() * expectedPageSize, expectedPageSize)
		val searchParams = ProjectProfileSearchParamModel(
			query = query,
			isAvailable = isAvailable,
			status = status,
			at = at?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(ProjectProfileModel()))
		whenever(useCase.findProjectProfilesPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ProjectProfileReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R), buildAuthority(REGISTRY_PROJECT_PROFILE_RESTORE))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(
						Pair("page", page),
						Pair("size", size),
						Pair("q", query),
						Pair("isAvailable", isAvailable),
						Pair("status", status),
						Pair("at", at),
					),
				)
			)
			.headers { headers -> requestedLocale?.let { headers.add(ACCEPT_LANGUAGE, it) } }
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(useCase).findProjectProfilesPage(defaultProjectId, pageable, searchParams)
		verify(readerMapper).toDtoPage(any())
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(projectProfileRoleReaderMapper)
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(profilesWriterMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findProjectProfiles throw due to wrong params`(
		page: Int?,
		size: Int?,
		expectedMessage: String,
	) {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(
						Pair("page", page),
						Pair("size", size),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedMessage)

		verifyNoInteractions(useCase)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(projectProfileRoleReaderMapper)
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(profilesWriterMapper)
	}

	@Test
	fun `Should findProjectProfileById only look for a visible profile without the restore permission`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(useCase.findProjectProfileById(any(), any(), anyOrNull())).thenReturn(Mono.just(ProjectProfileModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)

		verify(useCase).findProjectProfileById(defaultProjectId, uuid, isVisible = true)
	}

	@Test
	fun `Should findProjectProfileById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(useCase.findProjectProfileById(any(), any(), anyOrNull())).thenReturn(Mono.just(ProjectProfileModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R), buildAuthority(REGISTRY_PROJECT_PROFILE_RESTORE))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)

		verify(useCase).findProjectProfileById(defaultProjectId, uuid, isVisible = null)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(projectProfileRoleReaderMapper)
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(profilesWriterMapper)
	}

	@Test
	fun `Should searchUsers return 200`() {
		// Arrange
		val searched = "John"
		val user = UserModel()

		whenever(useCase.searchUsers(anyOrNull())).thenReturn(Flux.just(user))
		whenever(partialUserReaderMapper.toDto(any())).thenReturn(PartialUserReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_METADATA_R))
			.get()
			.uri(
				uriBuilder("${BASE_URL}/search/users", listOf(defaultProjectId), listOf(Pair("q", searched)))
			)
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(useCase).searchUsers(searched)
		verifyNoInteractions(readerMapper)
		verify(partialUserReaderMapper).toDto(any())
		verifyNoInteractions(projectProfileRoleReaderMapper)
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(profilesWriterMapper)
	}

	@Test
	fun `Should getAssignableProjectProfileRoles return 200`() {
		// Arrange
		whenever(useCase.getAssignableProjectRoles(any(), any())).thenReturn(Flux.just("role"))
		whenever(projectProfileRoleReaderMapper.toDto(any())).thenReturn(LabelDto("value", "label"))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/roles", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verify(projectProfileRoleReaderMapper).toDto(any())
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(profilesWriterMapper)
		verify(useCase).getAssignableProjectRoles(any(), eq(defaultProjectId))
	}

	@Test
	fun `Should createProjectProfiles return 200`() {
		// Arrange
		val profiles = ProjectProfilesWriterDto(emails = listOf("invited@sgdf.fr"), role = "ROLE")
		whenever(useCase.createProjectProfiles(any(), any(), any())).thenReturn(
			Mono.just(Pair(listOf("invited@sgdf.fr"), emptyList()))
		)
		whenever(profilesWriterMapper.toModels(any(), any())).thenReturn(emptyList())
		whenever(createdProjectProfilesReaderMapper.toDto(any())).thenReturn(
			CreatedProjectProfilesReaderDto(
				emptyList(),
				emptyList()
			)
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(profiles)
			.exchange()

		// Assert
		result.body<CreatedProjectProfilesReaderDto>(OK)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(projectProfileRoleReaderMapper)
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(profilesWriterMapper).toModels(profiles, defaultProjectId)
		verify(createdProjectProfilesReaderMapper).toDto(any())
		verify(useCase).createProjectProfiles(any(), eq(defaultProjectId), any())
	}

	@Test
	fun `Should createProjectProfiles return 207`() {
		// Arrange
		val email = "first@sgdf.fr"
		val email2 = "second@sgdf.fr"
		val profiles = ProjectProfilesWriterDto(emails = listOf(email, email2), role = "ROLE")

		whenever(useCase.createProjectProfiles(any(), any(), any())).thenReturn(
			Mono.just(Pair(listOf(email2), listOf(email)))
		)
		whenever(profilesWriterMapper.toModels(any(), any())).thenReturn(emptyList())
		whenever(createdProjectProfilesReaderMapper.toDto(any())).thenReturn(
			CreatedProjectProfilesReaderDto(
				listOf(email2),
				listOf(email)
			)
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(profiles)
			.exchange()

		// Assert
		val body = result.body<CreatedProjectProfilesReaderDto>(MULTI_STATUS)
		assertEquals(email2, body?.createdEmails?.first())
		assertEquals(email, body?.notCreatedEmails?.first())

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(projectProfileRoleReaderMapper)
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(profilesWriterMapper).toModels(profiles, defaultProjectId)
		verify(useCase).createProjectProfiles(any(), eq(defaultProjectId), any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createProjectProfiles return 400`(
		profile: ProjectProfilesWriterDto,
		expectedCode: String,
	) {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(profile)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(projectProfileRoleReaderMapper)
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(profilesWriterMapper)
		verifyNoInteractions(useCase)
	}

	@Test
	fun `Should updateProjectProfile return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val profile = ProjectProfileWriterDto(role = "ROLE")

		whenever(
			useCase.updateProjectProfileById(
				any(),
				any(),
				any(),
				any()
			)
		).thenReturn(Mono.just(ProjectProfileModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(ProjectProfileModel())
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(profile)
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(projectProfileRoleReaderMapper)
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verify(writerMapper).toModel(profile, defaultProjectId)
		verifyNoInteractions(profilesWriterMapper)
		verify(useCase).updateProjectProfileById(any(), eq(defaultProjectId), eq(uuid), any())
	}

	@Test
	fun `Should updateProjectProfile accept the matching If-Match`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val profile = ProjectProfileWriterDto(role = "ROLE")

		whenever(
			useCase.updateProjectProfileById(
				any(),
				any(),
				any(),
				any()
			)
		).thenReturn(Mono.just(ProjectProfileModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(ProjectProfileModel())
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())
		whenever(useCase.findProjectProfileById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(ProjectProfileModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1791885731000\"")
			.bodyValue(profile)
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(projectProfileRoleReaderMapper)
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verify(writerMapper).toModel(profile, defaultProjectId)
		verifyNoInteractions(profilesWriterMapper)
		verify(useCase).updateProjectProfileById(any(), eq(defaultProjectId), eq(uuid), any())
	}

	@Test
	fun `Should updateProjectProfileById throw when the element changed since it was read`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val profile = ProjectProfileWriterDto(role = "ROLE")

		whenever(
			useCase.updateProjectProfileById(
				any(),
				any(),
				any(),
				any()
			)
		).thenReturn(Mono.just(ProjectProfileModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(ProjectProfileModel())
		whenever(useCase.findProjectProfileById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(ProjectProfileModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1\"")
			.bodyValue(profile)
			.exchange()

		// Assert
		result.assertError(PRECONDITION_FAILED, RESOURCE_MODIFIED_SINCE_READ)

		verify(useCase, never()).updateProjectProfileById(any(), eq(defaultProjectId), eq(uuid), any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateProjectProfile return 400`(
		profile: ProjectProfileWriterDto,
		expectedCode: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(profile)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(projectProfileRoleReaderMapper)
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(profilesWriterMapper)
		verifyNoInteractions(useCase)
	}

	@Test
	fun `Should blockProjectProfileById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(useCase.blockProjectProfileById(any(), eq(defaultProjectId), eq(uuid))).thenReturn(
			Mono.just(
				ProjectProfileModel()
			)
		)
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_SOFT_DELETE))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/block", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(projectProfileRoleReaderMapper)
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(profilesWriterMapper)
		verify(useCase).blockProjectProfileById(any(), eq(defaultProjectId), eq(uuid))
	}

	@Test
	fun `Should unblockProjectProfileById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(useCase.unblockProjectProfileById(any(), eq(defaultProjectId), eq(uuid))).thenReturn(
			Mono.just(
				ProjectProfileModel()
			)
		)
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_RESTORE))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/unblock", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(projectProfileRoleReaderMapper)
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(profilesWriterMapper)
		verify(useCase).unblockProjectProfileById(any(), eq(defaultProjectId), eq(uuid))
	}

	@Test
	fun `Should deleteProjectProfileById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(useCase.deleteProjectProfileById(any(), eq(defaultProjectId), eq(uuid))).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(projectProfileRoleReaderMapper)
		verifyNoInteractions(projectProfileStatusReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(profilesWriterMapper)
		verify(useCase).deleteProjectProfileById(any(), eq(defaultProjectId), eq(uuid))
	}

	@Test
	fun `Should findProjectProfiles forward the role filter to the service`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(ProjectProfileModel()))
		whenever(useCase.findProjectProfilesPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ProjectProfileReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R), buildAuthority(REGISTRY_PROJECT_PROFILE_RESTORE))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("role", "PROJECT_ADMINISTRATOR"))))
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(useCase).findProjectProfilesPage(
			defaultProjectId,
			pageable,
			ProjectProfileSearchParamModel(query = null, role = "PROJECT_ADMINISTRATOR"),
		)
	}

	@Test
	fun `Should createProjectProfiles refuse more than 200 users`() {
		// Arrange
		val profiles = ProjectProfilesWriterDto(emails = List(201) { "invited$it@sgdf.fr" }, role = "ROLE")

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(profiles)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, LIST_TOO_LARGE)
		verifyNoInteractions(useCase)
	}

	@Test
	fun `Should createProjectProfiles refuse a role over 50 characters`() {
		// Arrange
		val profiles = ProjectProfilesWriterDto(emails = listOf("invited@sgdf.fr"), role = "R".repeat(51))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(profiles)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_TOO_LONG)
		verifyNoInteractions(useCase)
	}

	@Test
	fun `Should updateProjectProfile refuse a role over 50 characters`() {
		// Arrange
		val profile = ProjectProfileWriterDto(role = "R".repeat(51))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, UUID.randomUUID()), emptyList()))
			.bodyValue(profile)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_TOO_LONG)
		verifyNoInteractions(useCase)
	}

	@Test
	fun `Should findProjectProfiles refuse a role filter over 50 characters`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("role", "R".repeat(51)))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_TOO_LONG)
		verifyNoInteractions(useCase)
	}

	@Test
	fun `Should createProjectProfiles refuse a role with forbidden characters`() {
		// Arrange
		val profiles = ProjectProfilesWriterDto(emails = listOf("invited@sgdf.fr"), role = "ROLE';--")

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(profiles)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_FORMAT_INVALID)
		verifyNoInteractions(useCase)
	}

	@Test
	fun `Should updateProjectProfile refuse a role with forbidden characters`() {
		// Arrange
		val profile = ProjectProfileWriterDto(role = "ROLE';--")

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, UUID.randomUUID()), emptyList()))
			.bodyValue(profile)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_FORMAT_INVALID)
		verifyNoInteractions(useCase)
	}

	@Test
	fun `Should findProjectProfiles refuse a role filter with forbidden characters`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("role", "ROLE';--"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_FORMAT_INVALID)
		verifyNoInteractions(useCase)
	}
}
