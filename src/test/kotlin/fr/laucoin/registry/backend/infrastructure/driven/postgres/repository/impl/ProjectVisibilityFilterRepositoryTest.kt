package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.test.TestContext
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

/**
 * Les colonnes du projet ne sont plus sélectionnées avec les éléments, mais le INNER JOIN sur le projet est conservé
 * donc les éléments d'un projet désactivé doivent rester masqués.
 */
class ProjectVisibilityFilterRepositoryTest: TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var vehiclePort: IVehiclePort

	private val hiddenProjectId = UUID.randomUUID()
	private val vehicleId = UUID.randomUUID()

	@BeforeEach
	fun setup() {
		databaseClient.sql("INSERT INTO tb_project (id, name, visible) VALUES (:id, 'Hidden project', FALSE)")
			.bind("id", hiddenProjectId).then().block()
		databaseClient.sql(
			"INSERT INTO tb_vehicle (id, license_plate, brand, model, project_id) VALUES (:id, :plate, 'Brand', 'Model', :projectId)"
		).bind("id", vehicleId).bind("plate", vehicleId.toString().take(20)).bind("projectId", hiddenProjectId).then().block()
	}

	@AfterEach
	fun cleanup() {
		databaseClient.sql("DELETE FROM tb_vehicle WHERE id = :id").bind("id", vehicleId).then().block()
		databaseClient.sql("DELETE FROM tb_project WHERE id = :id").bind("id", hiddenProjectId).then().block()
	}

	@Test
	fun `Should findById not return an element of a disabled project`() {
		// Act
		val result = vehiclePort.findById(hiddenProjectId, vehicleId, isVisible = null).block()

		// Assert
		assertNull(result)
	}

	@Test
	fun `Should findById return the element with its project id once the project is enabled`() {
		// Arrange
		databaseClient.sql("UPDATE tb_project SET visible = TRUE WHERE id = :id").bind("id", hiddenProjectId)
			.then().block()

		// Act
		val result = vehiclePort.findById(hiddenProjectId, vehicleId, isVisible = null).block()

		// Assert
		assertNotNull(result)
		assertEquals(vehicleId, result.id)
		assertEquals(hiddenProjectId, result.projectId)
	}
}
