package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import java.time.Clock
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ALERT
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.VEHICLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectQueries
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ProjectEntityMapperTest {
	private val mapper = ProjectEntityMapper(Clock.systemUTC())

	private fun entityWithCounts(options: List<ProjectOptionEnum>?) = ProjectEntity(
		options = options,
		participants = 5,
		groups = 4,
		acceptedProfiles = 3,
		vehicles = 2,
		activities = 1,
		activeAlerts = 7,
	)

	private companion object {
		@JvmStatic
		fun `Should toModel keep only the counts of the enabled options`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(listOf(VEHICLE), 2L, null, null),
				Arguments.of(listOf(ACTIVITY), null, 1L, null),
				Arguments.of(listOf(ALERT), null, null, 7L),
				Arguments.of(listOf(VEHICLE, ACTIVITY, ALERT), 2L, 1L, 7L),
				Arguments.of(emptyList<ProjectOptionEnum>(), null, null, null),
				Arguments.of(null, null, null, null),
			)
		}
	}

	@Test
	fun `Should count queries use the same statuses as the enums`() {
		// Arrange
		val acceptedProfile = ProfileStatusEnum.ACCEPTED.name
		val activeAlert = AlertStatusEnum.IN_PROGRESS.name

		// Act
		val queryStatuses = ProjectQueries.ACCEPTED_PROFILE_STATUS to ProjectQueries.ACTIVE_ALERT_STATUS

		// Assert
		assertEquals(acceptedProfile to activeAlert, queryStatuses)
	}

	@Test
	fun `Should toModel return no counts when the entity was not read with counts`() {
		// Arrange
		val entity = ProjectEntity(options = listOf(VEHICLE))

		// Act
		val result = mapper.toModel(entity)

		// Assert
		assertNull(result.counts)
	}

	@Test
	fun `Should toModel default the missing mandatory counts to zero`() {
		// Arrange
		val entity = ProjectEntity(participants = 5)

		// Act
		val result = mapper.toModel(entity)

		// Assert
		assertEquals(5, result.counts?.participants)
		assertEquals(0, result.counts?.groups)
		assertEquals(0, result.counts?.acceptedProfiles)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should toModel keep only the counts of the enabled options`(
		options: List<ProjectOptionEnum>?,
		vehicles: Long?,
		activities: Long?,
		activeAlerts: Long?,
	) {
		// Arrange
		val entity = entityWithCounts(options)

		// Act
		val result = mapper.toModel(entity)

		// Assert
		assertEquals(5, result.counts?.participants)
		assertEquals(4, result.counts?.groups)
		assertEquals(3, result.counts?.acceptedProfiles)
		assertEquals(vehicles, result.counts?.vehicles)
		assertEquals(activities, result.counts?.activities)
		assertEquals(activeAlerts, result.counts?.activeAlerts)
	}
}
