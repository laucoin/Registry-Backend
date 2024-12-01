package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_BIRTHDAY_FUTURE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.ParticipantDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.ParticipantDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.assertPage
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.TestContainerDatabase
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.LocalDate
import java.time.ZonedDateTime.now
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream
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
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = [TestContainerDatabase::class])
class ParticipantControllerTest(
    @Autowired private val webClient: WebTestClient,
) {
    @MockitoBean
    private lateinit var service: IParticipantService

    @MockitoSpyBean
    private lateinit var mapper: ParticipantDtoMapper

    companion object {
        private const val BASE_URL = "/api/events/{eventId}/participants"

        @JvmStatic
        fun `Should findParticipants return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null, null, null, null, null, null, null),
            Arguments.of(50, null, null, null, null, null, null, null),
            Arguments.of(null, 25, null, null, null, null, null, null),
            Arguments.of(null, null, ASC, null, null, null, null, null),
            Arguments.of(null, null, DESC, null, null, null, null, null),
            Arguments.of(null, null, null, true, null, null, null, null),
            Arguments.of(null, null, null, false, null, null, null, null),
            Arguments.of(null, null, null, null, true, null, null, null),
            Arguments.of(null, null, null, null, false, null, null, null),
            Arguments.of(null, null, null, null, null, "searched", null, null),
            Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z", null),
            Arguments.of(null, null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
        )

        @JvmStatic
        fun `Wrong ParticipantDto`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ParticipantDto(lastName = "DOE", birthday = LocalDate.now()),
                PARTICIPANT_FIRST_NAME_BLANK,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                ParticipantDto(firstName = "", lastName = "DOE", birthday = LocalDate.now()),
                PARTICIPANT_FIRST_NAME_BLANK,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                ParticipantDto(
                    firstName = "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiop",
                    lastName = "DOE",
                    birthday = LocalDate.now()
                ),
                PARTICIPANT_FIRST_NAME_TOO_LONG,
                mapOf(
                    "constraint_0" to "150",
                ),
            ),
            Arguments.of(
                ParticipantDto(firstName = "John", birthday = LocalDate.now()),
                PARTICIPANT_LAST_NAME_BLANK,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                ParticipantDto(firstName = "John", lastName = "", birthday = LocalDate.now()),
                PARTICIPANT_LAST_NAME_BLANK,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                ParticipantDto(
                    firstName = "John",
                    lastName = "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiop",
                    birthday = LocalDate.now()
                ),
                PARTICIPANT_LAST_NAME_TOO_LONG,
                mapOf(
                    "constraint_0" to "150",
                ),
            ),
            Arguments.of(
                ParticipantDto(firstName = "John", lastName = "DOE", birthday = LocalDate.now().plusDays(1)),
                PARTICIPANT_BIRTHDAY_FUTURE,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                ParticipantDto(
                    firstName = "John",
                    lastName = "DOE",
                    birthday = LocalDate.now(),
                    begin = now().plusDays(1),
                    end = now()
                ),
                PARTICIPANT_START_LATER_THAN_END,
                emptyMap<String, String>(),
            ),
        )

        @JvmStatic
        fun `Participant management routes`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            val participant = ParticipantDto(firstName = "John", lastName = "DOE", birthday = LocalDate.now())
            return Stream.of(
                Arguments.of(GET, BASE_URL, listOf(eventId), null),
                Arguments.of(GET, "$BASE_URL/{id}", listOf(eventId, uuid), null),
                Arguments.of(POST, BASE_URL, listOf(eventId), participant),
                Arguments.of(PATCH, "$BASE_URL/{id}", listOf(eventId, uuid), participant),
                Arguments.of(PATCH, "$BASE_URL/{id}/disable", listOf(eventId, uuid), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/enable", listOf(eventId, uuid), null),
                Arguments.of(DELETE, "$BASE_URL/{id}", listOf(eventId, uuid), null),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("Participant management routes")
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
        verifyNoInteractions(mapper)
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource("Participant management routes")
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
        verifyNoInteractions(mapper)
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findParticipants return 200`(
        offset: Int?,
        limit: Int?,
        order: Direction?,
        onlyVisible: Boolean?,
        onlyPresent: Boolean?,
        searched: String?,
        startDateTime: String?,
        endDateTime: String?,
    ) {
        // Arrange
        val expectedOrder = order ?: ASC
        val expectedOnlyVisible = onlyVisible ?: true
        val expectedOnlyPresent = onlyPresent ?: true
        val expectedOffset = offset ?: 0
        val expectedLimit = limit ?: 20
        val expectedSize = 0

        `when`(
            service.findParticipantsByEventId(
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PARTICIPANT_R"))
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
                        Pair("onlyPresent", onlyPresent),
                        Pair("searched", searched),
                        Pair("startDateTime", startDateTime),
                        Pair("endDateTime", endDateTime),
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

        verifyNoInteractions(mapper)
        verify(service, times(1)).findParticipantsByEventId(
            eventId = eq(eventId),
            order = eq(expectedOrder),
            onlyVisible = eq(expectedOnlyVisible),
            onlyPresent = eq(expectedOnlyPresent),
            searched = eq(searched),
            startDateTime = anyOrNull(),
            endDateTime = anyOrNull(),
        )
    }

    @Test
    fun `Should findParticipantById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.findParticipantById(any(), any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PARTICIPANT_R"))
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<ParticipantModel>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).findParticipantById(eventId, uuid, onlyVisible = false)
    }

    @Test
    fun `Should createParticipant return 200`() {
        // Arrange
        val participant = ParticipantDto(firstName = "John", lastName = "DOE", birthday = LocalDate.now())
        `when`(service.createParticipant(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PARTICIPANT_C"))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(participant)
            .exchange()

        // Assert
        result.body<ParticipantModel>(OK)
        verify(mapper, times(1)).toModel(participant, eventId)
        verify(service, times(1)).createParticipant(any(), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong ParticipantDto")
    fun `Should createParticipant return 400`(
        participant: ParticipantDto,
        expectedMessage: String,
        expectedArgs: Map<String, String>
    ) {
        // Arrange
        // Act
        val result = webClient
            .authenticate()
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(participant)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedMessage, expectedArgs)
        verifyNoInteractions(mapper)
        verifyNoInteractions(service)
    }


    @Test
    fun `Should updateEventProfile return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val participant = ParticipantDto(firstName = "John", lastName = "DOE", birthday = LocalDate.now())

        `when`(service.updateParticipantById(any(), any(), any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PARTICIPANT_U"))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .bodyValue(participant)
            .exchange()

        // Assert
        result.body<ParticipantModel>(OK)
        verify(mapper, times(1)).toModel(participant, eventId)
        verify(service, times(1)).updateParticipantById(any(), eq(eventId), eq(uuid), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong ParticipantDto")
    fun `Should updateParticipantById return 400`(
        participant: ParticipantDto,
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
            .bodyValue(participant)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedMessage, expectedArgs)
        verifyNoInteractions(mapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should disableParticipantById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.disableParticipantById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PARTICIPANT_U"))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/disable", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<ParticipantModel>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).disableParticipantById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should enableParticipantById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.enableParticipantById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PARTICIPANT_U"))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/enable", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<ParticipantModel>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).enableParticipantById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should deleteParticipantById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.deleteParticipantById(any(), any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_PARTICIPANT_D"))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).deleteParticipantById(any(), eq(eventId), eq(uuid))
    }
}
