package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.service.core.IGroupService
import fr.laucoin.registry.backend.domain.service.usecase.IGroupUseCase
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class GroupUseCaseTest {
	private val service: IGroupService = mock()
	private val useCase: IGroupUseCase = GroupUseCase(service)

	@Test
	fun `Should findGroupsPage delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: GroupSearchParamModel = mock()
		val expected: Mono<PageModel<GroupModel>> = Mono.empty()
		whenever(service.findGroupsPage(projectId, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findGroupsPage(projectId, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(service).findGroupsPage(projectId, pageable, searchParams)
	}

	@Test
	fun `Should findGroupMembersPageByGroupId delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: ParticipantSearchParamModel = mock()
		val expected: Mono<PageModel<ParticipantModel>> = Mono.empty()
		whenever(service.findGroupMembersPageByGroupId(projectId, id, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findGroupMembersPageByGroupId(projectId, id, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(service).findGroupMembersPageByGroupId(projectId, id, pageable, searchParams)
	}

	@Test
	fun `Should findGroupById delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val isVisible: Boolean? = null
		val memberIsVisible: Boolean? = null
		val memberIsAvailable: Boolean? = null
		val expected: Mono<GroupModel> = Mono.empty()
		whenever(service.findGroupById(projectId, id, isVisible, memberIsVisible, memberIsAvailable)).thenReturn(expected)

		// Act
		val result = useCase.findGroupById(projectId, id, isVisible, memberIsVisible, memberIsAvailable)

		// Assert
		assertSame(expected, result)
		verify(service).findGroupById(projectId, id, isVisible, memberIsVisible, memberIsAvailable)
	}

	@Test
	fun `Should findGroupSummaryById delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<GroupModel> = Mono.empty()
		whenever(service.findGroupSummaryById(projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.findGroupSummaryById(projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).findGroupSummaryById(projectId, id)
	}

	@Test
	fun `Should searchParticipantsByText delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val query: String? = null
		val expected: Flux<ParticipantModel> = Flux.empty()
		whenever(service.searchParticipantsByText(projectId, query)).thenReturn(expected)

		// Act
		val result = useCase.searchParticipantsByText(projectId, query)

		// Assert
		assertSame(expected, result)
		verify(service).searchParticipantsByText(projectId, query)
	}

	@Test
	fun `Should createGroup delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val group: GroupModel = mock()
		val expected: Mono<GroupModel> = Mono.empty()
		whenever(service.createGroup(currentUser, group)).thenReturn(expected)

		// Act
		val result = useCase.createGroup(currentUser, group)

		// Assert
		assertSame(expected, result)
		verify(service).createGroup(currentUser, group)
	}

	@Test
	fun `Should updateGroupById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val group: GroupModel = mock()
		val expected: Mono<GroupModel> = Mono.empty()
		whenever(service.updateGroupById(currentUser, projectId, id, group)).thenReturn(expected)

		// Act
		val result = useCase.updateGroupById(currentUser, projectId, id, group)

		// Assert
		assertSame(expected, result)
		verify(service).updateGroupById(currentUser, projectId, id, group)
	}

	@Test
	fun `Should addMembersToGroupById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val memberIds: List<UUID> = emptyList()
		val expected: Mono<Pair<List<UUID>, List<UUID>>> = Mono.empty()
		whenever(service.addMembersToGroupById(currentUser, projectId, id, memberIds)).thenReturn(expected)

		// Act
		val result = useCase.addMembersToGroupById(currentUser, projectId, id, memberIds)

		// Assert
		assertSame(expected, result)
		verify(service).addMembersToGroupById(currentUser, projectId, id, memberIds)
	}

	@Test
	fun `Should removeMemberFromGroupById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val memberId: UUID = UUID.randomUUID()
		val expected: Mono<GroupModel> = Mono.empty()
		whenever(service.removeMemberFromGroupById(currentUser, projectId, id, memberId)).thenReturn(expected)

		// Act
		val result = useCase.removeMemberFromGroupById(currentUser, projectId, id, memberId)

		// Assert
		assertSame(expected, result)
		verify(service).removeMemberFromGroupById(currentUser, projectId, id, memberId)
	}

	@Test
	fun `Should disableGroupById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<GroupModel> = Mono.empty()
		whenever(service.disableGroupById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.disableGroupById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).disableGroupById(currentUser, projectId, id)
	}

	@Test
	fun `Should enableGroupById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<GroupModel> = Mono.empty()
		whenever(service.enableGroupById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.enableGroupById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).enableGroupById(currentUser, projectId, id)
	}

	@Test
	fun `Should deleteGroupById delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<Unit> = Mono.empty()
		whenever(service.deleteGroupById(projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.deleteGroupById(projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).deleteGroupById(projectId, id)
	}
}
