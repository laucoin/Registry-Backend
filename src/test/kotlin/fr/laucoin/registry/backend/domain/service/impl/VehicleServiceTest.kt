package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IVehicleService
import fr.laucoin.registry.backend.test.ModelExt.commonVehicle
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.ModelExt.vehicleId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

class VehicleServiceTest {
	private val port: IVehiclePort = mock()
	private val projectService: IProjectService = mock()
	private val movementPort: IMovementPort = mock()
	private val service: IVehicleService = VehicleService(port, projectService, movementPort)

	@Test
	fun `Should findVehiclesPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = VehicleSearchParamModel()

		whenever(port.findPage(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findVehiclesPage(projectId, pageable, params).block()

		// Assert
		verify(port).findPage(projectId, pageable, params, emptyList())
	}

	@Test
	fun `Should findVehicleById call port findById`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonVehicle()))

		// Act
		service.findVehicleById(projectId, vehicleId, onlyVisible).block()

		// Assert
		verify(port).findById(projectId, vehicleId, onlyVisible)
	}

	@Test
	fun `Should findVehicleById call port findById throw on empty result`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findVehicleById(projectId, vehicleId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(vehicleId.toString(), result.args?.first())

		verify(port).findById(projectId, vehicleId, onlyVisible)
	}

	@Test
	fun `Should findVehicleMovementsPage call port findPageByVehicleId`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(typeSearched = MovementTypeEnum.IN)

		whenever(movementPort.findPageByVehicleId(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findVehicleMovementsPage(projectId, uuid, pageable, params).block()

		// Assert
		verify(movementPort).findPageByVehicleId(projectId, uuid, pageable, params)
	}

	/**
	 * A vehicle's history is read to find out WHO had it, so each movement is
	 * annotated with the content rows naming this vehicle — its drivers. Rows for
	 * other vehicles on the same movement belong to another vehicle's history and
	 * are dropped.
	 */
	@Test
	fun `Should findVehicleMovementsPage attach only this vehicle's drivers`() {
		// Arrange
		val movementId = UUID.randomUUID()
		val movement = MovementModel().apply { id = movementId }
		val driver = MovementModel.MovementContentModel(
			participant = ParticipantModel().apply { firstName = "Nour" },
			vehicle = VehicleModel().apply { id = vehicleId },
		)
		val passengerOfAnotherVehicle = MovementModel.MovementContentModel(
			participant = ParticipantModel().apply { firstName = "Ilan" },
			vehicle = VehicleModel().apply { id = UUID.randomUUID() },
		)

