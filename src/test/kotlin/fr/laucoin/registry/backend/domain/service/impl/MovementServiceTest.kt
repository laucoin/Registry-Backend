package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_DELETED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_DISABLED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_ENABLED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DRIVERS_NOT_MAJOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_VISIBLE
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
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
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
import fr.laucoin.registry.backend.test.ModelExt.activityId
import fr.laucoin.registry.backend.test.ModelExt.commonActivity
import fr.laucoin.registry.backend.test.ModelExt.commonGroup
import fr.laucoin.registry.backend.test.ModelExt.commonMovement
import fr.laucoin.registry.backend.test.ModelExt.commonParticipant
import fr.laucoin.registry.backend.test.ModelExt.commonVehicle
import fr.laucoin.registry.backend.test.ModelExt.groupId
import fr.laucoin.registry.backend.test.ModelExt.movementId
import fr.laucoin.registry.backend.test.ModelExt.participantId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.ModelExt.vehicleId
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
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
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

	private val service = MovementService(
		projectService,
		port,
		participantPort,
		vehiclePort,
		activityPort,
		communicationPort,
		groupPort,
		transactionalOperator,
		MAX_PARTICIPANTS,
		MAX_GROUPS,
		MAX_VEHICLES,
		MAX_ACTIVITIES,
	)

	private companion object {
		private const val MAX_PARTICIPANTS = 1
		private const val MAX_GROUPS = 2
		private const val MAX_VEHICLES = 3
		private const val MAX_ACTIVITIES = 4

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
				Arguments.of(REGISTERED, 1, listOf(commonActivity())),
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
				Flux.just(commonParticipant().apply { isVisible = false }),
				NOT_FOUND,
				MOVEMENT_PARTICIPANTS_NOT_VISIBLE,
			),
			Arguments.of(
				Flux.just(commonParticipant().apply { isPurged = true }),
				NOT_FOUND,
				MOVEMENT_PARTICIPANTS_NOT_VISIBLE,
			),
			Arguments.of(
				Flux.just(commonParticipant().apply { isPurged = true; isVisible = false }),
				NOT_FOUND,
				MOVEMENT_PARTICIPANTS_NOT_VISIBLE,
			),
			Arguments.of(
				Flux.just(commonParticipant().apply { birthday = LocalDate.now() }),
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
				Mono.just(commonActivity().apply { isVisible = false }),
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
				Flux.just(commonVehicle().apply { isVisible = false }),
				NOT_FOUND,
				MOVEMENT_VEHICLES_NOT_VISIBLE,
			),
		)

		@JvmStatic
		fun `Should disableMovementById throw on lastParticipantMovement`(): Stream<Arguments> = Stream.of(
			Arguments.of(commonMovement().apply { reason = DEFINITIVE_DEPARTURE }),
			Arguments.of(commonMovement().apply { type = OUT; contentType = GUEST }),
		)

		@JvmStatic
		fun `Should enableMovementById throw on lastParticipantMovement`(): Stream<Arguments> = Stream.of(
			Arguments.of(commonMovement().apply { reason = DEFINITIVE_DEPARTURE }),
			Arguments.of(commonMovement().apply { type = OUT; contentType = GUEST }),
		)

		@JvmStatic
		fun `Should deleteMovementById throw on lastParticipantMovement`(): Stream<Arguments> = Stream.of(
			Arguments.of(commonMovement().apply { reason = DEFINITIVE_DEPARTURE }),
			Arguments.of(commonMovement().apply { type = OUT; contentType = GUEST }),
		)
	}

	@BeforeEach
	fun setup() {
		whenever(projectService.validateDateTime(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(projectId))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }
	}

	@Test
	fun `Should findMovementsPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel()

		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findMovementsPage(projectId, pageable, params).block()

		// Assert
		verify(port).findPage(projectId, pageable, params)
	}

	@Test
	fun `Should findCurrentMovementsPage call port findCurrentPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel()

		whenever(port.findCurrentPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findCurrentMovementsPage(projectId, pageable, params).block()

		// Assert
		verify(port).findCurrentPage(projectId, pageable, params)
	}

	@Test
	fun `Should findMovementsContent call port findContent`() {
		// Arrange
		val ids = listOf(movementId)

		whenever(port.findContent(any(), any()))
			.thenReturn(Flux.just(Pair(movementId, emptyList())))

		// Act
		service.findMovementsContent(projectId, ids).collectList().block()

		// Assert
		verify(port).findContent(projectId, ids)
	}

	@Test
	fun `Should findCurrentMovementsContent call port findCurrentContent`() {
		// Arrange
		val ids = listOf(movementId)

		whenever(port.findCurrentContent(any(), any()))
			.thenReturn(Flux.just(Pair(movementId, emptyList())))

		// Act
		service.findCurrentMovementsContent(projectId, ids).collectList().block()

		// Assert
		verify(port).findCurrentContent(projectId, ids)
	}

	@Test
	fun `Should findMovementById call port findById`() {
		// Arrange
		val onlyVisible = true
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonMovement()))

		// Act
		service.findMovementById(projectId, movementId, onlyVisible).block()

		// Assert
		verify(port).findById(projectId, movementId, onlyVisible)
	}

	@Test
	fun `Should findMovementById throw on empty result`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findMovementById(projectId, movementId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(movementId.toString(), result.args?.first())

		verify(port).findById(projectId, movementId, onlyVisible)
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

		whenever(participantPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(commonParticipant()))
		whenever(groupPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(commonGroup()))
		whenever(groupPort.findContent(any(), any(), anyOrNull(), anyOrNull()))
			.thenReturn(Flux.just(Pair(groupId, listOf(commonParticipant()))))

		// Act
		val result = service.searchParticipantsAndGroupsByText(projectId, type, query).block()

		// Assert
		assertEquals(1, result?.t1?.size)
		assertEquals(commonParticipant(), result?.t1?.first())
		assertEquals(1, result?.t2?.size)
		assertEquals(commonGroup().apply { members = listOf(commonParticipant()) }, result?.t2?.first())

		verify(participantPort).findWithLimit(MAX_PARTICIPANTS, projectId, expectedParticipantSearch)
		verify(groupPort).findWithLimit(MAX_GROUPS, projectId, expectedGroupSearch)
		verify(groupPort).findContent(
			projectId, listOf(groupId), isVisible = true, isAvailable = true
		)
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

		whenever(vehiclePort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(commonVehicle()))

		// Act
		service.searchVehiclesByText(projectId, query).collectList().block()

		// Assert
		verify(vehiclePort).findWithLimit(MAX_VEHICLES, projectId, expectedVehicleSearch)
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

		whenever(activityPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(commonActivity()))

		// Act
		val result = service.searchActivitiesByText(projectId, type, query).collectList().block()

		// Assert
		assertEquals(expectedActivities, result)

		verify(activityPort, times(expectedPortCall)).findWithLimit(MAX_ACTIVITIES, projectId, expectedActivitySearch)
	}

	@Test
	fun `Should findMovementCommunicationsPage call communication port findPageByMovementId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = CommunicationSearchParamModel()

		whenever(communicationPort.findPageByMovementId(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findMovementCommunicationsPage(projectId, movementId, pageable, params).block()

		// Assert
		verify(communicationPort).findPageByMovementId(projectId, movementId, pageable, params)
	}

	@Test
	fun `Should findParticipantsStatus call participant port 5 times`() {
		// Arrange
		val registeredPresentAdult = 1L
		val registeredPresentAdultSearch = ParticipantSearchParamModel(
			query = null,
			isMajor = true,
			type = REGISTERED,
			status = PresenceStatusEnum.IN,
			isVisible = true,
			dateTime = null
		)
		val registeredAbsentAdult = 2L
		val registeredAbsentAdultSearch = ParticipantSearchParamModel(
			query = null,
			isMajor = true,
			type = REGISTERED,
			status = PresenceStatusEnum.OUT,
			isVisible = true,
			dateTime = null
		)
		val registeredPresentMinor = 3L
		val registeredPresentChildSearch = ParticipantSearchParamModel(
			query = null,
			isMajor = false,
			type = REGISTERED,
			status = PresenceStatusEnum.IN,
			isVisible = true,
			dateTime = null
		)
		val registeredAbsentMinor = 4L
		val registeredAbsentChildSearch = ParticipantSearchParamModel(
			query = null,
			isMajor = false,
			type = REGISTERED,
			status = PresenceStatusEnum.OUT,
			isVisible = true,
			dateTime = null
		)
		val guestPresent = 5L
		val guestPresentSearch = ParticipantSearchParamModel(
			query = null,
			isMajor = null,
			type = GUEST,
			status = PresenceStatusEnum.IN,
			isVisible = true,
			dateTime = null
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
		val result = service.findParticipantsStatus(projectId).block()

		// Assert
		assertEquals(registeredPresentAdult, result?.registered?.presentMajors)
		assertEquals(registeredAbsentAdult, result?.registered?.absentMajors)
		assertEquals(registeredPresentMinor, result?.registered?.presentMinors)
		assertEquals(registeredAbsentMinor, result?.registered?.absentMinors)
		assertEquals(guestPresent, result?.guests)

		verify(participantPort).countAll(projectId, registeredPresentAdultSearch)
		verify(participantPort).countAll(projectId, registeredAbsentAdultSearch)
		verify(participantPort).countAll(projectId, registeredPresentChildSearch)
		verify(participantPort).countAll(projectId, registeredAbsentChildSearch)
		verify(participantPort).countAll(projectId, guestPresentSearch)
	}

	@Test
	fun `Should findVehiclesStatus call vehicle port 2 times`() {
		// Arrange
		val presentVehicle = 1L
		val presentVehicleSearch = VehicleSearchParamModel(
			query = null,
			isVisible = true,
			status = PresenceStatusEnum.IN,
			dateTime = null
		)
		val absentVehicle = 2L
		val absentVehicleSearch = VehicleSearchParamModel(
			query = null,
			isVisible = true,
			status = PresenceStatusEnum.OUT,
			dateTime = null
		)

		whenever(vehiclePort.countAll(any(), eq(presentVehicleSearch))).thenReturn(Mono.just(presentVehicle))
		whenever(vehiclePort.countAll(any(), eq(absentVehicleSearch))).thenReturn(Mono.just(absentVehicle))

		// Act
		val result = service.findVehiclesStatus(projectId).block()

		// Assert
		assertEquals(presentVehicle, result?.present)
		assertEquals(absentVehicle, result?.absent)

		verify(vehiclePort).countAll(projectId, presentVehicleSearch)
		verify(vehiclePort).countAll(projectId, absentVehicleSearch)
	}

	@Test
	fun `Should createMovement with registered participant`() {
		// Arrange
		val participantContent = MovementContentModel().apply { participant = commonParticipant() }
		val movement = commonMovement().apply { contentType = REGISTERED; content = listOf(participantContent) }

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(commonParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(commonMovement()))

		// Act
		service.createMovement(currentUser(), movement).block()

		// Assert
		verify(participantPort).findAllByIds(projectId, listOf(participantId), isVisible = null)
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
			MovementContentModel().apply { participant = commonParticipant(); vehicle = commonVehicle() }
		val movement = commonMovement().apply { contentType = REGISTERED; content = listOf(participantContent) }

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(returnedParticipant)
		whenever(vehiclePort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(commonVehicle()))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(currentUser(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(expectedStatus, result.status)
		assertEquals(expectedCode, result.code)

		verify(participantPort).findAllByIds(projectId, listOf(participantId), isVisible = null)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should createMovement with guest participant`() {
		// Arrange
		val guest = commonParticipant().apply { id = null; }
		val movement = commonMovement().apply { type = IN; contentType = GUEST }

		whenever(participantPort.findAllByIds(any(), eq(emptyList()), anyOrNull())).thenReturn(Flux.empty())
		whenever(participantPort.findAllByIds(any(), eq(listOf(participantId)), anyOrNull()))
			.thenReturn(Flux.just(commonParticipant()))
		whenever(participantPort.saveAllGuest(any())).thenReturn(Flux.just(commonParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(commonMovement()))

		// Act
		service.createMovement(currentUser(), movement, listOf(guest)).block()

		// Assert
		verify(participantPort).findAllByIds(projectId, emptyList(), isVisible = null)
		verify(participantPort).findAllByIds(projectId, listOf(participantId), isVisible = null)
		verify(participantPort).saveAllGuest(any())
		verify(port).create(any())
	}

	@Test
	fun `Should createMovement linked to an activity`() {
		// Arrange
		val participantContent = MovementContentModel().apply { participant = commonParticipant() }
		val movement = commonMovement().apply {
			contentType = REGISTERED; content = listOf(participantContent); activity = commonActivity()
		}

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(commonParticipant()))
		whenever(activityPort.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonActivity()))
		whenever(port.create(any())).thenReturn(Mono.just(commonMovement()))

		// Act
		service.createMovement(currentUser(), movement).block()

		// Assert
		verify(participantPort).findAllByIds(projectId, listOf(participantId), isVisible = null)
		verify(activityPort).findById(projectId, activityId, isVisible = null)
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
		val participantContent = MovementContentModel().apply { participant = commonParticipant() }
		val movement = commonMovement().apply {
			contentType = REGISTERED; content = listOf(participantContent); activity = commonActivity()
		}

		whenever(activityPort.findById(any(), any(), anyOrNull())).thenReturn(returnedActivity)

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(currentUser(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(expectedStatus, result.status)
		assertEquals(expectedCode, result.code)

		verify(activityPort).findById(projectId, activityId, isVisible = null)
		verifyNoInteractions(participantPort)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should createMovement linked to a vehicle`() {
		// Arrange
		val driver = commonParticipant().apply { birthday = LocalDate.EPOCH }
		val participantContent = MovementContentModel().apply { participant = driver; vehicle = commonVehicle() }
		val movement = commonMovement().apply { contentType = REGISTERED; content = listOf(participantContent) }

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(driver))
		whenever(vehiclePort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(commonVehicle()))
		whenever(port.create(any())).thenReturn(Mono.just(commonMovement()))

		// Act
		service.createMovement(currentUser(), movement).block()

		// Assert
		verify(participantPort).findAllByIds(projectId, listOf(participantId), isVisible = null)
		verify(vehiclePort).findAllByIds(projectId, listOf(vehicleId), isVisible = null)
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
		val driver = commonParticipant().apply { birthday = LocalDate.EPOCH }
		val participantContent = MovementContentModel().apply { participant = driver; vehicle = commonVehicle() }
		val movement = commonMovement().apply { contentType = REGISTERED; content = listOf(participantContent) }

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(driver))
		whenever(vehiclePort.findAllByIds(any(), any(), anyOrNull())).thenReturn(returnedVehicle)

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(currentUser(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(expectedStatus, result.status)
		assertEquals(expectedCode, result.code)

		verify(participantPort).findAllByIds(projectId, listOf(participantId), isVisible = null)
		verify(vehiclePort).findAllByIds(projectId, listOf(vehicleId), isVisible = null)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should createMovement with last participant movement`() {
		// Arrange
		val participantContent = MovementContentModel().apply { participant = commonParticipant() }
		val movementDate = ZonedDateTime.now()
		val movement = commonMovement().apply {
			dateTime = movementDate
			contentType = REGISTERED
			reason = DEFINITIVE_DEPARTURE
			content = listOf(participantContent)
		}

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(commonParticipant()))
		whenever(participantPort.updateAllEndAvailability(any(), any())).thenReturn(Flux.just(commonParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(commonMovement()))

		// Act
		service.createMovement(currentUser(), movement).block()

		// Assert
		verify(participantPort).findAllByIds(projectId, listOf(participantId), isVisible = null)
		verify(participantPort).updateAllEndAvailability(listOf(participantId), CustomDateTimeModel(movementDate))
		verify(port).create(any())
	}

	@Test
	fun `Should createMovement with guest out`() {
		// Arrange
		val participantContent = MovementContentModel().apply { participant = commonParticipant() }
		val movementDate = ZonedDateTime.now()
		val movement = commonMovement().apply {
			dateTime = movementDate
			contentType = GUEST
			type = OUT
			content = listOf(participantContent)
		}

		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(commonParticipant()))
		whenever(participantPort.updateAllEndAvailability(any(), any())).thenReturn(Flux.just(commonParticipant()))
		whenever(port.create(any())).thenReturn(Mono.just(commonMovement()))

		// Act
		service.createMovement(currentUser(), movement).block()

		// Assert
		verify(participantPort).findAllByIds(projectId, listOf(participantId), isVisible = null)
		verify(participantPort).updateAllEndAvailability(listOf(participantId), CustomDateTimeModel(movementDate))
		verify(port).create(any())
	}

	@Test
	fun `Should disableMovementById hide and return a Movement`() {
		// Arrange
		val movement = commonMovement().apply { contentType = REGISTERED }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.update(any())).thenReturn(Mono.just(movement))

		// Act
		service.disableMovementById(currentUser(), projectId, movementId).block()

		// Assert
		verify(port).findById(projectId, movementId, isVisible = true)
		verify(port).update(movement.apply { isVisible = false })
	}

	@ParameterizedTest
	@MethodSource
	fun `Should disableMovementById throw on lastParticipantMovement`(movement: MovementModel) {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.disableMovementById(currentUser(), projectId, movementId).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(MOVEMENT_CANNOT_BE_DISABLED, result.code)

		verify(port).findById(projectId, movementId, isVisible = true)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should enableMovementById restore and return a Movement`() {
		// Arrange
		val movement = commonMovement().apply { contentType = REGISTERED; isVisible = false }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.update(any())).thenReturn(Mono.just(movement))

		// Act
		service.enableMovementById(currentUser(), projectId, movementId).block()

		// Assert
		verify(port).findById(projectId, movementId, isVisible = false)
		verify(port).update(movement.apply { isVisible = true })
	}

	@ParameterizedTest
	@MethodSource
	fun `Should enableMovementById throw on lastParticipantMovement`(movement: MovementModel) {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.enableMovementById(currentUser(), projectId, movementId).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(MOVEMENT_CANNOT_BE_ENABLED, result.code)

		verify(port).findById(projectId, movementId, isVisible = false)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should deleteMovementById delete a Movement`() {
		// Arrange
		val movement = commonMovement().apply { contentType = REGISTERED }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteMovementById(projectId, movementId).block()

		// Assert
		verify(port).findById(projectId, movementId, isVisible = null)
		verify(port).deleteById(movementId)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should deleteMovementById throw on lastParticipantMovement`(movement: MovementModel) {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.deleteMovementById(projectId, movementId).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(MOVEMENT_CANNOT_BE_DELETED, result.code)

		verify(port).findById(projectId, movementId, isVisible = null)
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
}
