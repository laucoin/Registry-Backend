package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_ROLE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_USERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PROFILE_C
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PROFILE_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PROFILE_METADATA_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PROFILE_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PROFILE_U
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IEventProfileService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CreatedEventProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventProfilesWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.CreatedEventProfilesReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventProfileReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventProfileRoleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventProfileStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.EventProfileWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.EventProfilesWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.LocalDate
import java.time.LocalTime
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

class EventProfileControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
    @MockitoBean
    private lateinit var service: IEventProfileService

    @MockitoBean
    private lateinit var readerMapper: EventProfileReaderDtoMapper

    @MockitoBean
    private lateinit var createdEventProfilesReaderMapper: CreatedEventProfilesReaderDtoMapper

    @MockitoBean
    private lateinit var partialUserReaderMapper: PartialUserReaderDtoMapper

    @MockitoBean
    private lateinit var eventProfileRoleReaderMapper: EventProfileRoleReaderDtoMapper

    @MockitoBean
    private lateinit var eventProfileStatusReaderMapper: EventProfileStatusReaderDtoMapper

    @MockitoBean
    private lateinit var writerMapper: EventProfileWriterDtoMapper

    @MockitoBean
    private lateinit var profilesWriterMapper: EventProfilesWriterDtoMapper

    companion object {
        private const val BASE_URL = "/api/events/{eventId}/profiles"

        @JvmStatic
        fun `Should findEventProfiles return 200`(): Stream<Arguments> = Stream.of(
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
        fun `Should findEventProfiles throw due to wrong params`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(- 1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
                Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
                Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
            )
        }

        @JvmStatic
        fun `Should createEventProfiles return 400`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                EventProfilesWriterDto(userIds = null, role = "ROLE"),
                EVENT_PROFILE_USERS_EMPTY,
                EVENT_PROFILE_USERS_EMPTY,
            ),
            Arguments.of(
                EventProfilesWriterDto(userIds = emptyList(), role = "ROLE"),
                EVENT_PROFILE_USERS_EMPTY,
            ),
            Arguments.of(
                EventProfilesWriterDto(role = "ROLE"),
                EVENT_PROFILE_USERS_EMPTY,
            ),
            Arguments.of(
                EventProfilesWriterDto(userIds = listOf(UUID.randomUUID()), role = ""),
                EVENT_PROFILE_ROLE_BLANK,
            ),
            Arguments.of(
                EventProfilesWriterDto(userIds = listOf(UUID.randomUUID())),
                EVENT_PROFILE_ROLE_BLANK,
            ),
            Arguments.of(
                EventProfilesWriterDto(
                    userIds = listOf(UUID.randomUUID()),
                    role = "ROLE",
                    startAccess = CustomDateTimeWriterDto(LocalDate.MAX, LocalTime.MAX),
                    endAccess = CustomDateTimeWriterDto(LocalDate.MIN, LocalTime.MIN),
                ),
                EVENT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS,
            ),
        )

        @JvmStatic
        fun `Should updateEventProfile return 400`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                EventProfileWriterDto(role = ""),
                EVENT_PROFILE_ROLE_BLANK,
            ),
            Arguments.of(
                EventProfileWriterDto(),
                EVENT_PROFILE_ROLE_BLANK,
            ),
            Arguments.of(
                EventProfileWriterDto(
                    role = "ROLE",
                    startAccess = CustomDateTimeWriterDto(LocalDate.MAX, LocalTime.MAX),
                    endAccess = CustomDateTimeWriterDto(LocalDate.MIN, LocalTime.MIN),
                ),
                EVENT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS,
            ),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findEventProfiles return 200`(
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
        val searchParams = EventProfileSearchParamModel(
            textSearched = textSearched,
            availabilitySearched = availabilitySearched,
            statusSearched = statusSearched,
            dateTimeSearched = dateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
        )
        val page = PageModel(pageable, totalElements = 1, listOf(EventProfileModel()))
        whenever(service.findEventProfilesPage(any(), any(), any())).thenReturn(Mono.just(page))
        whenever(readerMapper.toDtoPage(any(), any())).thenReturn(
            PageModel(pageable, totalElements = 1, listOf(EventProfileReaderDto())),
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PROFILE_R))
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    listOf(eventId),
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

        verify(service).findEventProfilesPage(eventId, pageable, searchParams)
        verify(readerMapper).toDtoPage(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(eventProfileRoleReaderMapper)
        verifyNoInteractions(eventProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findEventProfiles throw due to wrong params`(
        pageNumber: Int?,
        pageSize: Int?,
        expectedMessage: String,
    ) {
        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PROFILE_R))
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    listOf(eventId),
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
        verifyNoInteractions(eventProfileRoleReaderMapper)
        verifyNoInteractions(eventProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
    }

    @Test
    fun `Should findEventProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.findEventProfileById(any(), any(), anyOrNull())).thenReturn(Mono.just(EventProfileModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(EventProfileReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PROFILE_R))
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<EventProfileReaderDto>(OK)

        verify(service).findEventProfileById(eventId, uuid, visibilitySearched = null)
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(eventProfileRoleReaderMapper)
        verifyNoInteractions(eventProfileStatusReaderMapper)
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
            .authenticate(buildAuthority(REGISTRY_EVENT_PROFILE_METADATA_R))
            .get()
            .uri(
                uriBuilder("${BASE_URL}/search/users", listOf(eventId), listOf(Pair("textSearched", searched)))
            )
            .exchange()

        // Assert
        result.body<List<*>>(OK)

        verify(service).searchUsers(searched)
        verifyNoInteractions(readerMapper)
        verify(partialUserReaderMapper).toDto(any(), any())
        verifyNoInteractions(eventProfileRoleReaderMapper)
        verifyNoInteractions(eventProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
    }

    @Test
    fun `Should getAssignableEventProfileRoles return 200`() {
        // Arrange
        whenever(service.getAssignableEventRoles(any(), any())).thenReturn(Flux.just("role"))
        whenever(eventProfileRoleReaderMapper.toDto(any(), any())).thenReturn(LabelDto("value", "label"))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PROFILE_METADATA_R))
            .get()
            .uri(uriBuilder("$BASE_URL/roles", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<List<*>>(OK)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verify(eventProfileRoleReaderMapper).toDto(any(), any())
        verifyNoInteractions(eventProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
        verify(service).getAssignableEventRoles(any(), eq(eventId))
    }

    @Test
    fun `Should createEventProfiles return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profiles = EventProfilesWriterDto(userIds = listOf(uuid), role = "ROLE")
        whenever(service.createEventProfiles(any(), any(), any(), any())).thenReturn(
            Mono.just(Pair(listOf(UUID.randomUUID()), emptyList()))
        )
        whenever(profilesWriterMapper.toModels(any(), any())).thenReturn(emptyList())
        whenever(createdEventProfilesReaderMapper.toDto(any(), any())).thenReturn(
            CreatedEventProfilesReaderDto(
                emptyList(),
                emptyList()
            )
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PROFILE_C))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(profiles)
            .exchange()

        // Assert
        result.body<CreatedEventProfilesReaderDto>(OK)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(eventProfileRoleReaderMapper)
        verifyNoInteractions(eventProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(profilesWriterMapper).toModels(profiles, eventId)
        verify(createdEventProfilesReaderMapper).toDto(any(), any())
        verify(service).createEventProfiles(
            any(),
            eq(eventId),
            eq(profiles.userIds !!),
            any()
        )
    }

    @Test
    fun `Should createEventProfiles return 207`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val profiles = EventProfilesWriterDto(userIds = listOf(uuid, uuid2), role = "ROLE")

        whenever(
            service.createEventProfiles(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(
            Mono.just(Pair(listOf(uuid2), listOf(uuid)))
        )
        whenever(profilesWriterMapper.toModels(any(), any())).thenReturn(emptyList())
        whenever(createdEventProfilesReaderMapper.toDto(any(), any())).thenReturn(
            CreatedEventProfilesReaderDto(
                listOf(uuid2),
                listOf(uuid)
            )
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PROFILE_C))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(profiles)
            .exchange()

        // Assert
        val body = result.body<CreatedEventProfilesReaderDto>(MULTI_STATUS)
        assertEquals(uuid2, body?.createdUserIds?.first())
        assertEquals(uuid, body?.notCreatedUserIds?.first())

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(eventProfileRoleReaderMapper)
        verifyNoInteractions(eventProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(profilesWriterMapper).toModels(profiles, eventId)
        verify(service).createEventProfiles(
            any(),
            eq(eventId),
            eq(profiles.userIds !!),
            any()
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should createEventProfiles return 400`(
        profile: EventProfilesWriterDto,
        expectedCode: String,
    ) {
        // Act
        val result = webClient
            .authenticate()
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(profile)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedCode)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(eventProfileRoleReaderMapper)
        verifyNoInteractions(eventProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should updateEventProfile return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = EventProfileWriterDto(role = "ROLE")

        whenever(service.updateEventProfileById(any(), any(), any(), any())).thenReturn(Mono.just(EventProfileModel()))
        whenever(writerMapper.toModel(any(), any())).thenReturn(EventProfileModel())
        whenever(readerMapper.toDto(any(), any())).thenReturn(EventProfileReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PROFILE_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .bodyValue(profile)
            .exchange()

        // Assert
        result.body<EventProfileReaderDto>(OK)
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(eventProfileRoleReaderMapper)
        verifyNoInteractions(eventProfileStatusReaderMapper)
        verify(writerMapper).toModel(profile, eventId)
        verifyNoInteractions(profilesWriterMapper)
        verify(service).updateEventProfileById(any(), eq(eventId), eq(uuid), any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateEventProfile return 400`(
        profile: EventProfileWriterDto,
        expectedCode: String,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = webClient
            .authenticate()
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .bodyValue(profile)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedCode)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(eventProfileRoleReaderMapper)
        verifyNoInteractions(eventProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should blockEventProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.blockEventProfileById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.just(EventProfileModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(EventProfileReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PROFILE_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/block", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<EventProfileReaderDto>(OK)
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(eventProfileRoleReaderMapper)
        verifyNoInteractions(eventProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
        verify(service).blockEventProfileById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should unblockEventProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.unblockEventProfileById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.just(EventProfileModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(EventProfileReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PROFILE_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/unblock", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<EventProfileReaderDto>(OK)
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(eventProfileRoleReaderMapper)
        verifyNoInteractions(eventProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
        verify(service).unblockEventProfileById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should deleteEventProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.deleteEventProfileById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PROFILE_D))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(eventProfileRoleReaderMapper)
        verifyNoInteractions(eventProfileStatusReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(profilesWriterMapper)
        verify(service).deleteEventProfileById(any(), eq(eventId), eq(uuid))
    }
}
