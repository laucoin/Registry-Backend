package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

class ProfileAcceptOrRejectValidatorTest {
	private val acceptanceValidator = ProfileAcceptOrRejectValidator()

	private companion object {
		@JvmStatic
		fun `Should isValid evaluate if Profile status is ACCEPTED or REJECTED`(): Stream<Arguments> = Stream.of(
			Arguments.of(INVITED, false),
			Arguments.of(ACCEPTED, true),
			Arguments.of(REJECTED, true),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid evaluate if Profile status is ACCEPTED or REJECTED`(
		status: ProfileStatusEnum,
		expected: Boolean,
	) {
		// Arrange
		val context: ConstraintValidatorContext = mock()

		// Act
		val result = acceptanceValidator.isValid(status, context)

		// Assert
		assertEquals(expected, result)
	}
}
