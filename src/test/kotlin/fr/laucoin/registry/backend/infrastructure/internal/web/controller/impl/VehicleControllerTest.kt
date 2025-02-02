package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_BRAND_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_BRAND_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_MODEL_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_MODEL_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_REGISTRATION_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_REGISTRATION_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_VEHICLE_C
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_VEHICLE_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_VEHICLE_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_VEHICLE_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_VEHICLE_U
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.VehicleWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.VehicleWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.assertPage
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.ZonedDateTime
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

class VehicleControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
    @MockitoBean
    private lateinit var service: IVehicleService

    @MockitoSpyBean
    private lateinit var readerMapper: VehicleReaderDtoMapper

    @MockitoSpyBean
    private lateinit var movementReaderMapper: MovementReaderDtoMapper

    @MockitoSpyBean
    private lateinit var writerMapper: VehicleWriterDtoMapper

    companion object {
        private const val BASE_URL = "/api/events/{eventId}/vehicles"

        @JvmStatic
        fun `Should findVehicles return 200`(): Stream<Arguments> = Stream.of(
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
        fun `Wrong VehicleDto`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                VehicleWriterDto(
                    registration = null,
                    brand = "Toyota",
                    model = "Hilux",
                    begin = ZonedDateTime.now(),
                    end = ZonedDateTime.now().plusDays(1),
                ),
                VEHICLE_REGISTRATION_NULL_OR_BLANK
            ),
            Arguments.of(
                VehicleWriterDto(
                    registration = "",
                    brand = "Toyota",
                    model = "Hilux",
                    begin = ZonedDateTime.now(),
                    end = ZonedDateTime.now().plusDays(1),
                ),
                VEHICLE_REGISTRATION_NULL_OR_BLANK
            ),
            Arguments.of(
                VehicleWriterDto(
                    registration = "AB-123-CDAB-123-CDAB-123-CD",
                    brand = "Toyota",
                    model = "Hilux",
                    begin = ZonedDateTime.now(),
                    end = ZonedDateTime.now().plusDays(1),
                ),
                VEHICLE_REGISTRATION_TOO_LONG
            ),
            Arguments.of(
                VehicleWriterDto(
                    registration = "AB-123-CD",
                    brand = null,
                    model = "Hilux",
                    begin = ZonedDateTime.now(),
                    end = ZonedDateTime.now().plusDays(1),
                ),
                VEHICLE_BRAND_NULL_OR_BLANK
            ),
            Arguments.of(
                VehicleWriterDto(
                    registration = "AB-123-CD",
                    brand = "",
                    model = "Hilux",
                    begin = ZonedDateTime.now(),
                    end = ZonedDateTime.now().plusDays(1),
                ),
                VEHICLE_BRAND_NULL_OR_BLANK
            ),
            Arguments.of(
                VehicleWriterDto(
                    registration = "AB-123-CD",
                    brand = "ToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyota",
                    model = "Hilux",
                    begin = ZonedDateTime.now(),
                    end = ZonedDateTime.now().plusDays(1),
                ),
                VEHICLE_BRAND_TOO_LONG
            ),
            Arguments.of(
                VehicleWriterDto(
                    registration = "AB-123-CD",
                    brand = "Toyota",
                    model = null,
                    begin = ZonedDateTime.now(),
                    end = ZonedDateTime.now().plusDays(1),
                ),
                VEHICLE_MODEL_NULL_OR_BLANK
            ),
            Arguments.of(
                VehicleWriterDto(
                    registration = "AB-123-CD",
                    brand = "Toyota",
                    model = "",
                    begin = ZonedDateTime.now(),
                    end = ZonedDateTime.now().plusDays(1),
                ),
                VEHICLE_MODEL_NULL_OR_BLANK
            ),
            Arguments.of(
                VehicleWriterDto(
                    registration = "AB-123-CD",
                    brand = "Toyota",
                    model = "HiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHilux",
                    begin = ZonedDateTime.now(),
                    end = ZonedDateTime.now().plusDays(1),
                ),
                VEHICLE_MODEL_TOO_LONG
            ),
            Arguments.of(
                VehicleWriterDto(
                    registration = "AB-123-CD",
                    brand = "Toyota",
                    model = "Hilux",
                    begin = ZonedDateTime.now().plusDays(1),
                    end = ZonedDateTime.now(),
                ),
                VEHICLE_START_LATER_THAN_END
            ),
        )

        @JvmStatic
        fun `Should findVehicleMovements return 200`(): Stream<Arguments> = Stream.of(
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
        fun `Vehicle management routes`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            val vehicle = VehicleWriterDto(registration = "AB-123-CD", brand = "Toyota", model = "Hilux")
            return Stream.of(
                Arguments.of(GET, BASE_URL, listOf(eventId), null),
                Arguments.of(GET, "$BASE_URL/{id}", listOf(eventId, uuid), null),
                Arguments.of(GET, "$BASE_URL/{id}/movements", listOf(eventId, uuid), null),
                Arguments.of(POST, BASE_URL, listOf(eventId), vehicle),
                Arguments.of(PATCH, "$BASE_URL/{id}", listOf(eventId, uuid), vehicle),
                Arguments.of(PATCH, "$BASE_URL/{id}/disable", listOf(eventId, uuid), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/enable", listOf(eventId, uuid), null),
                Arguments.of(DELETE, "$BASE_URL/{id}", listOf(eventId, uuid), null),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("Vehicle management routes")
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
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource("Vehicle management routes")
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
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findVehicles return 200`(
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
            service.findVehiclesByEventId(
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
            .authenticate(buildAuthority(REGISTRY_EVENT_VEHICLE_R))
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
        verifyNoInteractions(movementReaderMapper)
        verify(service, times(1)).findVehiclesByEventId(
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
    fun `Should findVehicleById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.findVehicleById(any(), any(), any())).thenReturn(Mono.just(VehicleModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_VEHICLE_R))
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<VehicleReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(movementReaderMapper)
        verify(service, times(1)).findVehicleById(eventId, uuid, onlyVisible = false)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findVehicleMovements return 200`(
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
            service.findVehicleMovements(
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
            .authenticate(buildAuthority(REGISTRY_EVENT_VEHICLE_HISTORY_R))
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
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).findVehicleMovements(
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
    fun `Should createVehicle return 200`() {
        // Arrange
        val vehicle = VehicleWriterDto(registration = "AB-123-CD", brand = "Toyota", model = "Hilux")
        `when`(service.createVehicle(any(), any())).thenReturn(Mono.just(VehicleModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_VEHICLE_C))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(vehicle)
            .exchange()

        // Assert
        result.body<VehicleReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(movementReaderMapper)
        verify(writerMapper, times(1)).toModel(vehicle, eventId)
        verify(service, times(1)).createVehicle(any(), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong VehicleDto")
    fun `Should createVehicle return 400`(
        vehicle: VehicleWriterDto,
        expectedCode: String,
    ) {
        // Arrange
        // Act
        val result = webClient
            .authenticate()
            .post()
            .uri(uriBuilder(BASE_URL, listOf(eventId), emptyList()))
            .bodyValue(vehicle)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedCode)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }


    @Test
    fun `Should updateEventProfile return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val vehicle = VehicleWriterDto(registration = "AB-123-CD", brand = "Toyota", model = "Hilux")

        `when`(service.updateVehicleById(any(), any(), any(), any())).thenReturn(Mono.just(VehicleModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_VEHICLE_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .bodyValue(vehicle)
            .exchange()

        // Assert
        result.body<VehicleReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(movementReaderMapper)
        verify(writerMapper, times(1)).toModel(vehicle, eventId)
        verify(service, times(1)).updateVehicleById(any(), eq(eventId), eq(uuid), any())
    }

    @ParameterizedTest
    @MethodSource("Wrong VehicleDto")
    fun `Should updateVehicleById return 400`(
        vehicle: VehicleWriterDto,
        expectedCode: String,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = webClient
            .authenticate()
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .bodyValue(vehicle)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedCode)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should disableVehicleById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.disableVehicleById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.just(VehicleModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_VEHICLE_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/disable", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<VehicleReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(movementReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).disableVehicleById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should enableVehicleById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.enableVehicleById(any(), eq(eventId), eq(uuid))).thenReturn(Mono.just(VehicleModel()))

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_VEHICLE_U))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/enable", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<VehicleReaderDto>(OK)

        verify(readerMapper, times(1)).toDto(any(), any())
        verifyNoInteractions(movementReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).enableVehicleById(any(), eq(eventId), eq(uuid))
    }

    @Test
    fun `Should deleteVehicleById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.deleteVehicleById(any(), any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_EVENT_VEHICLE_D))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(eventId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service, times(1)).deleteVehicleById(any(), eq(eventId), eq(uuid))
    }
}
