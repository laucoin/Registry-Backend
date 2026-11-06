package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.PROJECT
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.service.core.IProjectProfileService
import fr.laucoin.registry.backend.domain.service.core.IProjectService
import fr.laucoin.registry.backend.domain.service.core.IUserProjectProfileService
import fr.laucoin.registry.backend.domain.service.usecase.IUserProjectProfileUseCase
import java.time.Clock
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class UserProjectProfileUseCaseTest {
	private val service: IUserProjectProfileService = mock()
	private val profileService: IProjectProfileService = mock()
	private val projectService: IProjectService = mock()
	private val rowLockPort: IRowLockPort = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val useCase: IUserProjectProfileUseCase =
		UserProjectProfileUseCase(service, profileService, projectService, rowLockPort, transactionalOperator)

	init {
		whenever(rowLockPort.lock(any(), any())).thenReturn(Mono.just(Unit))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }
	}

	private fun supportProfile(currentUser: CurrentUserModel, projectId: UUID): ProjectProfileModel {
		val profile = ProjectProfileModel().apply {
			startAccess = CustomDateTimeModel.now(Clock.systemUTC())
			endAccess = CustomDateTimeModel.now(Clock.systemUTC()).plusHours(1)
		}
		whenever(service.supportProjectProfile(currentUser, projectId)).thenReturn(profile)
		return profile
	}

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
	fun `Should createSupportProjectProfile persist the support profile and return the project with its profile`() {
		// Arrange
		val currentUser = CurrentUserModel().apply { id = UUID.randomUUID() }
		val projectId = UUID.randomUUID()
		val profile = supportProfile(currentUser, projectId)
		val project = ProjectModel().apply { id = projectId }
		whenever(profileService.validateNoProfileConflict(eq(projectId), eq(listOf(currentUser.id!!)), isNull(), any(), any()))
			.thenReturn(Mono.just(listOf(currentUser.id!!)))
		whenever(service.createSupportProjectProfile(currentUser, profile)).thenReturn(Mono.just(profile))
		whenever(projectService.findProjectWithProfileById(currentUser, projectId)).thenReturn(Mono.just(project))

		// Act
		val result = useCase.createSupportProjectProfile(currentUser, projectId)

		// Assert
		StepVerifier.create(result).expectNext(project).verifyComplete()
		verify(rowLockPort).lock(PROJECT, projectId)
	}

	@Test
	fun `Should createSupportProjectProfile persist nothing when another profile overlaps the support range`() {
		// Arrange
		val currentUser = CurrentUserModel().apply { id = UUID.randomUUID() }
		val projectId = UUID.randomUUID()
		supportProfile(currentUser, projectId)
		val conflict = RegistryException(CONFLICT, PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE)
		whenever(profileService.validateNoProfileConflict(any(), any(), isNull(), any(), any())).thenReturn(Mono.error(conflict))

		// Act
		val result = useCase.createSupportProjectProfile(currentUser, projectId)

		// Assert
		StepVerifier.create(result).expectErrorMatches { it === conflict }.verify()
		verify(service, never()).createSupportProjectProfile(any(), any())
		verify(projectService, never()).findProjectWithProfileById(any(), any())
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
