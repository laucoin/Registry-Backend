package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The presence rule lives twice — as `ParticipantQueries.PARTICIPANT_STATUS_EXPRESSION`
 * for filtering and as `AvailabilityElementExt.status` for the value returned beside
 * the row — and the two have already drifted once. So rather than restate the rule a
 * third time, this walks the whole fixture matrix (departed × has a movement × inside
 * the window) and asserts the set SQL selects for a status is exactly the set whose
 * returned status reads as that status. Any divergence fails, whichever side moved.
 */
class ParticipantStatusParityTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var repository: IParticipantPort

	private val fixtureIds: MutableList<UUID> = mutableListOf()
	private val movementIds: MutableList<UUID> = mutableListOf()

	@BeforeEach
	fun createMatrix() {
		listOf(false, true).forEach { departed ->
			listOf(null, "IN", "OUT").forEach { lastMovement ->
				listOf(false, true).forEach { available ->
					createFixture(departed, lastMovement, available)
				}
			}
		}
	}

	@AfterEach
	fun removeMatrix() {
		movementIds.forEach { execute("DELETE FROM tb_movement_content WHERE movement_id = '$it'") }
		movementIds.forEach { execute("DELETE FROM tb_movement WHERE id = '$it'") }
		fixtureIds.forEach { execute("DELETE FROM tb_participant WHERE id = '$it'") }
		movementIds.clear()
		fixtureIds.clear()
	}

	private fun createFixture(departed: Boolean, lastMovement: String?, available: Boolean) {
		val id = UUID.randomUUID()
		fixtureIds.add(id)
		val window = if (available) "NULL, NULL" else "CURRENT_DATE - 10, CURRENT_DATE - 5"
		val departedAt = if (departed) "CURRENT_TIMESTAMP" else "NULL"
		execute(
			"""
            INSERT INTO tb_participant (id, first_name, last_name, birthday, project_id, type, start_availability_date, end_availability_date, departed_at)
            VALUES ('$id', 'Parity', 'FIXTURE', DATE '1990-01-01', '$projectId', 'REGISTERED', $window, $departedAt)
            """
		)
		if (lastMovement == null) return
		val movementId = UUID.randomUUID()
		movementIds.add(movementId)
		execute(
			"""
            INSERT INTO tb_movement (id, date_time, type, project_id)
            VALUES ('$movementId', CURRENT_TIMESTAMP - INTERVAL '1 hour', '$lastMovement', '$projectId')
            """
		)
		execute(
			"""
            INSERT INTO tb_movement_content (movement_id, participant_id)
            VALUES ('$movementId', '$id')
            """
		)
	}

	private fun execute(sql: String) {
		databaseClient.sql(sql).fetch().rowsUpdated().block()
	}

	private fun fixtures(searchParams: ParticipantSearchParamModel): List<ParticipantModel> {
		val page = repository.findPage(projectId, PageableModel(0, 200), searchParams).block()!!
		return page.content.filter { fixtureIds.contains(it.id) }
	}

	@ParameterizedTest
	@EnumSource(PresenceStatusEnum::class)
	fun `Should select in SQL exactly the participants the domain reads as that status`(
		status: PresenceStatusEnum,
	) {
		// Arrange
		val expected = fixtures(ParticipantSearchParamModel())
			.filter { it.status == status }
			.mapNotNull { it.id }
			.toSet()

		// Act
		val selected = fixtures(ParticipantSearchParamModel(statusSearched = status, dateTimeSearched = null))
			.mapNotNull { it.id }
			.toSet()

		// Assert
		assertTrue(expected.isNotEmpty())
		assertEquals(expected, selected)
	}

	@ParameterizedTest
	@ValueSource(booleans = [true, false])
	fun `Should select in SQL exactly the participants the domain flags as warned`(warned: Boolean) {
		// Arrange
		val expected = fixtures(ParticipantSearchParamModel())
			.filter { it.availabilityWarning == warned }
			.mapNotNull { it.id }
			.toSet()

		// Act
		val selected = fixtures(ParticipantSearchParamModel(warnedSearched = warned))
			.mapNotNull { it.id }
			.toSet()

		// Assert
		assertTrue(expected.isNotEmpty())
		assertEquals(expected, selected)
	}

	@ParameterizedTest
	@ValueSource(booleans = [true, false])
	fun `Should select in SQL exactly the participants the domain reads as departed`(departed: Boolean) {
		// Arrange
		val expected = fixtures(ParticipantSearchParamModel())
			.filter { (it.status == PresenceStatusEnum.DEPARTED) == departed }
			.mapNotNull { it.id }
			.toSet()

		// Act
		val selected = fixtures(ParticipantSearchParamModel(departedSearched = departed))
			.mapNotNull { it.id }
			.toSet()

		// Assert
		assertTrue(expected.isNotEmpty())
		assertEquals(expected, selected)
	}
}
