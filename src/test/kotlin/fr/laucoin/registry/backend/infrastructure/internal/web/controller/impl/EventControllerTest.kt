package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_BEGIN_LATER_THAN_END_TIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_NAME_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.MOVEMENT_REPORT
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.SMOKE_REPORT
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.EventDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.EventDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.assertPage
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.TestContainerDatabase
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
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
class EventControllerTest(
    @Autowired private val webClient: WebTestClient,
) {
    @MockitoBean
    private lateinit var service: IEventService

    @MockitoSpyBean
    private lateinit var mapper: EventDtoMapper

    companion object {
        private const val BASE_URL = "/api/events"

        @JvmStatic
        fun `Should findEvents return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null, null, null, null, null, null),
            Arguments.of(50, null, null, null, null, null, null),
            Arguments.of(null, 25, null, null, null, null, null),
            Arguments.of(null, null, ASC, null, null, null, null),
            Arguments.of(null, null, DESC, null, null, null, null),
            Arguments.of(null, null, null, true, null, null, null),
            Arguments.of(null, null, null, false, null, null, null),
            Arguments.of(null, null, null, null, "searched", null, null),
            Arguments.of(null, null, null, null, null, "2024-11-14T18:34:33.000Z", null),
            Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
        )

        @JvmStatic
        fun `Wrong EventDto`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                EventDto(name = "", begin = now(), end = now().plusDays(1), options = emptyList()),
                EVENT_NAME_BLANK,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                EventDto(
                    name = "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiop",
                    begin = now(),
                    end = now().plusDays(1),
                    options = emptyList()
                ),
                EVENT_NAME_TOO_LONG,
                mapOf(
                    "constraint_0" to "150",
                ),
            ),
            Arguments.of(
                EventDto(name = "event", begin = now().plusDays(1), end = now(), options = emptyList()),
                EVENT_BEGIN_LATER_THAN_END_TIME,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                EventDto(name = "event", begin = now(), end = now().plusDays(1), options = listOf(ACTIVITY_COMMUNICATION)),
                EVENT_OPTIONS_MISSING,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                EventDto(name = "event", begin = now(), end = now().plusDays(1), options = listOf(MOVEMENT_REPORT)),
                EVENT_OPTIONS_MISSING,
                emptyMap<String, String>(),
            ),
            Arguments.of(
                EventDto(name = "event", begin = now(), end = now().plusDays(1), options = listOf(SMOKE_REPORT)),
                EVENT_OPTIONS_MISSING,
                emptyMap<String, String>(),
            ),
        )

        @JvmStatic
        fun `Event management routes`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            val event = EventDto(name = "event", begin = now(), end = now().plusDays(1), options = listOf(ACTIVITY))
            return Stream.of(
                Arguments.of(GET, "$BASE_URL/{id}", listOf(uuid), null),
                Arguments.of(POST, BASE_URL, emptyList<String>(), event),
                Arguments.of(PATCH, "$BASE_URL/{id}", listOf(uuid), event),
                Arguments.of(PATCH, "$BASE_URL/{id}/disable", listOf(uuid), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/enable", listOf(uuid), null),
                Arguments.of(DELETE, "$BASE_URL/{id}", listOf(uuid), null),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("Event management routes")
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
    @MethodSource("Event management routes")
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
    fun `Should findEvents return 200`(
        offset: Int?,
        limit: Int?,
        order: Direction?,
        onlyVisible: Boolean?,
        searched: String?,
        startDateTime: String?,
        endDateTime: String?,
    ) {
        // Arrange
        val expectedOrder = order ?: ASC
        val expectedOnlyVisible = onlyVisible ?: true
        val expectedOffset = offset ?: 0
        val expectedLimit = limit ?: 20
        val expectedSize = 0

        `when`(service.findEvents(any(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Flux.empty())

        // Act
        val result = webClient
            .authenticate()
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    emptyList(),
                    listOf(
                        Pair("offset", offset),
                        Pair("limit", limit),
                        Pair("order", order),
                        Pair("onlyVisible", onlyVisible),
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
        verify(service, times(1)).findEvents(
            currentUser = eq(currentUser()),
            order = eq(expectedOrder),
            onlyVisible = eq(expectedOnlyVisible),
            searched = eq(searched),
            startDateTime = anyOrNull(),
            endDateTime = anyOrNull(),
        )
    }

    @Test
    fun `Should findEventById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.findEventById(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate("REGISTRY_EVENT_R")
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(uuid), emptyList()))
            .exchange()

        // Assert
        result.body<EventModel>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).findEventById(uuid, onlyVisible = false)
    }

    @Test
    fun `Should createEvent return 200`() {
        // Arrange
        val event = EventDto(name = "event", begin = now(), end = now().plusDays(1), options = listOf(ACTIVITY))
        `when`(service.createEvent(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate("REGISTRY_EVENT_C")
            .post()
            .uri(BASE_URL)
            .bodyValue(event)
            .exchange()

        // Assert
        result.body<EventModel>(OK)
        verify(mapper, times(1)).toModel(any())
        verify(service, times(1)).createEvent(any(), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong EventDto")
    fun `Should createEvent return 400`(
        event: EventDto,
        expectedMessage: String,
        expectedArgs: Map<String, String>
    ) {
        // Arrange
        // Act
        val result = webClient
            .authenticate()
            .post()
            .uri(BASE_URL)
            .bodyValue(event)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedMessage, expectedArgs)
        verifyNoInteractions(mapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should updateEventById return 200`() {
        // Arrange
        val event = EventDto(name = "event", begin = now(), end = now().plusDays(1), options = listOf(ACTIVITY))

        `when`(service.updateEventById(any(), any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_U"))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId), emptyList()))
            .bodyValue(event)
            .exchange()

        // Assert
        result.body<EventModel>(OK)
        verify(mapper, times(1)).toModel(any())
        verify(service, times(1)).updateEventById(any(), eq(eventId), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong EventDto")
    fun `Should updateEventById return 400`(
        event: EventDto,
        expectedMessage: String,
        expectedArgs: Map<String, String>
    ) {
        // Arrange
        // Act
        val result = webClient
            .authenticate()
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId), emptyList()))
            .bodyValue(event)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedMessage, expectedArgs)
        verifyNoInteractions(mapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should disableEventById return 200`() {
        // Arrange
        `when`(service.disableEventById(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_U"))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/disable", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<EventModel>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).disableEventById(any(), eq(eventId))
    }

    @Test
    fun `Should enableEventById return 200`() {
        // Arrange
        `when`(service.enableEventById(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_U"))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/enable", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<EventModel>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).enableEventById(any(), eq(eventId))
    }

    @Test
    fun `Should deleteEventById return 200`() {
        // Arrange
        `when`(service.deleteEventById(any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority("REGISTRY_EVENT_D"))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).deleteEventById(eq(eventId))
    }
}
