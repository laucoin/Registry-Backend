package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.OUT
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Disabling a movement must take it out of the presence arithmetic entirely: it
 * is how a mis-keyed entry or exit is undone, and a status that keeps counting
 * it tells the board someone — or some vehicle — is on site when they are not.
 *
 * The trap both status CTEs fell into was matching the outer movement on its
 * TIMESTAMP alone. The inner aggregate correctly ignored invisible movements
 * when picking the latest instant, but the outer join then accepted ANY movement
 * at that instant: the hidden one, or one belonging to a different participant
 * or vehicle. So each case here records two movements at the SAME instant.
 *
 * The fixture owns its participant and vehicle rather than borrowing the
 * dataset's: presence is only read on an entity whose availability window is
 * open, so the window is left unbounded (NULL) to keep the assertions about
 * visibility and nothing else.
 */
class PresenceStatusVisibilityTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var vehicleRepository: IVehiclePort

	@Autowired
	private lateinit var participantRepository: IParticipantPort

	@Autowired
	private lateinit var movementRepository: IMovementPort

	private lateinit var vehicleId: UUID
	private lateinit var participantId: UUID
	private val movementIds = mutableListOf<UUID>()

	/**
	 * Far enough ahead that these are unambiguously the latest movements the
	 * fixture's entities hold, which is the only way the status is read from them.
	 */
	private val instant = "TIMESTAMPTZ '2999-01-01 10:00:00+00'"
	private val laterInstant = "TIMESTAMPTZ '2999-01-01 10:01:00+00'"

	@BeforeEach
	fun createFixture() {
		vehicleId = UUID.randomUUID()
		participantId = UUID.randomUUID()
		execute(
			"""
            INSERT INTO tb_vehicle (id, license_plate, brand, model, project_id)
            VALUES ('$vehicleId', 'ZZ-999-ZZ', 'Fixture', 'Presence', '$projectId')
            """
		)
		execute(
			"""
            INSERT INTO tb_participant (id, first_name, last_name, birthday, project_id, type)
            VALUES ('$participantId', 'Presence', 'FIXTURE', DATE '1990-01-01', '$projectId', 'REGISTERED')
            """
		)
	}

	@AfterEach
	fun removeFixture() {
		movementIds.forEach { id ->
			execute("DELETE FROM tb_movement_content WHERE movement_id = '$id'")
			execute("DELETE FROM tb_movement WHERE id = '$id'")
		}
		movementIds.clear()
		execute("DELETE FROM tb_participant WHERE id = '$participantId'")
		execute("DELETE FROM tb_vehicle WHERE id = '$vehicleId'")
	}

	private fun execute(sql: String) {
		databaseClient.sql(sql).fetch().rowsUpdated().block()
	}

	private fun recordMovement(
		type: String,
		visible: Boolean = true,
		withVehicle: Boolean = true,
		at: String = instant,
	): UUID {
		val id = UUID.randomUUID()
		movementIds += id
		execute(
			"""
            INSERT INTO tb_movement (id, date_time, type, reason, project_id, visible)
            VALUES ('$id', $at, '$type', ${if (type == "OUT") "'OTHER'" else "NULL"}, '$projectId', $visible)
            """
		)
		execute(
			"""
            INSERT INTO tb_movement_content (movement_id, participant_id, vehicle_id)
            VALUES ('$id', '$participantId', ${if (withVehicle) "'$vehicleId'" else "NULL"})
            """
		)
		return id
	}

	private fun vehicleStatus() =
		vehicleRepository.findById(projectId, vehicleId, visibilitySearched = null).block()

	private fun participantStatus() =
		participantRepository.findById(projectId, participantId, visibilitySearched = null).block()

	@Test
	fun `Should not let a hidden exit take a vehicle out of the yard`() {
		// Arrange
		recordMovement(type = "IN")

		// Act
		recordMovement(type = "OUT", visible = false)

		// Assert
		val vehicle = vehicleStatus()
		assertNotNull(vehicle)
		assertEquals(IN, vehicle.status)
	}

	/**
	 * A movement carrying no vehicle must not lend its direction to one either —
	 * the same uncorrelated join let any movement sharing the instant do that.
	 */
	@Test
	fun `Should not let a vehicle-less exit take a vehicle out of the yard`() {
		// Arrange
		recordMovement(type = "IN")

		// Act
		recordMovement(type = "OUT", withVehicle = false)

		// Assert
		val vehicle = vehicleStatus()
		assertNotNull(vehicle)
		assertEquals(IN, vehicle.status)
	}

	@Test
	fun `Should not let a hidden exit send a participant away`() {
		// Arrange
		recordMovement(type = "IN", withVehicle = false)

		// Act
		recordMovement(type = "OUT", visible = false, withVehicle = false)

		// Assert
		val participant = participantStatus()
		assertNotNull(participant)
		assertEquals(IN, participant.status)
	}

	/**
	 * "Current movements" — the project home's second tab — asks the same
	 * question of the same CTE: whose latest movement is still an exit. A hidden
	 * exit answered yes, so a mistaken outing stayed on the home page after being
	 * disabled, which is precisely what disabling is for.
	 */
	@Test
	fun `Should not list a hidden exit among the current movements`() {
		// Arrange
		val hiddenExit = recordMovement(type = "OUT", visible = false, withVehicle = false)

		// Act
		val page = movementRepository.findCurrentPage(
			projectId,
			PageableModel(0, 200),
			MovementSearchParamModel(visibilitySearched = true, linkedToActivity = null, typeSearched = null),
		).block()

		// Assert
		assertNotNull(page)
		assertEquals(false, page.content.any { it.id == hiddenExit })
	}

	/**
	 * The visible movement still decides — otherwise every test above would pass
	 * on a query that simply ignored exits. Recorded a minute later so it is
	 * unambiguously the latest: two visible movements at the same instant are a
	 * genuine tie the status has no basis to break.
	 */
	@Test
	fun `Should still read a later visible exit as the current state`() {
		// Arrange
		recordMovement(type = "IN")

		// Act
		recordMovement(type = "OUT", at = laterInstant)

		// Assert
		val vehicle = vehicleStatus()
		assertNotNull(vehicle)
		assertEquals(OUT, vehicle.status)
	}
}
