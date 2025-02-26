package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_DATE_CONFLICT_WITH_ELEMENTS
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_R
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IEventModelRepository
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.LocalDate
import java.time.LocalTime
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Mono

class EventServiceTest {
    private val repository: IEventModelRepository = mock()
    private val eventProfileService: IUserEventProfileService = mock()
    private val transactionalOperator: TransactionalOperator = mock()
    private val roleService: IRoleService = mock()
    private val service: IEventService = EventService(
        repository,
        eventProfileService,
        transactionalOperator,
        roleService
    )

    companion object {
        @JvmStatic
        fun `Should validateDateTime call repository findById and validate the request date is in event range`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null, null, null),
                Arguments.of(
                    null, null,
                    CustomDateTimeModel(LocalDate.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MAX, LocalTime.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.MIN),
                    CustomDateTimeModel(LocalDate.MAX, LocalTime.MAX),
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.MIN),
                    CustomDateTimeModel(LocalDate.MAX, LocalTime.MAX),
                    CustomDateTimeModel(LocalDate.MAX, LocalTime.MAX),
                ),
            )
        }

        @JvmStatic
        fun `Should validateDateTime call repository findById and throw on invalid date`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.of(0, 0, 1)),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX, LocalTime.of(23, 59, 59)),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
            )
        }

        @JvmStatic
        fun `Should validateDateTimes call repository findById and validate the request dates are in event range`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null, null, null, null),
                Arguments.of(
                    null, null,
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.MIN),
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.MIN),
                    CustomDateTimeModel(LocalDate.MAX, LocalTime.MAX),
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.MIN),
                    CustomDateTimeModel(LocalDate.MAX, LocalTime.MAX),
                ),
            )
        }

        @JvmStatic
        fun `Should validateDateTimes call repository findById and throw on invalid dates`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.of(0, 0, 1)),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX, LocalTime.of(23, 59, 59)),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
            )
        }

        @JvmStatic
        fun `Should updateEventById call repository findById, validDateTime and update`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null, null, null, null, 0),
                Arguments.of(
                    null, null,
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    1,
                ),
                Arguments.of(
                    null,
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                    null,
                    1,
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    null,
                    null,
                    CustomDateTimeModel(LocalDate.MAX),
                    1,
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    0,
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.MIN),
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.MAX),
                    1,
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.MIN),
                    CustomDateTimeModel(LocalDate.MAX, LocalTime.MAX),
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.MIN),
                    CustomDateTimeModel(LocalDate.MAX, LocalTime.MAX),
                    0,
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.EPOCH),
                    CustomDateTimeModel(LocalDate.EPOCH),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    0,
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN, LocalTime.of(0, 0, 1)),
                    CustomDateTimeModel(LocalDate.MAX, LocalTime.of(23, 59, 59)),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    0,
                ),
            )
        }
    }

    @Test
    fun `Should findEventsPage call repository findPage`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = EventSearchParamModel()
        whenever(roleService.getAuthoritiesByUserRole(anyOrNull())).thenReturn(listOf(REGISTRY_EVENT_R))
        whenever(repository.findPage(any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findEventsPage(currentUser(REGISTRY_EVENT_R), pageable, params).block()

        // Assert
        verify(repository).findPage(pageable, params)
        verify(repository, never()).findPage(any(), any(), any())
    }

    @Test
    fun `Should findEventsPage call repository findPage for currentUserEvent`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = EventSearchParamModel()
        whenever(roleService.getAuthoritiesByUserRole(anyOrNull())).thenReturn(emptyList())
        whenever(roleService.getEventIdsFromCurrentUserProfiles(any())).thenReturn(emptyList())
        whenever(repository.findPage(any(), any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findEventsPage(currentUser(), pageable, params).block()

        // Assert
        verify(repository).findPage(emptyList(), pageable, params)
        verify(repository, never()).findPage(any(), any())
    }

    @Test
    fun `Should findEventById call repository findById`() {
        // Arrange
        val event = EventModel()
        val onlyVisible = true
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(event))

        // Act
        service.findEventById(eventId, onlyVisible).block()

        // Assert
        verify(repository).findById(eventId, onlyVisible)
    }

    @Test
    fun `Should findActivityById call repository findById throw on empty result`() {
        // Arrange
        val onlyVisible = true
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.empty())

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.findEventById(eventId, onlyVisible).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(1, result.args?.size)
        verify(repository).findById(eventId, onlyVisible)
    }

    @Test
    fun `Should availableEventOptions not throw`() {
        // Act
        assertDoesNotThrow {
            service.availableEventOptions().blockFirst()
        }

        // Assert
        verifyNoInteractions(repository)
        verifyNoInteractions(eventProfileService)
        verifyNoInteractions(transactionalOperator)
        verifyNoInteractions(roleService)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validateDateTime call repository findById and validate the request date is in event range`(
        eventBeginDateTime: CustomDateTimeModel?,
        eventEndDateTime: CustomDateTimeModel?,
        dateTime: CustomDateTimeModel?,
    ) {
        // Arrange
        val event = EventModel().apply { begin = eventBeginDateTime; end = eventEndDateTime }
        val errorMessage = "ERROR_MESSAGE"
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(event))

        // Act
        val result = service.validateDateTime(eventId, dateTime, errorMessage).block()

        // Assert
        assertEquals(eventId, result)
        verify(repository).findById(eventId, visibilitySearched = null)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validateDateTime call repository findById and throw on invalid date`(
        eventBeginDateTime: CustomDateTimeModel?,
        eventEndDateTime: CustomDateTimeModel?,
        dateTime: CustomDateTimeModel?,
    ) {
        // Arrange
        val event = EventModel().apply { begin = eventBeginDateTime; end = eventEndDateTime }
        val errorMessage = "ERROR_MESSAGE"
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(event))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.validateDateTime(eventId, dateTime, errorMessage).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(errorMessage, result.message)
        assertEquals(3, result.args?.size)
        verify(repository).findById(eventId, visibilitySearched = null)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validateDateTimes call repository findById and validate the request dates are in event range`(
        eventBeginDateTime: CustomDateTimeModel?,
        eventEndDateTime: CustomDateTimeModel?,
        startDateTime: CustomDateTimeModel?,
        endDateTime: CustomDateTimeModel?,
    ) {
        // Arrange
        val event = EventModel().apply { begin = eventBeginDateTime; end = eventEndDateTime }
        val errorMessage = "ERROR_MESSAGE"
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(event))

        // Act
        val result = service.validateDateTimes(eventId, startDateTime, endDateTime, errorMessage).block()

        // Assert
        assertEquals(eventId, result)
        verify(repository).findById(eventId, visibilitySearched = null)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validateDateTimes call repository findById and throw on invalid dates`(
        eventBeginDateTime: CustomDateTimeModel?,
        eventEndDateTime: CustomDateTimeModel?,
        startDateTime: CustomDateTimeModel?,
        endDateTime: CustomDateTimeModel?,
    ) {
        // Arrange
        val event = EventModel().apply { begin = eventBeginDateTime; end = eventEndDateTime }
        val errorMessage = "ERROR_MESSAGE"
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(event))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.validateDateTimes(eventId, startDateTime, endDateTime, errorMessage).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(errorMessage, result.message)
        assertEquals(4, result.args?.size)
        verify(repository).findById(eventId, visibilitySearched = null)
    }

    @Test
    fun `Should createEvent call profile service createUserEventProfileFromEvent and repository create`() {
        // Arrange
        val event = EventModel()
        whenever(eventProfileService.createUserEventProfileFromEvent(any(), any())).thenReturn(Mono.just(EventProfileModel()))
        whenever(repository.create(any())).thenReturn(Mono.just(event))
        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

        // Act
        service.createEvent(currentUser(), event).block()

        // Assert
        verify(eventProfileService).createUserEventProfileFromEvent(currentUser(), event)
        verify(repository).create(event)
        verify(transactionalOperator).transactional(any<Mono<*>>())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateEventById call repository findById, validDateTime and update`(
        eventBeginDateTime: CustomDateTimeModel?,
        eventEndDateTime: CustomDateTimeModel?,
        newEventBeginDateTime: CustomDateTimeModel?,
        newEventEndDateTime: CustomDateTimeModel?,
        expectedVerificationCall: Int,
    ) {
        // Arrange
        val eventToUpdate = EventModel().apply { id = eventId; begin = eventBeginDateTime; end = eventEndDateTime }
        val eventUpdated = EventModel().apply { id = eventId; begin = newEventBeginDateTime; end = newEventEndDateTime }
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(eventToUpdate))
        whenever(repository.validDateTime(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(true))
        whenever(repository.update(any())).thenReturn(Mono.just(eventUpdated))

        // Act
        service.updateEventById(currentUser(), eventId, eventUpdated).block()

        // Assert
        verify(repository).findById(eventId, visibilitySearched = null)
        verify(repository, times(expectedVerificationCall)).validDateTime(
            eventId,
            newEventBeginDateTime?.toLocalDateTime(LocalTime.MIN),
            newEventEndDateTime?.toLocalDateTime(LocalTime.MAX),
        )
        verify(repository).update(any())
    }

    @Test
    fun `Should updateEventById call repository findById, validDateTime and throw due to date conflict`() {
        // Arrange
        val eventToUpdate = EventModel().apply { id = eventId }
        val dateTime = CustomDateTimeModel(LocalDate.EPOCH)
        val eventUpdated = EventModel().apply { id = eventId; begin = dateTime; end = dateTime }
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(eventToUpdate))
        whenever(repository.validDateTime(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(false))
        whenever(repository.update(any())).thenReturn(Mono.just(eventUpdated))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.updateEventById(currentUser(), eventId, eventUpdated).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(EVENT_DATE_CONFLICT_WITH_ELEMENTS, result.message)
        assertNull(result.args)
        verify(repository).findById(eventId, visibilitySearched = null)
        verify(repository).validDateTime(
            eventId,
            dateTime.toLocalDateTime(LocalTime.MIN),
            dateTime.toLocalDateTime(LocalTime.MAX),
        )
        verify(repository, never()).update(any())
    }

    @Test
    fun `Should disableEventById call existing event and call repository update`() {
        // Arrange
        val event = EventModel().apply { visible = true }
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(event))
        whenever(repository.update(any())).thenReturn(Mono.just(event))

        // Act
        service.disableEventById(currentUser(), eventId).block()

        // Assert
        verify(repository).findById(eventId, visibilitySearched = true)
        verify(repository).update(event.apply { visible = false })
    }

    @Test
    fun `Should enableEventById call existing event and call repository update`() {
        // Arrange
        val event = EventModel().apply { visible = false }
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(event))
        whenever(repository.update(any())).thenReturn(Mono.just(event))

        // Act
        service.enableEventById(currentUser(), eventId).block()

        // Assert
        verify(repository).findById(eventId, visibilitySearched = false)
        verify(repository).update(event.apply { visible = true })
    }

    @Test
    fun `Should deleteEventById call existing event and call repository deleteById`() {
        // Arrange
        val event = EventModel()
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(event))
        whenever(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteEventById(eventId).block()

        // Assert
        verify(repository).findById(eventId, visibilitySearched = null)
        verify(repository).deleteById(eventId)
    }
}
