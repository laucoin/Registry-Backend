package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.helper.UsageGuardHelper.rejectIfStillUsed
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleModel
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.CONFLICT
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class UsageGuardHelperTest {
	private val vehicle = VehicleModel().apply { id = UUID.randomUUID() }

	@Test
	fun `Should rejectIfStillUsed keep an element without any movement`() {
		// Act
		val result = Mono.just(vehicle).rejectIfStillUsed("ELEMENT_DELETE_HAS_MOVEMENT") { Mono.just(0L) }

		// Assert
		StepVerifier.create(result).expectNext(vehicle).verifyComplete()
	}

	@Test
	fun `Should rejectIfStillUsed refuse with a conflict an element linked to a movement`() {
		// Act
		val result = Mono.just(vehicle).rejectIfStillUsed("ELEMENT_DELETE_HAS_MOVEMENT") { Mono.just(1L) }

		// Assert
		StepVerifier.create(result)
			.expectErrorSatisfies {
				val error = it as RegistryException
				assertEquals(CONFLICT, error.status)
				assertEquals("ELEMENT_DELETE_HAS_MOVEMENT", error.code)
			}
			.verify()
	}
}
