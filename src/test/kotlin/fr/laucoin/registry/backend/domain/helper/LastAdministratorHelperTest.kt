package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.test.ModelExt.defaultProject
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.ModelExt.defaultUser
import fr.laucoin.registry.backend.test.ModelExt.defaultUserId
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import reactor.core.Exceptions
import reactor.core.publisher.Flux

class LastAdministratorHelperTest {
	private val port: IProjectProfilePort = mock()
	private val helper = LastAdministratorHelper(port)

	init {
		whenever(port.lockLevel0ProjectProfilesByUserId(any())).thenReturn(Flux.empty())
	}

	private companion object {
		@JvmStatic
		fun `Should validateNotLastProjectRoleLevel0 return the given Object`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(null, null),
				Arguments.of(
					defaultProjectId, ProjectProfileRoleCountModel(defaultProject().apply { id = UUID.randomUUID() }, 0)
				),
				Arguments.of(defaultProjectId, ProjectProfileRoleCountModel(defaultProject(), 2)),
			)
		}

		@JvmStatic
		fun `Should validateNotLastProjectRoleLevel0 throw RegistryException`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(defaultProjectId, ProjectProfileRoleCountModel(defaultProject(), 1)),
				Arguments.of(defaultProjectId, ProjectProfileRoleCountModel(defaultProject(), 1)),
				Arguments.of(null, ProjectProfileRoleCountModel(ProjectModel(), 1)),
			)
		}
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateNotLastProjectRoleLevel0 return the given Object`(
		projectId: UUID?,
		profileCount: ProjectProfileRoleCountModel?
	) {
		// Arrange
		val user = defaultUser()

		whenever(port.findLevel0ProjectProfileRoleByUserId(any(), any()))
			.thenReturn(if (Objects.nonNull(profileCount)) Flux.just(profileCount!!) else Flux.empty())

		// Act
		val result = helper
			.validateNotLastProjectRoleLevel0(defaultUserId, projectId = projectId, user, error = "ERROR_MESSAGE")
			.block()

		// Assert
		assertEquals(user, result)

		verify(port).lockLevel0ProjectProfilesByUserId(defaultUserId)
		verify(port).findLevel0ProjectProfileRoleByUserId(defaultUserId, isVisible = true)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateNotLastProjectRoleLevel0 throw RegistryException`(
		projectId: UUID?,
		profileCount: ProjectProfileRoleCountModel
	) {
		// Arrange
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findLevel0ProjectProfileRoleByUserId(any(), any()))
			.thenReturn(Flux.just(profileCount))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			helper.validateNotLastProjectRoleLevel0(defaultUserId, projectId = projectId, UserModel(), errorMessage)
				.block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(errorMessage, result.message)

		verify(port).findLevel0ProjectProfileRoleByUserId(defaultUserId, isVisible = true)
	}
}
