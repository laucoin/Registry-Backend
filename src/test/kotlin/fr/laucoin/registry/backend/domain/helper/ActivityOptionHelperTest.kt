package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.helper.ActivityOptionHelper.requireActivityOption
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.FORBIDDEN
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ActivityOptionHelperTest {
	private val projectA = UUID.randomUUID()
	private val projectB = UUID.randomUUID()

	@Test
	fun `Should requireActivityOption pass through when activity is not requested`() {
		// Arrange
		val user = currentUser()

		// Act
		val result = Mono.just("value").requireActivityOption(user, projectA, isActivityRequested = false)

		// Assert
		StepVerifier.create(result).expectNext("value").verifyComplete()
	}

	@Test
	fun `Should requireActivityOption pass through when the project has the activity option`() {
		// Arrange
		val user = currentUser("${projectA}_$REGISTRY_PROJECT_OPTION_ACTIVITY")

		// Act
		val result = Mono.just("value").requireActivityOption(user, projectA, isActivityRequested = true)

		// Assert
		StepVerifier.create(result).expectNext("value").verifyComplete()
	}

	@Test
	fun `Should requireActivityOption forbid when the project lacks the activity option`() {
		// Arrange
		val user = currentUser()

		// Act
		val result = Mono.just("value").requireActivityOption(user, projectA, isActivityRequested = true)

		// Assert
		StepVerifier.create(result)
			.expectErrorSatisfies { assertForbidden(it) }
			.verify()
	}

	@Test
	fun `Should requireActivityOption forbid when the activity option belongs to another project`() {
		// Arrange
		val user = currentUser("${projectB}_$REGISTRY_PROJECT_OPTION_ACTIVITY")

		// Act
		val result = Mono.just("value").requireActivityOption(user, projectA, isActivityRequested = true)

		// Assert
		StepVerifier.create(result)
			.expectErrorSatisfies { assertForbidden(it) }
			.verify()
	}

	private fun assertForbidden(error: Throwable) {
		val exception = error as RegistryException
		assertEquals(FORBIDDEN, exception.status)
		assertEquals(NOT_ENOUGH_PERMISSION, exception.code)
	}
}
