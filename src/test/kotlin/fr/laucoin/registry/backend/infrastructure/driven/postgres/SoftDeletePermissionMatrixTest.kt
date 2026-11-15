package fr.laucoin.registry.backend.infrastructure.driven.postgres

import fr.laucoin.registry.backend.test.TestContext
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

class SoftDeletePermissionMatrixTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	companion object {
		private const val ADMINISTRATOR = "PROJECT_ADMINISTRATOR"
		private const val COORDINATOR = "PROJECT_COORDINATOR"
		private const val PARTICIPANT = "PROJECT_PARTICIPANT"

		@JvmStatic
		fun `Should grant soft delete and restore to the expected project roles`(): Stream<Arguments> = Stream.of(
			Arguments.of("REGISTRY_PROJECT", listOf(ADMINISTRATOR), listOf(ADMINISTRATOR)),
			Arguments.of("REGISTRY_PROJECT_PROFILE", listOf(ADMINISTRATOR), listOf(ADMINISTRATOR)),
			Arguments.of(
				"REGISTRY_PROJECT_PARTICIPANT",
				listOf(ADMINISTRATOR, COORDINATOR, PARTICIPANT),
				listOf(ADMINISTRATOR, COORDINATOR),
			),
			Arguments.of(
				"REGISTRY_PROJECT_GROUP",
				listOf(ADMINISTRATOR, COORDINATOR, PARTICIPANT),
				listOf(ADMINISTRATOR, COORDINATOR),
			),
			Arguments.of(
				"REGISTRY_PROJECT_MOVEMENT",
				listOf(ADMINISTRATOR, COORDINATOR, PARTICIPANT),
				listOf(ADMINISTRATOR, COORDINATOR),
			),
			Arguments.of(
				"REGISTRY_PROJECT_COMMUNICATION",
				listOf(ADMINISTRATOR, COORDINATOR, PARTICIPANT),
				listOf(ADMINISTRATOR, COORDINATOR),
			),
			Arguments.of(
				"REGISTRY_PROJECT_ALERT",
				listOf(ADMINISTRATOR, COORDINATOR, PARTICIPANT),
				listOf(ADMINISTRATOR, COORDINATOR),
			),
			Arguments.of(
				"REGISTRY_PROJECT_VEHICLE",
				listOf(ADMINISTRATOR, COORDINATOR),
				listOf(ADMINISTRATOR, COORDINATOR),
			),
			Arguments.of(
				"REGISTRY_PROJECT_ACTIVITY",
				listOf(ADMINISTRATOR, COORDINATOR),
				listOf(ADMINISTRATOR, COORDINATOR),
			),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should grant soft delete and restore to the expected project roles`(
		prefix: String,
		softDeleteRoles: List<String>,
		restoreRoles: List<String>,
	) {
		// Act
		val softDelete = rolesOf("${prefix}_SOFT_DELETE")
		val restore = rolesOf("${prefix}_RESTORE")

		// Assert
		assertEquals(softDeleteRoles.sorted(), softDelete)
		assertEquals(restoreRoles.sorted(), restore)
	}

	@Test
	fun `Should grant block and unblock of users to the user administrator only`() {
		// Act
		val block = userRolesOf("REGISTRY_USER_SOFT_DELETE")
		val unblock = userRolesOf("REGISTRY_USER_RESTORE")

		// Assert
		assertEquals(listOf("USER_ADMINISTRATOR"), block)
		assertEquals(listOf("USER_ADMINISTRATOR"), unblock)
	}

	private fun rolesOf(permission: String): List<String> =
		databaseClient.sql("SELECT role FROM tb_project_role_permission WHERE permission = :permission ORDER BY role")
			.bind("permission", permission)
			.map { row -> row.get(0, String::class.java)!! }
			.all()
			.collectList()
			.block()!!

	private fun userRolesOf(permission: String): List<String> =
		databaseClient.sql("SELECT role FROM tb_user_role_permission WHERE permission = :permission ORDER BY role")
			.bind("permission", permission)
			.map { row -> row.get(0, String::class.java)!! }
			.all()
			.collectList()
			.block()!!
}
