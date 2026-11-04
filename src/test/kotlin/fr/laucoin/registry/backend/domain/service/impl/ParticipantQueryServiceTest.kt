package fr.laucoin.registry.backend.domain.service.impl

import reactor.test.StepVerifier
import java.time.ZonedDateTime
import java.time.ZoneOffset
import fr.laucoin.registry.backend.domain.model.TimelineRangeModel
import fr.laucoin.registry.backend.domain.model.ParticipantTimelineModel
import fr.laucoin.registry.backend.domain.model.OutingModel
import fr.laucoin.registry.backend.domain.model.MovementEventModel
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.SHOPPING
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.HttpStatus.FORBIDDEN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.test.ModelExt.defaultParticipant
import fr.laucoin.registry.backend.test.ModelExt.defaultParticipantId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.LocalDate

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.IParticipantQueryService

class ParticipantQueryServiceTest {
	private val port: IParticipantPort = mock()
	private val userPort: IUserPort = mock()
	private val movementPort: IMovementPort = mock()
	private val groupPort: IGroupPort = mock()

	private val service: IParticipantQueryService =
		ParticipantQueryService(port, userPort, movementPort, groupPort, Clock.systemUTC(), MAX_USERS, MAX_GROUPS)

	private companion object {
		private const val MAX_USERS = 1

		private const val MAX_GROUPS = 2

		private val past = CustomDateTimeModel(LocalDate.of(2025, 9, 1))

		private val future = CustomDateTimeModel(LocalDate.of(2025, 10, 1))
	}


	@Test
	fun `Should findParticipantTimeline return the planned window and the outings of a registered participant`() {
		// Arrange
		val departure = ZonedDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC)
		val participant = defaultParticipant().apply {
			type = REGISTERED
			startAvailability = past
			endAvailability = future
		}
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participant))
		whenever(movementPort.findEventsByParticipantId(any(), any())).thenReturn(
			Flux.just(MovementEventModel(departure, OUT, SHOPPING), MovementEventModel(departure.plusHours(2), IN))
		)

		// Act
		val timeline = service.findParticipantTimeline(defaultProjectId, defaultParticipantId).block()!!

		// Assert
		assertEquals(listOf(TimelineRangeModel(past, future)), timeline.planned)
		assertEquals(listOf(OutingModel(departure, departure.plusHours(2))), timeline.actual)
	}

	@Test
	fun `Should findParticipantTimeline return no planned range when the participant has no availability window`() {
		// Arrange
		val participant = defaultParticipant().apply { type = REGISTERED }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participant))
		whenever(movementPort.findEventsByParticipantId(any(), any())).thenReturn(Flux.empty())

		// Act
		val timeline = service.findParticipantTimeline(defaultProjectId, defaultParticipantId).block()!!

		// Assert
		assertEquals(ParticipantTimelineModel(), timeline)
	}

	@Test
	fun `Should findParticipantTimeline keep a half open planned range`() {
		// Arrange
		val participant = defaultParticipant().apply { type = REGISTERED; endAvailability = future }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participant))
		whenever(movementPort.findEventsByParticipantId(any(), any())).thenReturn(Flux.empty())

		// Act
		val timeline = service.findParticipantTimeline(defaultProjectId, defaultParticipantId).block()!!

		// Assert
		assertEquals(listOf(TimelineRangeModel(null, future)), timeline.planned)
	}

	@Test
	fun `Should findParticipantTimeline have no outing for a guest without reading the movements`() {
		// Arrange
		val participant = defaultParticipant().apply { type = GUEST }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participant))

		// Act
		val timeline = service.findParticipantTimeline(defaultProjectId, defaultParticipantId).block()!!

		// Assert
		assertEquals(emptyList<OutingModel>(), timeline.actual)
		verifyNoInteractions(movementPort)
	}

	@Test
	fun `Should findParticipantTimeline throw when the participant does not exist`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = service.findParticipantTimeline(defaultProjectId, defaultParticipantId)

		// Assert
		StepVerifier.create(result).expectError(RegistryException::class.java).verify()
		verifyNoInteractions(movementPort)
	}

	@Test
	fun `Should findParticipantsPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ParticipantSearchParamModel()

		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findParticipantsPage(defaultProjectId, pageable, params).block()

		// Assert
		verify(port).findPage(defaultProjectId, pageable, params)
	}

	@Test
	fun `Should findParticipantsByIds call port findAllByIds`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))

		// Act
		service.findParticipantsByIds(defaultProjectId, listOf(defaultParticipantId), onlyVisible).blockFirst()

		// Assert
		verify(port).findAllByIds(defaultProjectId, listOf(defaultParticipantId), onlyVisible)
	}

	@Test
	fun `Should findBirthdays call port findAllByIds`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findBirthdays(any(), any())).thenReturn(Flux.just(defaultParticipant()))

		// Act
		service.findBirthdays(defaultProjectId).blockFirst()

		// Assert
		verify(port).findBirthdays(defaultProjectId, onlyVisible)
	}

	@Test
	fun `Should findParticipantById call port findById`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultParticipant()))

		// Act
		service.findParticipantById(defaultProjectId, defaultParticipantId, onlyVisible).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultParticipantId, onlyVisible)
	}

	@Test
	fun `Should findParticipantById call port findById throw on empty result`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findParticipantById(defaultProjectId, defaultParticipantId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(defaultParticipantId.toString(), result.args?.first())

		verify(port).findById(defaultProjectId, defaultParticipantId, onlyVisible)
	}

	@Test
	fun `Should searchUsers call userPort findWithLimit`() {
		// Arrange
		val query = "text"
		val expectedSearch = UserSearchParamModel(query, isVisible = true)

		whenever(userPort.findWithLimit(any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchUsersByText(defaultProjectId, query).blockFirst()

		// Assert
		verify(
			userPort,
		).findWithLimit(eq(MAX_USERS), argThat { this == expectedSearch && memberOfProjectId == defaultProjectId })
	}

	@Test
	fun `Should searchGroups call groupPort findWithLimit`() {
		// Arrange
		val query = "text"
		val expectedSearch = GroupSearchParamModel(query, isVisible = true)

		whenever(groupPort.findWithLimit(any(), any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchGroupsByText(defaultProjectId, query).blockFirst()

		// Assert
		verify(groupPort).findWithLimit(MAX_GROUPS, defaultProjectId, expectedSearch)
	}

	@Test
	fun `Should findParticipantMovementsPage refuse the hasActivity filter without the activity option`() {
		// Arrange
		val params = MovementSearchParamModel(type = null, hasActivity = true)

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findParticipantMovementsPage(
				currentUser(), defaultProjectId, defaultParticipantId, PageableModel(0, 10), params,
			).block()
		}) as RegistryException

		// Assert
		assertEquals(FORBIDDEN, result.status)
		assertEquals(NOT_ENOUGH_PERMISSION, result.code)
		verifyNoInteractions(movementPort)
	}

	@Test
	fun `Should findParticipantMovementsPage call movementPort findPageByParticipantId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(type = IN)

		whenever(movementPort.findPageByParticipantId(any(), any(), any(), any())).thenReturn(Mono.empty())

		// Act
		service.findParticipantMovementsPage(currentUser(), defaultProjectId, defaultParticipantId, pageable, params).block()

		// Assert
		verify(movementPort).findPageByParticipantId(defaultProjectId, defaultParticipantId, pageable, params)
	}
}
