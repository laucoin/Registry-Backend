package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_BIRTHDAY_FUTURE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PARTICIPANT_C
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PARTICIPANT_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PARTICIPANT_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PARTICIPANT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PARTICIPANT_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_PARTICIPANT_U
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ParticipantWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.ParticipantWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.assertPage
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.LocalDate
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
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ParticipantControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
    @MockitoBean
    private lateinit var service: IParticipantService

    @MockitoSpyBean
    private lateinit var readerMapper: ParticipantReaderDtoMapper

    @MockitoSpyBean
    private lateinit var groupReaderMapper: GroupWithoutMemberReaderDtoMapper

    @MockitoSpyBean
    private lateinit var movementReaderMapper: MovementReaderDtoMapper

    @MockitoSpyBean
    private lateinit var partialUserReaderMapper: PartialUserReaderDtoMapper

    @MockitoSpyBean
    private lateinit var writerMapper: ParticipantWriterDtoMapper

    companion object {
        private const val BASE_URL = "/api/events/{eventId}/participants"

        @JvmStatic
        fun `Should findParticipants return 200`(): Stream<Arguments> = Stream.of(
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
        fun `Wrong ParticipantDto`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ParticipantWriterDto(lastName = "DOE", birthday = LocalDate.now()),
                PARTICIPANT_FIRST_NAME_NULL_OR_BLANK,
            ),
            Arguments.of(
                ParticipantWriterDto(firstName = "", lastName = "DOE", birthday = LocalDate.now()),
                PARTICIPANT_FIRST_NAME_NULL_OR_BLANK,
            ),
            Arguments.of(
                ParticipantWriterDto(
                    firstName = "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiop",
                    lastName = "DOE",
                    birthday = LocalDate.now()
                ),
                PARTICIPANT_FIRST_NAME_TOO_LONG,
            ),
            Arguments.of(
                ParticipantWriterDto(firstName = "John", birthday = LocalDate.now()),
                PARTICIPANT_LAST_NAME_NULL_OR_BLANK,
            ),
            Arguments.of(
                ParticipantWriterDto(firstName = "John", lastName = "", birthday = LocalDate.now()),
                PARTICIPANT_LAST_NAME_NULL_OR_BLANK,
            ),
            Arguments.of(
                ParticipantWriterDto(
                    firstName = "John",
                    lastName = "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiop",
                    birthday = LocalDate.now()
                ),
                PARTICIPANT_LAST_NAME_TOO_LONG,
            ),
            Arguments.of(
                ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.now().plusDays(1)),
                PARTICIPANT_BIRTHDAY_FUTURE,
            ),
            Arguments.of(
                ParticipantWriterDto(
                    firstName = "John",
                    lastName = "DOE",
                    birthday = LocalDate.now(),
                    begin = now().plusDays(1),
                    end = now()
                ),
                PARTICIPANT_START_LATER_THAN_END,
            ),
        )

        @JvmStatic
        fun `Should findParticipantMovements return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null, null, null, null, null, null, null),
            Arguments.of(50, null, null, null, null, null, null, null),
            Arguments.of(null, 25, null, null, null, null, null, null),
            Arguments.of(null, null, ASC, null, null, null, null, null),
            Arguments.of(null, null, DESC, null, null, null, null, null),
            Arguments.of(null, null, null, true, null, null, null, null),
            Arguments.of(null, null, null, false, null, null, null, null),
            Arguments.of(null, null, null, null, "searched", null, null, null),
            Arguments.of(null, null, null, null, null, IN, null, null),
            Arguments.of(null, null, null, null, null, OUT, null, null),
            Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z", null),
            Arguments.of(null, null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
        )

        @JvmStatic
        fun `Participant management routes`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            val participant = ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.now())
            return Stream.of(
                Arguments.of(GET, BASE_URL, listOf(eventId), null),
                Arguments.of(GET, "$BASE_URL/{id}", listOf(eventId, uuid), null),
                Arguments.of(GET, "$BASE_URL/{id}/movements", listOf(eventId, uuid), null),
                Arguments.of(GET, "$BASE_URL/search/users", listOf(eventId), null),
                Arguments.of(GET, "$BASE_URL/search/groups", listOf(eventId), null),
                Arguments.of(POST, BASE_URL, listOf(eventId), participant),
                Arguments.of(PATCH, "$BASE_URL/{id}", listOf(eventId, uuid), participant),
                Arguments.of(PATCH, "$BASE_URL/{id}/disable", listOf(eventId, uuid), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/enable", listOf(eventId, uuid), null),
                Arguments.of(DELETE, "$BASE_URL/{id}", listOf(eventId, uuid), null),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("Participant management routes")
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
        verifyNoInteractions(groupReaderMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource("Participant management routes")
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
        verifyNoInteractions(groupReaderMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findParticipants return 200`(
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
        val expectedOrder = order ?: ASC
        val expectedOnlyVisible = onlyVisible ?: true
        val expectedOnlyPresent = onlyPresent ?: false
        val expectedOffset = offset ?: 0
        val expectedLimit = limit ?: 20
        val expectedSize = 0

        `when`(
            service.findParticipantsByEventId(
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PARTICIPANT_R))
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
        verifyNoInteractions(groupReaderMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).findParticipantsByEventId(
            eventId = eq(eventId),
            order = eq(expectedOrder),
            onlyVisible = eq(expectedOnlyVisible),
            onlyPresent = eq(expectedOnlyPresent),
            searched = eq(searched),
            startDateTime = anyOrNull(),
            endDateTime = anyOrNull(),
        )
    }

    @Test
    fun `Should findParticipantById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.findParticipantById(any(), any(), any())).thenReturn(Mono.just(ParticipantModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PARTICIPANT_R))
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<ParticipantReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).findParticipantById(eventId, uuid, onlyVisible = false)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findParticipantMovements return 200`(
        offset: Int?,
        limit: Int?,
        order: Direction?,
        onlyVisible: Boolean?,
        searched: String?,
        type: MovementTypeEnum?,
        startDateTime: String?,
        endDateTime: String?,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()

        val expectedOrder = order ?: DESC
        val expectedOnlyVisible = onlyVisible ?: true
        val expectedOffset = offset ?: 0
        val expectedLimit = limit ?: 20
        val expectedSize = 0

        `when`(
            service.findParticipantMovements(
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PARTICIPANT_HISTORY_R))
            .get()
            .uri(
                uriBuilder(
                    "$BASE_URL/{id}/movements",
                    listOf(eventId, uuid),
                    listOf(
                        Pair("offset", offset),
                        Pair("limit", limit),
                        Pair("order", order),
                        Pair("onlyVisible", onlyVisible),
                        Pair("searched", searched),
                        Pair("type", type),
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

        verify(movementReaderMapper, times(1)).toDtoPage(any(), any())
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(groupReaderMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).findParticipantMovements(
            eventId = eq(eventId),
            id = eq(uuid),
            order = eq(expectedOrder),
            onlyVisible = eq(expectedOnlyVisible),
            searched = eq(searched),
            type = eq(type),
            startDateTime = anyOrNull(),
            endDateTime = anyOrNull(),
        )
    }

    @Test
    fun `Should searchUsers return 200`() {
        // Arrange
        val testConfigMaxResult = 1
        val searched = "John"
        val user = UserModel()
        `when`(service.searchUsers(any(), anyOrNull())).thenReturn(Flux.just(user, user))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PARTICIPANT_METADATA_R))
            .get()
            .uri(
                uriBuilder("$BASE_URL/search/users", listOf(eventId), listOf(Pair("searched", searched)))
            )
            .exchange()

        // Assert
        val users = result.body<List<*>>(OK)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(groupReaderMapper)
        verify(partialUserReaderMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        assertEquals(testConfigMaxResult, users?.size)
        verify(service, times(1)).searchUsers(
            eventId,
            searched,
        )
    }

    @Test
    fun `Should searchGroups return 200`() {
        // Arrange
        val testConfigMaxResult = 1
        val searched = "Group"
        val group = GroupModel()
        `when`(service.searchGroups(any(), anyOrNull())).thenReturn(Flux.just(group, group))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PARTICIPANT_METADATA_R))
            .get()
            .uri(
                uriBuilder("$BASE_URL/search/groups", listOf(eventId), listOf(Pair("searched", searched)))
            )
            .exchange()

        // Assert
        val groups = result.body<List<*>>(OK)

        verifyNoInteractions(readerMapper)
        verify(groupReaderMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(writerMapper)
        assertEquals(testConfigMaxResult, groups?.size)
        verify(service, times(1)).searchGroups(
            eventId,
            searched,
        )
    }

    @Test
    fun `Should createParticipant return 200`() {
        // Arrange
        val participant = ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.now())
        `when`(service.createParticipant(any(), any())).thenReturn(Mono.just(ParticipantModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PARTICIPANT_C))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(participant)
            .exchange()

        // Assert
        result.body<ParticipantReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verify(writerMapper, times(1)).toModel(participant, eventId)
        verify(service, times(1)).createParticipant(any(), any())
    }

    @Test
    fun `Should createParticipant return 200 with User and Groups`() {
        // Arrange
        val participant = ParticipantWriterDto(
            firstName = "John",
            lastName = "DOE",
            birthday = LocalDate.now(),
            userId = UUID.randomUUID(),
            groupIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        )
        `when`(service.createParticipant(any(), any())).thenReturn(Mono.just(ParticipantModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PARTICIPANT_C))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(participant)
            .exchange()

        // Assert
        result.body<ParticipantReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verify(writerMapper, times(1)).toModel(participant, eventId)
        verify(service, times(1)).createParticipant(any(), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong ParticipantDto")
    fun `Should createParticipant return 400`(
        participant: ParticipantWriterDto,
        expectedCode: String,
    ) {
        // Arrange
        // Act
        val result = webClient
            .authenticate()
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(participant)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedCode)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(groupReaderMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }


    @Test
    fun `Should updateEventProfile return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val participant = ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.now())

        `when`(service.updateParticipantById(any(), any(), any(), any())).thenReturn(Mono.just(ParticipantModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PARTICIPANT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .bodyValue(participant)
            .exchange()

        // Assert
        result.body<ParticipantReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verify(writerMapper, times(1)).toModel(participant, eventId)
        verify(service, times(1)).updateParticipantById(any(), eq(eventId), eq(uuid), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong ParticipantDto")
    fun `Should updateParticipantById return 400`(
        participant: ParticipantWriterDto,
        expectedCode: String,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = webClient
            .authenticate()
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .bodyValue(participant)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedCode)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(groupReaderMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should disableParticipantById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.disableParticipantById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.just(ParticipantModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PARTICIPANT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/disable", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<ParticipantReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).disableParticipantById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should enableParticipantById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.enableParticipantById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.just(ParticipantModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PARTICIPANT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/enable", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<ParticipantReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).enableParticipantById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should deleteParticipantById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.deleteParticipantById(any(), any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_PARTICIPANT_D))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(groupReaderMapper)
        verifyNoInteractions(partialUserReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).deleteParticipantById(any(), eq(eventId), eq(uuid))
    }
}
