package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_ROLE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_USERS_EMPTY
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IEventProfileService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.CreatedEventProfilesDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.EventProfileDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.EventProfilesDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.EventProfileDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.EventProfilesDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.assertPage
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.TestContainerDatabase
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.ZonedDateTime.now
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = [TestContainerDatabase::class])
class EventProfileControllerTest(
    @Autowired private val webClient: WebTestClient,
) {
    @MockitoBean
    private lateinit var service: IEventProfileService

    @MockitoSpyBean
    private lateinit var profilesMapper: EventProfilesDtoMapper

    @MockitoSpyBean
    private lateinit var profileMapper: EventProfileDtoMapper

    companion object {
        private const val BASE_URL = "/api/events/{eventId}/profiles"

        @JvmStatic
        fun `Should findEventProfiles return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null, null, null, null, null, null, null, null),
            Arguments.of(50, null, null, null, null, null, null, null, null),
            Arguments.of(null, 25, null, null, null, null, null, null, null),
            Arguments.of(null, null, ASC, null, null, null, null, null, null),
            Arguments.of(null, null, DESC, null, null, null, null, null, null),
            Arguments.of(null, null, null, true, null, null, null, null, null),
            Arguments.of(null, null, null, false, null, null, null, null, null),
            Arguments.of(null, null, null, null, true, null, null, null, null),
            Arguments.of(null, null, null, null, false, null, null, null, null),
            Arguments.of(null, null, null, null, null, INVITED, null, null, null),
            Arguments.of(null, null, null, null, null, ACCEPTED, null, null, null),
            Arguments.of(null, null, null, null, null, REJECTED, null, null, null),
            Arguments.of(null, null, null, null, null, null, "searched", null, null),
            Arguments.of(null, null, null, null, null, null, null, "2024-11-14T18:34:33.000Z", null),
            Arguments.of(null, null, null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
        )

        @JvmStatic
        fun `Should createEventProfiles return 400`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                EventProfilesDto(userIds = null, role = "ROLE"),
                EVENT_PROFILE_USERS_EMPTY,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                EventProfilesDto(userIds = emptyList(), role = "ROLE"),
                EVENT_PROFILE_USERS_EMPTY,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                EventProfilesDto(role = "ROLE"),
                EVENT_PROFILE_USERS_EMPTY,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                EventProfilesDto(userIds = listOf(UUID.randomUUID()), role = ""),
                EVENT_PROFILE_ROLE_BLANK,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                EventProfilesDto(userIds = listOf(UUID.randomUUID())),
                EVENT_PROFILE_ROLE_BLANK,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                EventProfilesDto(
                    userIds = listOf(UUID.randomUUID()),
                    role = "ROLE",
                    startAccess = now().plusDays(1),
                    endAccess = now()
                ),
                EVENT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS,
                emptyMap<String, String>(),
            ),
        )

        @JvmStatic
        fun `Should updateEventProfile return 400`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                EventProfileDto(role = ""),
                EVENT_PROFILE_ROLE_BLANK,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                EventProfileDto(),
                EVENT_PROFILE_ROLE_BLANK,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                EventProfileDto(
                    role = "ROLE",
                    startAccess = now().plusDays(1),
                    endAccess = now()
                ),
                EVENT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS,
                emptyMap<String, String>(),
            ),
        )

        @JvmStatic
        fun `Event Profile management routes`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            val profiles = EventProfilesDto(userIds = listOf(uuid), role = "ROLE")
            val profile = EventProfileDto(role = "ROLE")
            return Stream.of(
                Arguments.of(GET, BASE_URL, listOf(eventId), null),
                Arguments.of(GET, "$BASE_URL/{id}", listOf(eventId, uuid), null),
                Arguments.of(POST, BASE_URL, listOf(eventId), profiles),
                Arguments.of(POST, "$BASE_URL/support", listOf(eventId), null),
                Arguments.of(PATCH, "$BASE_URL/{id}", listOf(eventId, uuid), profile),
                Arguments.of(PATCH, "$BASE_URL/{id}/block", listOf(eventId, uuid), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/unblock", listOf(eventId, uuid), null),
                Arguments.of(DELETE, "$BASE_URL/{id}", listOf(eventId, uuid), null),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("Event Profile management routes")
    fun `Should return 401`(
        method: HttpMethod, uri: String, params: List<String>, body: Any?
    ) {
        // Arrange
        val request = webClient
            .method(method)
            .uri(uriBuilder(uri, params, listOf()))

        if (Objects.nonNull(body)) {
            request.bodyValue(body !!)
        }

        // Act
        val result = request.exchange()

        // Assert
        result.expectStatus().isUnauthorized
        verifyNoInteractions(profileMapper)
        verifyNoInteractions(profilesMapper)
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource("Event Profile management routes")
    fun `Should return 403`(
        method: HttpMethod, uri: String, params: List<String>, body: Any?
    ) {
        // Arrange
        val request = webClient
            .authenticate()
            .method(method)
            .uri(uriBuilder(uri, params, listOf()))

        if (Objects.nonNull(body)) {
            request.bodyValue(body !!)
        }

        // Act
        val result = request.exchange()

        // Assert
        result.expectStatus().isForbidden
        verifyNoInteractions(profileMapper)
        verifyNoInteractions(profilesMapper)
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findEventProfiles return 200`(
        offset: Int?,
        limit: Int?,
        order: Direction?,
        onlyVisible: Boolean?,
        onlyUsable: Boolean?,
        status: ProfileStatusEnum?,
        searched: String?,
        startAccess: String?,
        endAccess: String?,
    ) {
        // Arrange
        val expectedOrder = order ?: ASC
        val expectedOnlyVisible = onlyVisible ?: true
        val expectedOnlyUsable = onlyUsable ?: true
        val expectedOffset = offset ?: 0
        val expectedLimit = limit ?: 20
        val expectedSize = 0

        `when`(
            service.findEventProfilesByEventId(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PROFILE_R"))
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    listOf(eventId),
                    listOf(
                        Pair("offset", offset),
                        Pair("limit", limit),
                        Pair("order", order),
                        Pair("onlyVisible", onlyVisible),
                        Pair("onlyUsable", onlyUsable),
                        Pair("status", status),
                        Pair("searched", searched),
                        Pair("startAccess", startAccess),
                        Pair("endAccess", endAccess),
                    ),
                )
            )
            .exchange()

        // Assert
        val body = result.body<PageModel<*>>(OK)

        assertNotNull(body)
        body !!.assertPage(
            expectedTotalElements = expectedSize,
            expectedOffset = expectedOffset,
            expectedLimit = expectedLimit,
        )

        verifyNoInteractions(profilesMapper)
        verifyNoInteractions(profileMapper)
        verify(service, times(1)).findEventProfilesByEventId(
            eventId = eq(eventId),
            order = eq(expectedOrder),
            onlyVisible = eq(expectedOnlyVisible),
            status = eq(status),
            searched = eq(searched),
            startAccess = anyOrNull(),
            endAccess = anyOrNull(),
        )
    }

    @Test
    fun `Should findEventProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.findEventProfileByEventIdAndId(any(), any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PROFILE_R"))
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<EventProfileModel>(OK)
        verifyNoInteractions(profilesMapper)
        verifyNoInteractions(profileMapper)
        verify(service, times(1)).findEventProfileByEventIdAndId(eventId, uuid, onlyVisible = false)
    }

    @Test
    fun `Should getAssignableEventProfileRoles return 200`() {
        // Arrange
        `when`(service.getAssignableEventRoles(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PROFILE_METADATA_R"))
            .get()
            .uri(uriBuilder("$BASE_URL/roles", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<List<*>>(OK)
        verifyNoInteractions(profilesMapper)
        verifyNoInteractions(profileMapper)
        verify(service, times(1)).getAssignableEventRoles(any(), eq(eventId))
    }

    @Test
    fun `Should createEventProfiles return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profiles = EventProfilesDto(userIds = listOf(uuid), role = "ROLE")
        `when`(service.createEventProfiles(any(), any(), any(), any())).thenReturn(Flux.just(EventProfileModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PROFILE_C"))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(profiles)
            .exchange()

        // Assert
        result.body<CreatedEventProfilesDto>(OK)
        verifyNoInteractions(profileMapper)
        verify(profilesMapper, times(1)).toModels(profiles, eventId)
        verify(service, times(1)).createEventProfiles(
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
        val profiles = EventProfilesDto(userIds = listOf(uuid, uuid2), role = "ROLE")
        `when`(
            service.createEventProfiles(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(Flux.just(EventProfileModel(user = UserModel().apply { id = uuid2 })))

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PROFILE_C"))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(profiles)
            .exchange()

        // Assert
        val body = result.body<CreatedEventProfilesDto>(MULTI_STATUS)
        assertEquals(uuid, body?.notCreatedUserIds?.first())
        assertEquals(uuid2, body?.profiles?.first()?.user?.id)

        verifyNoInteractions(profileMapper)
        verify(profilesMapper, times(1)).toModels(profiles, eventId)
        verify(service, times(1)).createEventProfiles(
            any(),
            eq(eventId),
            eq(profiles.userIds !!),
            any()
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should createEventProfiles return 400`(
        profile: EventProfilesDto,
        expectedMessage: String,
        expectedArgs: Map<String, String>
    ) {
        // Arrange
        // Act
        val result = webClient
            .authenticate()
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(profile)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedMessage, expectedArgs)
        verifyNoInteractions(profilesMapper)
        verifyNoInteractions(profileMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should createSupportEventProfile return 200`() {
        // Arrange
        `when`(service.createSupportEventProfile(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate("REGISTRY_PROFILE_C")
            .post()
            .uri(uriBuilder("$BASE_URL/support", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<ResponseEntity<*>>(OK)
        verifyNoInteractions(profileMapper)
        verifyNoInteractions(profilesMapper)
        verify(service, times(1)).createSupportEventProfile(any(), eq(eventId))
    }

    @Test
    fun `Should updateEventProfile return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = EventProfileDto(role = "ROLE")

        `when`(service.updateEventProfileById(any(), any(), any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PROFILE_U"))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .bodyValue(profile)
            .exchange()

        // Assert
        result.body<EventProfileModel>(OK)
        verifyNoInteractions(profilesMapper)
        verify(profileMapper, times(1)).toModel(profile, eventId)
        verify(service, times(1)).updateEventProfileById(any(), eq(eventId), eq(uuid), any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateEventProfile return 400`(
        profile: EventProfileDto,
        expectedMessage: String,
        expectedArgs: Map<String, String>
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
        result.assertError(BAD_REQUEST, expectedMessage, expectedArgs)
        verifyNoInteractions(profilesMapper)
        verifyNoInteractions(profileMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should blockEventProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.blockEventProfileById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PROFILE_U"))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/block", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<EventProfileModel>(OK)
        verifyNoInteractions(profilesMapper)
        verifyNoInteractions(profileMapper)
        verify(service, times(1)).blockEventProfileById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should unblockEventProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.unblockEventProfileById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PROFILE_U"))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/unblock", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<EventProfileModel>(OK)
        verifyNoInteractions(profilesMapper)
        verifyNoInteractions(profileMapper)
        verify(service, times(1)).unblockEventProfileById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should deleteEventProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.deleteEventProfileById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PROFILE_D"))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)
        verifyNoInteractions(profilesMapper)
        verifyNoInteractions(profileMapper)
        verify(service, times(1)).deleteEventProfileById(any(), eq(eventId), eq(uuid))
    }
}
