package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_NOT_FOUND_IN_COMMUNICATION_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IAlertModelRepository
import fr.laucoin.registry.backend.domain.repository.ICommunicationModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.test.ModelExt.movementId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Mono

class CommunicationServiceTest {
    private val repository: ICommunicationModelRepository = mock()
    private val projectService: IProjectService = mock()
    private val movementRepository: IMovementModelRepository = mock()
    private val alertRepository: IAlertModelRepository = mock()
    private val service: ICommunicationService =
        CommunicationService(projectService, repository, movementRepository, alertRepository, 1, 1)

    @Test
    fun `Should findCommunicationsPage call repository findPage`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = CommunicationSearchParamModel()
        whenever(repository.findPage(any(), any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findCommunicationPage(projectId, pageable, params).block()

        // Assert
        verify(repository).findPage(projectId, pageable, params)
    }

    @Test
    fun `Should findCommunicationById call repository findById`() {
        // Arrange
        val communication = CommunicationModel().apply { project = ProjectModel().apply { id = projectId } }
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(communication))

        // Act
        service.findCommunicationById(projectId, uuid, onlyVisible).block()

        // Assert
        verify(repository).findById(projectId, uuid, onlyVisible)
    }

    @Test
    fun `Should findCommunicationById call repository findById throw on empty result`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.findCommunicationById(projectId, uuid, onlyVisible).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
        assertEquals(1, result.args?.size)
        verify(repository).findById(projectId, uuid, onlyVisible)
    }

    @Test
    fun `Should createCommunication check date and call repository create`() {
        // Arrange
        val now = ZonedDateTime.now()
        val movement = MovementModel().apply {
            id = movementId
            type = OUT
            contentType = REGISTERED
            dateTime = now
            visible = true
        }
        val communication = CommunicationModel(dateTime = now, movement = movement).apply {
            project = ProjectModel().apply { id = projectId }
        }
        whenever(projectService.validateDateTime(any(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
        whenever(movementRepository.findById(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(movement))
        whenever(repository.create(any())).thenReturn(Mono.just(communication))

        // Act
        service.createCommunication(currentUser(), communication).block()

        // Assert
        verify(projectService).validateDateTime(
            projectId,
            CustomDateTimeModel(now),
            COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(movementRepository).findById(projectId, movementId, visibilitySearched = null)
        verify(repository).create(communication)
    }

    @Test
    fun `Should createCommunication throw because of not found movement`() {
        // Arrange
        val now = ZonedDateTime.now()
        val movementUuid = UUID.randomUUID()
        val communication = CommunicationModel(dateTime = now, movement = MovementModel().apply { id = movementUuid }).apply {
            project = ProjectModel().apply { id = projectId }
        }
        whenever(projectService.validateDateTime(any(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
        whenever(movementRepository.findById(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.empty())
        whenever(repository.create(any())).thenReturn(Mono.just(communication))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createCommunication(currentUser(), communication).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(COMMUNICATION_MOVEMENT_NOT_FOUND_IN_COMMUNICATION_PROJECT, result.message)
        verify(projectService).validateDateTime(
            projectId,
            CustomDateTimeModel(now),
            COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(movementRepository).findById(projectId, movementUuid, visibilitySearched = null)
        verifyNoInteractions(repository)
    }

    @Test
    fun `Should createCommunication throw because of movement is not visible`() {
        // Arrange
        val now = ZonedDateTime.now()
        val communication = CommunicationModel(dateTime = now, movement = MovementModel().apply { id = movementId }).apply {
            project = ProjectModel().apply { id = projectId }
        }
        whenever(projectService.validateDateTime(any(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
        whenever(movementRepository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(MovementModel().apply {
            dateTime = now
            visible = false
        }))
        whenever(repository.create(any())).thenReturn(Mono.just(communication))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createCommunication(currentUser(), communication).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(COMMUNICATION_MOVEMENT_NOT_VISIBLE, result.message)
        verify(projectService).validateDateTime(
            projectId,
            CustomDateTimeModel(now),
            COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(movementRepository).findById(projectId, movementId, visibilitySearched = null)
        verifyNoInteractions(repository)
    }

    @Test
    fun `Should updateCommunicationById check date, check existing communication, call repository updateCommunication`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val movementUuid = UUID.randomUUID()
        val now = ZonedDateTime.now()
        val communication = CommunicationModel(dateTime = now, movement = MovementModel().apply { id = movementUuid }).apply {
            id = uuid
            project = ProjectModel().apply { id = projectId }
        }
        whenever(projectService.validateDateTime(any(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(communication))
        whenever(movementRepository.findById(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(MovementModel().apply {
            visible = true
        }))
        whenever(repository.update(any())).thenReturn(Mono.just(communication))

        // Act
        service.updateCommunicationById(currentUser(), projectId, uuid, communication).block()

        // Assert
        verify(projectService).validateDateTime(
            projectId,
            CustomDateTimeModel(now),
            COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(repository).findById(projectId, uuid, visibilitySearched = null)
        verify(movementRepository, never()).findById(projectId, movementUuid, visibilitySearched = null)
        verify(repository).update(communication)
    }

    @Test
    fun `Should disableCommunicationById call existing communication and call repository update`() {
        // Arrange
        val communication = CommunicationModel().apply { project = ProjectModel().apply { id = projectId }; visible = true }
        val uuid = UUID.randomUUID()
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(communication))
        whenever(repository.update(any())).thenReturn(Mono.just(communication))

        // Act
        service.disableCommunicationById(currentUser(), projectId, uuid).block()

        // Assert
        verify(repository).findById(projectId, uuid, visibilitySearched = true)
        verify(repository).update(communication.apply { visible = false })
    }

    @Test
    fun `Should enableCommunicationById call existing communication and call repository update`() {
        // Arrange
        val communication = CommunicationModel().apply { project = ProjectModel().apply { id = projectId }; visible = false }
        val uuid = UUID.randomUUID()
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(communication))
        whenever(repository.update(any())).thenReturn(Mono.just(communication))

        // Act
        service.enableCommunicationById(currentUser(), projectId, uuid).block()

        // Assert
        verify(repository).findById(projectId, uuid, visibilitySearched = false)
        verify(repository).update(communication.apply { visible = true })
    }

    @Test
    fun `Should deleteCommunicationById call existing communication, check no movement, and call repository deleteById`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val communication = CommunicationModel().apply { id = uuid; project = ProjectModel().apply { id = projectId } }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(communication))
        whenever(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteCommunicationById(currentUser(), projectId, uuid).block()

        // Assert
        verify(repository).findById(projectId, uuid, visibilitySearched = null)
        verify(repository).deleteById(uuid)
    }
}
