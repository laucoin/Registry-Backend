package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IActivityModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Mono

class ActivityServiceTest {
    private val repository: IActivityModelRepository = mock()
    private val projectService: IProjectService = mock()
    private val movementRepository: IMovementModelRepository = mock()
    private val service: IActivityService = ActivityService(projectService, repository, movementRepository)

    @Test
    fun `Should findActivitiesPage call repository findPage`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = ActivitySearchParamModel()
        whenever(repository.findPage(any(), any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findActivitiesPage(projectId, pageable, params).block()

        // Assert
        verify(repository).findPage(projectId, pageable, params)
    }

    @Test
    fun `Should findActivityById call repository findById`() {
        // Arrange
        val activity = ActivityModel().apply { project = ProjectModel().apply { id = projectId } }
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(activity))

        // Act
        service.findActivityById(projectId, uuid, onlyVisible).block()

        // Assert
        verify(repository).findById(projectId, uuid, onlyVisible)
    }

    @Test
    fun `Should findActivityById call repository findById throw on empty result`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.findActivityById(projectId, uuid, onlyVisible).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(1, result.args?.size)
        verify(repository).findById(projectId, uuid, onlyVisible)
    }

    @Test
    fun `Should findActivityMovementsPage call repository findPageByActivityId`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val pageable = PageableModel(0, 10)
        val params = MovementSearchParamModel(typeSearched = MovementTypeEnum.IN)
        whenever(movementRepository.findPageByActivityId(any(), any(), any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findActivityMovementsPage(projectId, uuid, pageable, params).block()

        // Assert
        verify(movementRepository).findPageByActivityId(projectId, uuid, pageable, params)
    }

    @Test
    fun `Should createActivity check date and call repository create`() {
        // Arrange
        val activity = ActivityModel().apply { project = ProjectModel().apply { id = projectId } }
        whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
        whenever(repository.create(any())).thenReturn(Mono.just(activity))

        // Act
        service.createActivity(currentUser(), activity).block()

        // Assert
        verify(projectService).validateDateTimes(
            projectId,
            start = null,
            end = null,
            ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(repository).create(activity)
    }

    @Test
    fun `Should updateActivityById check date, check existing activity, call repository updateActivity`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val activity = ActivityModel().apply { id = uuid; project = ProjectModel().apply { id = projectId } }
        whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(activity))
        whenever(repository.update(any())).thenReturn(Mono.just(activity))

        // Act
        service.updateActivityById(currentUser(), projectId, uuid, activity).block()

        // Assert
        verify(projectService).validateDateTimes(
            projectId,
            start = null,
            end = null,
            ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(repository).findById(projectId, uuid, visibilitySearched = null)
        verify(repository).update(activity)
    }

    @Test
    fun `Should disableActivityById call existing activity and call repository update`() {
        // Arrange
        val activity = ActivityModel().apply { project = ProjectModel().apply { id = projectId }; visible = true }
        val uuid = UUID.randomUUID()
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(activity))
        whenever(repository.update(any())).thenReturn(Mono.just(activity))

        // Act
        service.disableActivityById(currentUser(), projectId, uuid).block()

        // Assert
        verify(repository).findById(projectId, uuid, visibilitySearched = true)
        verify(repository).update(activity.apply { visible = false })
    }

    @Test
    fun `Should enableActivityById call existing activity and call repository update`() {
        // Arrange
        val activity = ActivityModel().apply { project = ProjectModel().apply { id = projectId }; visible = false }
        val uuid = UUID.randomUUID()
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(activity))
        whenever(repository.update(any())).thenReturn(Mono.just(activity))

        // Act
        service.enableActivityById(currentUser(), projectId, uuid).block()

        // Assert
        verify(repository).findById(projectId, uuid, visibilitySearched = false)
        verify(repository).update(activity.apply { visible = true })
    }

    @Test
    fun `Should deleteActivityById call existing activity, check no movement, and call repository deleteById`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val activity = ActivityModel().apply { id = uuid; project = ProjectModel().apply { id = projectId } }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(activity))
        whenever(movementRepository.countAllByActivityId(any(), any())).thenReturn(Mono.just(0))
        whenever(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteActivityById(currentUser(), projectId, uuid).block()

        // Assert
        verify(repository).findById(projectId, uuid, visibilitySearched = null)
        verify(movementRepository).countAllByActivityId(projectId, uuid)
        verify(repository).deleteById(uuid)
    }

    @Test
    fun `Should deleteActivityById call existing activity, throw if movements are linked`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val activity = ActivityModel().apply { id = uuid; project = ProjectModel().apply { id = projectId } }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(activity))
        whenever(movementRepository.countAllByActivityId(any(), any())).thenReturn(Mono.just(1))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.deleteActivityById(currentUser(), projectId, uuid).block()
        }) as RegistryException

        // Assert
        assertEquals(FORBIDDEN, result.status)
        assertEquals(ACTIVITY_DELETE_HAS_MOVEMENT, result.message)
        verify(repository).findById(projectId, uuid, visibilitySearched = null)
        verify(movementRepository).countAllByActivityId(projectId, uuid)
        verify(repository, never()).deleteById(any())
    }
}
