package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.service.core.IUserProjectProfileService
import fr.laucoin.registry.backend.domain.service.usecase.IUserProjectProfileUseCase
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono

class UserProjectProfileUseCaseTest {
	private val service: IUserProjectProfileService = mock()
	private val useCase: IUserProjectProfileUseCase = UserProjectProfileUseCase(service)

	@Test
	fun `Should findProjectProfilesPage delegate to the service`() {
		// Arrange
		val userId: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: ProjectProfileSearchParamModel = mock()
		val expected: Mono<PageModel<ProjectProfileModel>> = Mono.empty()
		whenever(service.findProjectProfilesPage(userId, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findProjectProfilesPage(userId, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(service).findProjectProfilesPage(userId, pageable, searchParams)
	}

	@Test
	fun `Should findProjectProfilesById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<ProjectProfileModel> = Mono.empty()
		whenever(service.findProjectProfilesById(currentUser, id)).thenReturn(expected)

		// Act
		val result = useCase.findProjectProfilesById(currentUser, id)

		// Assert
		assertSame(expected, result)
		verify(service).findProjectProfilesById(currentUser, id)
	}

	@Test
	fun `Should updateUserProjectProfileStatusById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val id: UUID = UUID.randomUUID()
		val status: ProfileStatusEnum = ProfileStatusEnum.entries.first()
		val expected: Mono<ProjectProfileModel> = Mono.empty()
		whenever(service.updateUserProjectProfileStatusById(currentUser, id, status)).thenReturn(expected)

		// Act
		val result = useCase.updateUserProjectProfileStatusById(currentUser, id, status)

		// Assert
		assertSame(expected, result)
		verify(service).updateUserProjectProfileStatusById(currentUser, id, status)
	}

	@Test
	fun `Should createSupportProjectProfile delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val expected: Mono<ProjectProfileModel> = Mono.empty()
		whenever(service.createSupportProjectProfile(currentUser, projectId)).thenReturn(expected)

		// Act
		val result = useCase.createSupportProjectProfile(currentUser, projectId)

		// Assert
		assertSame(expected, result)
		verify(service).createSupportProjectProfile(currentUser, projectId)
	}

	@Test
	fun `Should deleteUserProjectProfileById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<Unit> = Mono.empty()
		whenever(service.deleteUserProjectProfileById(currentUser, id)).thenReturn(expected)

		// Act
		val result = useCase.deleteUserProjectProfileById(currentUser, id)

		// Assert
		assertSame(expected, result)
		verify(service).deleteUserProjectProfileById(currentUser, id)
	}
}
