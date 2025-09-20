package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_VISIBLE
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
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.test.ModelExt.movementId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class MovementServiceTest {
	private val port: IMovementPort = mock()
	private val projectService: IProjectService = mock()
	private val participantPort: IParticipantPort = mock()
	private val vehiclePort: IVehiclePort = mock()
	private val activityPort: IActivityPort = mock()
	private val communicationPort: ICommunicationPort = mock()
	private val groupPort: IGroupPort = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val maxParticipants: Int = 1
	private val maxGroups: Int = 1
	private val maxVehicles: Int = 1
	private val maxActivities: Int = 1
	private val service: IMovementService = MovementService(
		projectService,
		port,
		participantPort,
		vehiclePort,
		activityPort,
		communicationPort,
		groupPort,
		transactionalOperator,
		maxParticipants,
		maxGroups,
		maxVehicles,
		maxActivities
	)

	companion object {
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
		fun `Should createMovement check date, validate activity, validate participants, validate vehicle and call port create`(): Stream<Arguments> {
			val activityId = UUID.randomUUID()
			val movementActivity = ActivityModel().apply { id = activityId; visible = true }
			val participantId1 = UUID.randomUUID()
			val movementParticipant1 = ParticipantModel().apply {
				id = participantId1; birthday = LocalDate.now().minusYears(18); visible = true; purged = false
			}
			val participantId2 = UUID.randomUUID()
			val movementParticipant2 = ParticipantModel().apply { id = participantId2; visible = true; purged = false }
			val vehicleId = UUID.randomUUID()
			val movementVehicle = VehicleModel().apply { id = vehicleId; visible = true }

			return Stream.of(
				Arguments.of(
					listOf(
						MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					IN,
					REGISTERED,
					null,
					movementActivity,
					1,
					1,
					0,
				),
				Arguments.of(
					listOf(
						MovementContentModel().apply { participant = movementParticipant1 },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					IN,
					REGISTERED,
					null,
					null,
					0,
					0,
					0,
				),
				Arguments.of(
					listOf(
						MovementContentModel().apply { participant = movementParticipant1 },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					OUT,
					REGISTERED,
					DEFINITIVE_DEPARTURE,
					null,
					0,
					0,
					1,
				),
				Arguments.of(
					listOf(
						MovementContentModel().apply { participant = movementParticipant1 },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					OUT,
					GUEST,
					null,
					null,
					0,
					0,
					1,
				),
			)
		}

		@JvmStatic
		fun `Should createMovement check date, validate activity, check participants and throw because of participant is not visible`(): Stream<Arguments> {
			val participantId1 = UUID.randomUUID()
			val movementParticipant1 = ParticipantModel().apply { id = participantId1 }
			val participantId2 = UUID.randomUUID()
			val movementParticipant2 = ParticipantModel().apply { id = participantId2 }

			return Stream.of(
				Arguments.of(
					listOf(
						MovementContentModel().apply { participant = movementParticipant1 },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					listOf(
						ParticipantModel().apply { id = UUID.randomUUID(); visible = true; purged = false },
						ParticipantModel().apply { id = UUID.randomUUID(); visible = false; purged = false },
					),
				),
				Arguments.of(
					listOf(
						MovementContentModel().apply { participant = movementParticipant1 },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					listOf(
						ParticipantModel().apply { id = UUID.randomUUID(); visible = true; purged = false },
						ParticipantModel().apply { id = UUID.randomUUID(); visible = true; purged = true },
					),
				)
			)
		}

		@JvmStatic
		fun `Should updateMovementById check date, get existing movement, validate activity, validate participants, validate vehicle and call port update`(): Stream<Arguments> {
			val activityId = UUID.randomUUID()
			val movementActivity = ActivityModel().apply { id = activityId; visible = true }
			val participantId1 = UUID.randomUUID()
			val movementParticipant1 = ParticipantModel().apply {
				id = participantId1; birthday = LocalDate.now().minusYears(18); visible = true; purged = false
			}
			val participantId2 = UUID.randomUUID()
			val movementParticipant2 = ParticipantModel().apply { id = participantId2; visible = true; purged = false }
			val vehicleId = UUID.randomUUID()
			val movementVehicle = VehicleModel().apply { id = vehicleId; visible = true }

			return Stream.of(
				Arguments.of(
					listOf(
						MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					listOf(
						MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					emptyList<ParticipantModel>(),
					emptyList<VehicleModel>(),
					movementActivity,
					movementActivity,
					0,
					0,
					0,
				),
				Arguments.of(
					listOf(
						MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					listOf(
						MovementContentModel().apply { participant = movementParticipant1 },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					listOf(movementParticipant1),
					emptyList<VehicleModel>(),
					movementActivity,
					movementActivity,
					0,
					1,
					0,
				),
				Arguments.of(
					listOf(
						MovementContentModel().apply { participant = movementParticipant1 },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					listOf(
						MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					listOf(movementParticipant1),
					listOf(movementVehicle),
					movementActivity,
					movementActivity,
					0,
					1,
					1,
				),
				Arguments.of(
					listOf(
						MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					listOf(
						MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					emptyList<ParticipantModel>(),
					emptyList<VehicleModel>(),
					null,
					movementActivity,
					1,
					0,
					0,
				),
				Arguments.of(
					listOf(
						MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					listOf(
						MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
						MovementContentModel().apply { participant = movementParticipant2 },
					),
					emptyList<ParticipantModel>(),
					emptyList<VehicleModel>(),
					movementActivity,
					null,
					0,
					0,
					0,
				),
			)
		}
	}

	@Test
	fun `Should findMovementsPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(typeSearched = IN)
		whenever(port.findPage(any(), any(), any())).thenReturn(
			Mono.just(PageModel(1, 2, 3, 4, emptyList()))
		)

		// Act
		service.findMovementsPage(projectId, pageable, params).block()

		// Assert
		verify(port).findPage(projectId, pageable, params)
	}

	@Test
	fun `Should findMovementById call port findById`() {
		// Arrange
		val movement =
			MovementModel(contentType = REGISTERED).apply { project = ProjectModel().apply { id = projectId } }
		val uuid = UUID.randomUUID()
		val onlyVisible = true
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))

		// Act
		service.findMovementById(projectId, uuid, onlyVisible).block()

		// Assert
		verify(port).findById(projectId, uuid, onlyVisible)
	}

	@Test
	fun `Should findCurrentMovementsPage call port findCurrentPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(typeSearched = IN)
		whenever(port.findCurrentPage(any(), any(), any())).thenReturn(
			Mono.just(PageModel(1, 2, 3, 4, emptyList()))
		)

		// Act
		service.findCurrentMovementsPage(projectId, pageable, params).block()

		// Assert
		verify(port).findCurrentPage(projectId, pageable, params)
	}

	@Test
	fun `Should findMovementsContent call port findContent`() {
		// Arrange
		val ids = listOf(UUID.randomUUID(), UUID.randomUUID())
		whenever(port.findContent(any(), any())).thenReturn(Flux.empty())

		// Act
		service.findMovementsContent(projectId, ids).blockFirst()

		// Assert
		verify(port).findContent(projectId, ids)
	}

	@Test
	fun `Should findCurrentMovementsContent call port findCurrentContent`() {
		// Arrange
		val ids = listOf(UUID.randomUUID(), UUID.randomUUID())
		whenever(port.findCurrentContent(any(), any())).thenReturn(Flux.empty())

		// Act
		service.findCurrentMovementsContent(projectId, ids).blockFirst()

		// Assert
		verify(port).findCurrentContent(projectId, ids)
	}

	@Test
	fun `Should searchParticipantsAndGroups call port findWithLimit and findWithLimit`() {
		// Arrange
		val textSearched = "test"
		val typeSearched = REGISTERED
		val uuid = UUID.randomUUID()
		val group = GroupModel().apply { id = uuid }
		val member = ParticipantModel()
		whenever(participantPort.findWithLimit(any(), any(), any())).thenReturn(Flux.empty())
		whenever(groupPort.findWithLimit(any(), any(), any())).thenReturn(Flux.just(group))
		whenever(groupPort.findContent(any(), any(), anyOrNull(), anyOrNull())).thenReturn(
			Flux.just(
				Pair(
					uuid,
					listOf(member)
				)
			)
		)

		// Act
		val result = service.searchParticipantsAndGroupsByText(projectId, typeSearched, textSearched).block()

		// Assert
		assertEquals(0, result?.t1?.size)
		assertEquals(1, result?.t2?.size)
		assertEquals(1, result?.t2?.first()?.members?.size)
		verify(participantPort).findWithLimit(
			maxParticipants,
			projectId,
			ParticipantSearchParamModel(
				isMajor = null,
				typeSearched,
				visibilitySearched = true,
				availabilitySearched = true
			).apply {
				this.textSearched = textSearched
			}
		)
		verify(groupPort).findWithLimit(
			maxGroups,
			projectId,
			GroupSearchParamModel(textSearched, visibilitySearched = true, presenceSearched = true)
		)
		verify(groupPort).findContent(
			projectId,
			listOf(uuid),
			visibilitySearched = true,
			availabilitySearched = true
		)
	}

	@Test
	fun `Should searchVehiclesByText call port findWithLimit`() {
		// Arrange
		val textSearched = "test"
		whenever(vehiclePort.findWithLimit(any(), any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchVehiclesByText(projectId, textSearched).blockFirst()

		// Assert
		verify(vehiclePort).findWithLimit(
			maxVehicles,
			projectId,
			VehicleSearchParamModel(visibilitySearched = true, availabilitySearched = true).apply {
				this.textSearched = textSearched
			},
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should searchReasonsByText return filtered reason depending params`(
		typeSearched: ParticipantTypeEnum,
		movementTypeSearched: MovementTypeEnum,
		expectedReasons: List<MovementReasonEnum>,
	) {
		// Act
		val result = service.searchReasonsByText(typeSearched, movementTypeSearched).collectList().block()

		// Assert
		assertEquals(expectedReasons, result)
	}

	@Test
	fun `Should searchActivitiesByText call port findWithLimit`() {
		// Arrange
		val textSearched = "test"
		val typeSearched = REGISTERED
		whenever(activityPort.findWithLimit(any(), any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchActivitiesByText(projectId, typeSearched, textSearched).blockFirst()

		// Assert
		verify(activityPort).findWithLimit(
			maxActivities,
			projectId,
			ActivitySearchParamModel(textSearched, visibilitySearched = true, availabilitySearched = true),
		)
	}

	@Test
	fun `Should searchActivitiesByText not call port findWithLimit for GUEST`() {
		// Arrange
		val textSearched = "test"
		val typeSearched = GUEST

		// Act
		service.searchActivitiesByText(projectId, typeSearched, textSearched).blockFirst()

		// Assert
		verifyNoInteractions(activityPort)
	}

	@Test
	fun `Should findMovementCommunicationsPage call port findPageByMovementId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = CommunicationSearchParamModel()
		whenever(communicationPort.findPageByMovementId(any(), any(), any(), any())).thenReturn(
			Mono.just(PageModel(1, 2, 3, 4, emptyList()))
		)

		// Act
		service.findMovementCommunicationsPage(projectId, movementId, pageable, params).block()

		// Assert
		verify(communicationPort).findPageByMovementId(projectId, movementId, pageable, params)
	}

	@Test
	fun `Should findParticipantsStatus call multiple port count`() {
		// Arrange
		val registeredPresentMajor = ParticipantSearchParamModel(
			textSearched = null,
			isMajor = true,
			typeSearched = REGISTERED,
			statusSearched = PresenceStatusEnum.IN,
			visibilitySearched = true,
			dateTimeSearched = null
		)
		val registeredAbsentMajor = ParticipantSearchParamModel(
			textSearched = null,
			isMajor = true,
			typeSearched = REGISTERED,
			statusSearched = PresenceStatusEnum.OUT,
			visibilitySearched = true,
			dateTimeSearched = null
		)
		val registeredPresentMinor = ParticipantSearchParamModel(
			textSearched = null,
			isMajor = false,
			typeSearched = REGISTERED,
			statusSearched = PresenceStatusEnum.IN,
			visibilitySearched = true,
			dateTimeSearched = null
		)
		val registeredAbsentMinor = ParticipantSearchParamModel(
			textSearched = null,
			isMajor = false,
			typeSearched = REGISTERED,
			statusSearched = PresenceStatusEnum.OUT,
			visibilitySearched = true,
			dateTimeSearched = null
		)
		val presentGuest = ParticipantSearchParamModel(
			textSearched = null,
			isMajor = null,
			typeSearched = GUEST,
			statusSearched = PresenceStatusEnum.IN,
			visibilitySearched = true,
			dateTimeSearched = null
		)
		whenever(participantPort.countAll(any(), eq(registeredPresentMajor))).thenReturn(Mono.just(1L))
		whenever(participantPort.countAll(any(), eq(registeredAbsentMajor))).thenReturn(Mono.just(2L))
		whenever(participantPort.countAll(any(), eq(registeredPresentMinor))).thenReturn(Mono.just(3L))
		whenever(participantPort.countAll(any(), eq(registeredAbsentMinor))).thenReturn(Mono.just(4L))
		whenever(participantPort.countAll(any(), eq(presentGuest))).thenReturn(Mono.just(5L))

		// Act
		val result = service.findParticipantsStatus(projectId).block()

		// Assert
		assertEquals(1, result!!.registered.presentMajors)
		assertEquals(2, result.registered.absentMajors)
		assertEquals(3, result.registered.presentMinors)
		assertEquals(4, result.registered.absentMinors)
		assertEquals(5, result.guests)
		verify(participantPort).countAll(projectId, registeredPresentMajor)
		verify(participantPort).countAll(projectId, registeredAbsentMajor)
		verify(participantPort).countAll(projectId, registeredPresentMinor)
		verify(participantPort).countAll(projectId, registeredAbsentMinor)
		verify(participantPort).countAll(projectId, presentGuest)
	}

	@Test
	fun `Should findVehiclesStatus call multiple port count`() {
		// Arrange
		val present = VehicleSearchParamModel(
			textSearched = null,
			visibilitySearched = true,
			statusSearched = PresenceStatusEnum.IN,
			dateTimeSearched = null
		)
		val absent = VehicleSearchParamModel(
			textSearched = null,
			visibilitySearched = true,
			statusSearched = PresenceStatusEnum.OUT,
			dateTimeSearched = null
		)
		whenever(vehiclePort.countAll(any(), eq(present))).thenReturn(Mono.just(1L))
		whenever(vehiclePort.countAll(any(), eq(absent))).thenReturn(Mono.just(2L))

		// Act
		val result = service.findVehiclesStatus(projectId).block()

		// Assert
		assertEquals(1, result!!.present)
		assertEquals(2, result.absent)
		verify(vehiclePort).countAll(projectId, present)
		verify(vehiclePort).countAll(projectId, absent)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createMovement check date, validate activity, validate participants, validate vehicle and call port create`(
		movementContent: List<MovementContentModel>,
		movementType: MovementTypeEnum,
		movementContentType: ParticipantTypeEnum,
		movementReason: MovementReasonEnum?,
		movementActivity: ActivityModel?,
		expectedCallToActivity: Int,
		expectedCallToVehicle: Int,
		expectedCallToUpdateParticipantEndDate: Int,
	) {
		// Arrange
		val movementDateTime = ZonedDateTime.now()

		val movement = MovementModel(contentType = movementContentType).apply {
			id = UUID.randomUUID()
			project = ProjectModel().apply { id = projectId }
			type = movementType
			reason = movementReason
			dateTime = movementDateTime
			activity = movementActivity
			content = movementContent
		}

		whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
		whenever(activityPort.findById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(movementActivity ?: ActivityModel())
		)
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(
			Flux.just(*movementContent.map { it.participant }.toTypedArray())
		)
		whenever(participantPort.updateAllEndAvailability(any(), any())).thenReturn(
			Flux.just(*movementContent.map { it.participant }.toTypedArray())
		)
		whenever(vehiclePort.findAllByIds(any(), any(), anyOrNull())).thenReturn(
			Flux.just(*movementContent.mapNotNull { it.vehicle }.toTypedArray())
		)
		whenever(port.create(any())).thenReturn(Mono.just(movement))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		service.createMovement(currentUser(), movement).block()

		// Assert
		verify(projectService).validateDateTime(
			projectId,
			CustomDateTimeModel(movementDateTime),
			MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(activityPort, times(expectedCallToActivity)).findById(
			projectId,
			movementActivity?.id ?: UUID.randomUUID(),
			visibilitySearched = null
		)
		verify(participantPort).findAllByIds(
			projectId,
			movementContent.mapNotNull { it.participant?.id },
			visibilitySearched = null,
		)
		verify(participantPort, times(expectedCallToUpdateParticipantEndDate)).updateAllEndAvailability(
			movementContent.mapNotNull { it.participant?.id },
			CustomDateTimeModel(movement.dateTime),
		)
		verify(vehiclePort, times(expectedCallToVehicle)).findAllByIds(
			projectId,
			movementContent.mapNotNull { it.vehicle?.id },
			visibilitySearched = null
		)
		verify(port).create(movement)
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should createMovement check date, validate activity and throw because of not found activity`() {
		// Arrange
		val movementDateTime = ZonedDateTime.now()

		val activityId = UUID.randomUUID()
		val movementActivity = ActivityModel().apply { id = activityId }

		val movement = MovementModel(contentType = REGISTERED).apply {
			id = UUID.randomUUID()
			project = ProjectModel().apply { id = projectId }
			dateTime = movementDateTime
			activity = movementActivity
		}

		whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
		whenever(activityPort.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(currentUser(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT, result.message)
		verify(projectService).validateDateTime(
			projectId,
			CustomDateTimeModel(movementDateTime),
			MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(activityPort).findById(
			projectId,
			movementActivity.id!!,
			visibilitySearched = null
		)
		verify(participantPort, never()).findAllByIds(any(), any(), anyOrNull())
		verify(vehiclePort, never()).findAllByIds(any(), any(), anyOrNull())
		verify(port, never()).create(any())
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should createMovement check date, validate activity and throw because of activity is not visible`() {
		// Arrange
		val movementDateTime = ZonedDateTime.now()

		val activityId = UUID.randomUUID()
		val movementActivity = ActivityModel().apply { id = activityId; visible = false }

		val movement = MovementModel(contentType = REGISTERED).apply {
			id = UUID.randomUUID()
			project = ProjectModel().apply { id = projectId }
			dateTime = movementDateTime
			activity = movementActivity
		}

		whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
		whenever(activityPort.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movementActivity))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(currentUser(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(MOVEMENT_ACTIVITY_NOT_VISIBLE, result.message)
		verify(projectService).validateDateTime(
			projectId,
			CustomDateTimeModel(movementDateTime),
			MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(activityPort).findById(
			projectId,
			movementActivity.id!!,
			visibilitySearched = null
		)
		verify(participantPort, never()).findAllByIds(any(), any(), anyOrNull())
		verify(vehiclePort, never()).findAllByIds(any(), any(), anyOrNull())
		verify(port, never()).create(any())
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should createMovement check date, validate activity, check participants and throw because of not found participant`() {
		// Arrange
		val movementDateTime = ZonedDateTime.now()
		val participantId1 = UUID.randomUUID()
		val movementParticipant1 = ParticipantModel().apply { id = participantId1; visible = true; purged = false }
		val participantId2 = UUID.randomUUID()
		val movementParticipant2 = ParticipantModel().apply { id = participantId2; visible = true; purged = false }
		val movementContent = listOf(
			MovementContentModel().apply { participant = movementParticipant1 },
			MovementContentModel().apply { participant = movementParticipant2 },
		)

		val movement = MovementModel(contentType = REGISTERED).apply {
			id = UUID.randomUUID()
			project = ProjectModel().apply { id = projectId }
			dateTime = movementDateTime
			content = movementContent
		}

		whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.empty())
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(currentUser(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT, result.message)
		verify(projectService).validateDateTime(
			projectId,
			CustomDateTimeModel(movementDateTime),
			MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(activityPort, never()).findById(any(), any(), anyOrNull())
		verify(participantPort).findAllByIds(
			projectId,
			movementContent.mapNotNull { it.participant?.id },
			visibilitySearched = null,
		)
		verify(vehiclePort, never()).findAllByIds(any(), any(), anyOrNull())
		verify(port, never()).create(any())
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createMovement check date, validate activity, check participants and throw because of participant is not visible`(
		movementContent: List<MovementContentModel>,
		participants: List<ParticipantModel>,
	) {
		// Arrange
		val movementDateTime = ZonedDateTime.now()

		val movement = MovementModel(contentType = REGISTERED).apply {
			id = UUID.randomUUID()
			project = ProjectModel().apply { id = projectId }
			dateTime = movementDateTime
			content = movementContent
		}

		whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
		whenever(
			participantPort.findAllByIds(
				any(),
				any(),
				anyOrNull()
			)
		).thenReturn(Flux.just(*participants.toTypedArray()))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(currentUser(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(MOVEMENT_PARTICIPANTS_NOT_VISIBLE, result.message)
		verify(projectService).validateDateTime(
			projectId,
			CustomDateTimeModel(movementDateTime),
			MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(activityPort, never()).findById(any(), any(), anyOrNull())
		verify(participantPort).findAllByIds(
			projectId,
			movementContent.mapNotNull { it.participant?.id },
			visibilitySearched = null,
		)
		verify(vehiclePort, never()).findAllByIds(any(), any(), anyOrNull())
		verify(port, never()).create(any())
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should createMovement check date, validate activity, check participants, check vehicles and throw because of not found vehicle`() {
		// Arrange
		val movementDateTime = ZonedDateTime.now()
		val participantId1 = UUID.randomUUID()
		val movementParticipant1 =
			ParticipantModel().apply {
				id = participantId1; birthday = LocalDate.now().minusYears(18); visible = true; purged = false
			}
		val participantId2 = UUID.randomUUID()
		val movementParticipant2 =
			ParticipantModel().apply {
				id = participantId2; birthday = LocalDate.now().minusYears(18); visible = true; purged = false
			}
		val vehicleId = UUID.randomUUID()
		val movementVehicle = VehicleModel().apply { id = vehicleId; visible = true }
		val movementContent = listOf(
			MovementContentModel().apply { participant = movementParticipant1 },
			MovementContentModel().apply { participant = movementParticipant2; vehicle = movementVehicle },
		)

		val movement = MovementModel(contentType = REGISTERED).apply {
			id = UUID.randomUUID()
			project = ProjectModel().apply { id = projectId }
			dateTime = movementDateTime
			content = movementContent
		}

		whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(
			Flux.just(*movementContent.map(MovementContentModel::participant).toTypedArray())
		)
		whenever(vehiclePort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.empty())
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(currentUser(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_PROJECT, result.message)
		verify(projectService).validateDateTime(
			projectId,
			CustomDateTimeModel(movementDateTime),
			MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(activityPort, never()).findById(any(), any(), anyOrNull())
		verify(participantPort).findAllByIds(
			projectId,
			movementContent.mapNotNull { it.participant?.id },
			visibilitySearched = null,
		)
		verify(vehiclePort).findAllByIds(
			projectId,
			movementContent.mapNotNull { it.vehicle?.id },
			visibilitySearched = null,
		)
		verify(port, never()).create(any())
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should createMovement check date, validate activity, check participants, check vehicles and throw because of vehicle is not visible`() {
		// Arrange
		val movementDateTime = ZonedDateTime.now()
		val participantId1 = UUID.randomUUID()
		val movementParticipant1 =
			ParticipantModel().apply {
				id = participantId1; birthday = LocalDate.now().minusYears(18); visible = true; purged = false
			}
		val participantId2 = UUID.randomUUID()
		val movementParticipant2 =
			ParticipantModel().apply {
				id = participantId2; birthday = LocalDate.now().minusYears(18); visible = true; purged = false
			}
		val vehicleId = UUID.randomUUID()
		val movementVehicle = VehicleModel().apply { id = vehicleId; visible = false }
		val movementContent = listOf(
			MovementContentModel().apply { participant = movementParticipant1 },
			MovementContentModel().apply { participant = movementParticipant2; vehicle = movementVehicle },
		)

		val movement = MovementModel(contentType = REGISTERED).apply {
			id = UUID.randomUUID()
			project = ProjectModel().apply { id = projectId }
			dateTime = movementDateTime
			content = movementContent
		}

		whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(
			Flux.just(*movementContent.map(MovementContentModel::participant).toTypedArray())
		)
		whenever(vehiclePort.findAllByIds(any(), any(), anyOrNull())).thenReturn(
			Flux.just(*movementContent.mapNotNull(MovementContentModel::vehicle).toTypedArray())
		)
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createMovement(currentUser(), movement).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(MOVEMENT_VEHICLES_NOT_VISIBLE, result.message)
		verify(projectService).validateDateTime(
			projectId,
			CustomDateTimeModel(movementDateTime),
			MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(activityPort, never()).findById(any(), any(), anyOrNull())
		verify(participantPort).findAllByIds(
			projectId,
			movementContent.mapNotNull { it.participant?.id },
			visibilitySearched = null,
		)
		verify(vehiclePort).findAllByIds(
			projectId,
			movementContent.mapNotNull { it.vehicle?.id },
			visibilitySearched = null,
		)
		verify(port, never()).create(any())
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateMovementById check date, get existing movement, validate activity, validate participants, validate vehicle and call port update`(
		previousMovementContent: List<MovementContentModel>,
		updatedMovementContent: List<MovementContentModel>,
		newMovementParticipant: List<ParticipantModel>,
		newMovementVehicle: List<VehicleModel>,
		previousMovementActivity: ActivityModel?,
		updatedMovementActivity: ActivityModel?,
		expectedCallToActivity: Int,
		expectedCallToParticipant: Int,
		expectedCallToVehicle: Int,
	) {
		// Arrange
		val movementDateTime = ZonedDateTime.now()

		val uuid = UUID.randomUUID()
		val movementToUpdate = MovementModel(contentType = REGISTERED).apply {
			id = uuid
			project = ProjectModel().apply { id = projectId }
			dateTime = movementDateTime
			activity = previousMovementActivity
			content = previousMovementContent
		}
		val movementUpdated = MovementModel(contentType = REGISTERED).apply {
			id = uuid
			project = ProjectModel().apply { id = projectId }
			dateTime = movementDateTime
			activity = updatedMovementActivity
			content = updatedMovementContent
		}

		whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movementToUpdate))
		whenever(activityPort.findById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(updatedMovementActivity ?: ActivityModel())
		)
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(
			Flux.just(*newMovementParticipant.toTypedArray())
		)
		whenever(vehiclePort.findAllByIds(any(), any(), anyOrNull())).thenReturn(
			Flux.just(*newMovementVehicle.toTypedArray())
		)
		whenever(port.update(any())).thenReturn(Mono.just(movementUpdated))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		service.updateMovementById(currentUser(), projectId, uuid, movementUpdated).block()

		// Assert
		verify(projectService).validateDateTime(
			projectId,
			CustomDateTimeModel(movementDateTime),
			MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(activityPort, times(expectedCallToActivity)).findById(
			projectId,
			updatedMovementActivity?.id ?: UUID.randomUUID(),
			visibilitySearched = null,
		)
		verify(participantPort, times(expectedCallToParticipant)).findAllByIds(
			projectId,
			newMovementParticipant.mapNotNull { it.id },
			visibilitySearched = null,
		)
		verify(vehiclePort, times(expectedCallToVehicle)).findAllByIds(
			projectId,
			newMovementVehicle.mapNotNull { it.id },
			visibilitySearched = null
		)
		verify(port).update(any())
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should disableMovementById hide and return a Movement`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val movement = MovementModel(contentType = REGISTERED).apply { id = uuid; visible = true }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.update(any())).thenReturn(Mono.just(movement))

		// Act
		service.disableMovementById(currentUser(), projectId, uuid).block()

		// Assert
		verify(port).findById(projectId, uuid, visibilitySearched = true)
		verify(port).update(movement.apply { visible = false })
	}

	@Test
	fun `Should enableMovementById restore and return a Movement`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val movement = MovementModel(contentType = REGISTERED).apply { id = uuid; visible = false }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.update(any())).thenReturn(Mono.just(movement))

		// Act
		service.enableMovementById(currentUser(), projectId, uuid).block()

		// Assert
		verify(port).findById(projectId, uuid, visibilitySearched = false)
		verify(port).update(movement.apply { visible = true })
	}

	@Test
	fun `Should deleteMovementById delete a Movement`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val movement = MovementModel(contentType = REGISTERED).apply { id = uuid }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteMovementById(projectId, uuid).block()

		// Assert
		verify(port).findById(projectId, uuid, visibilitySearched = null)
		verify(port).deleteById(uuid)
	}
}
