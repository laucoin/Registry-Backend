package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
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
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.PlannedMovementModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.test.ModelExt.defaultActivity
import fr.laucoin.registry.backend.test.ModelExt.defaultGroup
import fr.laucoin.registry.backend.test.ModelExt.defaultGroupId
import fr.laucoin.registry.backend.test.ModelExt.defaultParticipant
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.ModelExt.defaultVehicle
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import java.util.stream.Stream

import fr.laucoin.registry.backend.domain.model.MovementFeatureModel

class MovementSearchServiceTest {
	private val port: IMovementPort = mock()
	private val participantPort: IParticipantPort = mock()
	private val vehiclePort: IVehiclePort = mock()
	private val activityPort: IActivityPort = mock()
	private val groupPort: IGroupPort = mock()

	private fun buildService(clock: Clock) = MovementSearchService(
		participantPort,
		groupPort,
		vehiclePort,
		activityPort,
		clock,
		MovementFeatureModel(
		MovementFeatureModel.Searched(MAX_PARTICIPANTS, MAX_GROUPS, MAX_VEHICLES, MAX_ACTIVITIES),
		MovementFeatureModel.Planned(MAX_PLANNED),
		FUTURE_TOLERANCE,
	),
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
		private val searchAt: ZonedDateTime = ZonedDateTime.of(2026, 7, 14, 10, 0, 0, 0, ZoneOffset.UTC)

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
		).apply {
			this.query = query
			at = searchAt
		}
		val expectedGroupSearch =
			GroupSearchParamModel(query, isVisible = true, isPresent = true)

		whenever(participantPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultParticipant()))
		whenever(groupPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(defaultGroup()))
		whenever(groupPort.findContent(any(), any(), anyOrNull(), anyOrNull()))
			.thenReturn(Flux.just(Pair(defaultGroupId, listOf(defaultParticipant()))))
		val detailedMember = defaultParticipant().apply { status = PresenceStatusEnum.IN }
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(detailedMember))

		// Act
		val result = service.searchParticipantsAndGroupsByText(defaultProjectId, type, query, searchAt, false).block()

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
	fun `Should searchParticipantsAndGroupsByText ignore the date when all is requested`() {
		// Arrange
		val expectedParticipantSearch = ParticipantSearchParamModel(
			isMajor = null,
			REGISTERED,
			isVisible = true,
		).apply { query = "searched" }
		val participant = defaultParticipant()
		whenever(participantPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(participant))
		whenever(groupPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.empty())
		whenever(groupPort.findContent(any(), any(), anyOrNull(), anyOrNull())).thenReturn(Flux.empty())
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.empty())

		// Act
		val result = service.searchParticipantsAndGroupsByText(defaultProjectId, REGISTERED, "searched", searchAt, true).block()

		// Assert
		assertEquals(listOf(participant), result?.t1)
		verify(participantPort).findWithLimit(MAX_PARTICIPANTS, defaultProjectId, expectedParticipantSearch)
		verify(groupPort).findContent(defaultProjectId, emptyList(), isVisible = true, isAvailable = null)
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
		val result =
			service.searchParticipantsAndGroupsByText(defaultProjectId, REGISTERED, "searched", searchAt, false).block()

		// Assert
		assertEquals(1, result?.t1?.size)
		assertEquals(0, result?.t2?.size)
	}

	@Test
	fun `Should searchVehiclesByText call vehicle port findWithLimit`() {
		// Arrange
		val expected = defaultVehicle()
		val query = "searched"
		val expectedVehicleSearch = VehicleSearchParamModel().apply {
			this.query = query
			this.isVisible = true
			this.isAvailable = true
		}

		whenever(vehiclePort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(expected))

		// Act
		val result = service.searchVehiclesByText(defaultProjectId, query).collectList().block()

		// Assert
		assertEquals(listOf(expected), result)
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
		val result = service.searchActivitiesByText(activityOptionHolder(), defaultProjectId, type, query).collectList().block()

		// Assert
		assertEquals(expectedActivities, result)

		verify(activityPort, times(expectedPortCall)).findWithLimit(
			MAX_ACTIVITIES,
			defaultProjectId,
			expectedActivitySearch
		)
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

	private fun activityOptionHolder() = currentUser("${defaultProjectId}_$REGISTRY_PROJECT_OPTION_ACTIVITY")

	@Test
	fun `Should searchActivitiesByText return no activity when the project has not the activity option`() {
		// Act
		val result = service.searchActivitiesByText(currentUser(), defaultProjectId, REGISTERED, "searched").collectList().block()

		// Assert
		assertEquals(emptyList<ActivityModel>(), result)
		verify(activityPort, never()).findWithLimit(any(), any(), anyOrNull())
	}
}
