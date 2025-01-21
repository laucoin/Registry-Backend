package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CONTENT_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANT_NULL
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_C
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_U
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.MovementWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.MovementWriterDto.MovementContentWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementParticipantsAndGroupsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.MovementWriterDtoMapper
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
import reactor.util.function.Tuples

class MovementControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
    @MockitoBean
    private lateinit var service: IMovementService

    @MockitoSpyBean
    private lateinit var readerMapper: MovementReaderDtoMapper

    @MockitoSpyBean
    private lateinit var movementTypeReaderMapper: MovementTypeReaderDtoMapper

    @MockitoSpyBean
    private lateinit var movementParticipantsAndGroupsReaderMapper: MovementParticipantsAndGroupsReaderDtoMapper

    @MockitoSpyBean
    private lateinit var writerMapper: MovementWriterDtoMapper

    companion object {
        private const val BASE_URL = "/api/events/{eventId}/movements"

        @JvmStatic
        fun `Should findMovements return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null, null, null, null, null, null),
            Arguments.of(50, null, null, null, null, null, null),
            Arguments.of(null, 25, null, null, null, null, null),
            Arguments.of(null, null, ASC, null, null, null, null),
            Arguments.of(null, null, DESC, null, null, null, null),
            Arguments.of(null, null, null, true, null, null, null),
            Arguments.of(null, null, null, false, null, null, null),
            Arguments.of(null, null, null, null, "searched", null, null),
            Arguments.of(null, null, null, null, null, "2024-11-14T18:34:33.000Z", null),
            Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
        )

        @JvmStatic
        fun `Wrong MovementDto`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                MovementWriterDto(
                    dateTime = null,
                    type = IN,
                    content = listOf(MovementContentWriterDto(participantId = UUID.randomUUID()))
                ),
                MOVEMENT_DATETIME_NULL,
            ),
            Arguments.of(
                MovementWriterDto(dateTime = now(), type = IN, content = null),
                MOVEMENT_CONTENT_EMPTY,
            ),
            Arguments.of(
                MovementWriterDto(dateTime = now(), type = IN, content = emptyList()),
                MOVEMENT_CONTENT_EMPTY,
            ),
            Arguments.of(
                MovementWriterDto(dateTime = now(), type = IN, content = listOf(MovementContentWriterDto(participantId = null))),
                MOVEMENT_PARTICIPANT_NULL,
            ),
        )

        @JvmStatic
        fun `Movement management routes`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            val movement =
                MovementWriterDto(dateTime = now(), type = IN, content = listOf(MovementContentWriterDto(participantId = uuid)))
            return Stream.of(
                Arguments.of(GET, BASE_URL, listOf(eventId), null),
                Arguments.of(GET, "$BASE_URL/{id}", listOf(eventId, uuid), null),
                Arguments.of(POST, BASE_URL, listOf(eventId), movement),
                Arguments.of(PATCH, "$BASE_URL/{id}", listOf(eventId, uuid), movement),
                Arguments.of(PATCH, "$BASE_URL/{id}/disable", listOf(eventId, uuid), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/enable", listOf(eventId, uuid), null),
                Arguments.of(DELETE, "$BASE_URL/{id}", listOf(eventId, uuid), null),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("Movement management routes")
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
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource("Movement management routes")
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
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findMovements return 200`(
        offset: Int?,
        limit: Int?,
        order: Direction?,
        onlyVisible: Boolean?,
        searched: String?,
        startDateTime: String?,
        endDateTime: String?,
    ) {
        // Arrange
        val expectedOrder = order ?: DESC
        val expectedOnlyVisible = onlyVisible ?: true
        val expectedOffset = offset ?: 0
        val expectedLimit = limit ?: 20
        val expectedSize = 1

        `when`(
            service.findMovements(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just(MovementModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_R))
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
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).findMovements(
            eventId = eq(eventId),
            order = eq(expectedOrder),
            onlyVisible = eq(expectedOnlyVisible),
            searched = eq(searched),
            type = eq(null),
            startDateTime = anyOrNull(),
            endDateTime = anyOrNull(),
        )
    }

    @Test
    fun `Should findMovementById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.findMovementById(any(), any(), any())).thenReturn(Mono.just(MovementModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_R))
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<MovementReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).findMovementById(eventId, uuid, onlyVisible = false)
    }

    @Test
    fun `Should searchParticipantsAndGroups return 200`() {
        // Arrange
        `when`(service.searchParticipantsAndGroups(any(), anyOrNull())).thenReturn(
            Mono.just(
                Tuples.of(
                    listOf(ParticipantModel()),
                    listOf(GroupModel()),
                )
            )
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_METADATA_R))
            .get()
            .uri(uriBuilder("$BASE_URL/search/participants-and-groups", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<MovementParticipantsAndGroupsReaderDto>(OK)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementTypeReaderMapper)
        verify(movementParticipantsAndGroupsReaderMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).searchParticipantsAndGroups(eventId, null)
    }

    @Test
    fun `Should getAvailableMovementTypes return 200`() {
        // Arrange
        `when`(service.availableMovementTypes()).thenReturn(Flux.just(*MovementTypeEnum.entries.toTypedArray()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_METADATA_R))
            .get()
            .uri(uriBuilder("$BASE_URL/types", listOf(eventId), emptyList()))
            .exchange()

        // Assert
        result.body<List<*>>(OK)

        verifyNoInteractions(readerMapper)
        verify(movementTypeReaderMapper, times(2)).toDto(any(), any())
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).availableMovementTypes()
    }

    @Test
    fun `Should createMovement return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val movement = MovementWriterDto(dateTime = now(), type = IN, content = listOf(MovementContentWriterDto(participantId = uuid)))
        `when`(service.createMovement(any(), any())).thenReturn(Mono.just(MovementModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_C))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(movement)
            .exchange()

        // Assert
        result.body<MovementReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verify(writerMapper, times(1)).toModel(any(), eq(eventId))
        verify(service, times(1)).createMovement(any(), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong MovementDto")
    fun `Should createMovement return 400`(
        movement: MovementWriterDto,
        expectedCode: String,
    ) {
        // Arrange
        // Act
        val result = webClient
            .authenticate()
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(movement)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedCode)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should updateMovementById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val movement = MovementWriterDto(dateTime = now(), type = IN, content = listOf(MovementContentWriterDto(participantId = uuid)))

        `when`(service.updateMovementById(any(), any(), any(), any())).thenReturn(Mono.just(MovementModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .bodyValue(movement)
            .exchange()

        // Assert
        result.body<MovementReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verify(writerMapper, times(1)).toModel(any(), eq(eventId))
        verify(service, times(1)).updateMovementById(any(), eq(eventId), eq(uuid), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong MovementDto")
    fun `Should updateMovementById return 400`(
        movement: MovementWriterDto,
        expectedCode: String,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = webClient
            .authenticate()
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .bodyValue(movement)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedCode)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should disableMovementById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.disableMovementById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.just(MovementModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/disable", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<MovementReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).disableMovementById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should enableMovementById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.enableMovementById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.just(MovementModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/enable", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<MovementReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).enableMovementById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should deleteMovementById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.deleteMovementById(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_D))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).deleteMovementById(eq(eventId), eq(uuid))
    }
}
