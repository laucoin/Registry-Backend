package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.UNAVAILABLE
import fr.laucoin.registry.backend.domain.enumeration.AvailabilitySubjectEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.OVERDUE
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.service.support.ITranslateService
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class StatusLabelMappingHelperTest {
	private val translateService: ITranslateService = mock()
	private val helper = StatusLabelMappingHelper(
		translateService,
		DurationLabelHelper(translateService),
		Clock.systemUTC(),
	)

	private companion object {
		private const val NOT_YET = "availability.status.profile.duration.NOT_YET"
		private const val NO_MORE = "availability.status.profile.duration.NO_MORE"
		private const val AVAILABLE_SINCE = "availability.status.profile.duration.AVAILABLE"
		private const val UNAVAILABLE_PLAIN = "availability.status.profile.UNAVAILABLE"
	}

	init {
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull(), any()))
			.thenAnswer { "${it.getArgument<String>(0)}|${it.getArgument<Array<Any>?>(1)?.joinToString()}" }
	}

	private fun day(offset: Long) = CustomDateTimeModel(LocalDate.now(ZoneOffset.UTC).plusDays(offset))

	private fun label(status: AvailabilityStatusEnum, start: CustomDateTimeModel?, end: CustomDateTimeModel?): String {
		val profile = ProjectProfileModel(availabilityStatus = status, startAccess = start, endAccess = end)
		return helper.profileAvailabilityStatus(profile)!!.label
	}

	@Test
	fun `Should announce the time before the start when the start is in the future`() {
		// Arrange
		val start = day(5)
		val end = day(10)

		// Act
		val label = label(UNAVAILABLE, start, end)

		// Assert
		assertTrue(label.startsWith("$NOT_YET|"), label)
		assertFalse(label.contains("-"), label)
	}

	@Test
	fun `Should announce the time since the end when the end is in the past`() {
		// Arrange
		val start = day(-10)
		val end = day(-5)

		// Act
		val label = label(UNAVAILABLE, start, end)

		// Assert
		assertTrue(label.startsWith("$NO_MORE|"), label)
		assertFalse(label.contains("-"), label)
	}

	@Test
	fun `Should announce the time before the start when there is no end`() {
		// Arrange
		val start = day(3)

		// Act
		val label = label(UNAVAILABLE, start, null)

		// Assert
		assertTrue(label.startsWith("$NOT_YET|"), label)
		assertFalse(label.contains("-"), label)
	}

	@Test
	fun `Should announce the time since the end when there is no start`() {
		// Arrange
		val end = day(-3)

		// Act
		val label = label(UNAVAILABLE, null, end)

		// Assert
		assertTrue(label.startsWith("$NO_MORE|"), label)
		assertFalse(label.contains("-"), label)
	}

	@Test
	fun `Should announce the time since the start when the element is available`() {
		// Arrange
		val start = day(-4)
		val end = day(4)

		// Act
		val label = label(AVAILABLE, start, end)

		// Assert
		assertTrue(label.startsWith("$AVAILABLE_SINCE|"), label)
		assertFalse(label.contains("-"), label)
	}

	@Test
	fun `Should fall back on the plain status when there is no date`() {
		// Arrange
		val start: CustomDateTimeModel? = null

		// Act
		val label = label(UNAVAILABLE, start, null)

		// Assert
		assertEquals("$UNAVAILABLE_PLAIN|null", label)
	}

	@Test
	fun `Should word the label with the translations of the subject`() {
		// Arrange
		val start = day(5)
		val labels = mapOf(
			AvailabilitySubjectEnum.ACTIVITY to helper.activityStatus(ActivityModel(status = UNAVAILABLE, startAvailability = start)),
			AvailabilitySubjectEnum.GROUP to helper.groupStatus(GroupModel(status = UNAVAILABLE, startAvailability = start)),
			AvailabilitySubjectEnum.PROJECT to helper.projectStatus(ProjectModel(status = UNAVAILABLE, begin = start)),
			AvailabilitySubjectEnum.PROFILE to helper.profileAvailabilityStatus(
				ProjectProfileModel(availabilityStatus = UNAVAILABLE, startAccess = start),
			),
		)

		// Act
		val keys = labels.mapValues { it.value!!.label }

		// Assert
		assertEquals(AvailabilitySubjectEnum.entries.toSet(), keys.keys)
		keys.forEach { (subject, label) ->
			assertTrue(label.startsWith("availability.status.${subject.name.lowercase()}.duration.NOT_YET|"), label)
		}
	}

	@Test
	fun `Should leave the status empty when the element has none`() {
		// Arrange
		val activity = ActivityModel(status = null)

		// Act
		val result = helper.activityStatus(activity)

		// Assert
		assertNull(result)
	}

	@Test
	fun `Should label an overdue presence without any duration`() {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull(), any())).thenAnswer { it.arguments[0] }
		val vehicle = VehicleModel(
			status = OVERDUE,
			lastMovement = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1),
			startAvailability = day(-5),
			endAvailability = day(-1),
		)
		val participant = ParticipantModel(status = OVERDUE, lastMovement = vehicle.lastMovement)

		// Act
		val vehicleLabel = helper.vehicleStatus(vehicle)
		val participantLabel = helper.participantStatus(participant)

		// Assert
		assertEquals("OVERDUE", vehicleLabel?.value)
		assertEquals("presence.status.OVERDUE", vehicleLabel?.label)
		assertEquals("presence.status.OVERDUE", participantLabel?.label)
	}
}
