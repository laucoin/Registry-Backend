package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_U
import fr.laucoin.registry.backend.domain.enumeration.GroupSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.GroupSortFieldEnum.END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.domain.enumeration.GroupSortFieldEnum.NAME
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GroupMembersWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GroupWriterDto
import fr.laucoin.registry.backend.test.ModelExt.commonGroup
import fr.laucoin.registry.backend.test.ModelExt.commonParticipant
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.test.assertEquals

class GroupV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IGroupService

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/groups"
	}

	private fun groupPage(): Mono<PageModel<GroupModel>> =
		Mono.just(PageModel(PageableModel(0, 20), 1, listOf(commonGroup())))

	@Test
	fun `Should findGroups return 200 with the v2 list grammar`() {
		// Arrange
		whenever(service.findGroupsPage(any(), any(), any(), any())).thenReturn(groupPage())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(projectId),
					listOf(
						Pair("page", 2),
						Pair("size", 10),
						Pair("q", "scouts"),
						Pair("visible", true),
						Pair("sort", "name,endAvailabilityDate"),
						Pair("direction", "DESC"),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<GroupWithoutMemberReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		val sortCaptor = argumentCaptor<List<SortModel<GroupSortFieldEnum>>>()
		verify(service).findGroupsPage(eq(projectId), pageableCaptor.capture(), any(), sortCaptor.capture())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
		assertEquals(
			listOf(SortModel(NAME, descending = true), SortModel(END_AVAILABILITY_DATE, descending = true)),
			sortCaptor.firstValue
		)
	}

	@Test
	fun `Should findGroups reject an unknown sort field with 400`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "membersCount"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
	}

	@Test
	fun `Should findGroups return 403 without the read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should enableGroupById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.enableGroupById(any(), any(), any())).thenReturn(Mono.just(commonGroup()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<GroupWithoutMemberReaderDto>(OK)
		verify(service).enableGroupById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should findAssignableParticipants expose the eligibility sub-collection with q`() {
		// Arrange
		whenever(service.searchParticipantsByText(any(), anyOrNull())).thenReturn(Flux.just(commonParticipant()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/assignable-participants", listOf(projectId), listOf(Pair("q", "john"))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).searchParticipantsByText(projectId, "john")
	}

	@Test
	fun `Should findGroupById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.findGroupById(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(Mono.just(commonGroup()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<GroupReaderDto>(OK)
		verify(service).findGroupById(projectId, id, null, null, null)
	}

	@Test
	fun `Should findGroupById return 403 without the read authority`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findGroupMembersByGroupId return 200 with the v2 list grammar`() {
		// Arrange
		val id = UUID.randomUUID()
		val page = PageModel(PageableModel(20, 10), totalElements = 1, listOf(commonParticipant()))
		whenever(service.findGroupMembersPageByGroupId(any(), any(), any(), any())).thenReturn(Mono.just(page))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/members",
					listOf(projectId, id),
					listOf(Pair("page", 2), Pair("size", 10)),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<ParticipantReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		verify(service).findGroupMembersPageByGroupId(eq(projectId), eq(id), pageableCaptor.capture(), any())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
	}

	/**
	 * "Everyone the group expects right now, in or out alike" — the pair
	 * (availability yes, presence unspecified) no `status` value can express, which
	 * is the whole point of the parameter.
	 */
	@Test
	fun `Should findGroupMembersByGroupId narrow to the available members`() {
		// Arrange
		val id = UUID.randomUUID()
		val page = PageModel(PageableModel(0, 20), totalElements = 1, listOf(commonParticipant()))
		whenever(service.findGroupMembersPageByGroupId(any(), any(), any(), any())).thenReturn(Mono.just(page))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/members",
					listOf(projectId, id),
					listOf(Pair("available", true)),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<ParticipantReaderDto>>(OK)
		val searchCaptor = argumentCaptor<ParticipantSearchParamModel>()
		verify(service).findGroupMembersPageByGroupId(eq(projectId), eq(id), any(), searchCaptor.capture())
		assertEquals(true, searchCaptor.firstValue.availabilitySearched)
		assertEquals(null, searchCaptor.firstValue.presenceStatusSearched)
	}

	@Test
	fun `Should findGroupMembersByGroupId return 403 without the read authority`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/{id}/members", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should createGroup return 200`() {
		// Arrange
		val group = GroupWriterDto(name = "Scouts", members = listOf(UUID.randomUUID()))
		whenever(service.createGroup(any(), any())).thenReturn(Mono.just(commonGroup()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		result.body<GroupWithoutMemberReaderDto>(OK)
		verify(service).createGroup(any(), any())
	}

	@Test
	fun `Should createGroup return 403 without the create authority`() {
		// Arrange
		val group = GroupWriterDto(name = "Scouts", members = listOf(UUID.randomUUID()))

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should updateGroupById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		val group = GroupWriterDto(name = "Scouts", members = listOf(UUID.randomUUID()))
		whenever(service.updateGroupById(any(), any(), any(), any())).thenReturn(Mono.just(commonGroup()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		result.body<GroupWithoutMemberReaderDto>(OK)
		verify(service).updateGroupById(any(), eq(projectId), eq(id), any())
	}

	@Test
	fun `Should updateGroupById return 403 without the update authority`() {
		// Arrange
		val id = UUID.randomUUID()
		val group = GroupWriterDto(name = "Scouts", members = listOf(UUID.randomUUID()))

		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should addMembersToGroupById return 200 when every member is added`() {
		// Arrange
		val id = UUID.randomUUID()
		val memberIds = listOf(UUID.randomUUID(), UUID.randomUUID())
		whenever(service.addMembersToGroupById(any(), any(), any(), any()))
			.thenReturn(Mono.just(Pair(memberIds, emptyList())))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/members", listOf(projectId, id), emptyList()))
			.bodyValue(GroupMembersWriterDto(participantIds = memberIds))
			.exchange()

		// Assert
		result.body<AddedGroupMembersReaderDto>(OK)
		verify(service).addMembersToGroupById(any(), eq(projectId), eq(id), eq(memberIds))
	}

	@Test
	fun `Should addMembersToGroupById return 207 when some members are not added`() {
		// Arrange
		val id = UUID.randomUUID()
		val addedMemberId = UUID.randomUUID()
		val notAddedMemberId = UUID.randomUUID()
		val memberIds = listOf(addedMemberId, notAddedMemberId)
		whenever(service.addMembersToGroupById(any(), any(), any(), any()))
			.thenReturn(Mono.just(Pair(listOf(addedMemberId), listOf(notAddedMemberId))))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/members", listOf(projectId, id), emptyList()))
			.bodyValue(GroupMembersWriterDto(participantIds = memberIds))
			.exchange()

		// Assert
		result.body<AddedGroupMembersReaderDto>(MULTI_STATUS)
		verify(service).addMembersToGroupById(any(), eq(projectId), eq(id), eq(memberIds))
	}

	@Test
	fun `Should addMembersToGroupById return 403 without the update authority`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/members", listOf(projectId, id), emptyList()))
			.bodyValue(GroupMembersWriterDto(participantIds = listOf(UUID.randomUUID())))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should removeMemberFromGroupById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		val memberId = UUID.randomUUID()
		whenever(service.removeMemberFromGroupById(any(), any(), any(), any())).thenReturn(Mono.just(commonGroup()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}/members/{memberId}", listOf(projectId, id, memberId), emptyList()))
			.exchange()

		// Assert
		result.body<GroupWithoutMemberReaderDto>(OK)
		verify(service).removeMemberFromGroupById(any(), eq(projectId), eq(id), eq(memberId))
	}

	@Test
	fun `Should removeMemberFromGroupById return 403 without the update authority`() {
		// Arrange
		val id = UUID.randomUUID()
		val memberId = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}/members/{memberId}", listOf(projectId, id, memberId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should disableGroupById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.disableGroupById(any(), any(), any())).thenReturn(Mono.just(commonGroup()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<GroupWithoutMemberReaderDto>(OK)
		verify(service).disableGroupById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should disableGroupById return 403 without the update authority`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should deleteGroupById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.deleteGroupById(any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(OK)
		verify(service).deleteGroupById(projectId, id)
	}

	@Test
	fun `Should deleteGroupById return 403 without the delete authority`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}
}
