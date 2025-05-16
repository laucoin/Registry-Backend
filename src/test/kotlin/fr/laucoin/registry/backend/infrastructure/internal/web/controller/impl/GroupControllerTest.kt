package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_U
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.GroupWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.AddedGroupMembersReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.GroupReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.GroupWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class GroupControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
    @MockitoBean
    private lateinit var service: IGroupService

    @MockitoBean
    private lateinit var readerMapper: GroupReaderDtoMapper

    @MockitoBean
    private lateinit var lightReaderMapper: GroupWithoutMemberReaderDtoMapper

    @MockitoBean
    private lateinit var participantReaderMapper: ParticipantReaderDtoMapper

    @MockitoBean
    private lateinit var addedGroupMembersReaderMapper: AddedGroupMembersReaderDtoMapper

    @MockitoBean
    private lateinit var writerMapper: GroupWriterDtoMapper

    companion object {
        private const val BASE_URL = "/api/projects/{projectId}/groups"

        @JvmStatic
        fun `Should findGroups return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of("not locale", null, null, null, null, null, null),
            Arguments.of(null, 0, null, null, null, null, null),
            Arguments.of(null, null, 200, null, null, null, null),
            Arguments.of(null, null, null, null, null, null, null),
            Arguments.of(null, null, null, "text", null, null, null),
            Arguments.of(null, null, null, null, true, null, null),
            Arguments.of(null, null, null, null, null, true, null),
            Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
        )

        @JvmStatic
        fun `Should findGroups throw due to wrong params`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(- 1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
                Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
                Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
            )
        }

        @JvmStatic
        fun `Should findGroupMembersByGroupId return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of("not locale", null, null, null, null, null, null),
            Arguments.of(null, 0, null, null, null, null, null),
            Arguments.of(null, null, 200, null, null, null, null),
            Arguments.of(null, null, null, null, null, null, null),
            Arguments.of(null, null, null, "text", null, null, null),
            Arguments.of(null, null, null, null, true, null, null),
            Arguments.of(null, null, null, null, null, IN, null),
            Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
        )

        @JvmStatic
        fun `Should findGroupMembersByGroupId throw due to wrong params`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(- 1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
                Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
                Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
            )
        }

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
                    startAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
                    endAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
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
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findGroups return 200`(
        requestedLocale: String?,
        pageNumber: Int?,
        pageSize: Int?,
        textSearched: String?,
        visibilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: String?,
    ) {
        // Arrange
        val expectedPageNumber = pageNumber ?: 0
        val expectedPageSize = pageSize ?: 20
        val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
        val searchParams = GroupSearchParamModel(
            textSearched = textSearched,
            visibilitySearched = visibilitySearched,
            presenceSearched = presenceSearched,
            dateTimeSearched = dateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
        )
        val page = PageModel(pageable, totalElements = 1, listOf(GroupModel()))
        whenever(service.findGroupsPage(any(), any(), any())).thenReturn(Mono.just(page))
        whenever(lightReaderMapper.toDtoPage(any(), any())).thenReturn(
            PageModel(pageable, totalElements = 1, listOf(GroupReaderDto())),
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    listOf(projectId),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                        Pair("textSearched", textSearched),
                        Pair("visibilitySearched", visibilitySearched),
                        Pair("presenceSearched", presenceSearched),
                        Pair("dateTimeSearched", dateTimeSearched),
                    ),
                )
            )
            .header(ACCEPT_LANGUAGE, requestedLocale)
            .exchange()

        // Assert
        result.body<PageModel<*>>(OK)

        verify(service).findGroupsPage(projectId, pageable, searchParams)
        verify(lightReaderMapper).toDtoPage(any(), any())
        verifyNoInteractions(participantReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findGroups throw due to wrong params`(
        pageNumber: Int?,
        pageSize: Int?,
        expectedMessage: String,
    ) {
        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    listOf(projectId),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                    ),
                )
            )
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedMessage)

        verifyNoInteractions(service)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(participantReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findGroupMembersByGroupId return 200`(
        requestedLocale: String?,
        pageNumber: Int?,
        pageSize: Int?,
        textSearched: String?,
        visibilitySearched: Boolean?,
        statusSearched: PresenceStatusEnum?,
        dateTimeSearched: String?,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val expectedPageNumber = pageNumber ?: 0
        val expectedPageSize = pageSize ?: 20
        val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
        val searchParams = ParticipantSearchParamModel(
            textSearched = textSearched,
            visibilitySearched = visibilitySearched,
            statusSearched = statusSearched,
            dateTimeSearched = dateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
        )
        val page = PageModel(pageable, totalElements = 1, listOf(ParticipantModel()))
        whenever(service.findGroupMembersPageByGroupId(any(), any(), any(), any())).thenReturn(Mono.just(page))
        whenever(participantReaderMapper.toDtoPage(any(), any())).thenReturn(
            PageModel(pageable, totalElements = 1, listOf(ParticipantReaderDto())),
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
            .get()
            .uri(
                uriBuilder(
                    "$BASE_URL/{id}/members",
                    listOf(projectId, uuid),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                        Pair("textSearched", textSearched),
                        Pair("visibilitySearched", visibilitySearched),
                        Pair("statusSearched", statusSearched),
                        Pair("dateTimeSearched", dateTimeSearched),
                    ),
                )
            )
            .header(ACCEPT_LANGUAGE, requestedLocale)
            .exchange()

        // Assert
        result.body<PageModel<*>>(OK)

        verify(service).findGroupMembersPageByGroupId(projectId, uuid, pageable, searchParams)
        verifyNoInteractions(readerMapper)
        verify(participantReaderMapper).toDtoPage(any(), any())
        verifyNoInteractions(writerMapper)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findGroupMembersByGroupId throw due to wrong params`(
        pageNumber: Int?,
        pageSize: Int?,
        expectedMessage: String,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
            .get()
            .uri(
                uriBuilder(
                    "$BASE_URL/{id}/members",
                    listOf(projectId, uuid),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                    ),
                )
            )
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedMessage)

        verifyNoInteractions(service)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(participantReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should findGroupById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        whenever(service.findGroupById(any(), any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(GroupModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(GroupReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<GroupReaderDto>(OK)

        verify(service).findGroupById(projectId, uuid, visibilitySearched = null, memberAvailabilitySearched = null)
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should searchParticipants return 200`() {
        // Arrange
        val searched = "John"
        val participant = ParticipantModel()
        whenever(service.searchParticipantsByText(any(), anyOrNull())).thenReturn(Flux.just(participant))
        whenever(participantReaderMapper.toDto(any(), any())).thenReturn(ParticipantReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_METADATA_R))
            .get()
            .uri(
                uriBuilder("${BASE_URL}/search/participants", listOf(projectId), listOf(Pair("textSearched", searched)))
            )
            .exchange()

        // Assert
        result.body<List<*>>(OK)

        verify(service).searchParticipantsByText(projectId, searched)
        verifyNoInteractions(readerMapper)
        verify(participantReaderMapper).toDto(any(), any())
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should createGroup return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val group = GroupWriterDto(name = "name", members = listOf(uuid))
        whenever(service.createGroup(any(), any())).thenReturn(Mono.just(GroupModel()))
        whenever(writerMapper.toModel(any(), any())).thenReturn(GroupModel())
        whenever(lightReaderMapper.toDto(any(), any())).thenReturn(GroupReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_C))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
            .bodyValue(group)
            .exchange()

        // Assert
        result.body<GroupReaderDto>(OK)

        verify(service).createGroup(any(), any())
        verify(lightReaderMapper).toDto(any(), any())
        verifyNoInteractions(participantReaderMapper)
        verify(writerMapper).toModel(any(), eq(projectId))
    }

    @ParameterizedTest
    @MethodSource("Wrong GroupDto")
    fun `Should createGroup return 400`(
        group: GroupWriterDto,
        expectedCode: String,
    ) {
        // Act
        val result = webClient
            .authenticate()
            .post()
            .uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
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

        whenever(service.updateGroupById(any(), any(), any(), any())).thenReturn(Mono.just(GroupModel()))
        whenever(writerMapper.toModel(any(), any())).thenReturn(GroupModel())
        whenever(lightReaderMapper.toDto(any(), any())).thenReturn(GroupReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
            .bodyValue(group)
            .exchange()

        // Assert
        result.body<GroupReaderDto>(OK)

        verify(lightReaderMapper).toDto(any(), any())
        verify(writerMapper).toModel(any(), eq(projectId))
        verifyNoInteractions(participantReaderMapper)
        verify(service).updateGroupById(any(), eq(projectId), eq(uuid), any())
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
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
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

        whenever(service.addMembersToGroupById(any(), any(), any(), any())).thenReturn(Mono.just(Pair(memberIds, emptyList())))
        whenever(addedGroupMembersReaderMapper.toDto(any(), any())).thenReturn(AddedGroupMembersReaderDto(emptyList(), emptyList()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/members", listOf(projectId, uuid), emptyList()))
            .bodyValue(memberIds)
            .exchange()

        // Assert
        result.body<AddedGroupMembersReaderDto>(OK)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(writerMapper)
        verify(service).addMembersToGroupById(any(), eq(projectId), eq(uuid), eq(memberIds))
    }

    @Test
    fun `Should addMembersToGroupById return 207`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val memberIds = listOf(uuid1, uuid2)

        whenever(service.addMembersToGroupById(any(), any(), any(), any())).thenReturn(Mono.just(Pair(listOf(uuid1), listOf(uuid2))))
        whenever(addedGroupMembersReaderMapper.toDto(any(), any())).thenReturn(
            AddedGroupMembersReaderDto(
                listOf(uuid1),
                listOf(uuid2)
            )
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/members", listOf(projectId, uuid), emptyList()))
            .bodyValue(memberIds)
            .exchange()

        // Assert
        result.body<AddedGroupMembersReaderDto>(MULTI_STATUS)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(writerMapper)
        verify(service).addMembersToGroupById(any(), eq(projectId), eq(uuid), eq(memberIds))
    }

    @Test
    fun `Should removeMemberFromGroupById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val uuid1 = UUID.randomUUID()

        whenever(service.removeMemberFromGroupById(any(), any(), any(), any())).thenReturn(Mono.just(GroupModel()))
        whenever(lightReaderMapper.toDto(any(), any())).thenReturn(GroupReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}/members/{memberId}", listOf(projectId, uuid, uuid1), emptyList()))
            .exchange()

        // Assert
        result.body<GroupReaderDto>(OK)
        verify(lightReaderMapper).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verify(service).removeMemberFromGroupById(any(), eq(projectId), eq(uuid), eq(uuid1))
    }

    @Test
    fun `Should disableGroupById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.disableGroupById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(GroupModel()))
        whenever(lightReaderMapper.toDto(any(), any())).thenReturn(GroupReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<GroupReaderDto>(OK)

        verify(lightReaderMapper).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verify(service).disableGroupById(any(), eq(projectId), eq(uuid))
    }

    @Test
    fun `Should enableGroupById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.enableGroupById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(GroupModel()))
        whenever(lightReaderMapper.toDto(any(), any())).thenReturn(GroupReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<GroupReaderDto>(OK)

        verify(lightReaderMapper).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verify(service).enableGroupById(any(), eq(projectId), eq(uuid))
    }

    @Test
    fun `Should deleteGroupById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.deleteGroupById(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_D))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(participantReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service).deleteGroupById(eq(projectId), eq(uuid))
    }
}
