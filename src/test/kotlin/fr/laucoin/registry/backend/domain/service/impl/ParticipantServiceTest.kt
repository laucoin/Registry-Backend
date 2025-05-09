package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_DISABLE_LAST_GROUP_MEMBER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_GROUPS_NOT_FOUND_IN_PARTICIPANT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_GROUPS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_IN_PROJECT_ALREADY_LINKED_TO_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IGroupModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.repository.IUserModelRepository
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ParticipantServiceTest {
    private val repository: IParticipantModelRepository = mock()
    private val projectService: IProjectService = mock()
    private val userRepository: IUserModelRepository = mock()
    private val movementRepository: IMovementModelRepository = mock()
    private val groupRepository: IGroupModelRepository = mock()
    private val maxUsers: Int = 1
    private val maxGroups: Int = 1
    private val service: IParticipantService = ParticipantService(
        projectService, repository, userRepository, movementRepository, groupRepository, maxUsers, maxGroups
    )

    companion object {
        @JvmStatic
        fun `Should createParticipant check date, user, groups and call repository create`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(ParticipantModel(), 0, 0),
                Arguments.of(ParticipantModel().apply { user = UserModel().apply { id = UUID.randomUUID() } }, 1, 0),
                Arguments.of(ParticipantModel().apply { groups = listOf(GroupModel().apply { id = UUID.randomUUID() }) }, 0, 1),
            )
        }

        @JvmStatic
        fun `Should updateParticipantById check date, get existing participants, check user, groups and call repository create`(): Stream<Arguments> {
            val participantUser = UserModel().apply { id = UUID.randomUUID() }
            val participantGroupId1 = UUID.randomUUID()
            val participantGroup1 = GroupModel().apply { id = participantGroupId1; visible = true }
            val participantGroupId2 = UUID.randomUUID()
            val participantGroup2 = GroupModel().apply { id = participantGroupId2; visible = true }

            return Stream.of(
                Arguments.of(
                    participantUser,
                    participantUser,
                    listOf(participantGroup1, participantGroup2),
                    listOf(participantGroup1, participantGroup2),
                    emptyList<GroupModel>(),
                    0,
                    0,
                ),
                Arguments.of(
                    null,
                    participantUser,
                    listOf(participantGroup1, participantGroup2),
                    listOf(participantGroup1, participantGroup2),
                    emptyList<GroupModel>(),
                    1,
                    0,
                ),
                Arguments.of(
                    participantUser,
                    null,
                    listOf(participantGroup1, participantGroup2),
                    listOf(participantGroup1, participantGroup2),
                    emptyList<GroupModel>(),
                    0,
                    0,
                ),
                Arguments.of(
                    participantUser,
                    participantUser,
                    listOf(participantGroup1),
                    listOf(participantGroup1, participantGroup2),
                    listOf(participantGroup2),
                    0,
                    1,
                ),
                Arguments.of(
                    participantUser,
                    participantUser,
                    listOf(participantGroup1, participantGroup2),
                    listOf(participantGroup1),
                    emptyList<GroupModel>(),
                    0,
                    0,
                ),
            )
        }

        @JvmStatic
        fun `Should disableParticipantById call existing participant, check if the last member of one of his groups and call repository update`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(emptyList<GroupModel>(), 0),
                Arguments.of(listOf(GroupModel().apply { id = UUID.randomUUID() }), 1),
            )
        }
    }

    @Test
    fun `Should findParticipantsPage call repository findPage`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = ParticipantSearchParamModel()
        whenever(repository.findPage(any(), any(), any())).thenReturn(
            Mono.just(PageModel(1, 2, 3, 4, emptyList()))
        )

        // Act
        service.findParticipantsPage(projectId, pageable, params).block()

        // Assert
        verify(repository).findPage(projectId, pageable, params)
    }

    @Test
    fun `Should findParticipantsByIds call repository findAllByIds`() {
        // Arrange
        val participant = ParticipantModel().apply { project = ProjectModel().apply { id = projectId } }
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        whenever(repository.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(participant))

        // Act
        service.findParticipantsByIds(projectId, listOf(uuid), onlyVisible).blockFirst()

        // Assert
        verify(repository).findAllByIds(projectId, listOf(uuid), onlyVisible)
    }

    @Test
    fun `Should findParticipantById call repository findById`() {
        // Arrange
        val participant = ParticipantModel().apply { project = ProjectModel().apply { id = projectId } }
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participant))

        // Act
        service.findParticipantById(projectId, uuid, onlyVisible).block()

        // Assert
        verify(repository).findById(projectId, uuid, onlyVisible)
    }

    @Test
    fun `Should searchUsers call userRepository findWithLimit`() {
        // Arrange
        val textSearched = "text"
        whenever(userRepository.findWithLimit(any(), any())).thenReturn(Flux.empty())

        // Act
        service.searchUsersByText(projectId, textSearched).blockFirst()

        // Assert
        verify(userRepository).findWithLimit(maxUsers, UserSearchParamModel(textSearched, visibilitySearched = true))
    }

    @Test
    fun `Should searchGroups call groupRepository findWithLimit`() {
        // Arrange
        val textSearched = "text"
        whenever(groupRepository.findWithLimit(any(), any(), any())).thenReturn(Flux.empty())

        // Act
        service.searchGroupsByText(projectId, textSearched).blockFirst()

        // Assert
        verify(groupRepository).findWithLimit(
            maxGroups,
            projectId,
            GroupSearchParamModel(textSearched, visibilitySearched = true),
        )
    }

    @Test
    fun `Should findParticipantMovementsPage call movementRepository findPageByParticipantId`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val pageable = PageableModel(0, 10)
        val params = MovementSearchParamModel(typeSearched = IN)
        whenever(movementRepository.findPageByParticipantId(any(), any(), any(), any())).thenReturn(Mono.empty())

        // Act
        service.findParticipantMovementsPage(projectId, uuid, pageable, params).block()

        // Assert
        verify(movementRepository).findPageByParticipantId(
            projectId,
            uuid,
            pageable,
            params,
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should createParticipant check date, user, groups and call repository create`(
        participant: ParticipantModel,
        expectedUserVerification: Int,
        expectedGroupVerification: Int,
    ) {
        // Arrange
        participant.apply { project = ProjectModel().apply { id = projectId } }
        whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
        whenever(repository.create(any())).thenReturn(Mono.just(participant))
        whenever(repository.findByUserId(any(), any())).thenReturn(Flux.empty())
        whenever(groupRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(*participant.groups.toTypedArray()))

        // Act
        service.createParticipant(currentUser(), participant).block()

        // Assert
        verify(projectService).validateDateTimes(
            projectId,
            start = null,
            end = null,
            PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(repository, times(expectedUserVerification)).findByUserId(projectId, participant.user?.id ?: UUID.randomUUID())
        verify(groupRepository, times(expectedGroupVerification)).findAllByIds(
            projectId,
            participant.groups.mapNotNull { it.id },
            null
        )
        verify(repository).create(participant)
    }

    @Test
    fun `Should createParticipant check date, user and throw because participant is already linked`() {
        // Arrange
        val participant = ParticipantModel().apply {
            user = UserModel().apply { id = UUID.randomUUID() }
            project = ProjectModel().apply { id = projectId }
        }
        whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
        whenever(repository.findByUserId(any(), any())).thenReturn(Flux.just(participant))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createParticipant(currentUser(), participant).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(PARTICIPANT_IN_PROJECT_ALREADY_LINKED_TO_USER, result.message)
        verify(projectService).validateDateTimes(
            projectId,
            start = null,
            end = null,
            PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(repository).findByUserId(projectId, participant.user?.id ?: UUID.randomUUID())
        verify(groupRepository, never()).findAllByIds(any(), any(), anyOrNull())
        verify(repository, never()).create(any())
    }

    @Test
    fun `Should createParticipant check date, user, groups and throw because group is not found`() {
        // Arrange
        val participant = ParticipantModel().apply {
            groups = listOf(GroupModel().apply { id = UUID.randomUUID() })
            project = ProjectModel().apply { id = projectId }
        }
        whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
        whenever(repository.create(any())).thenReturn(Mono.just(participant))
        whenever(repository.findByUserId(any(), any())).thenReturn(Flux.empty())
        whenever(groupRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.empty())

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createParticipant(currentUser(), participant).block()
        }) as RegistryException

        // Assert
        assertEquals(NOT_FOUND, result.status)
        assertEquals(PARTICIPANT_GROUPS_NOT_FOUND_IN_PARTICIPANT_PROJECT, result.message)
        verify(projectService).validateDateTimes(
            projectId,
            start = null,
            end = null,
            PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(repository, never()).findByUserId(any(), any())
        verify(groupRepository).findAllByIds(projectId, participant.groups.mapNotNull { it.id }, null)
        verify(repository, never()).create(any())
    }

    @Test
    fun `Should createParticipant check date, user, groups and throw because group is not visible`() {
        // Arrange
        val participant = ParticipantModel().apply {
            groups = listOf(GroupModel().apply { id = UUID.randomUUID() })
            project = ProjectModel().apply { id = projectId }
        }
        whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
        whenever(repository.create(any())).thenReturn(Mono.just(participant))
        whenever(repository.findByUserId(any(), any())).thenReturn(Flux.empty())
        whenever(groupRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(
            Flux.just(GroupModel().apply { id = UUID.randomUUID(); visible = false })
        )

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.createParticipant(currentUser(), participant).block()
        }) as RegistryException

        // Assert
        assertEquals(CONFLICT, result.status)
        assertEquals(PARTICIPANT_GROUPS_NOT_VISIBLE, result.message)
        verify(projectService).validateDateTimes(
            projectId,
            start = null,
            end = null,
            PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(repository, never()).findByUserId(any(), any())
        verify(groupRepository).findAllByIds(projectId, participant.groups.mapNotNull { it.id }, null)
        verify(repository, never()).create(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateParticipantById check date, get existing participants, check user, groups and call repository create`(
        previousUser: UserModel?,
        updatedUser: UserModel?,
        previousGroups: List<GroupModel>,
        updatedGroups: List<GroupModel>,
        newGroups: List<GroupModel>,
        expectedUserVerification: Int,
        expectedGroupVerification: Int,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val participantToUpdate = ParticipantModel().apply {
            id = uuid
            groups = previousGroups
            user = previousUser
            project = ProjectModel().apply { id = projectId }
        }
        val participantUpdated = ParticipantModel().apply {
            id = uuid
            groups = updatedGroups
            user = updatedUser
            project = ProjectModel().apply { id = projectId }
        }
        whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participantToUpdate))
        whenever(repository.update(any())).thenReturn(Mono.just(participantUpdated))
        whenever(repository.findByUserId(any(), any())).thenReturn(Flux.empty())
        whenever(groupRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(*newGroups.toTypedArray()))

        // Act
        service.updateParticipantById(currentUser(), projectId, uuid, participantUpdated)
            .block()

        // Assert
        verify(projectService).validateDateTimes(
            projectId,
            start = null,
            end = null,
            PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
        )
        verify(repository).findById(projectId, uuid, visibilitySearched = null)
        verify(repository, times(expectedUserVerification)).findByUserId(projectId, updatedUser?.id ?: UUID.randomUUID())
        verify(groupRepository, times(expectedGroupVerification)).findAllByIds(
            projectId,
            newGroups.mapNotNull { it.id },
            visibilitySearched = null
        )
        verify(repository).update(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should disableParticipantById call existing participant, check if the last member of one of his groups and call repository update`(
        participantGroups: List<GroupModel>,
        expectedCallGroupVerification: Int,
    ) {
        // Arrange
        val participant = ParticipantModel().apply {
            groups = participantGroups
            project = ProjectModel().apply { id = projectId }
            visible = true
        }
        val uuid = UUID.randomUUID()
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participant))
        whenever(groupRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(
            Flux.just(
                *participantGroups.map { GroupModel().apply { members = listOf(participant, ParticipantModel()) } }.toTypedArray()
            )
        )
        whenever(repository.update(any())).thenReturn(Mono.just(participant))

        // Act
        service.disableParticipantById(currentUser(), projectId, uuid).block()

        // Assert
        verify(repository).findById(projectId, uuid, visibilitySearched = true)
        verify(groupRepository, times(expectedCallGroupVerification)).findAllByIds(
            projectId,
            participantGroups.mapNotNull { it.id },
            visibilitySearched = null,
        )
        verify(repository).update(participant.apply { visible = false })
    }

    @Test
    fun `Should disableParticipantById call existing participant, check if the last member of one of his groups and throw`() {
        // Arrange
        val groupId = UUID.randomUUID()
        val participantGroup = GroupModel().apply { id = groupId }
        val uuid = UUID.randomUUID()
        val participant = ParticipantModel().apply {
            id = uuid
            groups = listOf(participantGroup)
            project = ProjectModel().apply { id = projectId }
            visible = true
        }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participant))
        whenever(groupRepository.findAllByIds(any(), any(), anyOrNull())).thenReturn(
            Flux.just(GroupModel().apply { members = listOf(participant) })
        )
        whenever(repository.update(any())).thenReturn(Mono.just(participant))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.disableParticipantById(currentUser(), projectId, uuid).block()
        }) as RegistryException

        // Assert
        assertEquals(FORBIDDEN, result.status)
        assertEquals(PARTICIPANT_DISABLE_LAST_GROUP_MEMBER, result.message)
        verify(repository).findById(projectId, uuid, visibilitySearched = true)
        verify(groupRepository).findAllByIds(projectId, listOf(groupId), visibilitySearched = null)
        verify(repository, never()).update(any())
    }

    @Test
    fun `Should enableParticipantById call existing participant and call repository update`() {
        // Arrange
        val participant = ParticipantModel().apply { project = ProjectModel().apply { id = projectId }; visible = false }
        val uuid = UUID.randomUUID()
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participant))
        whenever(repository.update(any())).thenReturn(Mono.just(participant))

        // Act
        service.enableParticipantById(currentUser(), projectId, uuid).block()

        // Assert
        verify(repository).findById(projectId, uuid, visibilitySearched = false)
        verify(repository).update(participant.apply { visible = true })
    }

    @Test
    fun `Should deleteParticipantById call existing participant, check no movement, and call repository deleteById`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val participant = ParticipantModel().apply { id = uuid; project = ProjectModel().apply { id = projectId } }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participant))
        whenever(movementRepository.countAllByParticipantId(any(), any(), any())).thenReturn(Mono.just(0))
        whenever(repository.deleteById(any())).thenReturn(Mono.empty())

        // Act
        service.deleteParticipantById(currentUser(), projectId, uuid).block()

        // Assert
        verify(repository).findById(projectId, uuid, visibilitySearched = null)
        verify(movementRepository).countAllByParticipantId(projectId, uuid, MovementSearchParamModel())
        verify(repository).deleteById(uuid)
    }

    @Test
    fun `Should deleteParticipantById call existing participant, throw if movements are linked`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val participant = ParticipantModel().apply { id = uuid; project = ProjectModel().apply { id = projectId } }
        whenever(repository.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participant))
        whenever(movementRepository.countAllByParticipantId(any(), any(), any())).thenReturn(Mono.just(1))

        // Act
        val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
            service.deleteParticipantById(currentUser(), projectId, uuid).block()
        }) as RegistryException

        // Assert
        assertEquals(FORBIDDEN, result.status)
        assertEquals(PARTICIPANT_DELETE_HAS_MOVEMENT, result.message)
        verify(repository).findById(projectId, uuid, visibilitySearched = null)
        verify(movementRepository).countAllByParticipantId(projectId, uuid, MovementSearchParamModel())
        verify(repository, never()).deleteById(any())
    }
}