		whenever(movementPort.findPageByVehicleId(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, listOf(movement))))
		whenever(movementPort.findContent(any(), any()))
			.thenReturn(Flux.just(movementId to listOf(driver, passengerOfAnotherVehicle)))

		// Act
		val result = service
			.findVehicleMovementsPage(projectId, vehicleId, PageableModel(0, 10), MovementSearchParamModel())
			.block()

		// Assert
		assertEquals(listOf(driver), result?.content?.first()?.content)
		verify(movementPort).findContent(projectId, listOf(movementId))
	}

	@Test
	fun `Should findVehicleMovementsPage not ask for content on an empty page`() {
		// Arrange
		whenever(movementPort.findPageByVehicleId(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 0, emptyList())))

		// Act
		val result = service
			.findVehicleMovementsPage(projectId, vehicleId, PageableModel(0, 10), MovementSearchParamModel())
			.block()

		// Assert
		assertEquals(emptyList<MovementModel>(), result?.content)
		verify(movementPort, never()).findContent(any(), any())
	}

	/**
	 * A movement the content query says nothing about carries no driver rather
	 * than keeping whatever the page happened to arrive with.
	 */
	@Test
	fun `Should findVehicleMovementsPage empty the content of a movement with no driver row`() {
		// Arrange
		val movement = MovementModel().apply { id = UUID.randomUUID() }

		whenever(movementPort.findPageByVehicleId(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, listOf(movement))))
		whenever(movementPort.findContent(any(), any())).thenReturn(Flux.empty())

		// Act
		val result = service
			.findVehicleMovementsPage(projectId, vehicleId, PageableModel(0, 10), MovementSearchParamModel())
			.block()

		// Assert
		assertEquals(emptyList<MovementModel.MovementContentModel>(), result?.content?.first()?.content)
	}

	@Test
	fun `Should createVehicle check date and call port create`() {
		// Arrange
		val vehicle = commonVehicle()

		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(projectId))

		whenever(port.create(any())).thenReturn(Mono.just(vehicle))

		// Act
		service.createVehicle(currentUser(), vehicle).block()

		// Assert
		verify(projectService).validateDateTimes(
			projectId,
			start = null,
			end = null,
			VEHICLE_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)

		verify(port).create(vehicle)
	}

	@Test
	fun `Should updateVehicleById check date, check existing vehicle, call port updateVehicle`() {
		// Arrange
		val vehicle = commonVehicle()

		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(projectId))

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(vehicle))
		whenever(port.update(any())).thenReturn(Mono.just(vehicle))

		// Act
		service.updateVehicleById(currentUser(), projectId, vehicleId, vehicle).block()

		// Assert
		verify(projectService).validateDateTimes(
			projectId,
			start = null,
			end = null,
			VEHICLE_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)

		verify(port).findById(projectId, vehicleId, visibilitySearched = null)
		verify(port).update(vehicle)
	}

	@Test
	fun `Should disableVehicleById call existing vehicle and call port update`() {
		// Arrange
		val vehicle = commonVehicle()

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(vehicle))
		whenever(port.update(any())).thenReturn(Mono.just(vehicle))

		// Act
		service.disableVehicleById(currentUser(), projectId, vehicleId).block()

		// Assert
		verify(port).findById(projectId, vehicleId, visibilitySearched = true)
		verify(port).update(vehicle.apply { visible = false })
	}

	@Test
	fun `Should enableVehicleById call existing vehicle and call port update`() {
		// Arrange
		val vehicle = commonVehicle().apply { visible = false }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(vehicle))
		whenever(port.update(any())).thenReturn(Mono.just(vehicle))

		// Act
		service.enableVehicleById(currentUser(), projectId, vehicleId).block()

		// Assert
		verify(port).findById(projectId, vehicleId, visibilitySearched = false)
		verify(port).update(vehicle.apply { visible = true })
	}

	@Test
	fun `Should deleteVehicleById call existing vehicle, check no movement, and call port deleteById`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonVehicle()))
		whenever(movementPort.countAllByVehicleId(any(), any(), any())).thenReturn(Mono.just(0))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteVehicleById(currentUser(), projectId, vehicleId).block()

		// Assert
		verify(port).findById(projectId, vehicleId, visibilitySearched = null)
		verify(movementPort).countAllByVehicleId(projectId, vehicleId, MovementSearchParamModel())
		verify(port).deleteById(vehicleId)
	}

	@Test
	fun `Should deleteVehicleById call existing vehicle, throw if movements are linked`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonVehicle()))
		whenever(movementPort.countAllByVehicleId(any(), any(), any())).thenReturn(Mono.just(1))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.deleteVehicleById(currentUser(), projectId, vehicleId).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(VEHICLE_DELETE_HAS_MOVEMENT, result.message)

		verify(port).findById(projectId, vehicleId, visibilitySearched = null)
		verify(movementPort).countAllByVehicleId(projectId, vehicleId, MovementSearchParamModel())
		verify(port, never()).deleteById(any())
	}

	@Test
	fun `Should purgeVehiclesIfNecessary call unused vehicle since a date, and call port deleteById`() {
		// Arrange
		val date = LocalDate.EPOCH
		val uuid1 = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()

		whenever(port.findUnusedSince(any())).thenReturn(Flux.just(uuid1, uuid2))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.purgeVehiclesIfNecessary(date, false).collectList().block()

		// Assert
		verify(port).findUnusedSince(date)
		verify(port).deleteById(uuid1)
		verify(port).deleteById(uuid2)
	}

	@Test
	fun `Should purgeVehiclesIfNecessary call unused vehicle since a date, and not call port deleteById because of dryRun`() {
		// Arrange
		val date = LocalDate.EPOCH

		whenever(port.findUnusedSince(any())).thenReturn(Flux.just(UUID.randomUUID()))

		// Act
		service.purgeVehiclesIfNecessary(date, true).collectList().block()

		// Assert
		verify(port).findUnusedSince(date)
		verify(port, never()).deleteById(any())
	}
}
