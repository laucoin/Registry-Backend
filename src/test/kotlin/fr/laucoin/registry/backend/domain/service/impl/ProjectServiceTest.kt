package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_DATE_CONFLICT_WITH_ELEMENTS
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IProjectModelRepository
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZoneOffset
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Mono

class ProjectServiceTest {
    private val repository: IProjectModelRepository = mock()
    private val projectProfileService: IUserProjectProfileService = mock()
    private val transactionalOperator: TransactionalOperator = mock()
    private val roleService: IRoleService = mock()
    private val service: IProjectService = ProjectService(
        repository,
        projectProfileService,
        transactionalOperator,
        roleService
    )

    companion object {
        @JvmStatic
        fun `Should validateDateTime call repository findById and validate the request date is in project range`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null, null, null),
                Arguments.of(
                    null, null,
                    CustomDateTimeModel(LocalDate.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MAX, OffsetTime.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.MIN),
                    CustomDateTimeModel(LocalDate.MAX, OffsetTime.MAX),
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.MIN),
                    CustomDateTimeModel(LocalDate.MAX, OffsetTime.MAX),
                    CustomDateTimeModel(LocalDate.MAX, OffsetTime.MAX),
                ),
            )
        }

        @JvmStatic
        fun `Should validateDateTime call repository findById and throw on invalid date`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.of(0, 0, 0, 1, ZoneOffset.of("Z"))),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX, OffsetTime.of(23, 59, 59, 0, ZoneOffset.of("Z"))),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
            )
        }

        @JvmStatic
        fun `Should validateDateTimes call repository findById and validate the request dates are in project range`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null, null, null, null),
                Arguments.of(
                    null, null,
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.MIN),
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.MIN),
                    CustomDateTimeModel(LocalDate.MAX, OffsetTime.MAX),
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.MIN),
                    CustomDateTimeModel(LocalDate.MAX, OffsetTime.MAX),
                ),
            )
        }

        @JvmStatic
        fun `Should validateDateTimes call repository findById and throw on invalid dates`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MIN),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.of(0, 0, 0, 1, ZoneOffset.of("Z"))),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX, OffsetTime.of(23, 59, 59, 0, ZoneOffset.of("Z"))),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                ),
            )
        }

        @JvmStatic
        fun `Should updateProjectById call repository findById, validDateTime and update`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null, null, null, null, 0),
                Arguments.of(
                    null, null,
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    1,
                ),
                Arguments.of(
                    null,
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                    null,
                    1,
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    null,
                    null,
                    CustomDateTimeModel(LocalDate.MAX),
                    1,
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    0,
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.MIN),
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.MAX),
                    1,
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.MIN),
                    CustomDateTimeModel(LocalDate.MAX, OffsetTime.MAX),
                    CustomDateTimeModel(LocalDate.MIN, OffsetTime.MIN),
                    CustomDateTimeModel(LocalDate.MAX, OffsetTime.MAX),
                    0,
                ),
                Arguments.of(
                    CustomDateTimeModel(LocalDate.EPOCH),
                    CustomDateTimeModel(LocalDate.EPOCH),
                    CustomDateTimeModel(LocalDate.MIN),
                    CustomDateTimeModel(LocalDate.MAX),
                    0,
                ),
            )
        }
    }

    @Test
    fun `Should findProjectsPage call repository findPage`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = ProjectSearchParamModel()
        val withProfile = false
        whenever(roleService.getAuthoritiesByUserRole(anyOrNull())).thenReturn(listOf(REGISTRY_PROJECT_R))
        whenever(repository.findPage(any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findProjectsPage(currentUser(REGISTRY_PROJECT_R), pageable, withProfile, params).block()

        // Assert
        verify(repository).findPage(pageable, params)
        verify(repository, never()).findPage(any(), any(), any())
    }

    @Test
    fun `Should findProjectsPage call repository findPage for currentUserProject`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val withProfile = true
        val params = ProjectSearchParamModel()
        whenever(roleService.getAuthoritiesByUserRole(anyOrNull())).thenReturn(emptyList())
        whenever(roleService.getProjectIdsFromCurrentUserProfiles(any())).thenReturn(emptyList())
        whenever(repository.findPage(any(), any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findProjectsPage(currentUser(), pageable, withProfile, params).block()

        // Assert
        verify(repository).findPage(emptyList(), pageable, params)
        verify(repository, never()).findPage(any(), any())
    }

    @Test
    fun `Should findProjectById call repository findById`() {
        // Arrange
        val project = ProjectModel()
        val onlyVisible = true
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

        // Act
        service.findProjectById(projectId, onlyVisible).block()

        // Assert
        verify(repository).findById(projectId, onlyVisible)
    }

    @Test
    fun `Should findActivityById call repository findById throw on empty result`() {
        // Arrange
        val onlyVisible = true
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.empty())

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.findProjectById(projectId, onlyVisible).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(1, result.args?.size)
        verify(repository).findById(projectId, onlyVisible)
    }

    @Test
    fun `Should availableProjectOptions not throw`() {
        // Act
        assertDoesNotThrow {
            service.availableProjectOptions().blockFirst()
        }

        // Assert
        verifyNoInteractions(repository)
        verifyNoInteractions(projectProfileService)
        verifyNoInteractions(transactionalOperator)
        verifyNoInteractions(roleService)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validateDateTime call repository findById and validate the request date is in project range`(
        projectBeginDateTime: CustomDateTimeModel?,
        projectEndDateTime: CustomDateTimeModel?,
        dateTime: CustomDateTimeModel?,
    ) {
        // Arrange
        val project = ProjectModel().apply { begin = projectBeginDateTime; end = projectEndDateTime }
        val errorMessage = "ERROR_MESSAGE"
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

        // Act
        val result = service.validateDateTime(projectId, dateTime, errorMessage).block()

        // Assert
        assertEquals(projectId, result)
        verify(repository).findById(projectId, visibilitySearched = null)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validateDateTime call repository findById and throw on invalid date`(
        projectBeginDateTime: CustomDateTimeModel?,
        projectEndDateTime: CustomDateTimeModel?,
        dateTime: CustomDateTimeModel?,
    ) {
        // Arrange
        val project = ProjectModel().apply { begin = projectBeginDateTime; end = projectEndDateTime }
        val errorMessage = "ERROR_MESSAGE"
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.validateDateTime(projectId, dateTime, errorMessage).block()
        }) as RegistryException

        // Assert
        assertEquals(UNPROCESSABLE_ENTITY, result.status)
        assertEquals(errorMessage, result.message)
        assertEquals(3, result.args?.size)
        verify(repository).findById(projectId, visibilitySearched = null)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validateDateTimes call repository findById and validate the request dates are in project range`(
        projectBeginDateTime: CustomDateTimeModel?,
        projectEndDateTime: CustomDateTimeModel?,
        startDateTime: CustomDateTimeModel?,
        endDateTime: CustomDateTimeModel?,
    ) {
        // Arrange
        val project = ProjectModel().apply { begin = projectBeginDateTime; end = projectEndDateTime }
        val errorMessage = "ERROR_MESSAGE"
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

        // Act
        val result = service.validateDateTimes(projectId, startDateTime, endDateTime, errorMessage).block()

        // Assert
        assertEquals(projectId, result)
        verify(repository).findById(projectId, visibilitySearched = null)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validateDateTimes call repository findById and throw on invalid dates`(
        projectBeginDateTime: CustomDateTimeModel?,
        projectEndDateTime: CustomDateTimeModel?,
        startDateTime: CustomDateTimeModel?,
        endDateTime: CustomDateTimeModel?,
    ) {
        // Arrange
        val project = ProjectModel().apply { begin = projectBeginDateTime; end = projectEndDateTime }
        val errorMessage = "ERROR_MESSAGE"
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.validateDateTimes(projectId, startDateTime, endDateTime, errorMessage).block()
        }) as RegistryException

        // Assert
        assertEquals(UNPROCESSABLE_ENTITY, result.status)
        assertEquals(errorMessage, result.message)
        assertEquals(4, result.args?.size)
        verify(repository).findById(projectId, visibilitySearched = null)
    }

    @Test
    fun `Should createProject call profile service createUserProjectProfileFromProject and repository create`() {
        // Arrange
        val project = ProjectModel()
        whenever(projectProfileService.createUserProjectProfileFromProject(any(), any())).thenReturn(Mono.just(ProjectProfileModel()))
        whenever(repository.create(any())).thenReturn(Mono.just(project))
        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

        // Act
        service.createProject(currentUser(), project).block()

        // Assert
        verify(projectProfileService).createUserProjectProfileFromProject(currentUser(), project)
        verify(repository).create(project)
        verify(transactionalOperator).transactional(any<Mono<*>>())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateProjectById call repository findById, validDateTime and update`(
        projectBeginDateTime: CustomDateTimeModel?,
        projectEndDateTime: CustomDateTimeModel?,
        newProjectBeginDateTime: CustomDateTimeModel?,
        newProjectEndDateTime: CustomDateTimeModel?,
        expectedVerificationCall: Int,
    ) {
        // Arrange
        val projectToUpdate = ProjectModel().apply { id = projectId; begin = projectBeginDateTime; end = projectEndDateTime }
        val projectUpdated = ProjectModel().apply { id = projectId; begin = newProjectBeginDateTime; end = newProjectEndDateTime }
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(projectToUpdate))
        whenever(repository.validDateTime(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(true))
        whenever(repository.update(any())).thenReturn(Mono.just(projectUpdated))

        // Act
        service.updateProjectById(currentUser(), projectId, projectUpdated).block()

        // Assert
        verify(repository).findById(projectId, visibilitySearched = null)
        verify(repository, times(expectedVerificationCall)).validDateTime(
            projectId,
            newProjectBeginDateTime?.toZonedDateTime(OffsetTime.MIN),
            newProjectEndDateTime?.toZonedDateTime(OffsetTime.MAX),
        )
        verify(repository).update(any())
    }

    @Test
    fun `Should updateProjectById call repository findById, validDateTime and throw due to date conflict`() {
        // Arrange
        val projectToUpdate = ProjectModel().apply { id = projectId }
        val dateTime = CustomDateTimeModel(LocalDate.EPOCH)
        val projectUpdated = ProjectModel().apply { id = projectId; begin = dateTime; end = dateTime }
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(projectToUpdate))
        whenever(repository.validDateTime(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(false))
        whenever(repository.update(any())).thenReturn(Mono.just(projectUpdated))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.updateProjectById(currentUser(), projectId, projectUpdated).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(PROJECT_DATE_CONFLICT_WITH_ELEMENTS, result.message)
        assertNull(result.args)
        verify(repository).findById(projectId, visibilitySearched = null)
        verify(repository).validDateTime(
            projectId,
            dateTime.toZonedDateTime(OffsetTime.MIN),
            dateTime.toZonedDateTime(OffsetTime.MAX),
        )
        verify(repository, never()).update(any())
    }

    @Test
    fun `Should disableProjectById call existing project and call repository update`() {
        // Arrange
        val project = ProjectModel().apply { visible = true }
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(project))
        whenever(repository.update(any())).thenReturn(Mono.just(project))

        // Act
        service.disableProjectById(currentUser(), projectId).block()

        // Assert
        verify(repository).findById(projectId, visibilitySearched = true)
        verify(repository).update(project.apply { visible = false })
    }

    @Test
    fun `Should enableProjectById call existing project and call repository update`() {
        // Arrange
        val project = ProjectModel().apply { visible = false }
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(project))
        whenever(repository.update(any())).thenReturn(Mono.just(project))

        // Act
        service.enableProjectById(currentUser(), projectId).block()

        // Assert
        verify(repository).findById(projectId, visibilitySearched = false)
        verify(repository).update(project.apply { visible = true })
    }

    @Test
    fun `Should deleteProjectById call existing project and call repository deleteById`() {
        // Arrange
        val project = ProjectModel()
        whenever(repository.findById(any(), anyOrNull())).thenReturn(Mono.just(project))
        whenever(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteProjectById(projectId).block()

        // Assert
        verify(repository).findById(projectId, visibilitySearched = null)
        verify(repository).deleteById(projectId)
    }
}
