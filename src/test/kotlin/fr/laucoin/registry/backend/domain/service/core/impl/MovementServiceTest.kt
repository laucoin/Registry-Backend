package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import org.mockito.kotlin.verifyNoMoreInteractions
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_DELETED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_DISABLED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_ENABLED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_UPDATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_COMMUNICATION_OUT_OF_MOVEMENT_DATETIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_IN_FUTURE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DEFINITIVE_DEPARTURE_BEFORE_LATER_MOVEMENTS
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DRIVERS_NOT_MAJOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_REMOVE_GUEST_CONTENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_UPDATE_CHANGE_CONTENT_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_UPDATE_CHANGE_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.MOVEMENT
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.DEFINITIVE_DEPARTURE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.port.IMovementHistoryPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.domain.helper.ProjectDateHelper
import fr.laucoin.registry.backend.test.ModelExt.defaultActivity
import fr.laucoin.registry.backend.test.ModelExt.defaultActivityId
import fr.laucoin.registry.backend.test.ModelExt.defaultMovement
import fr.laucoin.registry.backend.test.ModelExt.defaultMovementId
import fr.laucoin.registry.backend.test.ModelExt.defaultParticipant
import fr.laucoin.registry.backend.test.ModelExt.defaultParticipantId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.ModelExt.defaultVehicle
import fr.laucoin.registry.backend.test.ModelExt.defaultVehicleId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.LocalDate
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import java.util.stream.Stream

import fr.laucoin.registry.backend.domain.helper.MovementContentHelper
import fr.laucoin.registry.backend.domain.helper.MovementParticipantHelper
import fr.laucoin.registry.backend.domain.helper.MovementPersistenceHelper
import fr.laucoin.registry.backend.domain.helper.MovementRulesHelper
import fr.laucoin.registry.backend.domain.model.MovementFeatureModel

class MovementServiceTest {
	private val projectDates: ProjectDateHelper = mock()
	private val port: IMovementPort = mock()
	private val historyPort: IMovementHistoryPort = mock()
	private val rowLockPort: IRowLockPort = mock()
	private val participantPort: IParticipantPort = mock()
	private val vehiclePort: IVehiclePort = mock()
	private val activityPort: IActivityPort = mock()
	private val communicationPort: ICommunicationPort = mock()
	private val transactionalOperator: TransactionalOperator = mock()

	private fun buildService(clock: Clock) = MovementService(
		port,
		MovementRulesHelper(projectDates, communicationPort, clock, MovementFeatureModel(
		MovementFeatureModel.Searched(MAX_PARTICIPANTS, MAX_GROUPS, MAX_VEHICLES, MAX_ACTIVITIES),
		MovementFeatureModel.Planned(MAX_PLANNED),
		FUTURE_TOLERANCE,
	)),
		MovementContentHelper(participantPort, vehiclePort, activityPort, clock),
		MovementParticipantHelper(participantPort, clock),
		MovementPersistenceHelper(port, historyPort, rowLockPort),
		transactionalOperator,
		clock,
	)

	private val service = buildService(Clock.systemUTC())

