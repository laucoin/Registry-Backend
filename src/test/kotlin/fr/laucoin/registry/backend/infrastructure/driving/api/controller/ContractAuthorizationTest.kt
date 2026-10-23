package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.importer.ClassFileImporter
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping

class ContractAuthorizationTest {
	private companion object {
		private val PUBLIC_ENDPOINTS = setOf(
			"ISecurityV2Controller.getLoginUri",
			"ISecurityV2Controller.getLogoutUri",
			"ISecurityV2Controller.fetchToken",
			"ISecurityV2Controller.refreshToken",
		)
		private const val PUBLIC_CONTRACT = "IMetadataV2Controller"
		private val MAPPINGS = listOf(
			GetMapping::class.java, PostMapping::class.java, PatchMapping::class.java,
			DeleteMapping::class.java, PutMapping::class.java,
		)
	}

	@Test
	fun `Should every endpoint of a contract interface carry an authorization rule except the public ones`() {
		// Arrange
		val contracts = ClassFileImporter()
			.importPackages(IExportV2Controller::class.java.packageName)
			.filter { it.isInterface && it.simpleName.endsWith("V2Controller") && it.packageName == IExportV2Controller::class.java.packageName }

		// Act
		val unprotected = contracts
			.filter { it.simpleName != PUBLIC_CONTRACT }
			.flatMap { contract -> contract.methods.filter(::isEndpoint).map { "${contract.simpleName}.${it.name}" to it } }
			.filter { (name, method) -> name !in PUBLIC_ENDPOINTS && !method.isAnnotatedWith(PreAuthorize::class.java) }
			.map { (name, _) -> name }

		// Assert
		assertTrue(contracts.isNotEmpty())
		assertTrue(unprotected.isEmpty(), "Endpoints without @PreAuthorize: $unprotected")
	}

	private fun isEndpoint(method: JavaMethod): Boolean = MAPPINGS.any { method.isAnnotatedWith(it) }
}
