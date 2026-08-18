package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The two filters the presence board reads the project through: who is expected
 * on site right now, and who no group holds.
 *
 * A participant carrying no dates of their own runs on their groups' window, so
 * the two questions meet: the fixture owns one date-less participant and moves
 * it between no group, a group that is open, and a group that has not opened
 * yet. Its own dates stay NULL throughout — that is precisely the case the
 * availability clause used to answer "available" for, while the status returned
 * beside it read UNAVAILABLE.
 */
class ParticipantAvailabilityFilterTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var repository: IParticipantPort

	private lateinit var participantId: UUID
	private lateinit var openGroupId: UUID
	private lateinit var futureGroupId: UUID

	@BeforeEach
	fun createFixture() {
		participantId = UUID.randomUUID()
		openGroupId = UUID.randomUUID()
		futureGroupId = UUID.randomUUID()
		execute(
			"""
            INSERT INTO tb_participant (id, first_name, last_name, birthday, project_id, type)
            VALUES ('$participantId', 'Availability', 'FIXTURE', DATE '1990-01-01', '$projectId', 'REGISTERED')
            """
		)
		execute(
			"""
            INSERT INTO tb_group (id, name, start_availability_date, end_availability_date, project_id)
            VALUES ('$openGroupId', 'Availability open', CURRENT_DATE - 1, CURRENT_DATE + 1, '$projectId')
            """
		)
		execute(
			"""
            INSERT INTO tb_group (id, name, start_availability_date, end_availability_date, project_id)
            VALUES ('$futureGroupId', 'Availability future', CURRENT_DATE + 10, CURRENT_DATE + 20, '$projectId')
            """
		)
	}

	@AfterEach
	fun removeFixture() {
		execute("DELETE FROM tb_group_content WHERE participant_id = '$participantId'")
		execute("DELETE FROM tb_participant WHERE id = '$participantId'")
		execute("DELETE FROM tb_group WHERE id IN ('$openGroupId', '$futureGroupId')")
	}

	private fun execute(sql: String) {
		databaseClient.sql(sql).fetch().rowsUpdated().block()
	}

	private fun joinGroup(groupId: UUID) {
		execute(
			"""
            INSERT INTO tb_group_content (group_id, participant_id)
            VALUES ('$groupId', '$participantId')
            """
		)
	}

	private fun listsFixture(searchParams: ParticipantSearchParamModel): Boolean {
		val page = repository.findPage(projectId, PageableModel(0, 200), searchParams).block()
		assertNotNull(page)
		return page.content.any { it.id == participantId }
	}

	@Test
	fun `Should list a participant no group holds under grouped false`() {
		// Act
		val ungrouped = listsFixture(ParticipantSearchParamModel(groupedSearched = false))
		val grouped = listsFixture(ParticipantSearchParamModel(groupedSearched = true))

		// Assert
		assertTrue(ungrouped)
		assertFalse(grouped)
	}

	@Test
	fun `Should count a participant as grouped once a visible group holds them`() {
		// Arrange
		joinGroup(openGroupId)

		// Act
		val ungrouped = listsFixture(ParticipantSearchParamModel(groupedSearched = false))
		val grouped = listsFixture(ParticipantSearchParamModel(groupedSearched = true))

		// Assert
		assertFalse(ungrouped)
		assertTrue(grouped)
	}

	/**
	 * A hidden group holds nobody as far as the API is concerned, so its members
	 * belong to the "no group" bucket rather than to a group the caller cannot see.
	 */
	@Test
	fun `Should keep a participant of a hidden group in the ungrouped bucket`() {
		// Arrange
		joinGroup(openGroupId)
		execute("UPDATE tb_group SET visible = FALSE WHERE id = '$openGroupId'")

		// Act
		val ungrouped = listsFixture(ParticipantSearchParamModel(groupedSearched = false))

		// Assert
		assertTrue(ungrouped)
	}

	@Test
	fun `Should treat a date-less participant with no group as available`() {
		// Act
		val available = listsFixture(ParticipantSearchParamModel(availabilitySearched = true))

		// Assert
		assertTrue(available)
	}

	@Test
	fun `Should run a date-less participant on their group window`() {
		// Arrange
		joinGroup(futureGroupId)

		// Act
		val available = listsFixture(ParticipantSearchParamModel(availabilitySearched = true))
		val unavailable = listsFixture(ParticipantSearchParamModel(availabilitySearched = false))

		// Assert
		assertFalse(available)
		assertTrue(unavailable)
	}

	/**
	 * Several groups are an OR, not an AND: one open window is enough to expect
	 * the participant on site.
	 */
	@Test
	fun `Should expect a participant held by one open group among several`() {
		// Arrange
		joinGroup(futureGroupId)
		joinGroup(openGroupId)

		// Act
		val available = listsFixture(ParticipantSearchParamModel(availabilitySearched = true))

		// Assert
		assertTrue(available)
	}
}
