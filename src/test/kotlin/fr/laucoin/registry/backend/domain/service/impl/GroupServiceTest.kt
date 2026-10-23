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
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.test.ModelExt.defaultGroup
import fr.laucoin.registry.backend.test.ModelExt.defaultGroupId
import fr.laucoin.registry.backend.test.ModelExt.defaultParticipant
import fr.laucoin.registry.backend.test.ModelExt.defaultParticipantId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
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
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID
import java.util.stream.Stream

class GroupServiceTest {
	private val projectService: IProjectService = mock()
	private val port: IGroupPort = mock()
	private val participantPort: IParticipantPort = mock()
	private val service: IGroupService =
		GroupService(projectService, port, participantPort, MAX_PARTICIPANTS)

	private companion object {
		private const val MAX_PARTICIPANTS = 1

		@JvmStatic
		fun `Should createGroup check date, members and throw because a member is not visible or purged`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(defaultParticipant().apply { visible = false; type = REGISTERED }),
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

		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findGroupsPage(defaultProjectId, pageable, params).block()

		// Assert
		verify(port).findPage(defaultProjectId, pageable, params)
	}

	@Test
	fun `Should findGroupMembersPageByGroupId call port findPageByGroupId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ParticipantSearchParamModel()

		whenever(participantPort.findPageByGroupId(any(), any(), any(), any())).thenReturn(
			Mono.just(PageModel(1, 2, 3, 4, emptyList()))
		)

		// Act
		service.findGroupMembersPageByGroupId(defaultProjectId, defaultParticipantId, pageable, params).block()

		// Assert
		verify(participantPort).findPageByGroupId(defaultProjectId, defaultParticipantId, pageable, params)
	}

	@Test
	fun `Should findGroupById call port findById`() {
		// Arrange
		val onlyVisible = true
		val memberIsAvailable = null
		val memberIsVisible = null

		whenever(port.findByIdWithContent(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(Mono.just(defaultGroup()))

		// Act
		service.findGroupById(defaultProjectId, defaultGroupId, onlyVisible, memberIsVisible, memberIsAvailable)
			.block()

		// Assert
		verify(port).findByIdWithContent(
			defaultProjectId, defaultGroupId, onlyVisible, memberIsVisible, memberIsAvailable
		)
	}

	@Test
	fun `Should findGroupById call port findById throw on empty result`() {
		// Arrange
		val onlyVisible = true
		val memberIsAvailable = true
		val memberIsVisible = true

		whenever(port.findByIdWithContent(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findGroupById(defaultProjectId, defaultGroupId, onlyVisible, memberIsVisible, memberIsAvailable)
				.block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(defaultGroupId.toString(), result.args?.first())

		verify(port).findByIdWithContent(
			defaultProjectId, defaultGroupId, onlyVisible, memberIsVisible, memberIsAvailable
		)
	}

	@Test
	fun `Should searchParticipants call port findWithLimit`() {
		// Arrange
		val query = "text"
		val expectedSearch = ParticipantSearchParamModel(null, REGISTERED, isVisible = true).apply {
			this.query = query
		}

		whenever(participantPort.findWithLimit(any(), any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchParticipantsByText(defaultProjectId, query).blockFirst()

		// Assert
		verify(participantPort).findWithLimit(MAX_PARTICIPANTS, defaultProjectId, expectedSearch)
	}

	@Test
	fun `Should createGroup check date, members and call port create`() {
		// Arrange
		val participant = defaultParticipant().apply { type = REGISTERED }
		val group = defaultGroup().apply { members = listOf(participant) }

		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(defaultProjectId))
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(participant))
		whenever(port.create(any())).thenReturn(Mono.just(group))

		// Act
		service.createGroup(currentUser(), group).block()

		// Assert
		verify(projectService).validateDateTimes(
			defaultProjectId, null, null, GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verify(port).create(group)
	}

	@Test
	fun `Should createGroup check date, members and throw because a member is not found`() {
		// Arrange
		val group = defaultGroup().apply { members = listOf(defaultParticipant()) }
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(defaultProjectId))
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createGroup(currentUser(), group).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(GROUP_MEMBERS_NOT_FOUND_IN_GROUP_PROJECT, result.message)
		verify(projectService).validateDateTimes(
			defaultProjectId, null, null, GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
		verify(port, never()).create(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createGroup check date, members and throw because a member is not visible or purged`(participant: ParticipantModel) {
		// Arrange
		val group = defaultGroup().apply { members = listOf(defaultParticipant()) }
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(defaultProjectId))
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(participant))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createGroup(currentUser(), group).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(GROUP_MEMBERS_NOT_VISIBLE, result.message)
		verify(projectService).validateDateTimes(
			defaultProjectId, null, null, GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(participantPort).findAllByIds(defaultProjectId, listOf(defaultParticipantId), isVisible = null)
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
		val groupToUpdate = defaultGroup().apply {
			members = previousParticipantIds.map { defaultParticipant().apply { id = it } }
		}
		val groupUpdated = defaultGroup().apply {
			members = updatedParticipantIds.map { defaultParticipant().apply { id = it } }
		}
		val newParticipants = newParticipantIds.map {
			defaultParticipant().apply { id = it; type = REGISTERED }
		}
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(defaultProjectId))
		whenever(port.findByIdWithContent(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(Mono.just(groupToUpdate))
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull()))
			.thenReturn(Flux.just(*newParticipants.toTypedArray()))
		whenever(port.update(any())).thenReturn(Mono.just(groupUpdated))

		// Act
		service.updateGroupById(currentUser(), defaultProjectId, defaultGroupId, groupUpdated).block()

		// Assert
		verify(projectService).validateDateTimes(
			defaultProjectId, null, null, GROUP_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(participantPort, times(exceptedCallOnParticipantIds))
			.findAllByIds(defaultProjectId, newParticipantIds, isVisible = null)
		verify(port).update(any())
	}

	@Test
	fun `Should addMembersToGroupById get existing, check members and call port update`() {
		// Arrange
		val uuid1 = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()
		val uuid3 = UUID.randomUUID()
		val previousParticipantIds = listOf(uuid1)
		val updatedParticipantIds = listOf(uuid1, uuid2, uuid3)
		val newParticipantIds = listOf(uuid2, uuid3)
		val group = defaultGroup().apply {
			members = previousParticipantIds.map { defaultParticipant().apply { id = it } }
		}
		val newParticipants = newParticipantIds.map {
			defaultParticipant().apply { id = it; type = REGISTERED }
		}
		whenever(port.findByIdWithContent(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(Mono.just(group))
		whenever(participantPort.findAllByIds(any(), any(), anyOrNull()))
			.thenReturn(Flux.just(*newParticipants.toTypedArray()))
		whenever(port.update(any())).thenReturn(Mono.just(group))

		// Act
		service.addMembersToGroupById(currentUser(), defaultProjectId, defaultGroupId, updatedParticipantIds).block()

		// Assert
		verify(port).findByIdWithContent(
			defaultProjectId,
			defaultGroupId,
			isVisible = null,
			memberIsVisible = null,
			memberIsAvailable = null,
		)
		verify(participantPort).findAllByIds(defaultProjectId, newParticipantIds, isVisible = null)
		verify(port).update(any())
	}

	@Test
	fun `Should addMembersToGroupById get existing, check members and throw if no member is new`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val participantIds = listOf(UUID.randomUUID())
		val group = GroupModel().apply {
			this.projectId = defaultProjectId
			members = participantIds.map { ParticipantModel().apply { id = it } }
		}
		whenever(port.findByIdWithContent(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(Mono.just(group))
		whenever(port.update(any())).thenReturn(Mono.just(group))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.addMembersToGroupById(currentUser(), defaultProjectId, uuid, participantIds).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(GROUP_MEMBERS_ALREADY_ADDED, result.message)
		verify(port).findByIdWithContent(
			defaultProjectId,
			uuid,
			isVisible = null,
			memberIsVisible = null,
			memberIsAvailable = null,
		)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should removeMemberFromGroupById get existing, check members and call port update`() {
		// Arrange
		val uuid1 = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()
		val uuid3 = UUID.randomUUID()
		val previousParticipantIds = listOf(uuid1, uuid2, uuid3)
		val group = defaultGroup().apply {
			members = previousParticipantIds.map { defaultParticipant().apply { id = it } }
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
		service.removeMemberFromGroupById(currentUser(), defaultProjectId, defaultGroupId, uuid3).block()

		// Assert
		verify(port).findByIdWithContent(
			defaultProjectId,
			defaultGroupId,
			isVisible = null,
			memberIsVisible = null,
			memberIsAvailable = null
		)
		verify(port).update(any())
	}

	@Test
	fun `Should removeMemberFromGroupById get existing, check members throw because it's the last group member`() {
		// Arrange
		val uuid1 = UUID.randomUUID()
		val previousParticipantIds = listOf(uuid1)
		val group = defaultGroup().apply {
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
			service.removeMemberFromGroupById(currentUser(), defaultProjectId, defaultGroupId, uuid1).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(GROUP_LAST_MEMBERS_CANNOT_BE_REMOVED, result.message)
		verify(port).findByIdWithContent(
			defaultProjectId,
			defaultGroupId,
			isVisible = null,
			memberIsVisible = null,
			memberIsAvailable = null
		)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should disableGroupById call existing group and call port updateGroup`() {
		// Arrange
		whenever(
			port.findByIdWithContent(
				any(),
				any(),
				anyOrNull(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(defaultGroup()))
		whenever(port.update(any())).thenReturn(Mono.just(defaultGroup()))

		// Act
		service.disableGroupById(currentUser(), defaultProjectId, defaultGroupId).block()

		// Assert
		verify(port).findByIdWithContent(
			defaultProjectId,
			defaultGroupId,
			isVisible = true,
			memberIsVisible = null,
			memberIsAvailable = null
		)
		verify(port).update(defaultGroup().apply { visible = false })
	}

	@Test
	fun `Should enableGroupById call existing group and call port updateGroup`() {
		// Arrange
		whenever(
			port.findByIdWithContent(
				any(),
				any(),
				anyOrNull(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(defaultGroup().apply { visible = false }))
		whenever(port.update(any())).thenReturn(Mono.just(defaultGroup()))

		// Act
		service.enableGroupById(currentUser(), defaultProjectId, defaultGroupId).block()

		// Assert
		verify(port).findByIdWithContent(
			defaultProjectId,
			defaultGroupId,
			isVisible = false,
			memberIsVisible = null,
			memberIsAvailable = null
		)
		verify(port).update(defaultGroup().apply { visible = true })
	}

	@Test
	fun `Should deleteGroupById call existing group, and call port deleteById`() {
		// Arrange
		whenever(
			port.findByIdWithContent(
				any(),
				any(),
				anyOrNull(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(defaultGroup().apply { visible = false }))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteGroupById(defaultProjectId, defaultGroupId).block()

		// Assert
		verify(port).findByIdWithContent(
			defaultProjectId,
			defaultGroupId,
			isVisible = null,
			memberIsVisible = null,
			memberIsAvailable = null
		)
		verify(port).deleteById(defaultGroupId)
	}

	@Test
	fun `Should purgeEmptyGroups call empty group vehicle since a date, and call port deleteById`() {
		// Arrange
		val uuid1 = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()
		whenever(port.findEmpty(any())).thenReturn(Flux.just(uuid1, uuid2))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.purgeEmptyGroups(emptyList(), false).collectList().block()

		// Assert
		verify(port).findEmpty(emptyList())
		verify(port).deleteById(uuid1)
		verify(port).deleteById(uuid2)
	}

	@Test
	fun `Should purgeEmptyGroups call empty group since a date, and not call port deleteById because of dryRun`() {
		// Arrange
		whenever(port.findEmpty(any())).thenReturn(Flux.just(UUID.randomUUID()))

		// Act
		service.purgeEmptyGroups(emptyList(), true).collectList().block()

		// Assert
		verify(port).findEmpty(emptyList())
		verify(port, never()).deleteById(any())
	}
}
