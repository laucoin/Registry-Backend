package fr.laucoin.registry.backend.domain.service.impl

import org.springframework.http.HttpStatus.FORBIDDEN
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_DELETED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_DISABLED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_ENABLED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_UPDATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_COMMUNICATION_OUT_OF_MOVEMENT_DATETIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_IN_FUTURE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DRIVERS_NOT_MAJOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_REMOVE_GUEST_CONTENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_UPDATE_CHANGE_CONTENT_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_UPDATE_CHANGE_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.DEFINITIVE_DEPARTURE
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.EMERGENCY
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.LOGISTICS
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.MEDICAL
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.OTHER
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.PARTNER_ANIMATION
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.SHOPPING
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.VISIT
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.PlannedMovementModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.test.ModelExt.defaultActivity
import fr.laucoin.registry.backend.test.ModelExt.defaultActivityId
import fr.laucoin.registry.backend.test.ModelExt.defaultGroup
import fr.laucoin.registry.backend.test.ModelExt.defaultGroupId
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
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import java.util.stream.Stream

class MovementServiceTest {
	private val projectService: IProjectService = mock()
	private val port: IMovementPort = mock()
	private val participantPort: IParticipantPort = mock()
	private val vehiclePort: IVehiclePort = mock()
	private val activityPort: IActivityPort = mock()
	private val communicationPort: ICommunicationPort = mock()
	private val groupPort: IGroupPort = mock()
	private val transactionalOperator: TransactionalOperator = mock()

	private fun buildService(clock: Clock) = MovementService(
		projectService,
		port,
		participantPort,
		vehiclePort,
		activityPort,
		communicationPort,
		groupPort,
		transactionalOperator,
		clock,
		MAX_PARTICIPANTS,
		MAX_GROUPS,
		MAX_VEHICLES,
		MAX_ACTIVITIES,
		MAX_PLANNED,
		FUTURE_TOLERANCE,
	)

	private val service = buildService(Clock.systemUTC())

	private companion object {
		private const val MAX_PARTICIPANTS = 1
		private const val MAX_GROUPS = 2
		private const val MAX_VEHICLES = 3
		private const val MAX_ACTIVITIES = 4
		private const val MAX_PLANNED = 5
		private val FUTURE_TOLERANCE: Duration = Duration.ofMinutes(2)
		private val PLANNED_DAY = LocalDate.of(2026, 7, 14)

		@JvmStatic
		fun `Should searchReasonsByText return filtered reason depending params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(REGISTERED, IN, emptyList<MovementReasonEnum>()),
				Arguments.of(REGISTERED, OUT, listOf(SHOPPING, MEDICAL, DEFINITIVE_DEPARTURE, OTHER)),
				Arguments.of(GUEST, IN, listOf(EMERGENCY, LOGISTICS, PARTNER_ANIMATION, VISIT)),
				Arguments.of(GUEST, OUT, emptyList<MovementReasonEnum>()),
			)
		}

		@JvmStatic
		fun `Should searchActivitiesByText call vehicle port findWithLimit`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(GUEST, 0, emptyList<ActivityModel>()),
				Arguments.of(REGISTERED, 1, listOf(defaultActivity())),
			)
		}

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
				Flux.just(defaultParticipant().apply { birthday = LocalDate.now() }),
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
		fun `Should updateMovementById with guest participant`(): Stream<Arguments> = Stream.of(
			Arguments.of(emptyList<ParticipantModel>(), 0, 0),
			Arguments.of(listOf(defaultParticipant()), 2, 1),
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
			projectService.validateDateTime(
				any(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(defaultProjectId))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }
		whenever(participantPort.refreshEffectiveDates(any())).thenReturn(Mono.just(Unit))
		whenever(participantPort.lockByIds(any())).thenReturn(Flux.empty())
	}

	@Test
	fun `Should findMovementsPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel()

		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findMovementsPage(currentUser(), defaultProjectId, pageable, params).block()

		// Assert
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
		service.findMovementsPage(
			currentUser(buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY)), defaultProjectId, pageable, params,
		).block()

		// Assert
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
		service.findCurrentMovementsPage(currentUser(), defaultProjectId, pageable, params).block()

