package fr.laucoin.registry.backend.infrastructure.driven.postgres

import com.tngtech.archunit.core.importer.ClassFileImporter
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IUserV2Controller
import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.security.access.prepost.PreAuthorize

class RbacSeedConsistencyTest : TestContext() {
	private companion object {
		private const val OPTION_PREFIX = ProjectPermissionConst.REGISTRY_PROJECT_OPTION_PREFIX
	}

	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Test
	fun `Should the user permission constants match the seeded user permissions`() {
		// Arrange
		val seeded = seededPermissions("tb_user_permission")

		// Act
		val constants = constantsOf(UserPermissionConst::class.java)

		// Assert
		assertEquals(seeded, constants)
	}

	@Test
	fun `Should the project permission constants match the seeded project permissions`() {
		// Arrange
		val seeded = seededPermissions("tb_project_permission")

		// Act
		val constants = constantsOf(ProjectPermissionConst::class.java).filterNot { it.startsWith(OPTION_PREFIX) }.toSet()

		// Assert
		assertEquals(seeded, constants)
	}

	@Test
	fun `Should every permission required by a PreAuthorize rule be seeded`() {
		// Arrange
		val seeded = seededPermissions("tb_user_permission") + seededPermissions("tb_project_permission")

		// Act
		val required = requiredPermissions().filterNot { it.startsWith(OPTION_PREFIX) }.toSet()

		// Assert
		assertTrue(required.isNotEmpty())
		assertEquals(emptySet<String>(), required - seeded, "Permissions required by @PreAuthorize but not seeded")
	}

	@Test
	fun `Should the option authority constants match the project options`() {
		// Arrange
		val expected = ProjectOptionEnum.entries.map { "$OPTION_PREFIX${it.name}" }.toSet()

		// Act
		val constants = constantsOf(ProjectPermissionConst::class.java)
			.filter { it.startsWith(OPTION_PREFIX) && it != OPTION_PREFIX }
			.toSet()

		// Assert
		assertEquals(expected, constants)
	}

	private fun seededPermissions(table: String): Set<String> {
		return databaseClient.sql("SELECT name FROM $table")
			.map { row -> row.get("name", String::class.java)!! }
			.all()
			.collectList()
			.block()!!
			.toSet()
	}

	private fun constantsOf(type: Class<*>): Set<String> {
		return type.declaredFields
			.filter { it.type == String::class.java }
			.map { it.apply { isAccessible = true }.get(null) as String }
			.toSet()
	}

	private fun requiredPermissions(): Set<String> {
		val pattern = Regex("REGISTRY_[A-Z_]+")
		return ClassFileImporter()
			.importPackages(IUserV2Controller::class.java.packageName)
			.filter { it.isInterface && it.simpleName.endsWith("V2Controller") }
			.flatMap { it.reflect().methods.toList() }
			.mapNotNull { it.getAnnotation(PreAuthorize::class.java)?.value }
			.flatMap { pattern.findAll(it).map { match -> match.value }.toList() }
			.toSet()
	}
}
