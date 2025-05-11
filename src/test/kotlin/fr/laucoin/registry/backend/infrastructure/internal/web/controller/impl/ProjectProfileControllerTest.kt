package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ROLE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_USERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_U
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IProjectProfileService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CreatedProjectProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ProjectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ProjectProfilesWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.CreatedProjectProfilesReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ProjectProfileReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ProjectProfileRoleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ProjectProfileStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.ProjectProfileWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.ProjectProfilesWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ProjectProfileControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
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

    companion object {
        private const val BASE_URL = "/api/projects/{projectId}/profiles"

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
                Arguments.of(- 1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
                Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
                Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
            )
        }

        @JvmStatic
        fun `Should createProjectProfiles return 400`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ProjectProfilesWriterDto(userIds = null, role = "ROLE"),
                PROJECT_PROFILE_USERS_EMPTY,
                PROJECT_PROFILE_USERS_EMPTY,
            ),
            Arguments.of(
                ProjectProfilesWriterDto(userIds = emptyList(), role = "ROLE"),
                PROJECT_PROFILE_USERS_EMPTY,
            ),
            Arguments.of(
                ProjectProfilesWriterDto(role = "ROLE"),
                PROJECT_PROFILE_USERS_EMPTY,
            ),
            Arguments.of(
                ProjectProfilesWriterDto(userIds = listOf(UUID.randomUUID()), role = ""),
                PROJECT_PROFILE_ROLE_BLANK,
            ),
            Arguments.of(
                ProjectProfilesWriterDto(userIds = listOf(UUID.randomUUID())),
                PROJECT_PROFILE_ROLE_BLANK,
            ),
            Arguments.of(
                ProjectProfilesWriterDto(
                    userIds = listOf(UUID.randomUUID()),
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

    @ParameterizedTest
    @MethodSource
    fun `Should findProjectProfiles return 200`(
        requestedLocale: String?,
        pageNumber: Int?,
        pageSize: Int?,
        textSearched: String?,
        availabilitySearched: Boolean?,
        statusSearched: ProfileStatusEnum?,
        dateTimeSearched: String?,
    ) {
        // Arrange
        val expectedPageNumber = pageNumber ?: 0
        val expectedPageSize = pageSize ?: 20
        val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
        val searchParams = ProjectProfileSearchParamModel(
            textSearched = textSearched,
            availabilitySearched = availabilitySearched,
            statusSearched = statusSearched,
            dateTimeSearched = dateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
        )
        val page = PageModel(pageable, totalElements = 1, listOf(ProjectProfileModel()))
        whenever(service.findProjectProfilesPage(any(), any(), any())).thenReturn(Mono.just(page))
        whenever(readerMapper.toDtoPage(any(), any())).thenReturn(
            PageModel(pageable, totalElements = 1, listOf(ProjectProfileReaderDto())),
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    listOf(projectId),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                        Pair("textSearched", textSearched),
                        Pair("availabilitySearched", availabilitySearched),
                        Pair("statusSearched", statusSearched),
                        Pair("dateTimeSearched", dateTimeSearched),
                    ),
                )
            )
            .header(ACCEPT_LANGUAGE, requestedLocale)
            .exchange()

        // Assert
        result.body<PageModel<*>>(OK)

        verify(service).findProjectProfilesPage(projectId, pageable, searchParams)
        verify(readerMapper).toDtoPage(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(projectProfileRoleReaderMapper)
        verifyNoInteractions(projectProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findProjectProfiles throw due to wrong params`(
        pageNumber: Int?,
        pageSize: Int?,
        expectedMessage: String,
    ) {
        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    listOf(projectId),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                    ),
                )
            )
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedMessage)

        verifyNoInteractions(service)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(projectProfileRoleReaderMapper)
        verifyNoInteractions(projectProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
    }

    @Test
    fun `Should findProjectProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.findProjectProfileById(any(), any(), anyOrNull())).thenReturn(Mono.just(ProjectProfileModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(ProjectProfileReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_R))
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<ProjectProfileReaderDto>(OK)

        verify(service).findProjectProfileById(projectId, uuid, visibilitySearched = null)
        verify(readerMapper).toDto(any(), any())
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

        whenever(service.searchUsers(anyOrNull())).thenReturn(Flux.just(user))
        whenever(partialUserReaderMapper.toDto(any(), any())).thenReturn(PartialUserReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_METADATA_R))
            .get()
            .uri(
                uriBuilder("${BASE_URL}/search/users", listOf(projectId), listOf(Pair("textSearched", searched)))
            )
            .exchange()

        // Assert
        result.body<List<*>>(OK)

        verify(service).searchUsers(searched)
        verifyNoInteractions(readerMapper)
        verify(partialUserReaderMapper).toDto(any(), any())
        verifyNoInteractions(projectProfileRoleReaderMapper)
        verifyNoInteractions(projectProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
    }

    @Test
    fun `Should getAssignableProjectProfileRoles return 200`() {
        // Arrange
        whenever(service.getAssignableProjectRoles(any(), any())).thenReturn(Flux.just("role"))
        whenever(projectProfileRoleReaderMapper.toDto(any(), any())).thenReturn(LabelDto("value", "label"))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_METADATA_R))
            .get()
            .uri(uriBuilder("$BASE_URL/roles", listOf(projectId), emptyList()))
            .exchange()

        // Assert
        result.body<List<*>>(OK)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verify(projectProfileRoleReaderMapper).toDto(any(), any())
        verifyNoInteractions(projectProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
        verify(service).getAssignableProjectRoles(any(), eq(projectId))
    }

    @Test
    fun `Should createProjectProfiles return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profiles = ProjectProfilesWriterDto(userIds = listOf(uuid), role = "ROLE")
        whenever(service.createProjectProfiles(any(), any(), any(), any())).thenReturn(
            Mono.just(Pair(listOf(UUID.randomUUID()), emptyList()))
        )
        whenever(profilesWriterMapper.toModels(any(), any())).thenReturn(emptyList())
        whenever(createdProjectProfilesReaderMapper.toDto(any(), any())).thenReturn(
            CreatedProjectProfilesReaderDto(
                emptyList(),
                emptyList()
            )
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_C))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
            .bodyValue(profiles)
            .exchange()

        // Assert
        result.body<CreatedProjectProfilesReaderDto>(OK)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(projectProfileRoleReaderMapper)
        verifyNoInteractions(projectProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(profilesWriterMapper).toModels(profiles, projectId)
        verify(createdProjectProfilesReaderMapper).toDto(any(), any())
        verify(service).createProjectProfiles(
            any(),
            eq(projectId),
            eq(profiles.userIds !!),
            any()
        )
    }

    @Test
    fun `Should createProjectProfiles return 207`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val profiles = ProjectProfilesWriterDto(userIds = listOf(uuid, uuid2), role = "ROLE")

        whenever(
            service.createProjectProfiles(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(
            Mono.just(Pair(listOf(uuid2), listOf(uuid)))
        )
        whenever(profilesWriterMapper.toModels(any(), any())).thenReturn(emptyList())
        whenever(createdProjectProfilesReaderMapper.toDto(any(), any())).thenReturn(
            CreatedProjectProfilesReaderDto(
                listOf(uuid2),
                listOf(uuid)
            )
        )

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

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(projectProfileRoleReaderMapper)
        verifyNoInteractions(projectProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(profilesWriterMapper).toModels(profiles, projectId)
        verify(service).createProjectProfiles(
            any(),
            eq(projectId),
            eq(profiles.userIds !!),
            any()
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should createProjectProfiles return 400`(
        profile: ProjectProfilesWriterDto,
        expectedCode: String,
    ) {
        // Act
        val result = webClient
            .authenticate()
            .post()
            .uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
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
        verifyNoInteractions(service)
    }

    @Test
    fun `Should updateProjectProfile return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = ProjectProfileWriterDto(role = "ROLE")

        whenever(service.updateProjectProfileById(any(), any(), any(), any())).thenReturn(Mono.just(ProjectProfileModel()))
        whenever(writerMapper.toModel(any(), any())).thenReturn(ProjectProfileModel())
        whenever(readerMapper.toDto(any(), any())).thenReturn(ProjectProfileReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
            .bodyValue(profile)
            .exchange()

        // Assert
        result.body<ProjectProfileReaderDto>(OK)
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(projectProfileRoleReaderMapper)
        verifyNoInteractions(projectProfileStatusReaderMapper)
        verify(writerMapper).toModel(profile, projectId)
        verifyNoInteractions(profilesWriterMapper)
        verify(service).updateProjectProfileById(any(), eq(projectId), eq(uuid), any())
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
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
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
        verifyNoInteractions(service)
    }

    @Test
    fun `Should blockProjectProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.blockProjectProfileById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(ProjectProfileModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(ProjectProfileReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/block", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<ProjectProfileReaderDto>(OK)
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(projectProfileRoleReaderMapper)
        verifyNoInteractions(projectProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
        verify(service).blockProjectProfileById(any(), eq(projectId), eq(uuid))
    }

    @Test
    fun `Should unblockProjectProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.unblockProjectProfileById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(ProjectProfileModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(ProjectProfileReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/unblock", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<ProjectProfileReaderDto>(OK)
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(projectProfileRoleReaderMapper)
        verifyNoInteractions(projectProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
        verify(service).unblockProjectProfileById(any(), eq(projectId), eq(uuid))
    }

    @Test
    fun `Should deleteProjectProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.deleteProjectProfileById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_PROFILE_D))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(projectProfileRoleReaderMapper)
        verifyNoInteractions(projectProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
        verify(service).deleteProjectProfileById(any(), eq(projectId), eq(uuid))
    }
}
