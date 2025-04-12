package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CONTENT_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CONTENT_PARTICIPANT_ID_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_TYPE_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_C
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_MOVEMENT_U
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.MovementWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.MovementWriterDto.MovementContentWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ActivityReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementContentReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementParticipantsAndGroupsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.MovementWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
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
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuples

class MovementControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
    @MockitoBean
    private lateinit var service: IMovementService

    @MockitoBean
    private lateinit var readerMapper: MovementReaderDtoMapper

    @MockitoBean
    private lateinit var readerContentMapper: MovementContentReaderDtoMapper

    @MockitoBean
    private lateinit var movementTypeReaderMapper: MovementTypeReaderDtoMapper

    @MockitoBean
    private lateinit var movementParticipantsAndGroupsReaderMapper: MovementParticipantsAndGroupsReaderDtoMapper

    @MockitoBean
    private lateinit var vehicleReaderMapper: VehicleReaderDtoMapper

    @MockitoBean
    private lateinit var activityReaderMapper: ActivityReaderDtoMapper

    @MockitoBean
    private lateinit var writerMapper: MovementWriterDtoMapper

    companion object {
        private const val BASE_URL = "/api/events/{eventId}/movements"

        @JvmStatic
        fun `Should findMovements return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of("not locale", null, null, null, null, null, null),
            Arguments.of(null, 0, null, null, null, null, null),
            Arguments.of(null, null, 200, null, null, null, null),
            Arguments.of(null, null, null, null, null, null, null),
            Arguments.of(null, null, null, true, null, null, null),
            Arguments.of(null, null, null, null, IN, null, null),
            Arguments.of(null, null, null, null, null, "2024-11-14T18:34:33.000Z", null),
            Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
        )

        @JvmStatic
        fun `Should findMovements throw due to wrong params`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(- 1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
                Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
                Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
            )
        }

        @JvmStatic
        fun `Wrong MovementDto`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                MovementWriterDto(
                    dateTime = null,
                    type = IN,
                    activityId = null,
                    content = listOf(MovementContentWriterDto(participantId = UUID.randomUUID()))
                ),
                MOVEMENT_DATETIME_NULL,
            ),
            Arguments.of(
                MovementWriterDto(
                    dateTime = now(),
                    type = null,
                    activityId = null,
                    content = listOf(MovementContentWriterDto(participantId = UUID.randomUUID()))
                ),
                MOVEMENT_TYPE_NULL,
            ),
            Arguments.of(
                MovementWriterDto(dateTime = now(), type = IN, activityId = null, content = null),
                MOVEMENT_CONTENT_EMPTY,
            ),
            Arguments.of(
                MovementWriterDto(dateTime = now(), type = IN, activityId = null, content = emptyList()),
                MOVEMENT_CONTENT_EMPTY,
            ),
            Arguments.of(
                MovementWriterDto(
                    dateTime = now(),
                    type = IN,
                    activityId = null,
                    content = listOf(MovementContentWriterDto(participantId = null))
                ),
                MOVEMENT_CONTENT_PARTICIPANT_ID_NULL,
            ),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findMovements return 200`(
        requestedLocale: String?,
        pageNumber: Int?,
        pageSize: Int?,
        visibilitySearched: Boolean?,
        typeSearched: MovementTypeEnum?,
        startDateTimeSearched: String?,
        endDateTimeSearched: String?,
    ) {
        // Arrange
        val expectedPageNumber = pageNumber ?: 0
        val expectedPageSize = pageSize ?: 20
        val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
        val searchParams = MovementSearchParamModel(
            visibilitySearched = visibilitySearched,
            typeSearched = typeSearched,
            startDateTimeSearched = startDateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
            endDateTimeSearched = endDateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
        )
        val page = PageModel(pageable, totalElements = 1, listOf(MovementModel()))
        whenever(service.findMovementsPage(any(), any(), any())).thenReturn(Mono.just(page))
        whenever(readerMapper.toDtoPage(any(), any())).thenReturn(
            PageModel(pageable, totalElements = 1, listOf(MovementReaderDto())),
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_R))
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    listOf(eventId),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                        Pair("visibilitySearched", visibilitySearched),
                        Pair("typeSearched", typeSearched),
                        Pair("startDateTimeSearched", startDateTimeSearched),
                        Pair("endDateTimeSearched", endDateTimeSearched),
                    ),
                )
            )
            .header(ACCEPT_LANGUAGE, requestedLocale)
            .exchange()

        // Assert
        result.body<PageModel<*>>(OK)

        verify(service).findMovementsPage(eventId, pageable, searchParams)
        verify(readerMapper).toDtoPage(any(), any())
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findMovements throw due to wrong params`(
        pageNumber: Int?,
        pageSize: Int?,
        expectedMessage: String,
    ) {
        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_R))
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    listOf(eventId),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                    ),
                )
            )
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedMessage)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should findMovementsContents return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        whenever(service.findMovementsContent(any(), any())).thenReturn(
            Flux.just(
                Pair(
                    uuid,
                    listOf(MovementModel.MovementContentModel())
                )
            )
        )
        whenever(readerContentMapper.toDto(any(), any())).thenReturn(MovementContentReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_R))
            .get()
            .uri(uriBuilder("$BASE_URL/contents", listOf(eventId), listOf(Pair("movementIds", uuid))))
            .exchange()

        // Assert
        result.body<List<*>>(OK)

        verify(service).findMovementsContent(eventId, listOf(uuid))
        verifyNoInteractions(readerMapper)
        verify(readerContentMapper).toDto(any(), any())
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should findMovementById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        whenever(service.findMovementById(any(), any(), anyOrNull())).thenReturn(Mono.just(MovementModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(MovementReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_R))
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<MovementReaderDto>(OK)

        verify(service).findMovementById(eventId, uuid, visibilitySearched = null)
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should searchParticipantsAndGroups return 200`() {
        // Arrange
        val searched = "text"
        whenever(service.searchParticipantsAndGroups(any(), anyOrNull())).thenReturn(
            Mono.just(
                Tuples.of(
                    listOf(ParticipantModel()),
                    listOf(GroupModel()),
                )
            )
        )
        whenever(movementParticipantsAndGroupsReaderMapper.toDto(any(), any())).thenReturn(
            MovementParticipantsAndGroupsReaderDto(
                emptyList(),
                emptyList(),
            )
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_METADATA_R))
            .get()
            .uri(uriBuilder("$BASE_URL/search/participants-and-groups", listOf(eventId), listOf(Pair("textSearched", searched))))
            .exchange()

        // Assert
        result.body<MovementParticipantsAndGroupsReaderDto>(OK)

        verify(service).searchParticipantsAndGroups(eventId, searched)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementTypeReaderMapper)
        verify(movementParticipantsAndGroupsReaderMapper).toDto(any(), any())
        verifyNoInteractions(vehicleReaderMapper)
        verifyNoInteractions(activityReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should searchVehicles return 200`() {
        // Arrange
        val searched = "text"
        whenever(service.searchVehicles(any(), anyOrNull())).thenReturn(Flux.just(VehicleModel()))
        whenever(vehicleReaderMapper.toDto(any(), any())).thenReturn(VehicleReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_METADATA_R), buildAuthority(REGISTRY_EVENT_OPTION_VEHICLE))
            .get()
            .uri(uriBuilder("$BASE_URL/search/vehicles", listOf(eventId), listOf(Pair("textSearched", searched))))
            .exchange()

        // Assert
        result.body<List<*>>(OK)

        verify(service).searchVehicles(eventId, searched)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verify(vehicleReaderMapper).toDto(any(), any())
        verifyNoInteractions(activityReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should searchActivities return 200`() {
        // Arrange
        val searched = "text"
        whenever(service.searchActivities(any(), anyOrNull())).thenReturn(Flux.just(ActivityModel()))
        whenever(activityReaderMapper.toDto(any(), any())).thenReturn(ActivityReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_METADATA_R), buildAuthority(REGISTRY_EVENT_OPTION_ACTIVITY))
            .get()
            .uri(uriBuilder("$BASE_URL/search/activities", listOf(eventId), listOf(Pair("textSearched", searched))))
            .exchange()

        // Assert
        result.body<List<*>>(OK)

        verify(service).searchActivities(eventId, searched)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(vehicleReaderMapper)
        verify(activityReaderMapper).toDto(any(), any())
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should createMovement return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val movement = MovementWriterDto(
            dateTime = now(),
            type = IN,
            activityId = null,
            content = listOf(MovementContentWriterDto(participantId = uuid))
        )
        whenever(service.createMovement(any(), any())).thenReturn(Mono.just(MovementModel()))
        whenever(writerMapper.toModel(any(), any())).thenReturn(MovementModel())
        whenever(readerMapper.toDto(any(), any())).thenReturn(MovementReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_C))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(movement)
            .exchange()

        // Assert
        result.body<MovementReaderDto>(OK)

        verify(service).createMovement(any(), any())
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verify(writerMapper).toModel(any(), eq(eventId))
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
        val movement = MovementWriterDto(
            dateTime = now(),
            type = IN,
            activityId = null,
            content = listOf(MovementContentWriterDto(participantId = uuid))
        )

        whenever(service.updateMovementById(any(), any(), any(), any())).thenReturn(Mono.just(MovementModel()))
        whenever(writerMapper.toModel(any(), any())).thenReturn(MovementModel())
        whenever(readerMapper.toDto(any(), any())).thenReturn(MovementReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .bodyValue(movement)
            .exchange()

        // Assert
        result.body<MovementReaderDto>(OK)

        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verify(writerMapper).toModel(any(), eq(eventId))
        verify(service).updateMovementById(any(), eq(eventId), eq(uuid), any())
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

        whenever(service.disableMovementById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.just(MovementModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(MovementReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/disable", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<MovementReaderDto>(OK)

        verify(service).disableMovementById(any(), eq(eventId), eq(uuid))
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should enableMovementById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.enableMovementById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.just(MovementModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(MovementReaderDto())


        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/enable", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<MovementReaderDto>(OK)

        verify(service).enableMovementById(any(), eq(eventId), eq(uuid))
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should deleteMovementById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.deleteMovementById(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_MOVEMENT_D))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)

        verify(service).deleteMovementById(eventId, uuid)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementTypeReaderMapper)
        verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
        verifyNoInteractions(writerMapper)
    }
}
