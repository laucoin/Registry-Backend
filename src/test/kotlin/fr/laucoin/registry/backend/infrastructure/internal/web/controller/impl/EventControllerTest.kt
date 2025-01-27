package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_BEGIN_LATER_THAN_END_TIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_U
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_EVENT_C
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_EVENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_EVENT_R
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.MOVEMENT_REPORT
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.SMOKE_REPORT
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventOptionsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.EventWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.assertPage
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.TestContext
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class EventControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
    @MockitoBean
    private lateinit var service: IEventService

    @MockitoSpyBean
    private lateinit var readerMapper: EventReaderDtoMapper

    @MockitoSpyBean
    private lateinit var optionsReaderMapper: EventOptionsReaderDtoMapper

    @MockitoSpyBean
    private lateinit var writerMapper: EventWriterDtoMapper

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
                EventWriterDto(name = "", begin = now(), end = now().plusDays(1), options = emptyList()),
                EVENT_NAME_NULL_OR_BLANK,
            ),
            Arguments.of(
                EventWriterDto(
                    name = "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiop",
                    begin = now(),
                    end = now().plusDays(1),
                    options = emptyList()
                ),
                EVENT_NAME_TOO_LONG,
            ),
            Arguments.of(
                EventWriterDto(name = "event", begin = now().plusDays(1), end = now(), options = emptyList()),
                EVENT_BEGIN_LATER_THAN_END_TIME,
            ),
            Arguments.of(
                EventWriterDto(name = "event", begin = now(), end = now().plusDays(1), options = listOf(ACTIVITY_COMMUNICATION)),
                EVENT_OPTIONS_MISSING,
            ),
            Arguments.of(
                EventWriterDto(name = "event", begin = now(), end = now().plusDays(1), options = listOf(MOVEMENT_REPORT)),
                EVENT_OPTIONS_MISSING,
            ),
            Arguments.of(
                EventWriterDto(name = "event", begin = now(), end = now().plusDays(1), options = listOf(SMOKE_REPORT)),
                EVENT_OPTIONS_MISSING,
            ),
        )

        @JvmStatic
        fun `Event management routes`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            val event = EventWriterDto(name = "event", begin = now(), end = now().plusDays(1), options = listOf(ACTIVITY))
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
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(optionsReaderMapper)
        verifyNoInteractions(writerMapper)
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
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(optionsReaderMapper)
        verifyNoInteractions(writerMapper)
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
        val expectedSize = 1

        `when`(service.findEvents(any(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(EventModel()))

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
        val body = result.body<PageDto<*>>(OK)

        assertNotNull(body)
        body !!.assertPage(
            expectedTotalElements = expectedSize,
            expectedOffset = expectedOffset,
            expectedLimit = expectedLimit,
        )

        verify(readerMapper, times(1)).toDtoPage(any(), any())
        verifyNoInteractions(writerMapper)
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
        `when`(service.findEventById(any(), any())).thenReturn(Mono.just(EventModel()))

        // Act
        val result = webClient
            .authenticate(REGISTRY_EVENT_R)
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(uuid), emptyList()))
            .exchange()

        // Assert
        result.body<EventReaderDto>(OK)
        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(optionsReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).findEventById(uuid, onlyVisible = false)
    }

    @Test
    fun `Should getAvailableEventOptions return 200`() {
        // Arrange
        `when`(service.availableEventOptions()).thenReturn(Flux.just(Pair(ACTIVITY_COMMUNICATION, listOf(ACTIVITY))))

        // Act
        val result = webClient
            .authenticate(REGISTRY_EVENT_METADATA_R)
            .get()
            .uri(uriBuilder("$BASE_URL/options", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<List<*>>(OK)
        verifyNoInteractions(readerMapper)
        verify(optionsReaderMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).availableEventOptions()
    }

    @Test
    fun `Should createEvent return 200`() {
        // Arrange
        val event = EventWriterDto(name = "event", begin = now(), end = now().plusDays(1), options = listOf(ACTIVITY))
        `when`(service.createEvent(any(), any())).thenReturn(Mono.just(EventModel()))

        // Act
        val result = webClient
            .authenticate(REGISTRY_EVENT_C)
            .post()
            .uri(BASE_URL)
            .bodyValue(event)
            .exchange()

        // Assert
        result.body<EventReaderDto>(OK)
        verify(readerMapper, times(1)).toDto(any(), any())
        verify(writerMapper, times(1)).toModel(any())
        verify(service, times(1)).createEvent(any(), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong EventDto")
    fun `Should createEvent return 400`(
        event: EventWriterDto,
        expectedCode: String,
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
        result.assertError(BAD_REQUEST, expectedCode)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(optionsReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should updateEventById return 200`() {
        // Arrange
        val event = EventWriterDto(name = "event", begin = now(), end = now().plusDays(1), options = listOf(ACTIVITY))

        `when`(service.updateEventById(any(), any(), any())).thenReturn(Mono.just(EventModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId), emptyList()))
            .bodyValue(event)
            .exchange()

        // Assert
        result.body<EventReaderDto>(OK)
        verify(readerMapper, times(1)).toDto(any(), any())
        verify(writerMapper, times(1)).toModel(any())
        verify(service, times(1)).updateEventById(any(), eq(eventId), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong EventDto")
    fun `Should updateEventById return 400`(
        event: EventWriterDto,
        expectedCode: String,
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
        result.assertError(BAD_REQUEST, expectedCode)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(optionsReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should disableEventById return 200`() {
        // Arrange
        `when`(service.disableEventById(any(), any())).thenReturn(Mono.just(EventModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/disable", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<EventReaderDto>(OK)
        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).disableEventById(any(), eq(eventId))
    }

    @Test
    fun `Should enableEventById return 200`() {
        // Arrange
        `when`(service.enableEventById(any(), any())).thenReturn(Mono.just(EventModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/enable", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<EventReaderDto>(OK)
        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(optionsReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).enableEventById(any(), eq(eventId))
    }

    @Test
    fun `Should deleteEventById return 200`() {
        // Arrange
        `when`(service.deleteEventById(any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_D))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(optionsReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).deleteEventById(eq(eventId))
    }
}
