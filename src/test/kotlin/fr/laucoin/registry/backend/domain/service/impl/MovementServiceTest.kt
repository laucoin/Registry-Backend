package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IActivityModelRepository
import fr.laucoin.registry.backend.domain.repository.ICommunicationModelRepository
import fr.laucoin.registry.backend.domain.repository.IGroupModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.repository.IVehicleModelRepository
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.ZonedDateTime
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
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
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class MovementServiceTest {
    private val repository: IMovementModelRepository = mock()
    private val projectService: IProjectService = mock()
    private val participantRepository: IParticipantModelRepository = mock()
    private val vehicleRepository: IVehicleModelRepository = mock()
    private val activityRepository: IActivityModelRepository = mock()
    private val communicationRepository: ICommunicationModelRepository = mock()
    private val groupRepository: IGroupModelRepository = mock()
    private val transactionalOperator: TransactionalOperator = mock()
    private val maxParticipants: Int = 1
    private val maxGroups: Int = 1
    private val maxVehicles: Int = 1
    private val maxActivities: Int = 1
    private val service: IMovementService = MovementService(
        projectService,
        repository,
        participantRepository,
        vehicleRepository,
        activityRepository,
        communicationRepository,
        groupRepository,
        transactionalOperator,
        maxParticipants,
        maxGroups,
        maxVehicles,
        maxActivities
    )

    companion object {
        @JvmStatic
        fun `Should createMovement check date, validate activity, validate participants, validate vehicle and call repository create`(): Stream<Arguments> {
            val activityId = UUID.randomUUID()
            val movementActivity = ActivityModel().apply { id = activityId; visible = true }
            val participantId1 = UUID.randomUUID()
            val movementParticipant1 = ParticipantModel().apply { id = participantId1; visible = true; purged = false }
            val participantId2 = UUID.randomUUID()
            val movementParticipant2 = ParticipantModel().apply { id = participantId2; visible = true; purged = false }
            val vehicleId = UUID.randomUUID()
            val movementVehicle = VehicleModel().apply { id = vehicleId; visible = true }

            return Stream.of(
                Arguments.of(
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    movementActivity,
                    1,
                    1,
                ),
                Arguments.of(
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1 },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    null,
                    0,
                    0,
                )
            )
        }

        @JvmStatic
        fun `Should createMovement check date, validate activity, check participants and throw because of participant is not visible`(): Stream<Arguments> {
            val participantId1 = UUID.randomUUID()
            val movementParticipant1 = ParticipantModel().apply { id = participantId1 }
            val participantId2 = UUID.randomUUID()
            val movementParticipant2 = ParticipantModel().apply { id = participantId2 }

            return Stream.of(
                Arguments.of(
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1 },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    listOf(
                        ParticipantModel().apply { id = UUID.randomUUID(); visible = true; purged = false },
                        ParticipantModel().apply { id = UUID.randomUUID(); visible = false; purged = false },
                    ),
                ),
                Arguments.of(
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1 },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    listOf(
                        ParticipantModel().apply { id = UUID.randomUUID(); visible = true; purged = false },
                        ParticipantModel().apply { id = UUID.randomUUID(); visible = true; purged = true },
                    ),
                )
            )
        }

        @JvmStatic
        fun `Should updateMovementById check date, get existing movement, validate activity, validate participants, validate vehicle and call repository update`(): Stream<Arguments> {
            val activityId = UUID.randomUUID()
            val movementActivity = ActivityModel().apply { id = activityId; visible = true }
            val participantId1 = UUID.randomUUID()
            val movementParticipant1 = ParticipantModel().apply { id = participantId1; visible = true; purged = false }
            val participantId2 = UUID.randomUUID()
            val movementParticipant2 = ParticipantModel().apply { id = participantId2; visible = true; purged = false }
            val vehicleId = UUID.randomUUID()
            val movementVehicle = VehicleModel().apply { id = vehicleId; visible = true }

            return Stream.of(
                Arguments.of(
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    emptyList<ParticipantModel>(),
                    emptyList<VehicleModel>(),
                    movementActivity,
                    movementActivity,
                    0,
                    0,
                    0,
                ),
                Arguments.of(
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1 },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    listOf(movementParticipant1),
                    emptyList<VehicleModel>(),
                    movementActivity,
                    movementActivity,
                    0,
                    1,
                    0,
                ),
                Arguments.of(
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1 },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    listOf(movementParticipant1),
                    listOf(movementVehicle),
                    movementActivity,
                    movementActivity,
                    0,
                    1,
                    1,
                ),
                Arguments.of(
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    emptyList<ParticipantModel>(),
                    emptyList<VehicleModel>(),
                    null,
                    movementActivity,
                    1,
                    0,
                    0,
                ),
                Arguments.of(
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    listOf(
                        MovementContentModel().apply { participant = movementParticipant1; vehicle = movementVehicle },
                        MovementContentModel().apply { participant = movementParticipant2 },
                    ),
                    emptyList<ParticipantModel>(),
                    emptyList<VehicleModel>(),
                    movementActivity,
                    null,
                    0,
                    0,
                    0,
                ),
            )
        }
    }

    @Test
    fun `Should findMovementsPage call repository findPage`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = MovementSearchParamModel(typeSearched = IN)
        whenever(repository.findPage(any(), any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findMovementsPage(projectId, pageable, params).block()

        // Assert
        verify(repository).findPage(projectId, pageable, params)
    }

    @Test
    fun `Should findMovementById call repository findById`() {
        // Arrange
        val movement = MovementModel(contentType = REGISTERED).apply { project = ProjectModel().apply { id = projectId } }
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))

        // Act
        service.findMovementById(projectId, uuid, onlyVisible).block()

        // Assert
        verify(repository).findById(projectId, uuid, onlyVisible)
    }

    @Test
    fun `Should findMovementsContent call repository findContent`() {
        // Arrange
        val ids = listOf(UUID.randomUUID(), UUID.randomUUID())
        whenever(repository.findContent(any(), any())).thenReturn(Flux.empty())

        // Act
        service.findMovementsContent(projectId, ids).blockFirst()

        // Assert
        verify(repository).findContent(projectId, ids)
    }

    @Test
    fun `Should searchParticipantsAndGroups call repository findWithLimit and findWithLimit`() {
        // Arrange
        val textSearched = "test"
        val typeSearched = REGISTERED
        val uuid = UUID.randomUUID()
        val group = GroupModel().apply { id = uuid }
        val member = ParticipantModel()
        whenever(participantRepository.findWithLimit(any(), any(), any())).thenReturn(Flux.empty())
        whenever(groupRepository.findWithLimit(any(), any(), any())).thenReturn(Flux.just(group))
        whenever(groupRepository.findContent(any(), any(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(Pair(uuid, listOf(member))))

        // Act
        val result = service.searchParticipantsAndGroupsByText(projectId, typeSearched, textSearched).block()

        // Assert
        assertEquals(0, result?.t1?.size)
        assertEquals(1, result?.t2?.size)
        assertEquals(1, result?.t2?.first()?.members?.size)
        verify(participantRepository).findWithLimit(
            maxParticipants,
            projectId,
            ParticipantSearchParamModel(isMajor = null, typeSearched, visibilitySearched = true, presenceSearched = true).apply {
                this.textSearched = textSearched
            }
        )
        verify(groupRepository).findWithLimit(
            maxGroups,
            projectId,
            GroupSearchParamModel(textSearched, visibilitySearched = true, presenceSearched = true)
        )
        verify(groupRepository).findContent(projectId, listOf(uuid), visibilitySearched = true, availabilitySearched = true)
    }

    @Test
    fun `Should searchVehicles call repository findWithLimit`() {
        // Arrange
        val textSearched = "test"
        whenever(vehicleRepository.findWithLimit(any(), any(), any())).thenReturn(Flux.empty())

        // Act
        service.searchVehiclesByText(projectId, textSearched).blockFirst()

        // Assert
        verify(vehicleRepository).findWithLimit(
            maxVehicles,
            projectId,
            VehicleSearchParamModel(visibilitySearched = true, availabilitySearched = true).apply { this.textSearched = textSearched },
        )
    }

    @Test
    fun `Should searchActivities call repository findWithLimit`() {
        // Arrange
        val textSearched = "test"
        val typeSearched = REGISTERED
        whenever(activityRepository.findWithLimit(any(), any(), any())).thenReturn(Flux.empty())

        // Act
        service.searchActivitiesByText(projectId, typeSearched, textSearched).blockFirst()

        // Assert
        verify(activityRepository).findWithLimit(
            maxActivities,
            projectId,
            ActivitySearchParamModel(textSearched, visibilitySearched = true, availabilitySearched = true),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should createMovement check date, validate activity, validate participants, validate vehicle and call repository create`(
        movementContent: List<MovementContentModel>,
        movementActivity: ActivityModel?,
        expectedCallToActivity: Int,
        expectedCallToVehicle: Int,
    ) {
        // Arrange
        val movementDateTime = ZonedDateTime.now()

        val movement = MovementModel(contentType = REGISTERED).apply {
            id = UUID.randomUUID()
            project = ProjectModel().apply { id = projectId }
            dateTime = movementDateTime
            activity = movementActivity
            content = movementContent
        }

        whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
        whenever(activityRepository.findById(any(), any(), anyOrNull())).thenReturn(
            Mono.just(movementActivity ?: ActivityModel())
        )
        whenever(participantRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(
            Flux.just(*movementContent.map { it.participant }.toTypedArray())
        )
        whenever(vehicleRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(
            Flux.just(*movementContent.mapNotNull { it.vehicle }.toTypedArray())
        )
        whenever(repository.create(any())).thenReturn(Mono.just(movement))
        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

        // Act
        service.createMovement(currentUser(), movement).block()

        // Assert
        verify(projectService).validateDateTime(
            projectId,
            CustomDateTimeModel(movementDateTime),
            MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(activityRepository, times(expectedCallToActivity)).findById(
            projectId,
            movementActivity?.id ?: UUID.randomUUID(),
            visibilitySearched = null
        )
        verify(participantRepository).findAllByIds(
            projectId,
            movementContent.mapNotNull { it.participant?.id },
            visibilitySearched = null,
        )
        verify(vehicleRepository, times(expectedCallToVehicle)).findAllByIds(
            projectId,
            movementContent.mapNotNull { it.vehicle?.id },
            visibilitySearched = null
        )
        verify(repository).create(movement)
        verify(transactionalOperator).transactional(any<Mono<*>>())
    }

    @Test
    fun `Should createMovement check date, validate activity and throw because of not found activity`() {
        // Arrange
        val movementDateTime = ZonedDateTime.now()

        val activityId = UUID.randomUUID()
        val movementActivity = ActivityModel().apply { id = activityId }

        val movement = MovementModel(contentType = REGISTERED).apply {
            id = UUID.randomUUID()
            project = ProjectModel().apply { id = projectId }
            dateTime = movementDateTime
            activity = movementActivity
        }

        whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
        whenever(activityRepository.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())
        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createMovement(currentUser(), movement).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT, result.message)
        verify(projectService).validateDateTime(
            projectId,
            CustomDateTimeModel(movementDateTime),
            MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(activityRepository).findById(
            projectId,
            movementActivity.id !!,
            visibilitySearched = null
        )
        verify(participantRepository, never()).findAllByIds(any(), any(), anyOrNull())
        verify(vehicleRepository, never()).findAllByIds(any(), any(), anyOrNull())
        verify(repository, never()).create(any())
        verify(transactionalOperator).transactional(any<Mono<*>>())
    }

    @Test
    fun `Should createMovement check date, validate activity and throw because of activity is not visible`() {
        // Arrange
        val movementDateTime = ZonedDateTime.now()

        val activityId = UUID.randomUUID()
        val movementActivity = ActivityModel().apply { id = activityId; visible = false }

        val movement = MovementModel(contentType = REGISTERED).apply {
            id = UUID.randomUUID()
            project = ProjectModel().apply { id = projectId }
            dateTime = movementDateTime
            activity = movementActivity
        }

        whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
        whenever(activityRepository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movementActivity))
        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createMovement(currentUser(), movement).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(MOVEMENT_ACTIVITY_NOT_VISIBLE, result.message)
        verify(projectService).validateDateTime(
            projectId,
            CustomDateTimeModel(movementDateTime),
            MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(activityRepository).findById(
            projectId,
            movementActivity.id !!,
            visibilitySearched = null
        )
        verify(participantRepository, never()).findAllByIds(any(), any(), anyOrNull())
        verify(vehicleRepository, never()).findAllByIds(any(), any(), anyOrNull())
        verify(repository, never()).create(any())
        verify(transactionalOperator).transactional(any<Mono<*>>())
    }

    @Test
    fun `Should createMovement check date, validate activity, check participants and throw because of not found participant`() {
        // Arrange
        val movementDateTime = ZonedDateTime.now()
        val participantId1 = UUID.randomUUID()
        val movementParticipant1 = ParticipantModel().apply { id = participantId1; visible = true; purged = false }
        val participantId2 = UUID.randomUUID()
        val movementParticipant2 = ParticipantModel().apply { id = participantId2; visible = true; purged = false }
        val movementContent = listOf(
            MovementContentModel().apply { participant = movementParticipant1 },
            MovementContentModel().apply { participant = movementParticipant2 },
        )

        val movement = MovementModel(contentType = REGISTERED).apply {
            id = UUID.randomUUID()
            project = ProjectModel().apply { id = projectId }
            dateTime = movementDateTime
            content = movementContent
        }

        whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
        whenever(participantRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.empty())
        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createMovement(currentUser(), movement).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT, result.message)
        verify(projectService).validateDateTime(
            projectId,
            CustomDateTimeModel(movementDateTime),
            MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(activityRepository, never()).findById(any(), any(), anyOrNull())
        verify(participantRepository).findAllByIds(
            projectId,
            movementContent.mapNotNull { it.participant?.id },
            visibilitySearched = null,
        )
        verify(vehicleRepository, never()).findAllByIds(any(), any(), anyOrNull())
        verify(repository, never()).create(any())
        verify(transactionalOperator).transactional(any<Mono<*>>())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should createMovement check date, validate activity, check participants and throw because of participant is not visible`(
        movementContent: List<MovementContentModel>,
        participants: List<ParticipantModel>,
    ) {
        // Arrange
        val movementDateTime = ZonedDateTime.now()

        val movement = MovementModel(contentType = REGISTERED).apply {
            id = UUID.randomUUID()
            project = ProjectModel().apply { id = projectId }
            dateTime = movementDateTime
            content = movementContent
        }

        whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
        whenever(participantRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(*participants.toTypedArray()))
        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createMovement(currentUser(), movement).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(MOVEMENT_PARTICIPANTS_NOT_VISIBLE, result.message)
        verify(projectService).validateDateTime(
            projectId,
            CustomDateTimeModel(movementDateTime),
            MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(activityRepository, never()).findById(any(), any(), anyOrNull())
        verify(participantRepository).findAllByIds(
            projectId,
            movementContent.mapNotNull { it.participant?.id },
            visibilitySearched = null,
        )
        verify(vehicleRepository, never()).findAllByIds(any(), any(), anyOrNull())
        verify(repository, never()).create(any())
        verify(transactionalOperator).transactional(any<Mono<*>>())
    }

    @Test
    fun `Should createMovement check date, validate activity, check participants, check vehicles and throw because of not found vehicle`() {
        // Arrange
        val movementDateTime = ZonedDateTime.now()
        val participantId1 = UUID.randomUUID()
        val movementParticipant1 = ParticipantModel().apply { id = participantId1; visible = true; purged = false }
        val participantId2 = UUID.randomUUID()
        val movementParticipant2 = ParticipantModel().apply { id = participantId2; visible = true; purged = false }
        val vehicleId = UUID.randomUUID()
        val movementVehicle = VehicleModel().apply { id = vehicleId; visible = true }
        val movementContent = listOf(
            MovementContentModel().apply { participant = movementParticipant1 },
            MovementContentModel().apply { participant = movementParticipant2; vehicle = movementVehicle },
        )

        val movement = MovementModel(contentType = REGISTERED).apply {
            id = UUID.randomUUID()
            project = ProjectModel().apply { id = projectId }
            dateTime = movementDateTime
            content = movementContent
        }

        whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
        whenever(participantRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(
            Flux.just(*movementContent.map(MovementContentModel::participant).toTypedArray())
        )
        whenever(vehicleRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.empty())
        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createMovement(currentUser(), movement).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_PROJECT, result.message)
        verify(projectService).validateDateTime(
            projectId,
            CustomDateTimeModel(movementDateTime),
            MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(activityRepository, never()).findById(any(), any(), anyOrNull())
        verify(participantRepository).findAllByIds(
            projectId,
            movementContent.mapNotNull { it.participant?.id },
            visibilitySearched = null,
        )
        verify(vehicleRepository).findAllByIds(
            projectId,
            movementContent.mapNotNull { it.vehicle?.id },
            visibilitySearched = null,
        )
        verify(repository, never()).create(any())
        verify(transactionalOperator).transactional(any<Mono<*>>())
    }

    @Test
    fun `Should createMovement check date, validate activity, check participants, check vehicles and throw because of vehicle is not visible`() {
        // Arrange
        val movementDateTime = ZonedDateTime.now()
        val participantId1 = UUID.randomUUID()
        val movementParticipant1 = ParticipantModel().apply { id = participantId1; visible = true; purged = false }
        val participantId2 = UUID.randomUUID()
        val movementParticipant2 = ParticipantModel().apply { id = participantId2; visible = true; purged = false }
        val vehicleId = UUID.randomUUID()
        val movementVehicle = VehicleModel().apply { id = vehicleId; visible = false }
        val movementContent = listOf(
            MovementContentModel().apply { participant = movementParticipant1 },
            MovementContentModel().apply { participant = movementParticipant2; vehicle = movementVehicle },
        )

        val movement = MovementModel(contentType = REGISTERED).apply {
            id = UUID.randomUUID()
            project = ProjectModel().apply { id = projectId }
            dateTime = movementDateTime
            content = movementContent
        }

        whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
        whenever(participantRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(
            Flux.just(*movementContent.map(MovementContentModel::participant).toTypedArray())
        )
        whenever(vehicleRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(
            Flux.just(*movementContent.mapNotNull(MovementContentModel::vehicle).toTypedArray())
        )
        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createMovement(currentUser(), movement).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(MOVEMENT_VEHICLES_NOT_VISIBLE, result.message)
        verify(projectService).validateDateTime(
            projectId,
            CustomDateTimeModel(movementDateTime),
            MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(activityRepository, never()).findById(any(), any(), anyOrNull())
        verify(participantRepository).findAllByIds(
            projectId,
            movementContent.mapNotNull { it.participant?.id },
            visibilitySearched = null,
        )
        verify(vehicleRepository).findAllByIds(
            projectId,
            movementContent.mapNotNull { it.vehicle?.id },
            visibilitySearched = null,
        )
        verify(repository, never()).create(any())
        verify(transactionalOperator).transactional(any<Mono<*>>())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateMovementById check date, get existing movement, validate activity, validate participants, validate vehicle and call repository update`(
        previousMovementContent: List<MovementContentModel>,
        updatedMovementContent: List<MovementContentModel>,
        newMovementParticipant: List<ParticipantModel>,
        newMovementVehicle: List<VehicleModel>,
        previousMovementActivity: ActivityModel?,
        updatedMovementActivity: ActivityModel?,
        expectedCallToActivity: Int,
        expectedCallToParticipant: Int,
        expectedCallToVehicle: Int,
    ) {
        // Arrange
        val movementDateTime = ZonedDateTime.now()

        val uuid = UUID.randomUUID()
        val movementToUpdate = MovementModel(contentType = REGISTERED).apply {
            id = uuid
            project = ProjectModel().apply { id = projectId }
            dateTime = movementDateTime
            activity = previousMovementActivity
            content = previousMovementContent
        }
        val movementUpdated = MovementModel(contentType = REGISTERED).apply {
            id = uuid
            project = ProjectModel().apply { id = projectId }
            dateTime = movementDateTime
            activity = updatedMovementActivity
            content = updatedMovementContent
        }

        whenever(projectService.validateDateTime(any(), any(), any())).thenReturn(Mono.just(projectId))
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movementToUpdate))
        whenever(activityRepository.findById(any(), any(), anyOrNull())).thenReturn(
            Mono.just(updatedMovementActivity ?: ActivityModel())
        )
        whenever(participantRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(
            Flux.just(*newMovementParticipant.toTypedArray())
        )
        whenever(vehicleRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(
            Flux.just(*newMovementVehicle.toTypedArray())
        )
        whenever(repository.update(any())).thenReturn(Mono.just(movementUpdated))
        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

        // Act
        service.updateMovementById(currentUser(), projectId, uuid, movementUpdated).block()

        // Assert
        verify(projectService).validateDateTime(
            projectId,
            CustomDateTimeModel(movementDateTime),
            MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(activityRepository, times(expectedCallToActivity)).findById(
            projectId,
            updatedMovementActivity?.id ?: UUID.randomUUID(),
            visibilitySearched = null,
        )
        verify(participantRepository, times(expectedCallToParticipant)).findAllByIds(
            projectId,
            newMovementParticipant.mapNotNull { it.id },
            visibilitySearched = null,
        )
        verify(vehicleRepository, times(expectedCallToVehicle)).findAllByIds(
            projectId,
            newMovementVehicle.mapNotNull { it.id },
            visibilitySearched = null
        )
        verify(repository).update(any())
        verify(transactionalOperator).transactional(any<Mono<*>>())
    }

    @Test
    fun `Should disableMovementById hide and return a Movement`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val movement = MovementModel(contentType = REGISTERED).apply { id = uuid; visible = true }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
        whenever(repository.update(any())).thenReturn(Mono.just(movement))

        // Act
        service.disableMovementById(currentUser(), projectId, uuid).block()

        // Assert
        verify(repository).findById(projectId, uuid, visibilitySearched = true)
        verify(repository).update(movement.apply { visible = false })
    }

    @Test
    fun `Should enableMovementById restore and return a Movement`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val movement = MovementModel(contentType = REGISTERED).apply { id = uuid; visible = false }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
        whenever(repository.update(any())).thenReturn(Mono.just(movement))

        // Act
        service.enableMovementById(currentUser(), projectId, uuid).block()

        // Assert
        verify(repository).findById(projectId, uuid, visibilitySearched = false)
        verify(repository).update(movement.apply { visible = true })
    }

    @Test
    fun `Should deleteMovementById delete a Movement`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val movement = MovementModel(contentType = REGISTERED).apply { id = uuid }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(movement))
        whenever(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteMovementById(projectId, uuid).block()

        // Assert
        verify(repository).findById(projectId, uuid, visibilitySearched = null)
        verify(repository).deleteById(uuid)
    }
}
