package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_IMPLEMENTED_YET
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import java.util.UUID
import java.util.stream.Stream

class PermissionServiceTest {
	private val service: PermissionService = PermissionService()

	private companion object {
		private const val RIGHT_ROLE = "ROLE_PROJECT"
		private const val WRONG_ROLE = "ROLE_USER"
		private val uuid = UUID.randomUUID()

		@JvmStatic
		fun `Should hasPermission check permission`(): Stream<Arguments> = Stream.of(
			Arguments.of(arrayOf("${uuid}_$RIGHT_ROLE"), uuid, RIGHT_ROLE, true),
			Arguments.of(arrayOf("${uuid}_$RIGHT_ROLE"), uuid.toString(), RIGHT_ROLE, true),
			Arguments.of(arrayOf("${uuid}_$RIGHT_ROLE"), CurrentUserModel(), RIGHT_ROLE, false),
			Arguments.of(arrayOf("${uuid}_$WRONG_ROLE"), uuid, RIGHT_ROLE, false),
			Arguments.of(emptyArray<String>(), uuid, RIGHT_ROLE, false),
			Arguments.of(arrayOf("${uuid}_$RIGHT_ROLE"), "", RIGHT_ROLE, false),
			Arguments.of(emptyArray<String>(), "", RIGHT_ROLE, false),
			Arguments.of(arrayOf("${uuid}_$RIGHT_ROLE"), uuid, CurrentUserModel(), false),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should hasPermission check permission`(
		permissions: Array<String>,
		target: Any?,
		wantedPermission: Any,
		expectedAccess: Boolean,
	) {
		// Arrange
		val authentication = authenticate(*permissions)

		// Act
		val result = service.hasPermission(authentication, target, wantedPermission)

		// Assert
		assertEquals(expectedAccess, result)

	}

	@Test
	fun `Should hasPermission throw not implemented exception`() {
		// Act
		val result = assertThrows(RegistryException::class.java) {
			service.hasPermission(authenticate(), uuid, "", "")
		}

		// Assert
		assertEquals(NOT_IMPLEMENTED, result.status)
		assertEquals(NOT_IMPLEMENTED_YET, result.message)
	}
}
