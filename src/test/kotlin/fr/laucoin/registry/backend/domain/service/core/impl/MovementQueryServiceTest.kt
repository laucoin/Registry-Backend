package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import org.springframework.http.HttpStatus.FORBIDDEN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.port.IMovementHistoryPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.test.ModelExt.defaultMovement
import fr.laucoin.registry.backend.test.ModelExt.defaultMovementId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.util.UUID

import fr.laucoin.registry.backend.domain.helper.MovementPersistenceHelper

class MovementQueryServiceTest {
	private val port: IMovementPort = mock()
	private val historyPort: IMovementHistoryPort = mock()
	private val rowLockPort: IRowLockPort = mock()
	private val participantPort: IParticipantPort = mock()
	private val vehiclePort: IVehiclePort = mock()
	private val communicationPort: ICommunicationPort = mock()

	private fun buildService(clock: Clock) = MovementQueryService(
		port,
		communicationPort,
		participantPort,
		vehiclePort,
		MovementPersistenceHelper(port, historyPort, rowLockPort),
		clock,
	)

	private val service = buildService(Clock.systemUTC())

	private companion object {
	}


	@Test
	fun `Should findMovementsPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel()

		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		val result = service.findMovementsPage(currentUser(), defaultProjectId, pageable, params).block()

		// Assert
		assertEquals(PageModel(1, 2, 3, 4, emptyList<Any>()), result)
		verify(port).findPage(defaultProjectId, pageable, params)
	}

	@Test
	fun `Should findMovementsPage refuse an activity filter without the activity option`() {
		// Arrange
		val params = MovementSearchParamModel(type = null, activityId = UUID.randomUUID())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findMovementsPage(currentUser(), defaultProjectId, PageableModel(0, 10), params).block()
		}) as RegistryException

		// Assert
		assertEquals(FORBIDDEN, result.status)
		assertEquals(NOT_ENOUGH_PERMISSION, result.code)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should findCurrentMovementsPage refuse the hasActivity filter without the activity option`() {
		// Arrange
		val params = MovementSearchParamModel(type = null, hasActivity = true)

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findCurrentMovementsPage(currentUser(), defaultProjectId, PageableModel(0, 10), params).block()
		}) as RegistryException

		// Assert
		assertEquals(FORBIDDEN, result.status)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should findMovementsPage accept an activity filter with the activity option`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(type = null, activityId = UUID.randomUUID())
		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		val result = service.findMovementsPage(
			currentUser(buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY)), defaultProjectId, pageable, params,
		).block()

		// Assert
		assertEquals(PageModel(1, 2, 3, 4, emptyList<Any>()), result)
		verify(port).findPage(defaultProjectId, pageable, params)
	}

	@Test
	fun `Should findCurrentMovementsPage call port findCurrentPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel()

		whenever(port.findCurrentPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		val result = service.findCurrentMovementsPage(currentUser(), defaultProjectId, pageable, params).block()

		// Assert
		assertEquals(PageModel(1, 2, 3, 4, emptyList<Any>()), result)
		verify(port).findCurrentPage(defaultProjectId, pageable, params)
	}

	@Test
	fun `Should findMovementsContent call port findContent`() {
		// Arrange
		val expected = Pair(defaultMovementId, emptyList<MovementContentModel>())
		val ids = listOf(defaultMovementId)

		whenever(port.findContent(any(), any()))
			.thenReturn(Flux.just(expected))

		// Act
		val result = service.findMovementsContent(defaultProjectId, ids).collectList().block()

		// Assert
		assertEquals(listOf(expected), result)
		verify(port).findContent(defaultProjectId, ids)
	}

	@Test
	fun `Should findCurrentMovementsContent call port findCurrentContent`() {
		// Arrange
		val expected = Pair(defaultMovementId, emptyList<MovementContentModel>())
		val ids = listOf(defaultMovementId)

		whenever(port.findCurrentContent(any(), any()))
			.thenReturn(Flux.just(expected))

		// Act
		val result = service.findCurrentMovementsContent(defaultProjectId, ids).collectList().block()

		// Assert
		assertEquals(listOf(expected), result)
		verify(port).findCurrentContent(defaultProjectId, ids)
	}

	@Test
	fun `Should findMovementById call port findById`() {
		// Arrange
		val expected = defaultMovement()
		val onlyVisible = true
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(expected))

		// Act
		val result = service.findMovementById(defaultProjectId, defaultMovementId, onlyVisible).block()

		// Assert
		assertEquals(expected, result)
		verify(port).findById(defaultProjectId, defaultMovementId, onlyVisible)
	}

	@Test
	fun `Should findMovementById throw on empty result`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findMovementById(defaultProjectId, defaultMovementId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(defaultMovementId.toString(), result.args?.first())

		verify(port).findById(defaultProjectId, defaultMovementId, onlyVisible)
	}

	@Test
	fun `Should findMovementCommunicationsPage call communication port findPageByMovementId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = CommunicationSearchParamModel()

		whenever(communicationPort.findPageByMovementId(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		val result = service.findMovementCommunicationsPage(defaultProjectId, defaultMovementId, pageable, params).block()

		// Assert
		assertEquals(PageModel(1, 2, 3, 4, emptyList<Any>()), result)
		verify(communicationPort).findPageByMovementId(defaultProjectId, defaultMovementId, pageable, params)
	}

	@Test
	fun `Should findParticipantsStatus call participant port 5 times`() {
		// Arrange
		val registeredPresentAdult = 1L
		val registeredPresentAdultSearch = ParticipantSearchParamModel(
			query = null,
			isMajor = true,
			type = REGISTERED,
			presence = PresenceStatusEnum.IN,
			isVisible = true,
			at = null
		)
		val registeredAbsentAdult = 2L
		val registeredAbsentAdultSearch = ParticipantSearchParamModel(
			query = null,
			isMajor = true,
			type = REGISTERED,
			presence = PresenceStatusEnum.OUT,
			isVisible = true,
			at = null
		)
		val registeredPresentMinor = 3L
		val registeredPresentChildSearch = ParticipantSearchParamModel(
			query = null,
			isMajor = false,
			type = REGISTERED,
			presence = PresenceStatusEnum.IN,
			isVisible = true,
			at = null
		)
		val registeredAbsentMinor = 4L
		val registeredAbsentChildSearch = ParticipantSearchParamModel(
			query = null,
			isMajor = false,
			type = REGISTERED,
			presence = PresenceStatusEnum.OUT,
			isVisible = true,
			at = null
		)
		val guestPresent = 5L
		val guestPresentSearch = ParticipantSearchParamModel(
			query = null,
			isMajor = null,
			type = GUEST,
			presence = PresenceStatusEnum.IN,
			isVisible = true,
			at = null
		)

		whenever(participantPort.countAll(any(), eq(registeredPresentAdultSearch)))
			.thenReturn(Mono.just(registeredPresentAdult))
		whenever(participantPort.countAll(any(), eq(registeredAbsentAdultSearch)))
			.thenReturn(Mono.just(registeredAbsentAdult))
		whenever(participantPort.countAll(any(), eq(registeredPresentChildSearch)))
			.thenReturn(Mono.just(registeredPresentMinor))
		whenever(participantPort.countAll(any(), eq(registeredAbsentChildSearch)))
			.thenReturn(Mono.just(registeredAbsentMinor))
		whenever(participantPort.countAll(any(), eq(guestPresentSearch))).thenReturn(Mono.just(guestPresent))

		// Act
		val result = service.findParticipantsStatus(defaultProjectId).block()

		// Assert
		assertEquals(registeredPresentAdult, result?.registered?.presentMajors)
		assertEquals(registeredAbsentAdult, result?.registered?.absentMajors)
		assertEquals(registeredPresentMinor, result?.registered?.presentMinors)
		assertEquals(registeredAbsentMinor, result?.registered?.absentMinors)
		assertEquals(guestPresent, result?.guests)

		verify(participantPort).countAll(defaultProjectId, registeredPresentAdultSearch)
		verify(participantPort).countAll(defaultProjectId, registeredAbsentAdultSearch)
		verify(participantPort).countAll(defaultProjectId, registeredPresentChildSearch)
		verify(participantPort).countAll(defaultProjectId, registeredAbsentChildSearch)
		verify(participantPort).countAll(defaultProjectId, guestPresentSearch)
	}

	@Test
	fun `Should findVehiclesStatus call vehicle port 2 times`() {
		// Arrange
		val presentVehicle = 1L
		val presentVehicleSearch = VehicleSearchParamModel(
			query = null,
			isVisible = true,
			status = PresenceStatusEnum.IN,
			at = null
		)
		val absentVehicle = 2L
		val absentVehicleSearch = VehicleSearchParamModel(
			query = null,
			isVisible = true,
			status = PresenceStatusEnum.OUT,
			at = null
		)

		whenever(vehiclePort.countAll(any(), eq(presentVehicleSearch))).thenReturn(Mono.just(presentVehicle))
		whenever(vehiclePort.countAll(any(), eq(absentVehicleSearch))).thenReturn(Mono.just(absentVehicle))

		// Act
		val result = service.findVehiclesStatus(defaultProjectId).block()

		// Assert
		assertEquals(presentVehicle, result?.present)
		assertEquals(absentVehicle, result?.absent)

		verify(vehiclePort).countAll(defaultProjectId, presentVehicleSearch)
		verify(vehiclePort).countAll(defaultProjectId, absentVehicleSearch)
	}
}
