package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IPreferencesModelRepository
import fr.laucoin.registry.backend.domain.repository.IProjectProfileModelRepository
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserProjectProfileServiceTest {
    private val repository: IProjectProfileModelRepository = mock()
    private val roleService: IRoleService = mock()
    private val preferencesRepository: IPreferencesModelRepository = mock()
    private val transactionalOperator: TransactionalOperator = mock()
    private val service: IUserProjectProfileService =
        UserProjectProfileService(repository, roleService, preferencesRepository, transactionalOperator)

    companion object {
        @JvmStatic
        fun `Should validateNotLastProjectRoleLevel0 return the given Object`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null, null),
                Arguments.of(projectId, ProjectProfileRoleCountModel(level0 = 0, project = ProjectModel().apply { id = projectId })),
            )
        }

        @JvmStatic
        fun `Should validateNotLastProjectRoleLevel0 throw RegistryException`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(projectId, ProjectProfileRoleCountModel(level0 = 0)),
                Arguments.of(
                    projectId,
                    ProjectProfileRoleCountModel(level0 = 0, project = ProjectModel().apply { id = UUID.randomUUID() })
                ),
                Arguments.of(null, ProjectProfileRoleCountModel(level0 = 0)),
                Arguments.of(
                    null,
                    ProjectProfileRoleCountModel(level0 = 0, project = ProjectModel().apply { id = UUID.randomUUID() })
                ),
            )
        }

        @JvmStatic
        fun `Should createUserProjectProfileFromProject create and return a Profile`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(PreferencesModel(), 1),
                Arguments.of(PreferencesModel().apply { selectedProfile = ProjectProfileModel() }, 0),
            )
        }
    }

    @Test
    fun `Should findProjectProfilesPage call repository findProjectProfilesPageByUserId`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = ProjectProfileSearchParamModel(statusSearched = ACCEPTED)
        whenever(repository.findProjectProfilesPageByUserId(any(), any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findProjectProfilesPage(projectId, pageable, params).block()

        // Assert
        verify(repository).findProjectProfilesPageByUserId(projectId, pageable, params)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validateNotLastProjectRoleLevel0 return the given Object`(
        projectId: UUID?,
        profileCount: ProjectProfileRoleCountModel?
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val user = UserModel()
        whenever(
            repository.findLevel0ProjectProfileRoleByUserId(
                any(),
                any()
            )
        ).thenReturn(if (Objects.nonNull(profileCount)) Flux.just(profileCount !!) else Flux.empty())

        // Act
        val result = service.validateNotLastProjectRoleLevel0(uuid, projectId = projectId, user, error = "ERROR_MESSAGE").block()

        // Assert
        assertEquals(user, result)
        verify(repository).findLevel0ProjectProfileRoleByUserId(uuid, visibilitySearched = true)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validateNotLastProjectRoleLevel0 throw RegistryException`(
        projectId: UUID?,
        profileCount: ProjectProfileRoleCountModel
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val errorMessage = "ERROR_MESSAGE"
        whenever(
            repository.findLevel0ProjectProfileRoleByUserId(
                any(),
                any()
            )
        ).thenReturn(Flux.just(profileCount))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.validateNotLastProjectRoleLevel0(uuid, projectId = projectId, UserModel(), errorMessage).block()
        }) as RegistryException

        // Assert
        assertEquals(FORBIDDEN, result.status)
        assertEquals(errorMessage, result.message)

        verify(repository).findLevel0ProjectProfileRoleByUserId(uuid, visibilitySearched = true)
    }

    @Test
    fun `Should createSupportProjectProfile call repository findUserIdsWithProjectProfile and create`() {
        // Arrange
        val profileRole = "PROJECT_ROLE"
        val profile = ProjectProfileModel().apply {
            user = currentUser()
            project = ProjectModel().apply { id = projectId }
            role = profileRole
        }
        whenever(roleService.getLevel0RoleFromProjectRoles()).thenReturn(profileRole)
        whenever(
            repository.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
                any(),
                any(),
                anyOrNull(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.empty())
        whenever(repository.create(any())).thenReturn(Mono.just(profile))

        // Act
        val result = service.createSupportProjectProfile(currentUser(), projectId).block()

        // Assert
        assertEquals(profile, result)
        verify(repository).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
            eq(projectId),
            eq(listOf(currentUser().id !!)),
            eq(null),
            eq(listOf(ACCEPTED, INVITED)),
            any(),
            any(),
        )
        verify(repository).create(any())
    }

    @Test
    fun `Should createSupportProjectProfile call repository findUserIdsWithProjectProfile and throw because profile duplicated`() {
        // Arrange
        val profileRole = "PROJECT_ROLE"
        val profile = ProjectProfileModel().apply {
            user = currentUser()
            project = ProjectModel().apply { id = projectId }
            role = profileRole
        }
        whenever(roleService.getLevel0RoleFromProjectRoles()).thenReturn(profileRole)
        whenever(
            repository.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
                any(),
                any(),
                anyOrNull(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just(currentUser().id !!))
        whenever(repository.create(any())).thenReturn(Mono.just(profile))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createSupportProjectProfile(currentUser(), projectId).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE, result.message)
        verify(repository).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
            eq(projectId),
            eq(listOf(currentUser().id !!)),
            eq(null),
            eq(listOf(ACCEPTED, INVITED)),
            any(),
            any(),
        )
        verify(repository, never()).create(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should createUserProjectProfileFromProject create and return a Profile`(
        userPreferences: PreferencesModel,
        expectedCallToUpdatePreferences: Int
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = ProjectProfileModel().apply {
            id = uuid
            project = ProjectModel().apply { id = projectId }
        }
        whenever(repository.create(any())).thenReturn(Mono.just(profile))
        whenever(preferencesRepository.findByUserId(any(), anyOrNull())).thenReturn(Mono.just(userPreferences))
        whenever(preferencesRepository.save(any())).thenReturn(Mono.just(userPreferences))
        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

        // Act
        service.createUserProjectProfileFromProject(currentUser(), ProjectModel()).block()

        // Assert
        verify(repository).create(any())
        verify(preferencesRepository).findByUserId(currentUser().id !!, visibilitySearched = null)
        verify(preferencesRepository, times(expectedCallToUpdatePreferences)).save(any())
        verify(transactionalOperator).transactional(any<Mono<*>>())
    }

    @Test
    fun `Should updateUserProjectProfileStatusById update Project Profile status is INVITED`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = ProjectProfileModel().apply {
            id = uuid
            project = ProjectModel().apply { id = projectId }
            status = INVITED
        }
        whenever(repository.findProjectProfileByUserIdAndId(any(), any(), any())).thenReturn(Mono.just(profile))
        whenever(repository.update(any())).thenReturn(Mono.just(profile))

        // Act
        service.updateUserProjectProfileStatusById(currentUser(), uuid, ACCEPTED).block()

        // Assert
        verify(repository).update(any())
        verify(repository).findProjectProfileByUserIdAndId(currentUser().id !!, uuid, visibilitySearched = true)
    }

    @Test
    fun `Should updateUserProjectProfileStatusById throw RegistryException`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = ProjectProfileModel().apply {
            id = uuid
            project = ProjectModel().apply { id = projectId }
            status = ACCEPTED
        }
        whenever(repository.findProjectProfileByUserIdAndId(any(), any(), any())).thenReturn(Mono.just(profile))
        whenever(repository.update(any())).thenReturn(Mono.just(profile))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.updateUserProjectProfileStatusById(currentUser(), uuid, ACCEPTED).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(uuid.toString(), result.args?.first())

        verify(repository, never()).update(any())
        verify(repository).findProjectProfileByUserIdAndId(currentUser().id !!, uuid, visibilitySearched = true)
    }

    @Test
    fun `Should deleteUserProjectProfileById delete Profile`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val profile = ProjectProfileModel().apply {
            id = uuid
            user = currentUser()
            project = ProjectModel().apply { id = projectId }
        }
        whenever(
            repository.findLevel0ProjectProfileRoleByUserId(
                any(),
                any()
            )
        ).thenReturn(Flux.empty())
        whenever(repository.findProjectProfileByUserIdAndId(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))
        whenever(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteUserProjectProfileById(currentUser(), uuid).block()

        // Assert
        verify(repository).findProjectProfileByUserIdAndId(currentUser().id !!, uuid, visibilitySearched = null)
        verify(repository).deleteById(uuid)
    }
}
