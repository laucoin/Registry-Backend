package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_U
import fr.laucoin.registry.backend.domain.enumeration.VehicleSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.VehicleSortFieldEnum.BRAND
import fr.laucoin.registry.backend.domain.enumeration.VehicleSortFieldEnum.LICENSE_PLATE
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.VehicleWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.commonMovement
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
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.test.assertEquals

class VehicleV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IVehicleService

	@MockitoBean
	private lateinit var readerMapper: VehicleReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/vehicles"
	}

	@Test
	fun `Should findVehicles return 200 with the v2 list grammar`() {
		// Arrange
		val pageable = PageableModel(20, 10)
		val page = PageModel(pageable, totalElements = 1, listOf(VehicleModel()))
		whenever(service.findVehiclesPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
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
						Pair("page", 2),
						Pair("size", 10),
						Pair("q", "toyota"),
						Pair("visible", true),
						Pair("sort", "brand,licensePlate"),
						Pair("direction", "DESC"),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<VehicleReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		val sortCaptor = argumentCaptor<List<SortModel<VehicleSortFieldEnum>>>()
		verify(service).findVehiclesPage(eq(projectId), pageableCaptor.capture(), any(), sortCaptor.capture())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
		assertEquals(listOf(SortModel(BRAND, descending = true), SortModel(LICENSE_PLATE, descending = true)), sortCaptor.firstValue)
	}

	@Test
	fun `Should findVehicles reject an unknown sort field with 400`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "speed"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
	}

	@Test
	fun `Should findVehicles return 403 without the read authority`() {
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
	fun `Should disableVehicleById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.disableVehicleById(any(), any(), any())).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_U), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)
		verify(service).disableVehicleById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should enableVehicleById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.enableVehicleById(any(), any(), any())).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_U), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)
		verify(service).enableVehicleById(any(), eq(projectId), eq(id))
	}

	private fun vehicle() = VehicleWriterDto(
		licensePlate = "AA-123-BB",
		brand = "Toyota",
		model = "Corolla",
	)

	@Test
	fun `Should findVehicleById return 200 with the mapped vehicle`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.findVehicleById(any(), any(), anyOrNull())).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE), buildAuthority(REGISTRY_PROJECT_VEHICLE_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)
		verify(service).findVehicleById(projectId, id, visibilitySearched = null)
		verify(readerMapper).toDto(any())
	}

	@Test
	fun `Should findVehicleById return 403 without the vehicle option`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_VEHICLE_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findVehicleMovements return 200 with the movements page`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.findVehicleMovementsPage(any(), any(), any(), any())).thenReturn(
			Mono.just(PageModel(PageableModel(0, 20), 1, listOf(commonMovement()))),
		)

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE),
				buildAuthority(REGISTRY_PROJECT_VEHICLE_HISTORY_R),
			)
			.get()
			.uri(uriBuilder("$BASE_URL/{id}/movements", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<PageModel<MovementReaderDto>>(OK)
		verify(service).findVehicleMovementsPage(eq(projectId), eq(id), eq(PageableModel(0, 20)), any())
	}

	@Test
	fun `Should findVehicleMovements return 403 without the history authority`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE), buildAuthority(REGISTRY_PROJECT_VEHICLE_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}/movements", listOf(projectId, UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should createVehicle return 200 and delegate to the service`() {
		// Arrange
		whenever(service.createVehicle(any(), any())).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE), buildAuthority(REGISTRY_PROJECT_VEHICLE_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(vehicle())
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)
		verify(service).createVehicle(any(), any())
	}

	@Test
	fun `Should createVehicle return 403 without the create authority`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(vehicle())
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should updateVehicleById return 200 and delegate to the service`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.updateVehicleById(any(), any(), any(), any())).thenReturn(Mono.just(VehicleModel()))
		whenever(readerMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE), buildAuthority(REGISTRY_PROJECT_VEHICLE_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.bodyValue(vehicle())
			.exchange()

		// Assert
		result.body<VehicleReaderDto>(OK)
		verify(service).updateVehicleById(any(), eq(projectId), eq(id), any())
	}

	@Test
	fun `Should updateVehicleById return 403 without the update authority`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, UUID.randomUUID()), emptyList()))
			.bodyValue(vehicle())
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should deleteVehicleById return 200 and delegate to the service`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.deleteVehicleById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE), buildAuthority(REGISTRY_PROJECT_VEHICLE_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(OK)
		verify(service).deleteVehicleById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should deleteVehicleById return 403 without the delete authority`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE), buildAuthority(REGISTRY_PROJECT_VEHICLE_U))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}
}
