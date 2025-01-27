package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_GROUP_C
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_GROUP_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_GROUP_METADATA_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_GROUP_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_GROUP_U
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.GroupWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.GroupReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.GroupWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.assertPage
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.ZonedDateTime.now
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class GroupControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
    @MockitoBean
    private lateinit var service: IGroupService

    @MockitoSpyBean
    private lateinit var readerMapper: GroupReaderDtoMapper

    @MockitoSpyBean
    private lateinit var participantReaderMapper: ParticipantReaderDtoMapper

    @MockitoSpyBean
    private lateinit var writerMapper: GroupWriterDtoMapper

    companion object {
        private const val BASE_URL = "/api/events/{eventId}/groups"

        @JvmStatic
        fun `Should findPage return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null, null, null, null, null, null, null),
            Arguments.of(50, null, null, null, null, null, null, null),
            Arguments.of(null, 25, null, null, null, null, null, null),
            Arguments.of(null, null, ASC, null, null, null, null, null),
            Arguments.of(null, null, DESC, null, null, null, null, null),
            Arguments.of(null, null, null, true, null, null, null, null),
            Arguments.of(null, null, null, false, null, null, null, null),
            Arguments.of(null, null, null, null, true, null, null, null),
            Arguments.of(null, null, null, null, false, null, null, null),
            Arguments.of(null, null, null, null, null, "searched", null, null),
            Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z", null),
            Arguments.of(null, null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
        )

        @JvmStatic
        fun `Wrong GroupDto`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                GroupWriterDto(name = null, members = listOf(UUID.randomUUID())),
                GROUP_NAME_NULL_OR_BLANK,
            ),
            Arguments.of(
                GroupWriterDto().apply {
                    name =
                        "azertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazerty"
                    members = listOf(UUID.randomUUID())
                },
                GROUP_NAME_TOO_LONG,
            ),
            Arguments.of(
                GroupWriterDto(
                    name = "name",
                    begin = now().plusDays(1),
                    end = now(),
                    members = listOf(UUID.randomUUID())
                ),
                GROUP_START_LATER_THAN_END,
            ),
            Arguments.of(
                GroupWriterDto().apply { name = "name"; members = null },
                GROUP_MEMBERS_EMPTY,
            ),
            Arguments.of(
                GroupWriterDto().apply { name = "name"; members = emptyList() },
                GROUP_MEMBERS_EMPTY,
            ),
        )

        @JvmStatic
        fun `Group management routes`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            val group = GroupWriterDto(name = "name", members = listOf(UUID.randomUUID()))
            return Stream.of(
                Arguments.of(GET, BASE_URL, listOf(eventId), null),
                Arguments.of(GET, "$BASE_URL/{id}", listOf(eventId, uuid), null),
                Arguments.of(POST, BASE_URL, listOf(eventId), group),
                Arguments.of(PATCH, "$BASE_URL/{id}", listOf(eventId, uuid), group),
                Arguments.of(PATCH, "$BASE_URL/{id}/disable", listOf(eventId, uuid), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/enable", listOf(eventId, uuid), null),
                Arguments.of(DELETE, "$BASE_URL/{id}", listOf(eventId, uuid), null),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("Group management routes")
    fun `Should return 401`(
        method: HttpMethod, uri: String, params: List<String>, body: Any?
    ) {
        // Arrange
        val request = webClient
            .method(method)
            .uri(uriBuilder(uri, params, listOf()))

        if (Objects.nonNull(body)) {
            request.bodyValue(body !!)
        }

        // Act
        val result = request.exchange()

        // Assert
        result.expectStatus().isUnauthorized
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(participantReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource("Group management routes")
    fun `Should return 403`(
        method: HttpMethod, uri: String, params: List<String>, body: Any?
    ) {
        // Arrange
        val request = webClient
            .authenticate()
            .method(method)
            .uri(uriBuilder(uri, params, listOf()))

        if (Objects.nonNull(body)) {
            request.bodyValue(body !!)
        }

        // Act
        val result = request.exchange()

        // Assert
        result.expectStatus().isForbidden
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(participantReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource("Should findPage return 200")
    fun `Should findGroups return 200`(
        offset: Int?,
        limit: Int?,
        order: Direction?,
        onlyVisible: Boolean?,
        onlyPresent: Boolean?,
        searched: String?,
        startDateTime: String?,
        endDateTime: String?,
    ) {
        // Arrange
        val expectedOrder = order ?: DESC
        val expectedOnlyVisible = onlyVisible ?: true
        val expectedOnlyPresent = onlyPresent ?: false
        val expectedOffset = offset ?: 0
        val expectedLimit = limit ?: 20
        val expectedSize = 1

        `when`(
            service.findGroups(
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just(GroupModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_GROUP_R))
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    listOf(eventId),
                    listOf(
                        Pair("offset", offset),
                        Pair("limit", limit),
                        Pair("order", order),
                        Pair("onlyVisible", onlyVisible),
                        Pair("onlyPresent", onlyPresent),
                        Pair("searched", searched),
                        Pair("startDateTime", startDateTime),
                        Pair("endDateTime", endDateTime),
                    ),
                )
            )
            .exchange()

        // Assert
        val body = result.body<PageDto<*>>(OK)

        assertNotNull(body)
        body !!.assertPage(
            expectedTotalElements = expectedSize,
            expectedOffset = expectedOffset,
            expectedLimit = expectedLimit,
        )

        verify(readerMapper, times(1)).toDtoPage(any(), any())
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).findGroups(
            eventId = eq(eventId),
            order = eq(expectedOrder),
            onlyVisible = eq(expectedOnlyVisible),
            onlyPresent = eq(expectedOnlyPresent),
            searched = eq(searched),
            startDateTime = anyOrNull(),
            endDateTime = anyOrNull(),
        )
    }

    @ParameterizedTest
    @MethodSource("Should findPage return 200")
    fun `Should findGroupMembersByGroupId return 200`(
        offset: Int?,
        limit: Int?,
        order: Direction?,
        onlyVisible: Boolean?,
        onlyPresent: Boolean?,
        searched: String?,
        startDateTime: String?,
        endDateTime: String?,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val expectedOrder = order ?: DESC
        val expectedOnlyVisible = onlyVisible ?: true
        val expectedOnlyPresent = onlyPresent ?: false
        val expectedOffset = offset ?: 0
        val expectedLimit = limit ?: 20
        val expectedSize = 1

        `when`(
            service.findGroupMembersByGroupId(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just(ParticipantModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_GROUP_R))
            .get()
            .uri(
                uriBuilder(
                    "$BASE_URL/{id}/members",
                    listOf(eventId, uuid),
                    listOf(
                        Pair("offset", offset),
                        Pair("limit", limit),
                        Pair("order", order),
                        Pair("onlyVisible", onlyVisible),
                        Pair("onlyPresent", onlyPresent),
                        Pair("searched", searched),
                        Pair("startDateTime", startDateTime),
                        Pair("endDateTime", endDateTime),
                    ),
                )
            )
            .exchange()

        // Assert
        val body = result.body<PageDto<*>>(OK)

        assertNotNull(body)
        body !!.assertPage(
            expectedTotalElements = expectedSize,
            expectedOffset = expectedOffset,
            expectedLimit = expectedLimit,
        )

        verifyNoInteractions(readerMapper)
        verify(participantReaderMapper, times(1)).toDtoPage(any(), any())
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).findGroupMembersByGroupId(
            eventId = eq(eventId),
            id = eq(uuid),
            order = eq(expectedOrder),
            onlyVisible = eq(expectedOnlyVisible),
            onlyPresent = eq(expectedOnlyPresent),
            searched = eq(searched),
            startDateTime = anyOrNull(),
            endDateTime = anyOrNull(),
        )
    }

    @Test
    fun `Should findGroupById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.findGroupById(any(), any(), any())).thenReturn(Mono.just(GroupModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_GROUP_R))
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<GroupReaderDto>(OK)
        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).findGroupById(eventId, uuid, onlyVisible = false)
    }

    @Test
    fun `Should searchParticipants return 200`() {
        // Arrange
        val testConfigMaxResult = 1
        val searched = "John"
        val participant = ParticipantModel()
        `when`(service.searchParticipants(any(), anyOrNull())).thenReturn(Flux.just(participant, participant))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_GROUP_METADATA_R))
            .get()
            .uri(
                uriBuilder("${BASE_URL}/search/participants", listOf(eventId), listOf(Pair("searched", searched)))
            )
            .exchange()

        // Assert
        val participants = result.body<List<*>>(OK)
        assertEquals(testConfigMaxResult, participants?.size)
        verifyNoInteractions(readerMapper)
        verify(participantReaderMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).searchParticipants(
            eventId,
            searched,
        )
    }

    @Test
    fun `Should createGroup return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val group = GroupWriterDto(name = "name", members = listOf(uuid))
        `when`(service.createGroup(any(), any())).thenReturn(Mono.just(GroupModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_GROUP_C))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(group)
            .exchange()

        // Assert
        result.body<GroupReaderDto>(OK)
        verify(readerMapper, times(1)).toDto(any(), any())
        verify(writerMapper, times(1)).toModel(any(), eq(eventId))
        verify(service, times(1)).createGroup(any(), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong GroupDto")
    fun `Should createGroup return 400`(
        group: GroupWriterDto,
        expectedCode: String,
    ) {
        // Arrange
        // Act
        val result = webClient
            .authenticate()
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(group)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedCode)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(participantReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should updateGroupById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val group = GroupWriterDto(name = "name", members = listOf(uuid))

        `when`(service.updateGroupById(any(), any(), any(), any())).thenReturn(Mono.just(GroupModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_GROUP_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .bodyValue(group)
            .exchange()

        // Assert
        result.body<GroupReaderDto>(OK)
        verify(readerMapper, times(1)).toDto(any(), any())
        verify(writerMapper, times(1)).toModel(any(), eq(eventId))
        verify(service, times(1)).updateGroupById(any(), eq(eventId), eq(uuid), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong GroupDto")
    fun `Should updateGroupById return 400`(
        group: GroupWriterDto,
        expectedCode: String,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = webClient
            .authenticate()
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .bodyValue(group)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedCode)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(participantReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should addMembersToGroupById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val memberIds = listOf(uuid1, uuid2)

        `when`(service.addMembersToGroupById(any(), any(), any(), any())).thenReturn(Mono.just(Pair(memberIds, emptyList())))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_GROUP_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/members", listOf(eventId, uuid), emptyList()))
            .bodyValue(memberIds)
            .exchange()

        // Assert
        result.body<AddedGroupMembersReaderDto>(OK)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).addMembersToGroupById(any(), eq(eventId), eq(uuid), eq(memberIds))
    }

    @Test
    fun `Should addMembersToGroupById return 207`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val memberIds = listOf(uuid1, uuid2)

        `when`(service.addMembersToGroupById(any(), any(), any(), any())).thenReturn(Mono.just(Pair(listOf(uuid1), listOf(uuid2))))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_GROUP_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/members", listOf(eventId, uuid), emptyList()))
            .bodyValue(memberIds)
            .exchange()

        // Assert
        result.body<AddedGroupMembersReaderDto>(MULTI_STATUS)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).addMembersToGroupById(any(), eq(eventId), eq(uuid), eq(memberIds))
    }

    @Test
    fun `Should removeMemberFromGroupById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val uuid1 = UUID.randomUUID()

        `when`(service.removeMemberFromGroupById(any(), any(), any(), any())).thenReturn(Mono.just(GroupModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_GROUP_U))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}/members/{memberId}", listOf(eventId, uuid, uuid1), emptyList()))
            .exchange()

        // Assert
        result.body<GroupReaderDto>(OK)
        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).removeMemberFromGroupById(any(), eq(eventId), eq(uuid), eq(uuid1))
    }

    @Test
    fun `Should disableGroupById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.disableGroupById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.just(GroupModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_GROUP_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/disable", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<GroupReaderDto>(OK)
        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).disableGroupById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should enableGroupById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.enableGroupById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.just(GroupModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_GROUP_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/enable", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<GroupReaderDto>(OK)
        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).enableGroupById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should deleteGroupById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.deleteGroupById(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_GROUP_D))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(participantReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).deleteGroupById(eq(eventId), eq(uuid))
    }
}
