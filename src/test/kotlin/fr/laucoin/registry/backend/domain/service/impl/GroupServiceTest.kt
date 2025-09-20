package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_LAST_MEMBERS_CANNOT_BE_REMOVED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_ALREADY_ADDED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_NOT_FOUND_IN_GROUP_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.service.IGroupService
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
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class GroupServiceTest {
	private val projectService: IProjectService = mock()
	private val port: IGroupPort = mock()
	private val participantPort: IParticipantPort = mock()
	private val maxParticipants = 1
	private val service: IGroupService =
		GroupService(projectService, port, participantPort, maxParticipants)

	companion object {
		@JvmStatic
		fun `Should createGroup check date, members and throw because a member is not visible or purged`(): Stream<Arguments> {
			val uuid = UUID.randomUUID()
			return Stream.of(
				Arguments.of(ParticipantModel().apply {
					id = uuid; visible = false; purged = false; type = REGISTERED
				}),
				Arguments.of(ParticipantModel().apply { id = uuid; visible = true; purged = true; type = REGISTERED }),
				Arguments.of(ParticipantModel().apply { id = uuid; visible = false; purged = true; type = REGISTERED }),
			)
		}

		@JvmStatic
		fun `Should updateGroupById check date, get existing, members and call port update`(): Stream<Arguments> {
			val uuid1 = UUID.randomUUID()
			val uuid2 = UUID.randomUUID()
			val uuid3 = UUID.randomUUID()

			return Stream.of(
				Arguments.of(listOf(uuid1), listOf(uuid1, uuid2, uuid3), listOf(uuid2, uuid3), 1),
				Arguments.of(listOf(uuid1), listOf(uuid1), emptyList<UUID>(), 0),
			)
		}
	}

	@Test
	fun `Should findGroupsPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = GroupSearchParamModel()
		whenever(port.findPage(any(), any(), any())).thenReturn(
			Mono.just(PageModel(1, 2, 3, 4, emptyList()))
		)

		// Act
		service.findGroupsPage(projectId, pageable, params).block()

		// Assert
		verify(port).findPage(projectId, pageable, params)
	}

	@Test
	fun `Should findGroupMembersPageByGroupId call port findPageByGroupId`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val pageable = PageableModel(0, 10)
		val params = ParticipantSearchParamModel()
		whenever(participantPort.findPageByGroupId(any(), any(), any(), any())).thenReturn(
			Mono.just(PageModel(1, 2, 3, 4, emptyList()))
		)

		// Act
		service.findGroupMembersPageByGroupId(projectId, uuid, pageable, params).block()

		// Assert
		verify(participantPort).findPageByGroupId(projectId, uuid, pageable, params)
	}

	@Test
	fun `Should findGroupById call port findById`() {
		// Arrange
		val group = GroupModel().apply { project = ProjectModel().apply { id = projectId } }
		val uuid = UUID.randomUUID()
		val onlyVisible = true
		val memberAvailabilitySearched = null
		val memberVisibilitySearched = null
		whenever(
			port.findByIdWithContent(
				any(),
				any(),
				anyOrNull(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(group))

		// Act
		service.findGroupById(projectId, uuid, onlyVisible, memberVisibilitySearched, memberAvailabilitySearched)
			.block()

		// Assert
		verify(port).findByIdWithContent(
			projectId,
			uuid,
			onlyVisible,
			memberVisibilitySearched,
			memberAvailabilitySearched
		)
	}

	@Test
	fun `Should findGroupById call port findById throw on empty result`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val onlyVisible = true
		val memberAvailabilitySearched = true
		val memberVisibilitySearched = true
		whenever(
			port.findByIdWithContent(
				any(),
				any(),
				anyOrNull(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findGroupById(projectId, uuid, onlyVisible, memberVisibilitySearched, memberAvailabilitySearched)
				.block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		verify(port).findByIdWithContent(
			projectId,
			uuid,
			onlyVisible,
			memberVisibilitySearched,
			memberAvailabilitySearched
		)
	}

	@Test
	fun `Should searchParticipants call port findWithLimit`() {
		// Arrange
		val textSearched = "text"
		whenever(participantPort.findWithLimit(any(), any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchParticipantsByText(projectId, textSearched).blockFirst()

		// Assert
		verify(participantPort).findWithLimit(
			maxParticipants,
			projectId,
			ParticipantSearchParamModel(null, REGISTERED, visibilitySearched = true).apply {
				this.textSearched = textSearched
			}
		)
	}

	@Test
	fun `Should createGroup check date, members and call port create`() {
		// Arrange
		val participantId = UUID.randomUUID()
		val participant =
			ParticipantModel().apply { id = participantId; visible = true; purged = false; type = REGISTERED }
		val group = GroupModel().apply {
			project = ProjectModel().apply { id = projectId }
			members = listOf(ParticipantModel().apply { id = participantId })
		}
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(
			Mono.just(
				projectId
			)
		)
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(participant))
		whenever(port.create(any())).thenReturn(Mono.just(group))

		// Act
		service.createGroup(currentUser(), group).block()

		// Assert
		verify(projectService).validateDateTimes(
			projectId, null, null, GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(participantPort).findAllByIds(projectId, listOf(participantId), visibilitySearched = null)
		verify(port).create(group)
	}

	@Test
	fun `Should createGroup check date, members and throw because a member is not found`() {
		// Arrange
		val participantId = UUID.randomUUID()
		val group = GroupModel().apply {
			project = ProjectModel().apply { id = projectId }
			members = listOf(ParticipantModel().apply { id = participantId })
		}
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(
			Mono.just(
				projectId
			)
		)
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createGroup(currentUser(), group).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(GROUP_MEMBERS_NOT_FOUND_IN_GROUP_PROJECT, result.message)
		verify(projectService).validateDateTimes(
			projectId, null, null, GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(participantPort).findAllByIds(projectId, listOf(participantId), visibilitySearched = null)
		verify(port, never()).create(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createGroup check date, members and throw because a member is not visible or purged`(participant: ParticipantModel) {
		// Arrange
		val participantId = UUID.randomUUID()
		val group = GroupModel().apply {
			project = ProjectModel().apply { id = projectId }
			members = listOf(ParticipantModel().apply { id = participantId })
		}
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(
			Mono.just(
				projectId
			)
		)
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(participant))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createGroup(currentUser(), group).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(GROUP_MEMBERS_NOT_VISIBLE, result.message)
		verify(projectService).validateDateTimes(
			projectId, null, null, GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(participantPort).findAllByIds(projectId, listOf(participantId), visibilitySearched = null)
		verify(port, never()).create(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateGroupById check date, get existing, members and call port update`(
		previousParticipantIds: List<UUID>,
		updatedParticipantIds: List<UUID>,
		newParticipantIds: List<UUID>,
		exceptedCallOnParticipantIds: Int
	) {
		// Arrange
		val uuid = UUID.randomUUID()
		val groupToUpdate = GroupModel().apply {
			project = ProjectModel().apply { id = projectId }
			members = previousParticipantIds.map { ParticipantModel().apply { id = it } }
		}
		val groupUpdated = GroupModel().apply {
			project = ProjectModel().apply { id = projectId }
			members = updatedParticipantIds.map { ParticipantModel().apply { id = it } }
		}
		val newParticipants =
			newParticipantIds.map {
				ParticipantModel().apply {
					id = it; visible = true; purged = false; type = REGISTERED
				}
			}
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any())).thenReturn(
			Mono.just(
				projectId
			)
		)
		whenever(port.findByIdWithContent(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(
			Mono.just(
				groupToUpdate
			)
		)
		whenever(
			participantPort.findAllByIds(
				any(),
				any(),
				anyOrNull()
			)
		).thenReturn(Flux.just(*newParticipants.toTypedArray()))
		whenever(port.update(any())).thenReturn(Mono.just(groupUpdated))

		// Act
		service.updateGroupById(currentUser(), projectId, uuid, groupUpdated).block()

		// Assert
		verify(projectService).validateDateTimes(
			projectId, null, null, GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(participantPort, times(exceptedCallOnParticipantIds)).findAllByIds(
			projectId,
			newParticipantIds,
			visibilitySearched = null,
		)
		verify(port).update(any())
	}

	@Test
	fun `Should addMembersToGroupById get existing, check members and call port update`() {
		// Arrange
		val uuid = UUID.randomUUID()

		val uuid1 = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()
		val uuid3 = UUID.randomUUID()
		val previousParticipantIds = listOf(uuid1)
		val updatedParticipantIds = listOf(uuid1, uuid2, uuid3)
		val newParticipantIds = listOf(uuid2, uuid3)
		val group = GroupModel().apply {
			project = ProjectModel().apply { id = projectId }
			members = previousParticipantIds.map { ParticipantModel().apply { id = it } }
		}
		val newParticipants =
			newParticipantIds.map {
				ParticipantModel().apply {
					id = it; visible = true; purged = false; type = REGISTERED
				}
			}
		whenever(
			port.findByIdWithContent(
				any(),
				any(),
				anyOrNull(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(group))
		whenever(
			participantPort.findAllByIds(
				any(),
				any(),
				anyOrNull()
			)
		).thenReturn(Flux.just(*newParticipants.toTypedArray()))
		whenever(port.update(any())).thenReturn(Mono.just(group))

		// Act
		service.addMembersToGroupById(currentUser(), projectId, uuid, updatedParticipantIds).block()

		// Assert
		verify(port).findByIdWithContent(
			projectId,
			uuid,
			visibilitySearched = null,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null
		)
		verify(participantPort).findAllByIds(
			projectId,
			newParticipantIds,
			visibilitySearched = null,
		)
		verify(port).update(any())
	}

	@Test
	fun `Should addMembersToGroupById get existing, check members and throw if no member is new`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val participantIds = listOf(UUID.randomUUID())
		val group = GroupModel().apply {
			project = ProjectModel().apply { id = projectId }
			members = participantIds.map { ParticipantModel().apply { id = it } }
		}
		whenever(
			port.findByIdWithContent(
				any(),
				any(),
				anyOrNull(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(group))
		whenever(port.update(any())).thenReturn(Mono.just(group))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.addMembersToGroupById(currentUser(), projectId, uuid, participantIds).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_ENTITY, result.status)
		assertEquals(GROUP_MEMBERS_ALREADY_ADDED, result.message)
		verify(port).findByIdWithContent(
			projectId,
			uuid,
			visibilitySearched = null,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null
		)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should removeMemberFromGroupById get existing, check members and call port update`() {
		// Arrange
		val uuid = UUID.randomUUID()

		val uuid1 = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()
		val uuid3 = UUID.randomUUID()
		val previousParticipantIds = listOf(uuid1, uuid2, uuid3)
		val group = GroupModel().apply {
			project = ProjectModel().apply { id = projectId }
			members = previousParticipantIds.map { ParticipantModel().apply { id = it } }
		}
		whenever(
			port.findByIdWithContent(
				any(),
				any(),
				anyOrNull(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(group))
		whenever(port.update(any())).thenReturn(Mono.just(group))

		// Act
		service.removeMemberFromGroupById(currentUser(), projectId, uuid, uuid3).block()

		// Assert
		verify(port).findByIdWithContent(
			projectId,
			uuid,
			visibilitySearched = null,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null
		)
		verify(port).update(any())
	}

	@Test
	fun `Should removeMemberFromGroupById get existing, check members throw because it's the last group member`() {
		// Arrange
		val uuid = UUID.randomUUID()

		val uuid1 = UUID.randomUUID()
		val previousParticipantIds = listOf(uuid1)
		val group = GroupModel().apply {
			project = ProjectModel().apply { id = projectId }
			members = previousParticipantIds.map { ParticipantModel().apply { id = it } }
		}
		whenever(
			port.findByIdWithContent(
				any(),
				any(),
				anyOrNull(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(group))
		whenever(port.update(any())).thenReturn(Mono.just(group))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.removeMemberFromGroupById(currentUser(), projectId, uuid, uuid1).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(GROUP_LAST_MEMBERS_CANNOT_BE_REMOVED, result.message)
		verify(port).findByIdWithContent(
			projectId,
			uuid,
			visibilitySearched = null,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null
		)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should disableGroupById call existing group and call port updateGroup`() {
		// Arrange
		val group = GroupModel().apply { project = ProjectModel().apply { id = projectId }; visible = true }
		val uuid = UUID.randomUUID()
		whenever(
			port.findByIdWithContent(
				any(),
				any(),
				anyOrNull(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(group))
		whenever(port.update(any())).thenReturn(Mono.just(group))

		// Act
		service.disableGroupById(currentUser(), projectId, uuid).block()

		// Assert
		verify(port).findByIdWithContent(
			projectId,
			uuid,
			visibilitySearched = true,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null
		)
		verify(port).update(group.apply { visible = false })
	}

	@Test
	fun `Should enableGroupById call existing group and call port updateGroup`() {
		// Arrange
		val group = GroupModel().apply { project = ProjectModel().apply { id = projectId }; visible = false }
		val uuid = UUID.randomUUID()
		whenever(
			port.findByIdWithContent(
				any(),
				any(),
				anyOrNull(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(group))
		whenever(port.update(any())).thenReturn(Mono.just(group))

		// Act
		service.enableGroupById(currentUser(), projectId, uuid).block()

		// Assert
		verify(port).findByIdWithContent(
			projectId,
			uuid,
			visibilitySearched = false,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null
		)
		verify(port).update(group.apply { visible = true })
	}

	@Test
	fun `Should deleteGroupById call existing group, and call port deleteById`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val group = GroupModel().apply { id = uuid; project = ProjectModel().apply { id = projectId }; visible = false }
		whenever(
			port.findByIdWithContent(
				any(),
				any(),
				anyOrNull(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(group))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteGroupById(projectId, uuid).block()

		// Assert
		verify(port).findByIdWithContent(
			projectId,
			uuid,
			visibilitySearched = null,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null
		)
		verify(port).deleteById(uuid)
	}
}
