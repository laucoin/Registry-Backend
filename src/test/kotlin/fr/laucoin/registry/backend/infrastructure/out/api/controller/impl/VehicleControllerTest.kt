package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
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
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.VehicleWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.VehicleWriterDtoMapper
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
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Mono

class VehicleControllerTest: TestContext() {
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
		private const val BASE_URL = "/api/v1/projects/{projectId}/vehicles"

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
		pageNumber: Int?,
		pageSize: Int?,
		textSearched: String?,
		visibilitySearched: Boolean?,
		statusSearched: PresenceStatusEnum?,
		dateTimeSearched: String?,
	) {
		// Arrange
		val expectedPageNumber = pageNumber ?: 0
		val expectedPageSize = pageSize ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = VehicleSearchParamModel(
			textSearched = textSearched,
			visibilitySearched = visibilitySearched,
			statusSearched = statusSearched,
			dateTimeSearched = dateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val page = PageModel(pageable, totalElements = 1, listOf(VehicleModel()))
		whenever(service.findVehiclesPage(any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(VehicleReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
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
						Pair("statusSearched", statusSearched),
						Pair("dateTimeSearched", dateTimeSearched),
					),
				)
			)
			.headers { headers -> requestedLocale?.let { headers.add(ACCEPT_LANGUAGE, it) } }
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findVehiclesPage(projectId, pageable, searchParams)
		verify(readerMapper).toDtoPage(page)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findVehicles throw due to wrong params`(
		pageNumber: Int?,
		pageSize: Int?,
		expectedMessage: String,
	) {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
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
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)

		verify(service).findVehicleById(projectId, uuid, visibilitySearched = null)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(movementReaderMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findVehicleMovements prepare param, call service and finally cast the result`(
		pageNumber: Int?,
		pageSize: Int?,
		visibilitySearched: Boolean?,
		typeSearched: MovementTypeEnum?,
		startDateTimeSearched: String?,
		endDateTimeSearched: String?,
	) {
		// Arrange
		val uuid = UUID.randomUUID()
		val expectedPageNumber = pageNumber ?: 0
		val expectedPageSize = pageSize ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = MovementSearchParamModel(
			visibilitySearched = visibilitySearched,
			typeSearched = typeSearched,
			startDateTimeSearched = startDateTimeSearched?.let {
				ZonedDateTime.parse(
					it,
					DateTimeFormatter.ISO_DATE_TIME
				)
			},
			endDateTimeSearched = endDateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val page = PageModel(pageable, totalElements = 1, listOf(MovementModel(contentType = REGISTERED)))
		whenever(service.findVehicleMovementsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(movementReaderMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(MovementReaderDto(contentType = REGISTERED))),
		)

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_VEHICLE_HISTORY_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE)
			)
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/movements",
					listOf(projectId, uuid),
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
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findVehicleMovementsPage(projectId, uuid, pageable, searchParams)
		verify(movementReaderMapper).toDtoPage(page)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findVehicleMovements throw due to wrong params`(
		pageNumber: Int?,
		pageSize: Int?,
		expectedMessage: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_VEHICLE_HISTORY_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE)
			)
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/movements",
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

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createVehicle return 200`() {
		// Arrange
		val vehicle = VehicleWriterDto(licensePlate = "AB-123-CD", brand = "Toyota", model = "Hilux")

		whenever(service.createVehicle(any(), any())).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())
		whenever(writerMapper.toModel(any(), any())).thenReturn(VehicleModel())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_C), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(vehicle)
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)

		verify(service).createVehicle(any(), any())
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(vehicle, projectId)
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
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
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
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.bodyValue(vehicle)
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)

		verify(service).updateVehicleById(any(), eq(projectId), eq(uuid), any())
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verify(writerMapper).toModel(vehicle, projectId)
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
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
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

		whenever(service.disableVehicleById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_U), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)

		verify(service).disableVehicleById(any(), eq(projectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should enableVehicleById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.enableVehicleById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_U), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)

		verify(service).enableVehicleById(any(), eq(projectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should deleteVehicleById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.deleteVehicleById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_D), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(OK)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).deleteVehicleById(any(), eq(projectId), eq(uuid))
	}
}
