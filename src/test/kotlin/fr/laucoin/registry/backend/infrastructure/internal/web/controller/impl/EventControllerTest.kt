package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_BEGIN_LATER_THAN_END_TIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
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
import fr.laucoin.registry.backend.domain.model.EventSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventOptionsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.EventWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.stream.Stream
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
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class EventControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
    @MockitoBean
    private lateinit var service: IEventService

    @MockitoBean
    private lateinit var readerMapper: EventReaderDtoMapper

    @MockitoBean
    private lateinit var optionsReaderMapper: EventOptionsReaderDtoMapper

    @MockitoBean
    private lateinit var writerMapper: EventWriterDtoMapper

    companion object {
        private const val BASE_URL = "/api/events"

        @JvmStatic
        fun `Should findEvents return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of("not locale", null, null, null, null, null),
            Arguments.of(null, 0, null, null, null, null),
            Arguments.of(null, null, 200, null, null, null),
            Arguments.of(null, null, null, null, null, null),
            Arguments.of(null, null, null, "text", null, null),
            Arguments.of(null, null, null, null, true, null),
            Arguments.of(null, null, null, null, null, null),
            Arguments.of(null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
        )

        @JvmStatic
        fun `Should findEvents throw due to wrong params`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(- 1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
                Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
                Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
            )
        }

        @JvmStatic
        fun `Wrong EventDto`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                EventWriterDto(
                    name = "",
                    begin = CustomDateTimeWriterDto(LocalDate.MIN, LocalTime.MIN),
                    end = CustomDateTimeWriterDto(LocalDate.MAX, LocalTime.MAX),
                    options = emptyList()
                ),
                EVENT_NAME_NULL_OR_BLANK,
            ),
            Arguments.of(
                EventWriterDto(
                    name = "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiop",
                    begin = CustomDateTimeWriterDto(LocalDate.MIN, LocalTime.MIN),
                    end = CustomDateTimeWriterDto(LocalDate.MAX, LocalTime.MAX),
                    options = emptyList()
                ),
                EVENT_NAME_TOO_LONG,
            ),
            Arguments.of(
                EventWriterDto(
                    name = "event",
                    begin = CustomDateTimeWriterDto(LocalDate.MAX, LocalTime.MAX),
                    end = CustomDateTimeWriterDto(LocalDate.MIN, LocalTime.MIN),
                    options = emptyList()
                ),
                EVENT_BEGIN_LATER_THAN_END_TIME,
            ),
            Arguments.of(
                EventWriterDto(
                    name = "event",
                    begin = CustomDateTimeWriterDto(LocalDate.MIN, LocalTime.MIN),
                    end = CustomDateTimeWriterDto(LocalDate.MAX, LocalTime.MAX),
                    options = listOf(ACTIVITY_COMMUNICATION)
                ),
                EVENT_OPTIONS_MISSING,
            ),
            Arguments.of(
                EventWriterDto(
                    name = "event",
                    begin = CustomDateTimeWriterDto(LocalDate.MIN, LocalTime.MIN),
                    end = CustomDateTimeWriterDto(LocalDate.MAX, LocalTime.MAX),
                    options = listOf(MOVEMENT_REPORT)
                ),
                EVENT_OPTIONS_MISSING,
            ),
            Arguments.of(
                EventWriterDto(
                    name = "event",
                    begin = CustomDateTimeWriterDto(LocalDate.MIN, LocalTime.MIN),
                    end = CustomDateTimeWriterDto(LocalDate.MAX, LocalTime.MAX),
                    options = listOf(SMOKE_REPORT)
                ),
                EVENT_OPTIONS_MISSING,
            ),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findEvents return 200`(
        requestedLocale: String?,
        pageNumber: Int?,
        pageSize: Int?,
        textSearched: String?,
        visibilitySearched: Boolean?,
        dateTimeSearched: String?,
    ) {
        // Arrange
        val expectedPageNumber = pageNumber ?: 0
        val expectedPageSize = pageSize ?: 20
        val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
        val searchParams = EventSearchParamModel(
            textSearched = textSearched,
            visibilitySearched = visibilitySearched,
            dateTimeSearched = dateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
        )
        val page = PageModel(pageable, totalElements = 1, listOf(EventModel()))
        whenever(service.findEventsPage(any(), any(), any())).thenReturn(Mono.just(page))
        whenever(readerMapper.toDtoPage(any(), any())).thenReturn(
            PageModel(pageable, totalElements = 1, listOf(EventReaderDto())),
        )

        // Act
        val result = webClient
            .authenticate()
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    emptyList(),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                        Pair("textSearched", textSearched),
                        Pair("visibilitySearched", visibilitySearched),
                        Pair("dateTimeSearched", dateTimeSearched),
                    ),
                )
            )
            .header(ACCEPT_LANGUAGE, requestedLocale)
            .exchange()

        // Assert
        result.body<PageModel<*>>(OK)

        verify(service).findEventsPage(currentUser(), pageable, searchParams)
        verify(readerMapper).toDtoPage(any(), any())
        verifyNoInteractions(optionsReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findEvents throw due to wrong params`(
        pageNumber: Int?,
        pageSize: Int?,
        expectedMessage: String,
    ) {
        // Act
        val result = webClient
            .authenticate()
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    emptyList(),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                    ),
                )
            )
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedMessage)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(optionsReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should findEventById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.findEventById(any(), anyOrNull())).thenReturn(Mono.just(EventModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(EventReaderDto())

        // Act
        val result = webClient
            .authenticate(REGISTRY_EVENT_R)
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(uuid), emptyList()))
            .exchange()

        // Assert
        result.body<EventReaderDto>(OK)

        verify(service).findEventById(uuid, visibilitySearched = null)
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(optionsReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should getAvailableEventOptions return 200`() {
        // Arrange
        whenever(service.availableEventOptions()).thenReturn(Flux.just(Pair(ACTIVITY_COMMUNICATION, listOf(ACTIVITY))))
        whenever(optionsReaderMapper.toDto(any(), any())).thenReturn(EventOptionsReaderDto(ACTIVITY, "label", "question", emptyList()))

        // Act
        val result = webClient
            .authenticate(REGISTRY_EVENT_METADATA_R)
            .get()
            .uri(uriBuilder("$BASE_URL/options", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<List<*>>(OK)

        verify(service).availableEventOptions()
        verifyNoInteractions(readerMapper)
        verify(optionsReaderMapper).toDto(any(), any())
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should createEvent return 200`() {
        // Arrange
        val event = EventWriterDto(
            name = "event",
            begin = CustomDateTimeWriterDto(LocalDate.MIN, LocalTime.MIN),
            end = CustomDateTimeWriterDto(LocalDate.MAX, LocalTime.MAX),
            options = listOf(ACTIVITY)
        )
        whenever(service.createEvent(any(), any())).thenReturn(Mono.just(EventModel()))
        whenever(writerMapper.toModel(any())).thenReturn(EventModel())
        whenever(readerMapper.toDto(any(), any())).thenReturn(EventReaderDto())

        // Act
        val result = webClient
            .authenticate(REGISTRY_EVENT_C)
            .post()
            .uri(BASE_URL)
            .bodyValue(event)
            .exchange()

        // Assert
        result.body<EventReaderDto>(OK)

        verify(service).createEvent(any(), any())
        verify(readerMapper).toDto(any(), any())
        verify(writerMapper).toModel(any())
    }

    @ParameterizedTest
    @MethodSource("Wrong EventDto")
    fun `Should createEvent return 400`(
        event: EventWriterDto,
        expectedCode: String,
    ) {
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
        val event = EventWriterDto(
            name = "event",
            begin = CustomDateTimeWriterDto(LocalDate.MIN, LocalTime.MIN),
            end = CustomDateTimeWriterDto(LocalDate.MAX, LocalTime.MAX),
            options = listOf(ACTIVITY)
        )

        whenever(service.updateEventById(any(), any(), any())).thenReturn(Mono.just(EventModel()))
        whenever(writerMapper.toModel(any())).thenReturn(EventModel())
        whenever(readerMapper.toDto(any(), any())).thenReturn(EventReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId), emptyList()))
            .bodyValue(event)
            .exchange()

        // Assert
        result.body<EventReaderDto>(OK)

        verify(service).updateEventById(any(), eq(eventId), any())
        verify(readerMapper).toDto(any(), any())
        verify(writerMapper).toModel(any())
    }

    @ParameterizedTest
    @MethodSource("Wrong EventDto")
    fun `Should updateEventById return 400`(
        event: EventWriterDto,
        expectedCode: String,
    ) {
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
        whenever(service.disableEventById(any(), any())).thenReturn(Mono.just(EventModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(EventReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/disable", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<EventReaderDto>(OK)

        verify(service).disableEventById(any(), eq(eventId))
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should enableEventById return 200`() {
        // Arrange
        whenever(service.enableEventById(any(), any())).thenReturn(Mono.just(EventModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(EventReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/enable", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<EventReaderDto>(OK)

        verify(service).enableEventById(any(), eq(eventId))
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(optionsReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should deleteEventById return 200`() {
        // Arrange
        whenever(service.deleteEventById(any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_D))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)

        verify(service).deleteEventById(eq(eventId))
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(optionsReaderMapper)
        verifyNoInteractions(writerMapper)
    }
}