	private companion object {
		private const val MAX_PARTICIPANTS = 1
		private const val MAX_GROUPS = 2
		private const val MAX_VEHICLES = 3
		private const val MAX_ACTIVITIES = 4
		private const val MAX_PLANNED = 5
		private val FUTURE_TOLERANCE: Duration = Duration.ofMinutes(2)
		private val searchAt: ZonedDateTime = ZonedDateTime.of(2026, 7, 14, 10, 0, 0, 0, ZoneOffset.UTC)

		@JvmStatic
		fun `Should createMovement throw on incompatible participant`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				Flux.empty<ParticipantModel>(),
				NOT_FOUND,
				MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT,
			),
			Arguments.of(
				Flux.just(defaultParticipant().apply { visible = false }),
				NOT_FOUND,
				MOVEMENT_PARTICIPANTS_NOT_VISIBLE,
			),
			Arguments.of(
				Flux.just(defaultParticipant().apply { birthday = LocalDate.now(ZoneOffset.UTC) }),
				UNPROCESSABLE_CONTENT,
				MOVEMENT_DRIVERS_NOT_MAJOR,
			),
		)

		@JvmStatic
		fun `Should createMovement throw on incompatible activity`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				Mono.empty<ActivityModel>(),
				NOT_FOUND,
				MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT,
			),
			Arguments.of(
				Mono.just(defaultActivity().apply { visible = false }),
				NOT_FOUND,
				MOVEMENT_ACTIVITY_NOT_VISIBLE,
			),
		)

		@JvmStatic
		fun `Should createMovement throw on incompatible vehicle`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				Flux.empty<VehicleModel>(),
				NOT_FOUND,
				MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_PROJECT,
			),
			Arguments.of(
				Flux.just(defaultVehicle().apply { visible = false }),
				NOT_FOUND,
				MOVEMENT_VEHICLES_NOT_VISIBLE,
			),
		)

		@JvmStatic
		fun `Should updateMovementById with registered participant`(): Stream<Arguments> = Stream.of(
			Arguments.of(emptyList<ParticipantModel>(), 0),
			Arguments.of(listOf(defaultParticipant()), 1),
		)

		@JvmStatic
		fun `Should updateMovementById throw on changing movement structure`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				defaultMovement().apply { type = IN },
				defaultMovement().apply { type = OUT },
				MOVEMENT_UPDATE_CHANGE_TYPE,
			),
			Arguments.of(
				defaultMovement().apply { contentType = GUEST },
				defaultMovement().apply { contentType = REGISTERED },
				MOVEMENT_UPDATE_CHANGE_CONTENT_TYPE,
			),
			Arguments.of(
				defaultMovement().apply {
					type = IN
					contentType = GUEST
					content = listOf(MovementContentModel().apply { participant = defaultParticipant() })
				},
				defaultMovement().apply {
					type = IN
					contentType = GUEST
					content = listOf(MovementContentModel().apply { participant = defaultParticipant() })
				},
				MOVEMENT_REMOVE_GUEST_CONTENT,
			),
		)

		@JvmStatic
		fun `Should updateMovementById throw on lastParticipantMovement`(): Stream<Arguments> = Stream.of(
			Arguments.of(defaultMovement().apply { reason = DEFINITIVE_DEPARTURE }),
			Arguments.of(defaultMovement().apply { type = OUT; contentType = GUEST }),
		)

		@JvmStatic
		fun `Should updateMovementById with guest participant`(): Stream<Arguments> = Stream.of(
			Arguments.of(emptyList<ParticipantModel>(), 0, 0),
			Arguments.of(listOf(defaultParticipant()), 2, 1),
		)

		@JvmStatic
		fun `Should disableMovementById throw on lastParticipantMovement`(): Stream<Arguments> = Stream.of(
			Arguments.of(defaultMovement().apply { reason = DEFINITIVE_DEPARTURE }),
			Arguments.of(defaultMovement().apply { type = OUT; contentType = GUEST }),
		)

		@JvmStatic
		fun `Should enableMovementById throw on lastParticipantMovement`(): Stream<Arguments> = Stream.of(
			Arguments.of(defaultMovement().apply { reason = DEFINITIVE_DEPARTURE }),
			Arguments.of(defaultMovement().apply { type = OUT; contentType = GUEST }),
		)

		@JvmStatic
		fun `Should deleteMovementById throw on lastParticipantMovement`(): Stream<Arguments> = Stream.of(
			Arguments.of(defaultMovement().apply { reason = DEFINITIVE_DEPARTURE }),
			Arguments.of(defaultMovement().apply { type = OUT; contentType = GUEST }),
		)
	}

	@BeforeEach
	fun setup() {
		whenever(
			projectDates.validateDateTime(
				any(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(defaultProjectId))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }
		whenever(participantPort.refreshEffectiveDates(any())).thenReturn(Mono.just(Unit))
		whenever(participantPort.lockByIds(any())).thenReturn(Flux.empty())
		whenever(rowLockPort.lock(any(), any())).thenReturn(Mono.just(Unit))
		whenever(historyPort.archiveMovement(any())).thenReturn(Mono.just(Unit))
	}

	@Test
	fun `Should createMovement throw on a definitive departure before a later movement`() {
		// Arrange
		val laterMovementParticipant = defaultParticipant().apply { lastMovement = searchAt.plusDays(1) }
		val participantContent = MovementContentModel().apply { participant = defaultParticipant() }
		val movement = defaultMovement().apply {
			type = OUT
			reason = DEFINITIVE_DEPARTURE
			contentType = REGISTERED
			content = listOf(participantContent)
			dateTime = searchAt
		}
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull()))
			.thenReturn(Flux.just(defaultParticipant()), Flux.just(laterMovementParticipant))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(optionHolder(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(MOVEMENT_DEFINITIVE_DEPARTURE_BEFORE_LATER_MOVEMENTS, result.code)
		verify(port, never()).create(any())
	}

	@Test
	fun `Should createMovement throw on a movement dated in the future`() {
		// Arrange
		val movement = defaultMovement().apply { dateTime = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(5) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(optionHolder(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(BAD_REQUEST, result.status)
		assertEquals(MOVEMENT_DATETIME_IN_FUTURE, result.code)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should updateMovementById throw on a movement dated in the future`() {
		// Arrange
		val movement = defaultMovement().apply { dateTime = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(5) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateMovementById(optionHolder(), defaultProjectId, defaultMovementId, movement).block()
		}) as RegistryException

		// Assert
		assertEquals(BAD_REQUEST, result.status)
		assertEquals(MOVEMENT_DATETIME_IN_FUTURE, result.code)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should createMovement accept a movement dated within the future tolerance`() {
		// Arrange
		val participantContent = MovementContentModel().apply { participant = defaultParticipant() }
		val movement = defaultMovement().apply {
			contentType = REGISTERED
			content = listOf(participantContent)
			dateTime = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(1)
		}
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(movement))

		// Act
		val result = service.createMovement(optionHolder(), movement).block()

		// Assert
		assertEquals(movement, result)
		verify(port).create(any())
	}

	@Test
	fun `Should createMovement with registered participant`() {
		// Arrange
		val expected = defaultMovement()
		val participantContent = MovementContentModel().apply { participant = defaultParticipant() }
		val movement = defaultMovement().apply { contentType = REGISTERED; content = listOf(participantContent) }

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(expected))

		// Act
		val result = service.createMovement(optionHolder(), movement).block()

		// Assert
		assertEquals(expected, result)
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verify(participantPort).lockByIds(listOf(defaultParticipantId))
		verify(port).create(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createMovement throw on incompatible participant`(
		returnedParticipant: Flux<ParticipantModel>,
		expectedStatus: HttpStatus,
		expectedCode: String,
	) {
		// Arrange
		val participantContent =
			MovementContentModel().apply { participant = defaultParticipant(); vehicle = defaultVehicle() }
		val movement = defaultMovement().apply { contentType = REGISTERED; content = listOf(participantContent) }

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(returnedParticipant)
		whenever(vehiclePort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultVehicle()))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(optionHolder(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(expectedStatus, result.status)
		assertEquals(expectedCode, result.code)

		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should createMovement with guest participant`() {
		// Arrange
		val expected = defaultMovement()
		val guest = defaultParticipant().apply { id = null; }
		val movement = defaultMovement().apply { type = IN; contentType = GUEST }

		whenever(participantPort.findAllByIds(any(), eq(emptyList()), anyOrNull())).thenReturn(Flux.empty())
		whenever(participantPort.findAllByIds(any(), eq(listOf(defaultParticipantId)), anyOrNull()))
			.thenReturn(Flux.just(defaultParticipant()))
		whenever(participantPort.saveAllGuest(any())).thenReturn(Flux.just(defaultParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(expected))

		// Act
		val result = service.createMovement(optionHolder(), movement, listOf(guest)).block()

		// Assert
		assertEquals(expected, result)
		verify(participantPort).findAllByIds(defaultProjectId, emptyList(), isVisible = null)
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verify(participantPort).saveAllGuest(any())
		verify(port).create(any())
	}

	@Test
	fun `Should createMovement linked to an activity`() {
		// Arrange
		val expected = defaultMovement()
		val participantContent = MovementContentModel().apply { participant = defaultParticipant() }
		val movement = defaultMovement().apply {
			contentType = REGISTERED; content = listOf(participantContent); activity = defaultActivity()
		}

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(activityPort.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultActivity()))
		whenever(port.create(any())).thenReturn(Mono.just(expected))

		// Act
		val result = service.createMovement(optionHolder(), movement).block()

		// Assert
		assertEquals(expected, result)
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verify(activityPort).findById(defaultProjectId, defaultActivityId, isVisible = null)
		verify(port).create(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createMovement throw on incompatible activity`(
		returnedActivity: Mono<ActivityModel>,
		expectedStatus: HttpStatus,
		expectedCode: String,
	) {
		// Arrange
		val participantContent = MovementContentModel().apply { participant = defaultParticipant() }
		val movement = defaultMovement().apply {
			contentType = REGISTERED; content = listOf(participantContent); activity = defaultActivity()
		}

		whenever(activityPort.findById(any(), any(), anyOrNull())).thenReturn(returnedActivity)

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(optionHolder(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(expectedStatus, result.status)
		assertEquals(expectedCode, result.code)

		verify(activityPort).findById(defaultProjectId, defaultActivityId, isVisible = null)
		verify(participantPort).lockByIds(any())
		verifyNoMoreInteractions(participantPort)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should createMovement linked to a vehicle`() {
		// Arrange
		val expected = defaultMovement()
		val driver = defaultParticipant().apply { birthday = LocalDate.EPOCH }
		val participantContent = MovementContentModel().apply { participant = driver; vehicle = defaultVehicle() }
		val movement = defaultMovement().apply { contentType = REGISTERED; content = listOf(participantContent) }

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(driver))
		whenever(vehiclePort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultVehicle()))
		whenever(port.create(any())).thenReturn(Mono.just(expected))

		// Act
		val result = service.createMovement(optionHolder(), movement).block()

		// Assert
		assertEquals(expected, result)
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verify(vehiclePort).findAllByIds(defaultProjectId, listOf(defaultVehicleId), isVisible = null)
		verify(port).create(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createMovement throw on incompatible vehicle`(
		returnedVehicle: Flux<VehicleModel>,
		expectedStatus: HttpStatus,
		expectedCode: String,
	) {
		// Arrange
		val driver = defaultParticipant().apply { birthday = LocalDate.EPOCH }
		val participantContent = MovementContentModel().apply { participant = driver; vehicle = defaultVehicle() }
		val movement = defaultMovement().apply { contentType = REGISTERED; content = listOf(participantContent) }

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(driver))
		whenever(vehiclePort.findAllByIds(any(), any(), anyOrNull())).thenReturn(returnedVehicle)

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(optionHolder(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(expectedStatus, result.status)
		assertEquals(expectedCode, result.code)

		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verify(vehiclePort).findAllByIds(defaultProjectId, listOf(defaultVehicleId), isVisible = null)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should createMovement with last participant movement`() {
		// Arrange
		val participantContent = MovementContentModel().apply { participant = defaultParticipant() }
		val movementDate = ZonedDateTime.now(ZoneOffset.UTC)
		val movement = defaultMovement().apply {
			dateTime = movementDate
			contentType = REGISTERED
			reason = DEFINITIVE_DEPARTURE
			content = listOf(participantContent)
		}

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(movement))

		// Act
		val result = service.createMovement(optionHolder(), movement).block()

		// Assert
		assertEquals(movement, result)
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verify(participantPort).lockByIds(listOf(defaultParticipantId))
		verify(participantPort).refreshEffectiveDates(listOf(defaultParticipantId))
		verify(port).create(any())
	}

	@Test
	fun `Should createMovement with guest out`() {
		// Arrange
		val participantContent = MovementContentModel().apply { participant = defaultParticipant() }
		val movementDate = ZonedDateTime.now(ZoneOffset.UTC)
		val movement = defaultMovement().apply {
			dateTime = movementDate
			contentType = GUEST
			type = OUT
			content = listOf(participantContent)
		}

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(movement))

		// Act
		val result = service.createMovement(optionHolder(), movement).block()

		// Assert
		assertEquals(movement, result)
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verify(participantPort).lockByIds(listOf(defaultParticipantId))
		verify(participantPort).refreshEffectiveDates(listOf(defaultParticipantId))
		verify(port).create(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateMovementById with registered participant`(
		newParticipants: List<ParticipantModel>,
		expectedCallNewParticipant: Int,
	) {
		// Arrange
		val contents = newParticipants.map { MovementContentModel().apply { participant = it } }
		val oldMovement = defaultMovement().apply { type = IN; contentType = REGISTERED }
		val updatedMovement =
			defaultMovement().apply { type = IN; contentType = REGISTERED; content = contents }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldMovement))
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull()))
			.thenReturn(Flux.just(*newParticipants.toTypedArray()))
		whenever(port.update(any())).thenReturn(Mono.just(updatedMovement))

		// Act
		service.updateMovementById(optionHolder(), defaultProjectId, defaultMovementId, updatedMovement).block()

		// Assert
		verify(port, times(2)).findById(defaultProjectId, defaultMovementId, isVisible = null)

		val participantIds = newParticipants.mapNotNull(ParticipantModel::id)
		verify(participantPort, times(expectedCallNewParticipant))
			.findAllByIds(defaultProjectId, participantIds, isVisible = null)

		verify(port).update(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateMovementById throw on changing movement structure`(
		oldMovement: MovementModel,
		updatedMovement: MovementModel,
		expectedCode: String,
	) {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldMovement))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateMovementById(optionHolder(), defaultProjectId, defaultMovementId, updatedMovement).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(expectedCode, result.code)

		verify(port, times(2)).findById(defaultProjectId, defaultMovementId, isVisible = null)
		verify(port, never()).update(any())
		verify(historyPort, never()).archiveMovement(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateMovementById throw on lastParticipantMovement`(movement: MovementModel) {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateMovementById(optionHolder(), defaultProjectId, defaultMovementId, movement).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(MOVEMENT_CANNOT_BE_UPDATED, result.code)

		verify(port, times(2)).findById(defaultProjectId, defaultMovementId, isVisible = null)
		verify(port, never()).update(any())
		verify(historyPort, never()).archiveMovement(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateMovementById with guest participant`(
		newGuests: List<ParticipantModel>,
		expectedCallFindParticipants: Int,
		expectedCallSaveGuest: Int,
	) {
		// Arrange
		val contents = newGuests.map { MovementContentModel().apply { participant = it } }
		val oldMovement = defaultMovement().apply { type = IN; contentType = GUEST }
		val updatedMovement = defaultMovement().apply { type = IN; contentType = GUEST; content = contents }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldMovement))
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull()))
			.thenReturn(Flux.just(*newGuests.toTypedArray()))

		whenever(port.update(any())).thenReturn(Mono.just(updatedMovement))
		whenever(participantPort.saveAllGuest(any())).thenReturn(Flux.just(*newGuests.toTypedArray()))

		// Act
		service.updateMovementById(optionHolder(), defaultProjectId, defaultMovementId, updatedMovement, newGuests)
			.block()

		// Assert
		verify(port, times(2)).findById(defaultProjectId, defaultMovementId, isVisible = null)

		val guestIds = newGuests.mapNotNull(ParticipantModel::id)
		verify(participantPort, times(expectedCallFindParticipants))
			.findAllByIds(defaultProjectId, guestIds, isVisible = null)

		verify(participantPort, times(expectedCallSaveGuest)).saveAllGuest(any())
		verify(port).update(any())
	}

	@Test
	fun `Should updateMovementById with guest throw on not found`() {
		// Arrange
		val contents = listOf(MovementContentModel().apply { participant = defaultParticipant() })
		val oldMovement = defaultMovement().apply { type = IN; contentType = GUEST }
		val updatedMovement = defaultMovement().apply { type = IN; contentType = GUEST; content = contents }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldMovement))
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service
				.updateMovementById(
					currentUser(),
					defaultProjectId,
					defaultMovementId,
					updatedMovement,
					listOf(defaultParticipant())
				)
				.block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT, result.code)

		verify(port, times(2)).findById(defaultProjectId, defaultMovementId, isVisible = null)
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verify(participantPort, never()).saveAllGuest(any())
		verify(port, never()).update(any())
	}

	@Test
	fun `Should updateMovementById date with communication`() {
		// Arrange
		val oldMovement =
			defaultMovement().apply {
				type = IN; contentType = REGISTERED; dateTime = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10)
			}
		val updatedMovement =
			defaultMovement().apply { type = IN; contentType = REGISTERED; dateTime = ZonedDateTime.now(ZoneOffset.UTC) }

		val expectedCommunicationSearch = CommunicationSearchParamModel(
			isVisible = null,
			from = null,
			to = updatedMovement.dateTime,
		)

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldMovement))
		whenever(communicationPort.countAllByMovementId(any(), any(), any())).thenReturn(Mono.just(0L))
		whenever(port.update(any())).thenReturn(Mono.just(updatedMovement))

		// Act
		val result = service.updateMovementById(optionHolder(), defaultProjectId, defaultMovementId, updatedMovement).block()

		// Assert
		assertEquals(updatedMovement, result)
		verify(port, times(2)).findById(defaultProjectId, defaultMovementId, isVisible = null)
		verify(communicationPort).countAllByMovementId(defaultProjectId, defaultMovementId, expectedCommunicationSearch)
		verify(port).update(any())
	}

	@Test
	fun `Should updateMovementById date should throw on communication conflict`() {
		// Arrange
		val oldMovement =
			defaultMovement().apply {
				type = IN; contentType = REGISTERED; dateTime = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10)
			}
		val updatedMovement =
			defaultMovement().apply { type = IN; contentType = REGISTERED; dateTime = ZonedDateTime.now(ZoneOffset.UTC) }
		val conflictCommunication = 1L

		val expectedCommunicationSearch = CommunicationSearchParamModel(
			isVisible = null,
			from = null,
			to = updatedMovement.dateTime,
		)

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldMovement))
		whenever(
			communicationPort.countAllByMovementId(
				any(),
				any(),
				any()
			)
		).thenReturn(Mono.just(conflictCommunication))
		whenever(port.update(any())).thenReturn(Mono.just(updatedMovement))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateMovementById(optionHolder(), defaultProjectId, defaultMovementId, updatedMovement).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(MOVEMENT_COMMUNICATION_OUT_OF_MOVEMENT_DATETIME, result.code)
		assertEquals(arrayListOf(conflictCommunication), result.args)

		verify(port, times(2)).findById(defaultProjectId, defaultMovementId, isVisible = null)
		verify(communicationPort).countAllByMovementId(defaultProjectId, defaultMovementId, expectedCommunicationSearch)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should updateMovementById refresh the effective dates of the previous and the new participants`() {
		// Arrange
		val previousParticipantId = UUID.randomUUID()
		val previousContent =
			MovementContentModel().apply { participant = ParticipantModel().apply { id = previousParticipantId } }
		val newContent = MovementContentModel().apply { participant = defaultParticipant() }
		val oldMovement =
			defaultMovement().apply { type = IN; contentType = REGISTERED; content = listOf(previousContent) }
		val updatedMovement =
			defaultMovement().apply { type = IN; contentType = REGISTERED; content = listOf(newContent) }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldMovement))
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(port.update(any())).thenReturn(Mono.just(updatedMovement))

		// Act
		val result = service.updateMovementById(optionHolder(), defaultProjectId, defaultMovementId, updatedMovement).block()

		// Assert
		assertEquals(updatedMovement, result)
		verify(participantPort).lockByIds(listOf(previousParticipantId, defaultParticipantId))
		verify(participantPort).refreshEffectiveDates(listOf(previousParticipantId, defaultParticipantId))
	}

	@Test
	fun `Should updateMovementById lock then archive the current state before updating`() {
		// Arrange
		val movement = defaultMovement().apply { type = IN; contentType = REGISTERED }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.update(any())).thenReturn(Mono.just(movement))

		// Act
		val result = service.updateMovementById(optionHolder(), defaultProjectId, defaultMovementId, movement).block()

		// Assert
		assertEquals(movement, result)
		val order = inOrder(rowLockPort, historyPort, port)
		order.verify(rowLockPort).lock(MOVEMENT, defaultMovementId)
		order.verify(historyPort).archiveMovement(defaultMovementId)
		order.verify(port).update(any())
	}

	@Test
	fun `Should disableMovementById not archive the state`() {
		// Arrange
		val movement = defaultMovement().apply { contentType = REGISTERED }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.update(any())).thenReturn(Mono.just(movement))

		// Act
		val result = service.disableMovementById(currentUser(), defaultProjectId, defaultMovementId).block()

		// Assert
		assertEquals(movement, result)
		verify(historyPort, never()).archiveMovement(any())
	}

	@Test
	fun `Should disableMovementById refresh the effective dates of its participants`() {
		// Arrange
		val movement = defaultMovement().apply {
			contentType = REGISTERED
			content = listOf(MovementContentModel().apply { participant = defaultParticipant() })
		}

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.update(any())).thenReturn(Mono.just(movement))

		// Act
		val result = service.disableMovementById(currentUser(), defaultProjectId, defaultMovementId).block()

		// Assert
		assertEquals(movement, result)
		verify(participantPort).lockByIds(listOf(defaultParticipantId))
		verify(participantPort).refreshEffectiveDates(listOf(defaultParticipantId))
	}

	@Test
	fun `Should enableMovementById refresh the effective dates of its participants`() {
		// Arrange
		val movement = defaultMovement().apply {
			contentType = REGISTERED
			visible = false
			content = listOf(MovementContentModel().apply { participant = defaultParticipant() })
		}

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.update(any())).thenReturn(Mono.just(movement))

		// Act
		val result = service.enableMovementById(currentUser(), defaultProjectId, defaultMovementId).block()

		// Assert
		assertEquals(movement, result)
		verify(participantPort).lockByIds(listOf(defaultParticipantId))
		verify(participantPort).refreshEffectiveDates(listOf(defaultParticipantId))
	}

	@Test
	fun `Should deleteMovementById refresh the effective dates of its participants`() {
		// Arrange
		val movement = defaultMovement().apply {
			contentType = REGISTERED
			content = listOf(MovementContentModel().apply { participant = defaultParticipant() })
		}

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteMovementById(defaultProjectId, defaultMovementId).block()

		// Assert
		verify(port).deleteById(defaultMovementId)
		verify(participantPort).lockByIds(listOf(defaultParticipantId))
		verify(participantPort).refreshEffectiveDates(listOf(defaultParticipantId))
	}

	@Test
	fun `Should disableMovementById hide and return a Movement`() {
		// Arrange
		val movement = defaultMovement().apply { contentType = REGISTERED }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.update(any())).thenReturn(Mono.just(movement))

		// Act
		val result = service.disableMovementById(currentUser(), defaultProjectId, defaultMovementId).block()

		// Assert
		assertEquals(movement, result)
		verify(port, times(2)).findById(defaultProjectId, defaultMovementId, isVisible = true)
		verify(port).update(movement.apply { visible = false })
	}

	@ParameterizedTest
	@MethodSource
	fun `Should disableMovementById throw on lastParticipantMovement`(movement: MovementModel) {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.disableMovementById(currentUser(), defaultProjectId, defaultMovementId).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(MOVEMENT_CANNOT_BE_DISABLED, result.code)

		verify(port, times(2)).findById(defaultProjectId, defaultMovementId, isVisible = true)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should enableMovementById restore and return a Movement`() {
		// Arrange
		val movement = defaultMovement().apply { contentType = REGISTERED; visible = false }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.update(any())).thenReturn(Mono.just(movement))

		// Act
		val result = service.enableMovementById(currentUser(), defaultProjectId, defaultMovementId).block()

		// Assert
		assertEquals(movement, result)
		verify(port, times(2)).findById(defaultProjectId, defaultMovementId, isVisible = false)
		verify(port).update(movement.apply { visible = true })
	}

	@ParameterizedTest
	@MethodSource
	fun `Should enableMovementById throw on lastParticipantMovement`(movement: MovementModel) {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.enableMovementById(currentUser(), defaultProjectId, defaultMovementId).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(MOVEMENT_CANNOT_BE_ENABLED, result.code)

		verify(port, times(2)).findById(defaultProjectId, defaultMovementId, isVisible = false)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should deleteMovementById delete a Movement`() {
		// Arrange
		val movement = defaultMovement().apply { contentType = REGISTERED }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteMovementById(defaultProjectId, defaultMovementId).block()

		// Assert
		verify(port, times(2)).findById(defaultProjectId, defaultMovementId, isVisible = null)
		verify(port).deleteById(defaultMovementId)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should deleteMovementById throw on lastParticipantMovement`(movement: MovementModel) {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.deleteMovementById(defaultProjectId, defaultMovementId).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(MOVEMENT_CANNOT_BE_DELETED, result.code)

		verify(port, times(2)).findById(defaultProjectId, defaultMovementId, isVisible = null)
		verify(port, never()).deleteById(any())
	}

	@Test
	fun `Should purgeMovementsIfNecessary call older and uncommented movement since a date, and call port deleteById`() {
		// Arrange
		val date = LocalDate.EPOCH
		val uuid1 = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()

		whenever(port.findOlderThanAndUncommentedSince(any())).thenReturn(Flux.just(uuid1, uuid2))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		val result = service.purgeMovementsIfNecessary(date, false).collectList().block()

		// Assert
		assertEquals(listOf(uuid1, uuid2), result)
		verify(port).findOlderThanAndUncommentedSince(date)
		verify(port).deleteById(uuid1)
		verify(port).deleteById(uuid2)
	}

	@Test
	fun `Should purgeMovementsIfNecessary call older and uncommented movement since a date, and not call port deleteById because of dryRun`() {
		// Arrange
		val expected = UUID.randomUUID()
		val date = LocalDate.EPOCH

		whenever(port.findOlderThanAndUncommentedSince(any())).thenReturn(Flux.just(expected))

		// Act
		val result = service.purgeMovementsIfNecessary(date, true).collectList().block()

		// Assert
		assertEquals(listOf(expected), result)
		verify(port).findOlderThanAndUncommentedSince(date)
		verify(port, never()).deleteById(any())
	}

	private fun optionHolder() = currentUser(
		"${defaultProjectId}_$REGISTRY_PROJECT_OPTION_ACTIVITY",
		"${defaultProjectId}_$REGISTRY_PROJECT_OPTION_VEHICLE",
	)

	@Test
	fun `Should createMovement refuse an activity when the project has not the activity option`() {
		// Arrange
		val movement = defaultMovement().apply { activity = defaultActivity() }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(currentUser("${defaultProjectId}_$REGISTRY_PROJECT_OPTION_VEHICLE"), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(FORBIDDEN, result.status)
		assertEquals(NOT_ENOUGH_PERMISSION, result.code)
		verify(port, never()).create(any())
	}

	@Test
	fun `Should updateMovementById refuse a vehicle when the project has not the vehicle option`() {
		// Arrange
		val movement = defaultMovement().apply { content = listOf(MovementContentModel(participant = defaultParticipant(), vehicle = defaultVehicle())) }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultMovement()))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateMovementById(currentUser("${defaultProjectId}_$REGISTRY_PROJECT_OPTION_ACTIVITY"), defaultProjectId, defaultMovementId, movement).block()
		}) as RegistryException

		// Assert
		assertEquals(FORBIDDEN, result.status)
		assertEquals(NOT_ENOUGH_PERMISSION, result.code)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should updateMovementById keep an existing activity editable once the activity option is disabled`() {
		// Arrange
		val oldMovement = defaultMovement().apply { type = IN; activity = defaultActivity() }
		val updatedMovement = defaultMovement().apply { type = OUT; activity = defaultActivity() }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldMovement))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateMovementById(currentUser(), defaultProjectId, defaultMovementId, updatedMovement).block()
		}) as RegistryException

		// Assert
		assertEquals(MOVEMENT_UPDATE_CHANGE_TYPE, result.code)
	}
}
