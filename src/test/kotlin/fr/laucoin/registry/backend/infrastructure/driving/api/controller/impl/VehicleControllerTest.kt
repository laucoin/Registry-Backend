package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.RESOURCE_MODIFIED_SINCE_READ
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_BRAND_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_BRAND_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_LICENSE_PLATE_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_LICENSE_PLATE_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_MODEL_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_MODEL_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_U
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.VehicleWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.VehicleWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.hiddenElementsAuthorities
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.stream.Stream

class VehicleControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IVehicleService

	@MockitoBean
	private lateinit var readerMapper: VehicleReaderDtoMapper

	@MockitoBean
	private lateinit var movementReaderMapper: MovementReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: VehicleWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/vehicles"

		@JvmStatic
		fun `Should findVehicles prepare param, call service and finally cast the result`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of("not locale", null, null, null, null, null, null),
				Arguments.of(null, 0, null, null, null, null, null),
				Arguments.of(null, null, 200, null, null, null, null),
				Arguments.of(null, null, null, null, null, null, null),
				Arguments.of(null, null, null, "text", null, null, null),
				Arguments.of(null, null, null, null, true, null, null),
				Arguments.of(null, null, null, null, null, PresenceStatusEnum.IN, null),
				Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
			)
		}

		@JvmStatic
		fun `Should findVehicles throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}

		@JvmStatic
		fun `Should findVehicleMovements prepare param, call service and finally cast the result`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(null, null, null, null, null, null),
				Arguments.of(0, null, null, null, null, null),
				Arguments.of(null, 200, null, null, null, null),
				Arguments.of(null, null, null, null, null, null),
				Arguments.of(null, null, true, null, null, null),
				Arguments.of(null, null, null, IN, null, null),
				Arguments.of(null, null, null, null, "2024-11-14T18:34:33.000Z", null),
				Arguments.of(null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
			)
		}

		@JvmStatic
		fun `Should findVehicleMovements throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}

		@JvmStatic
		fun `Wrong VehicleDto`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				VehicleWriterDto(
					licensePlate = null,
					brand = "Toyota",
					model = "Hilux",
					startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
				),
				VEHICLE_LICENSE_PLATE_NULL_OR_BLANK
			),
			Arguments.of(
				VehicleWriterDto(
					licensePlate = "",
					brand = "Toyota",
					model = "Hilux",
					startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
				),
				VEHICLE_LICENSE_PLATE_NULL_OR_BLANK
			),
			Arguments.of(
				VehicleWriterDto(
					licensePlate = "AB-123-CDAB-123-CDAB-123-CD",
					brand = "Toyota",
					model = "Hilux",
					startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
				),
				VEHICLE_LICENSE_PLATE_TOO_LONG
			),
			Arguments.of(
				VehicleWriterDto(
					licensePlate = "AB-123-CD",
					brand = null,
					model = "Hilux",
					startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
				),
				VEHICLE_BRAND_NULL_OR_BLANK
			),
			Arguments.of(
				VehicleWriterDto(
					licensePlate = "AB-123-CD",
					brand = "",
					model = "Hilux",
					startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
				),
				VEHICLE_BRAND_NULL_OR_BLANK
			),
			Arguments.of(
				VehicleWriterDto(
					licensePlate = "AB-123-CD",
					brand = "ToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyotaToyota",
					model = "Hilux",
					startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
				),
				VEHICLE_BRAND_TOO_LONG
			),
			Arguments.of(
				VehicleWriterDto(
					licensePlate = "AB-123-CD",
					brand = "Toyota",
					model = null,
					startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
				),
				VEHICLE_MODEL_NULL_OR_BLANK
			),
			Arguments.of(
				VehicleWriterDto(
					licensePlate = "AB-123-CD",
					brand = "Toyota",
					model = "",
					startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
				),
				VEHICLE_MODEL_NULL_OR_BLANK
			),
			Arguments.of(
				VehicleWriterDto(
					licensePlate = "AB-123-CD",
					brand = "Toyota",
					model = "HiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHiluxHilux",
					startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
				),
				VEHICLE_MODEL_TOO_LONG
			),
			Arguments.of(
				VehicleWriterDto(
					licensePlate = "AB-123-CD",
					brand = "Toyota",
					model = "Hilux",
					startAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
					endAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
				),
				VEHICLE_START_LATER_THAN_END
			),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findVehicles prepare param, call service and finally cast the result`(
		requestedLocale: String?,
		page: Int?,
		size: Int?,
		query: String?,
		isVisible: Boolean?,
		status: PresenceStatusEnum?,
		at: String?,
	) {
		// Arrange
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = VehicleSearchParamModel(
			query = query,
			isVisible = isVisible,
			status = status,
			at = at?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(VehicleModel()))
		whenever(service.findVehiclesPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(VehicleReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_VEHICLE_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(
						Pair("page", page),
						Pair("size", size),
						Pair("q", query),
						Pair("isVisible", isVisible),
						Pair("status", status),
						Pair("at", at),
					),
				)
			)
			.headers { headers -> requestedLocale?.let { headers.add(ACCEPT_LANGUAGE, it) } }
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findVehiclesPage(defaultProjectId, pageable, searchParams)
		verify(readerMapper).toDtoPage(expectedPage)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findVehicles throw due to wrong params`(
		page: Int?,
		size: Int?,
		expectedMessage: String,
	) {
		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_VEHICLE_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(
						Pair("page", page),
						Pair("size", size),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedMessage)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findVehicleById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findVehicleById(any(), any(), anyOrNull())).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_VEHICLE_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)

		verify(service).findVehicleById(defaultProjectId, uuid, isVisible = null)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(movementReaderMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findVehicleMovements prepare param, call service and finally cast the result`(
		page: Int?,
		size: Int?,
		isVisible: Boolean?,
		type: MovementTypeEnum?,
		from: String?,
		to: String?,
	) {
		// Arrange
		val uuid = UUID.randomUUID()
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = MovementSearchParamModel(
			isVisible = isVisible,
			type = type,
			from = from?.let {
				ZonedDateTime.parse(
					it,
					DateTimeFormatter.ISO_DATE_TIME
				)
			},
			to = to?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(MovementModel(contentType = REGISTERED)))
		whenever(service.findVehicleMovementsPage(any(), any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(movementReaderMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(MovementReaderDto(contentType = REGISTERED))),
		)

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), 
				buildAuthority(REGISTRY_PROJECT_VEHICLE_HISTORY_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE)
			)
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/movements",
					listOf(defaultProjectId, uuid),
					listOf(
						Pair("page", page),
						Pair("size", size),
						Pair("isVisible", isVisible),
						Pair("type", type),
						Pair("from", from),
						Pair("to", to),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findVehicleMovementsPage(defaultProjectId, uuid, pageable, searchParams)
		verify(movementReaderMapper).toDtoPage(expectedPage)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findVehicleMovements throw due to wrong params`(
		page: Int?,
		size: Int?,
		expectedMessage: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), 
				buildAuthority(REGISTRY_PROJECT_VEHICLE_HISTORY_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE)
			)
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/movements",
					listOf(defaultProjectId, uuid),
					listOf(
						Pair("page", page),
						Pair("size", size),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedMessage)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createVehicle return 201`() {
		// Arrange
		val createdId = UUID.randomUUID()
		val vehicle = VehicleWriterDto(licensePlate = "AB-123-CD", brand = "Toyota", model = "Hilux")

		whenever(service.createVehicle(any(), any())).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto().apply { id = createdId })
		whenever(writerMapper.toModel(any(), any())).thenReturn(VehicleModel())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_C), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(vehicle)
			.exchange()

		// Assert
		result.expectHeader()
			.valueEquals("Location", "${BASE_URL.replace("{projectId}", defaultProjectId.toString())}/$createdId")
		result.body<VehicleReaderDto>(CREATED)

		verify(service).createVehicle(any(), any())
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(vehicle, defaultProjectId)
		verifyNoInteractions(movementReaderMapper)
	}

	@ParameterizedTest
	@MethodSource("Wrong VehicleDto")
	fun `Should createVehicle return 400`(
		vehicle: VehicleWriterDto,
		expectedCode: String,
	) {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
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
	fun `Should updateVehicle return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val vehicle = VehicleWriterDto(licensePlate = "AB-123-CD", brand = "Toyota", model = "Hilux")

		whenever(service.updateVehicleById(any(), any(), any(), any())).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())
		whenever(writerMapper.toModel(any(), any())).thenReturn(VehicleModel())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_U), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(vehicle)
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)

		verify(service).updateVehicleById(any(), eq(defaultProjectId), eq(uuid), any())
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verify(writerMapper).toModel(vehicle, defaultProjectId)
	}

	@Test
	fun `Should updateVehicle accept the matching If-Match`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val vehicle = VehicleWriterDto(licensePlate = "AB-123-CD", brand = "Toyota", model = "Hilux")

		whenever(service.updateVehicleById(any(), any(), any(), any())).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())
		whenever(writerMapper.toModel(any(), any())).thenReturn(VehicleModel())
		whenever(service.findVehicleById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(VehicleModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_U), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1791885731000\"")
			.bodyValue(vehicle)
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)

		verify(service).updateVehicleById(any(), eq(defaultProjectId), eq(uuid), any())
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verify(writerMapper).toModel(vehicle, defaultProjectId)
	}

	@Test
	fun `Should updateVehicleById throw when the element changed since it was read`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val vehicle = VehicleWriterDto(licensePlate = "AB-123-CD", brand = "Toyota", model = "Hilux")

		whenever(writerMapper.toModel(any(), any())).thenReturn(VehicleModel())
		whenever(service.findVehicleById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(VehicleModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_U), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1\"")
			.bodyValue(vehicle)
			.exchange()

		// Assert
		result.assertError(PRECONDITION_FAILED, RESOURCE_MODIFIED_SINCE_READ)

		verify(service, never()).updateVehicleById(any(), eq(defaultProjectId), eq(uuid), any())
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
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
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

		whenever(
			service.disableVehicleById(
				any(),
				eq(defaultProjectId),
				eq(uuid)
			)
		).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_U), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)

		verify(service).disableVehicleById(any(), eq(defaultProjectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should enableVehicleById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.enableVehicleById(any(), eq(defaultProjectId), eq(uuid))).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_D), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)

		verify(service).enableVehicleById(any(), eq(defaultProjectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should deleteVehicleById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.deleteVehicleById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_D), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).deleteVehicleById(any(), eq(defaultProjectId), eq(uuid))
	}

	@Test
	fun `Should findVehicles forward brand and model to the service`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val searchParams = VehicleSearchParamModel(
			query = null,
			isVisible = null,
			status = null,
			at = null,
			brand = "Renault",
			model = "Trafic"
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(VehicleModel()))
		whenever(service.findVehiclesPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(VehicleReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_VEHICLE_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(Pair("brand", "Renault"), Pair("model", "Trafic"))
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findVehiclesPage(defaultProjectId, pageable, searchParams)
	}
}