		// Assert
		verify(port).findCurrentPage(defaultProjectId, pageable, params)
	}

	@Test
	fun `Should findMovementsContent call port findContent`() {
		// Arrange
		val ids = listOf(defaultMovementId)

		whenever(port.findContent(any(), any()))
			.thenReturn(Flux.just(Pair(defaultMovementId, emptyList())))

		// Act
		service.findMovementsContent(defaultProjectId, ids).collectList().block()

		// Assert
		verify(port).findContent(defaultProjectId, ids)
	}

	@Test
	fun `Should findCurrentMovementsContent call port findCurrentContent`() {
		// Arrange
		val ids = listOf(defaultMovementId)

		whenever(port.findCurrentContent(any(), any()))
			.thenReturn(Flux.just(Pair(defaultMovementId, emptyList())))

		// Act
		service.findCurrentMovementsContent(defaultProjectId, ids).collectList().block()

		// Assert
		verify(port).findCurrentContent(defaultProjectId, ids)
	}

	@Test
	fun `Should findMovementById call port findById`() {
		// Arrange
		val onlyVisible = true
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultMovement()))

		// Act
		service.findMovementById(defaultProjectId, defaultMovementId, onlyVisible).block()

		// Assert
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
	fun `Should searchParticipantsAndGroupsByText call participant and group port findWithLimit`() {
		// Arrange
		val query = "searched"
		val type = REGISTERED
		val expectedParticipantSearch = ParticipantSearchParamModel(
			isMajor = null,
			type,
			isVisible = true,
			isAvailable = true,
		).apply { this.query = query }
		val expectedGroupSearch =
			GroupSearchParamModel(query, isVisible = true, isPresent = true)

		whenever(participantPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(groupPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultGroup()))
		whenever(groupPort.findContent(any(), any(), anyOrNull(), anyOrNull()))
			.thenReturn(Flux.just(Pair(defaultGroupId, listOf(defaultParticipant()))))
		val detailedMember = defaultParticipant().apply { status = PresenceStatusEnum.IN }
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(detailedMember))

		// Act
		val result = service.searchParticipantsAndGroupsByText(defaultProjectId, type, query).block()

		// Assert
		assertEquals(1, result?.t1?.size)
		assertEquals(defaultParticipant(), result?.t1?.first())
		assertEquals(1, result?.t2?.size)
		assertEquals(defaultGroup().apply { members = listOf(detailedMember) }, result?.t2?.first())
		assertEquals(PresenceStatusEnum.IN, result?.t2?.first()?.members?.first()?.status)

		verify(participantPort).findWithLimit(MAX_PARTICIPANTS, defaultProjectId, expectedParticipantSearch)
		verify(groupPort).findWithLimit(MAX_GROUPS, defaultProjectId, expectedGroupSearch)
		verify(groupPort).findContent(
			defaultProjectId, listOf(defaultGroupId), isVisible = true, isAvailable = true
		)
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipant().id!!), isVisible = true)
	}

	@Test
	fun `Should searchParticipantsAndGroupsByText not return a group without any available member`() {
		// Arrange
		whenever(participantPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(groupPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultGroup()))
		whenever(groupPort.findContent(any(), any(), anyOrNull(), anyOrNull()))
			.thenReturn(Flux.just(Pair(defaultGroupId, emptyList<ParticipantModel>())))
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.empty())

		// Act
		val result = service.searchParticipantsAndGroupsByText(defaultProjectId, REGISTERED, "searched").block()

		// Assert
		assertEquals(1, result?.t1?.size)
		assertEquals(0, result?.t2?.size)
	}

	@Test
	fun `Should searchVehiclesByText call vehicle port findWithLimit`() {
		// Arrange
		val query = "searched"
		val expectedVehicleSearch = VehicleSearchParamModel().apply {
			this.query = query
			this.isVisible = true
			this.isAvailable = true
		}

		whenever(vehiclePort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultVehicle()))

		// Act
		service.searchVehiclesByText(defaultProjectId, query).collectList().block()

		// Assert
		verify(vehiclePort).findWithLimit(MAX_VEHICLES, defaultProjectId, expectedVehicleSearch)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should searchReasonsByText return filtered reason depending params`(
		type: ParticipantTypeEnum,
		movementType: MovementTypeEnum,
		expectedReasons: List<MovementReasonEnum>,
	) {
		// Act
		val result = service.searchReasonsByText(type, movementType).collectList().block()

		// Assert
		assertEquals(expectedReasons, result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should searchActivitiesByText call vehicle port findWithLimit`(
		type: ParticipantTypeEnum,
		expectedPortCall: Int,
		expectedActivities: List<ActivityModel>,
	) {
		// Arrange
		val query = "searched"
		val expectedActivitySearch = ActivitySearchParamModel().apply {
			this.query = query
			this.isVisible = true
			this.isAvailable = true
		}

		whenever(activityPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultActivity()))

		// Act
		val result = service.searchActivitiesByText(defaultProjectId, type, query).collectList().block()

		// Assert
		assertEquals(expectedActivities, result)

		verify(activityPort, times(expectedPortCall)).findWithLimit(
			MAX_ACTIVITIES,
			defaultProjectId,
			expectedActivitySearch
		)
	}

	@Test
	fun `Should findMovementCommunicationsPage call communication port findPageByMovementId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = CommunicationSearchParamModel()

		whenever(communicationPort.findPageByMovementId(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findMovementCommunicationsPage(defaultProjectId, defaultMovementId, pageable, params).block()

		// Assert
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

	@Test
	fun `Should createMovement throw on a movement dated in the future`() {
		// Arrange
		val movement = defaultMovement().apply { dateTime = ZonedDateTime.now().plusMinutes(5) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(currentUser(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(BAD_REQUEST, result.status)
		assertEquals(MOVEMENT_DATETIME_IN_FUTURE, result.code)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should updateMovementById throw on a movement dated in the future`() {
		// Arrange
		val movement = defaultMovement().apply { dateTime = ZonedDateTime.now().plusMinutes(5) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateMovementById(currentUser(), defaultProjectId, defaultMovementId, movement).block()
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
			dateTime = ZonedDateTime.now().plusMinutes(1)
		}
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(movement))

		// Act
		service.createMovement(currentUser(), movement).block()

		// Assert
		verify(port).create(any())
	}

	@Test
	fun `Should createMovement with registered participant`() {
		// Arrange
		val participantContent = MovementContentModel().apply { participant = defaultParticipant() }
		val movement = defaultMovement().apply { contentType = REGISTERED; content = listOf(participantContent) }

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(defaultMovement()))

		// Act
		service.createMovement(currentUser(), movement).block()

		// Assert
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
			service.createMovement(currentUser(), movement).block()
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
		val guest = defaultParticipant().apply { id = null; }
		val movement = defaultMovement().apply { type = IN; contentType = GUEST }

		whenever(participantPort.findAllByIds(any(), eq(emptyList()), anyOrNull())).thenReturn(Flux.empty())
		whenever(participantPort.findAllByIds(any(), eq(listOf(defaultParticipantId)), anyOrNull()))
			.thenReturn(Flux.just(defaultParticipant()))
		whenever(participantPort.saveAllGuest(any())).thenReturn(Flux.just(defaultParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(defaultMovement()))

		// Act
		service.createMovement(currentUser(), movement, listOf(guest)).block()

		// Assert
		verify(participantPort).findAllByIds(defaultProjectId, emptyList(), isVisible = null)
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verify(participantPort).saveAllGuest(any())
		verify(port).create(any())
	}

	@Test
	fun `Should createMovement linked to an activity`() {
		// Arrange
		val participantContent = MovementContentModel().apply { participant = defaultParticipant() }
		val movement = defaultMovement().apply {
			contentType = REGISTERED; content = listOf(participantContent); activity = defaultActivity()
		}

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(activityPort.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultActivity()))
		whenever(port.create(any())).thenReturn(Mono.just(defaultMovement()))

		// Act
		service.createMovement(currentUser(), movement).block()

		// Assert
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
			service.createMovement(currentUser(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(expectedStatus, result.status)
		assertEquals(expectedCode, result.code)

		verify(activityPort).findById(defaultProjectId, defaultActivityId, isVisible = null)
		verifyNoInteractions(participantPort)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should createMovement linked to a vehicle`() {
		// Arrange
		val driver = defaultParticipant().apply { birthday = LocalDate.EPOCH }
		val participantContent = MovementContentModel().apply { participant = driver; vehicle = defaultVehicle() }
		val movement = defaultMovement().apply { contentType = REGISTERED; content = listOf(participantContent) }

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(driver))
		whenever(vehiclePort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultVehicle()))
		whenever(port.create(any())).thenReturn(Mono.just(defaultMovement()))

		// Act
		service.createMovement(currentUser(), movement).block()

		// Assert
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
			service.createMovement(currentUser(), movement).block()
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
		val movementDate = ZonedDateTime.now()
		val movement = defaultMovement().apply {
			dateTime = movementDate
			contentType = REGISTERED
			reason = DEFINITIVE_DEPARTURE
			content = listOf(participantContent)
		}

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(movement))

		// Act
		service.createMovement(currentUser(), movement).block()

		// Assert
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verify(participantPort).lockByIds(listOf(defaultParticipantId))
		verify(participantPort).refreshEffectiveDates(listOf(defaultParticipantId))
		verify(port).create(any())
	}

	@Test
	fun `Should createMovement with guest out`() {
		// Arrange
		val participantContent = MovementContentModel().apply { participant = defaultParticipant() }
		val movementDate = ZonedDateTime.now()
		val movement = defaultMovement().apply {
			dateTime = movementDate
			contentType = GUEST
			type = OUT
			content = listOf(participantContent)
		}

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(movement))

		// Act
		service.createMovement(currentUser(), movement).block()

		// Assert
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
		service.updateMovementById(currentUser(), defaultProjectId, defaultMovementId, updatedMovement).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultMovementId, isVisible = null)

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
			service.updateMovementById(currentUser(), defaultProjectId, defaultMovementId, updatedMovement).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(expectedCode, result.code)

		verify(port).findById(defaultProjectId, defaultMovementId, isVisible = null)
		verify(port, never()).update(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateMovementById throw on lastParticipantMovement`(movement: MovementModel) {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateMovementById(currentUser(), defaultProjectId, defaultMovementId, movement).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(MOVEMENT_CANNOT_BE_UPDATED, result.code)

		verify(port).findById(defaultProjectId, defaultMovementId, isVisible = null)
		verify(port, never()).update(any())
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
		service.updateMovementById(currentUser(), defaultProjectId, defaultMovementId, updatedMovement, newGuests)
			.block()

		// Assert
		verify(port).findById(defaultProjectId, defaultMovementId, isVisible = null)

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

		verify(port).findById(defaultProjectId, defaultMovementId, isVisible = null)
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verify(participantPort, never()).saveAllGuest(any())
		verify(port, never()).update(any())
	}

	@Test
	fun `Should updateMovementById date with communication`() {
		// Arrange
		val oldMovement =
			defaultMovement().apply {
				type = IN; contentType = REGISTERED; dateTime = ZonedDateTime.now().minusDays(10)
			}
		val updatedMovement =
			defaultMovement().apply { type = IN; contentType = REGISTERED; dateTime = ZonedDateTime.now() }

		val expectedCommunicationSearch = CommunicationSearchParamModel(
			isVisible = null,
			from = null,
			to = updatedMovement.dateTime,
		)

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldMovement))
		whenever(communicationPort.countAllByMovementId(any(), any(), any())).thenReturn(Mono.just(0L))
		whenever(port.update(any())).thenReturn(Mono.just(updatedMovement))

		// Act
		service.updateMovementById(currentUser(), defaultProjectId, defaultMovementId, updatedMovement).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultMovementId, isVisible = null)
		verify(communicationPort).countAllByMovementId(defaultProjectId, defaultMovementId, expectedCommunicationSearch)
		verify(port).update(any())
	}

	@Test
	fun `Should updateMovementById date should throw on communication conflict`() {
		// Arrange
		val oldMovement =
			defaultMovement().apply {
				type = IN; contentType = REGISTERED; dateTime = ZonedDateTime.now().minusDays(10)
			}
		val updatedMovement =
			defaultMovement().apply { type = IN; contentType = REGISTERED; dateTime = ZonedDateTime.now() }
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
			service.updateMovementById(currentUser(), defaultProjectId, defaultMovementId, updatedMovement).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(MOVEMENT_COMMUNICATION_OUT_OF_MOVEMENT_DATETIME, result.code)
		assertEquals(arrayListOf(conflictCommunication), result.args)

		verify(port).findById(defaultProjectId, defaultMovementId, isVisible = null)
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
		service.updateMovementById(currentUser(), defaultProjectId, defaultMovementId, updatedMovement).block()

		// Assert
		verify(participantPort).lockByIds(listOf(previousParticipantId, defaultParticipantId))
		verify(participantPort).refreshEffectiveDates(listOf(previousParticipantId, defaultParticipantId))
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
		service.disableMovementById(currentUser(), defaultProjectId, defaultMovementId).block()

		// Assert
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
		service.enableMovementById(currentUser(), defaultProjectId, defaultMovementId).block()

		// Assert
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
		service.disableMovementById(currentUser(), defaultProjectId, defaultMovementId).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultMovementId, isVisible = true)
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

		verify(port).findById(defaultProjectId, defaultMovementId, isVisible = true)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should enableMovementById restore and return a Movement`() {
		// Arrange
		val movement = defaultMovement().apply { contentType = REGISTERED; visible = false }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.update(any())).thenReturn(Mono.just(movement))

		// Act
		service.enableMovementById(currentUser(), defaultProjectId, defaultMovementId).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultMovementId, isVisible = false)
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

		verify(port).findById(defaultProjectId, defaultMovementId, isVisible = false)
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
		verify(port).findById(defaultProjectId, defaultMovementId, isVisible = null)
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

		verify(port).findById(defaultProjectId, defaultMovementId, isVisible = null)
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
		service.purgeMovementsIfNecessary(date, false).collectList().block()

		// Assert
		verify(port).findOlderThanAndUncommentedSince(date)
		verify(port).deleteById(uuid1)
		verify(port).deleteById(uuid2)
	}

	@Test
	fun `Should purgeMovementsIfNecessary call older and uncommented movement since a date, and not call port deleteById because of dryRun`() {
		// Arrange
		val date = LocalDate.EPOCH

		whenever(port.findOlderThanAndUncommentedSince(any())).thenReturn(Flux.just(UUID.randomUUID()))

		// Act
		service.purgeMovementsIfNecessary(date, true).collectList().block()

		// Assert
		verify(port).findOlderThanAndUncommentedSince(date)
		verify(port, never()).deleteById(any())
	}

	@Test
	fun `Should findPlannedMovements build one arrival draft per participant planned on its own date`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val participants = listOf(plannedParticipant(), plannedParticipant())
		stubPlanned(projectId, IN, own = participants)

		// Act
		val drafts = service.findPlannedMovements(projectId, IN, PLANNED_DAY)

		// Assert
		StepVerifier.create(drafts)
			.expectNextMatches { it.isDraft(IN, null, null, participants[0]) }
			.expectNextMatches { it.isDraft(IN, null, null, participants[1]) }
			.verifyComplete()
	}

	@Test
	fun `Should findPlannedMovements build definitive departure drafts for participants planned on their own date`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val participant = plannedParticipant()
		stubPlanned(projectId, OUT, own = listOf(participant))

		// Act
		val drafts = service.findPlannedMovements(projectId, OUT, PLANNED_DAY)

		// Assert
		StepVerifier.create(drafts)
			.expectNextMatches { it.isDraft(OUT, DEFINITIVE_DEPARTURE, null, participant) }
			.verifyComplete()
	}

	@Test
	fun `Should findPlannedMovements use the participant start date for an arrival and its end date for a departure`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val start = CustomDateTimeModel(PLANNED_DAY)
		val end = CustomDateTimeModel(PLANNED_DAY.plusDays(3))
		val participant = plannedParticipant().apply { startAvailability = start; endAvailability = end }
		stubPlanned(projectId, IN, own = listOf(participant))
		stubPlanned(projectId, OUT, own = listOf(participant))

		// Act
		val arrivals = service.findPlannedMovements(projectId, IN, PLANNED_DAY)
		val departures = service.findPlannedMovements(projectId, OUT, PLANNED_DAY)

		// Assert
		StepVerifier.create(arrivals).expectNextMatches { it.plannedAt == start }.verifyComplete()
		StepVerifier.create(departures).expectNextMatches { it.plannedAt == end }.verifyComplete()
	}

	@Test
	fun `Should findPlannedMovements build one arrival draft per group with the group name as pool name`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val group = GroupModel(name = "Alpha").apply { id = UUID.randomUUID() }
		val members = listOf(plannedParticipant(), plannedParticipant())
		stubPlanned(projectId, IN, inherited = members.map { group.id!! to it }, groups = listOf(group))

		// Act
		val drafts = service.findPlannedMovements(projectId, IN, PLANNED_DAY)

		// Assert
		StepVerifier.create(drafts)
			.expectNextMatches { it.type == IN && it.reason == null && it.poolName == "Alpha" && it.participants == members }
			.verifyComplete()
	}

	@Test
	fun `Should findPlannedMovements use the group dates as planned date-time of a group draft`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val start = CustomDateTimeModel(PLANNED_DAY)
		val end = CustomDateTimeModel(PLANNED_DAY.plusDays(2))
		val group = GroupModel(name = "Alpha", startAvailability = start, endAvailability = end).apply { id = UUID.randomUUID() }
		stubPlanned(projectId, IN, inherited = listOf(group.id!! to plannedParticipant()), groups = listOf(group))
		stubPlanned(projectId, OUT, inherited = listOf(group.id!! to plannedParticipant()), groups = listOf(group))

		// Act
		val arrivals = service.findPlannedMovements(projectId, IN, PLANNED_DAY)
		val departures = service.findPlannedMovements(projectId, OUT, PLANNED_DAY)

		// Assert
		StepVerifier.create(arrivals).expectNextMatches { it.plannedAt == start }.verifyComplete()
		StepVerifier.create(departures).expectNextMatches { it.plannedAt == end }.verifyComplete()
	}

	@Test
	fun `Should findPlannedMovements sort the group drafts by group name and gather their members`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val alpha = GroupModel(name = "Alpha").apply { id = UUID.randomUUID() }
		val beta = GroupModel(name = "Beta").apply { id = UUID.randomUUID() }
		val (first, second, third) = listOf(plannedParticipant(), plannedParticipant(), plannedParticipant())
		val inherited = listOf(beta.id!! to third, alpha.id!! to first, alpha.id!! to second)
		stubPlanned(projectId, OUT, inherited = inherited, groups = listOf(beta, alpha))

		// Act
		val drafts = service.findPlannedMovements(projectId, OUT, PLANNED_DAY)

		// Assert
		StepVerifier.create(drafts)
			.expectNextMatches { it.participants == listOf(first, second) && it.reason == DEFINITIVE_DEPARTURE }
			.expectNextMatches { it.participants == listOf(third) && it.poolName == "Beta" }
			.verifyComplete()
	}

	@Test
	fun `Should findPlannedMovements list the individual drafts before the group drafts`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val group = GroupModel(name = "Alpha").apply { id = UUID.randomUUID() }
		val alone = plannedParticipant()
		stubPlanned(
			projectId,
			IN,
			own = listOf(alone),
			inherited = listOf(group.id!! to plannedParticipant()),
			groups = listOf(group)
		)

		// Act
		val drafts = service.findPlannedMovements(projectId, IN, PLANNED_DAY)

		// Assert
		StepVerifier.create(drafts)
			.expectNextMatches { it.participants.single() == alone && it.poolName == null }
			.expectNextMatches { it.poolName == "Alpha" }
			.verifyComplete()
	}

	@Test
	fun `Should findPlannedMovements drop the members of a group that can no longer be resolved`() {
		// Arrange
		val projectId = UUID.randomUUID()
		stubPlanned(projectId, IN, inherited = listOf(UUID.randomUUID() to plannedParticipant()), groups = emptyList())

		// Act
		val drafts = service.findPlannedMovements(projectId, IN, PLANNED_DAY)

		// Assert
		StepVerifier.create(drafts).verifyComplete()
	}

	@Test
	fun `Should findPlannedMovements default to the clock date`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val fixedClock = Clock.fixed(Instant.parse("2026-07-14T23:59:59Z"), ZoneOffset.UTC)
		stubPlanned(projectId, IN)

		// Act
		val drafts = buildService(fixedClock).findPlannedMovements(projectId, IN, null)

		// Assert
		StepVerifier.create(drafts).verifyComplete()
		verify(participantPort).findPlannedOnOwnDate(projectId, IN, PLANNED_DAY, MAX_PLANNED)
		verify(participantPort).findPlannedOnGroupDate(projectId, IN, PLANNED_DAY, MAX_PLANNED)
	}

	private fun plannedParticipant() = ParticipantModel().apply { id = UUID.randomUUID() }

	private fun PlannedMovementModel.isDraft(
		expectedType: MovementTypeEnum,
		expectedReason: MovementReasonEnum?,
		expectedPoolName: String?,
		expectedParticipant: ParticipantModel,
	): Boolean {
		return type == expectedType && reason == expectedReason && contentType == REGISTERED &&
				poolName == expectedPoolName && participants.single() == expectedParticipant
	}

	private fun stubPlanned(
		projectId: UUID,
		type: MovementTypeEnum,
		own: List<ParticipantModel> = emptyList(),
		inherited: List<Pair<UUID, ParticipantModel>> = emptyList(),
		groups: List<GroupModel> = emptyList(),
	) {
		whenever(
			participantPort.findPlannedOnOwnDate(
				projectId,
				type,
				PLANNED_DAY,
				MAX_PLANNED
			)
		).thenReturn(Flux.fromIterable(own))
		whenever(
			participantPort.findPlannedOnGroupDate(
				projectId,
				type,
				PLANNED_DAY,
				MAX_PLANNED
			)
		).thenReturn(Flux.fromIterable(inherited))
		whenever(groupPort.findAllByIds(eq(projectId), any(), eq(true))).thenReturn(Flux.fromIterable(groups))
	}
}
