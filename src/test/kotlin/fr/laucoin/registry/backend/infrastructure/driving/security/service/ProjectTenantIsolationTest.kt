package fr.laucoin.registry.backend.infrastructure.driving.security.service

import com.tngtech.archunit.core.importer.ClassFileImporter
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IProjectV2Controller
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.lang.reflect.Method
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.security.access.expression.ExpressionUtils
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.util.SimpleMethodInvocation

class ProjectTenantIsolationTest {
	private val handler = DefaultMethodSecurityExpressionHandler().apply { setPermissionEvaluator(PermissionService()) }
	private val parser = SpelExpressionParser()
	private val projectA = UUID.randomUUID()
	private val projectB = UUID.randomUUID()
	private val permissions = permissionValues()

	@Test
	fun `Should every project scoped endpoint grant the holder of the permissions of its own project`() {
		// Arrange
		val holderOfA = authenticationWith(projectA)

		// Act
		val denied = scopedEndpoints().filterNot { decide(it, holderOfA, projectA) }

		// Assert
		assertTrue(scopedEndpoints().size > MIN_SCOPED_ENDPOINTS)
		assertEquals(emptyList<String>(), denied.map(::label))
	}

	@Test
	fun `Should every project scoped endpoint refuse the holder of the permissions of another project`() {
		// Arrange
		val holderOfB = authenticationWith(projectB)

		// Act
		val granted = scopedEndpoints().filter { decide(it, holderOfB, projectA) }

		// Assert
		assertEquals(emptyList<String>(), granted.map(::label))
	}

	@Test
	fun `Should every project scoped endpoint refuse a user without any project permission`() {
		// Arrange
		val nobody = authenticationWith()

		// Act
		val granted = scopedEndpoints().filter { decide(it, nobody, projectA) }

		// Assert
		assertEquals(emptyList<String>(), granted.map(::label))
	}

	@Test
	fun `Should every project scoped endpoint refuse a permission on the right project that is not the required one`() {
		// Arrange
		val almost = authenticationWith(projectA, only = setOf(ProjectPermissionConst.REGISTRY_PROJECT_OPTION_PREFIX))

		// Act
		val granted = scopedEndpoints().filter { decide(it, almost, projectA) }

		// Assert
		assertEquals(emptyList<String>(), granted.map(::label))
	}

	private fun scopedEndpoints(): List<Method> {
		return ClassFileImporter()
			.importPackages(IProjectV2Controller::class.java.packageName)
			.filter { it.isInterface && it.simpleName.endsWith("V2Controller") && it.packageName == IProjectV2Controller::class.java.packageName }
			.flatMap { it.reflect().methods.toList() }
			.filter { isProjectScoped(it) }
	}

	private fun isProjectScoped(method: Method): Boolean {
		val rule = method.getAnnotation(PreAuthorize::class.java)?.value ?: return false
		return rule.contains("hasPermission(#projectId") || rule.contains("hasPermission(#id")
	}

	private fun decide(method: Method, authentication: Authentication, targetProject: UUID): Boolean {
		val arguments: List<Any?> = method.parameters.map { parameter ->
			when {
				parameter.name == "projectId" -> targetProject
				parameter.name == "id" && method.declaringClass == IProjectV2Controller::class.java -> targetProject
				parameter.type == UUID::class.java -> UUID.randomUUID()
				else -> null
			}
		}
		val invocation = SimpleMethodInvocation(Any(), method, *(arguments.toTypedArray() as Array<Any>))
		val context = handler.createEvaluationContext(authentication, invocation)
		val expression = parser.parseExpression(method.getAnnotation(PreAuthorize::class.java).value)
		return ExpressionUtils.evaluateAsBoolean(expression, context)
	}

	private fun authenticationWith(project: UUID? = null, only: Set<String>? = null): Authentication {
		val authorities = permissions.filter { only == null || it in only }.map { "${project}_$it" }
		val currentUser = currentUser(*(if (project == null) emptyList() else authorities).toTypedArray())
		return UsernamePasswordAuthenticationToken(currentUser, null, currentUser.authorities)
	}

	private fun permissionValues(): List<String> {
		return ProjectPermissionConst::class.java.declaredFields
			.filter { it.type == String::class.java }
			.map { it.apply { isAccessible = true }.get(null) as String }
	}

	private fun label(method: Method) = "${method.declaringClass.simpleName}.${method.name}"

	private companion object {
		const val MIN_SCOPED_ENDPOINTS = 60
	}
}
