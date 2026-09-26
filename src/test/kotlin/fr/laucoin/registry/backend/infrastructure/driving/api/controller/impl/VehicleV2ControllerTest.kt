package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_BRAND_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_U
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.VehicleSortFieldEnum
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
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
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.LocalDate
import java.time.OffsetTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

class VehicleV2ControllerTest: TestContext() {
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
	}

	@Test
	fun `Should findVehicles call service with page, size and empty sort by default`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 1, listOf(VehicleModel()))
		whenever(service.findVehiclesPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findVehiclesPage(
			projectId,
			pageable,
			VehicleSearchParamModel(textSearched = null, visibilitySearched = null, statusSearched = null, dateTimeSearched = null),
			emptyList(),
		)
		verify(readerMapper, atLeastOnce()).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should findVehicles resolve q, visible, status and dateTime into VehicleSearchParamModel`() {
		// Arrange
		val pageable = PageableModel(40, 20)
		val page = PageModel(pageable, totalElements = 0, emptyList<VehicleModel>())
		whenever(service.findVehiclesPage(any(), any(), any(), any())).thenReturn(Mono.just(page))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(projectId),
					listOf(
						Pair("page", 2),
						Pair("size", 20),
						Pair("q", "hello"),
						Pair("visible", true),
						Pair("status", PresenceStatusEnum.IN),
					),
				)
			)
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findVehiclesPage(
			projectId,
			pageable,
			VehicleSearchParamModel(textSearched = "hello", visibilitySearched = true, statusSearched = PresenceStatusEnum.IN, dateTimeSearched = null),
			emptyList(),
		)
	}

	@Test
	fun `Should findVehicles return 400 when a query param is invalid`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("page", -1))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PAGE_NUMBER_IS_LOWER_THAN_ZERO)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findVehicles return 400 on an unknown sort field`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "unknownField"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findVehicles resolve a known sort field with direction`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 0, emptyList<VehicleModel>())
		whenever(service.findVehiclesPage(any(), any(), any(), any())).thenReturn(Mono.just(page))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(projectId),
					listOf(Pair("sort", "brand"), Pair("direction", "DESC")),
				)
			)
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)
		val sortCaptor = argumentCaptor<List<SortModel<VehicleSortFieldEnum>>>()
		verify(service).findVehiclesPage(eq(projectId), eq(pageable), any(), sortCaptor.capture())
		assertTrue(sortCaptor.firstValue.isNotEmpty())
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

	@Test
	fun `Should findVehicleMovements drop the Searched suffix and call service`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val pageable = PageableModel(0, 20)
		val searchParams = MovementSearchParamModel(visibilitySearched = true, typeSearched = IN)
		val page = PageModel(pageable, totalElements = 1, listOf(MovementModel(contentType = REGISTERED)))
		whenever(service.findVehicleMovementsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(movementReaderMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_HISTORY_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/movements",
					listOf(projectId, uuid),
					listOf(Pair("visible", true), Pair("type", IN)),
				)
			)
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findVehicleMovementsPage(projectId, uuid, pageable, searchParams)
		verify(movementReaderMapper, atLeastOnce()).toDto(any())
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should createVehicle return 201 with a Location header`() {
		// Arrange
		val vehicle = VehicleWriterDto(licensePlate = "AB-123-CD", brand = "Toyota", model = "Hilux")
		val createdId = UUID.randomUUID()

		whenever(service.createVehicle(any(), any())).thenReturn(Mono.just(VehicleModel().apply { id = createdId }))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto().apply { id = createdId })
		whenever(writerMapper.toModel(any(), any())).thenReturn(VehicleModel())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_C), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(vehicle)
			.exchange()

		// Assert
		val entityResult = result.expectStatus().isCreated.expectBody(VehicleReaderDto::class.java).returnResult()
		val location = entityResult.responseHeaders.getFirst("Location")
		assertNotNull(location)
		assertTrue(location!!.endsWith("/api/v2/projects/$projectId/vehicles/$createdId"))

		verify(service).createVehicle(any(), any())
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(vehicle, projectId)
		verifyNoInteractions(movementReaderMapper)
	}

	@Test
	fun `Should createVehicle return 400`() {
		// Arrange
		val vehicle = VehicleWriterDto(
			licensePlate = "AB-123-CD",
			brand = null,
			model = "Hilux",
			startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
			endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
		)

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(vehicle)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, VEHICLE_BRAND_NULL_OR_BLANK)

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

	@Test
	fun `Should disableVehicleById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.disableVehicleById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_U), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.post()
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
	fun `Should enableVehicleById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.enableVehicleById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_U), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.post()
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
	fun `Should deleteVehicleById return 204`() {
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
		result.body<Void>(NO_CONTENT)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).deleteVehicleById(any(), eq(projectId), eq(uuid))
	}
}
