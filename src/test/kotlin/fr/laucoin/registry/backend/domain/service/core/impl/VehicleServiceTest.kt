package fr.laucoin.registry.backend.domain.service.core.impl

import reactor.test.StepVerifier
import org.mockito.kotlin.verifyNoInteractions
import kotlin.test.assertNull
import java.time.ZonedDateTime
import java.time.ZoneOffset
import fr.laucoin.registry.backend.domain.model.TimelineRangeModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.OutingModel
import fr.laucoin.registry.backend.domain.model.MovementEventModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.VEHICLE
import org.springframework.transaction.reactive.TransactionalOperator
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.domain.helper.ProjectDateHelper
import fr.laucoin.registry.backend.domain.service.core.IVehicleService
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.ModelExt.defaultVehicle
import fr.laucoin.registry.backend.test.ModelExt.defaultVehicleId
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
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

class VehicleServiceTest {
	private val port: IVehiclePort = mock()
	private val projectDates: ProjectDateHelper = mock()
	private val projectPort: IProjectPort = mock()
	private val movementPort: IMovementPort = mock()
	private val rowLockPort: IRowLockPort = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	init {
		whenever(rowLockPort.lock(any(), any())).thenReturn(Mono.just(Unit))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Any>(0) }
	}

	private val service: IVehicleService = VehicleService(port, projectDates, projectPort, movementPort, rowLockPort, transactionalOperator, Clock.systemUTC())

	@Test
	fun `Should findVehiclesPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = VehicleSearchParamModel()

		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		val result = service.findVehiclesPage(defaultProjectId, pageable, params).block()

		// Assert
		assertEquals(PageModel(1, 2, 3, 4, emptyList<Any>()), result)
		verify(port).findPage(defaultProjectId, pageable, params)
	}

	@Test
	fun `Should findVehicleTimeline return the outings, the time out and the usage rate over the availability window`() {
		// Arrange
		val departure = ZonedDateTime.of(2026, 7, 2, 9, 0, 0, 0, ZoneOffset.UTC)
		val vehicle = defaultVehicle().apply {
			startAvailability = CustomDateTimeModel(LocalDate.of(2026, 7, 1))
			endAvailability = CustomDateTimeModel(LocalDate.of(2026, 7, 2))
		}
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(vehicle))
		whenever(projectPort.findById(any(), anyOrNull())).thenReturn(Mono.just(ProjectModel()))
		whenever(movementPort.findEventsByVehicleId(any(), any())).thenReturn(
			Flux.just(MovementEventModel(departure, OUT), MovementEventModel(departure.plusHours(3), IN))
		)

		// Act
		val timeline = service.findVehicleTimeline(defaultProjectId, defaultVehicleId).block()!!

		// Assert
		assertEquals(listOf(OutingModel(departure, departure.plusHours(3))), timeline.actual)
		assertEquals(3 * 3600L, timeline.outSeconds)
		assertEquals(3.0 * 3600 / (48 * 3600 - 1), timeline.usageRate!!, 0.001)
		assertEquals(1, timeline.planned.size)
	}

	@Test
	fun `Should findVehicleTimeline fall back on the project dates when the vehicle has no availability window`() {
		// Arrange
		val departure = ZonedDateTime.of(2026, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC)
		val project = ProjectModel(
			begin = CustomDateTimeModel(LocalDate.of(2026, 7, 1)),
			end = CustomDateTimeModel(LocalDate.of(2026, 7, 1)),
		)
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultVehicle()))
		whenever(projectPort.findById(any(), anyOrNull())).thenReturn(Mono.just(project))
		whenever(movementPort.findEventsByVehicleId(any(), any())).thenReturn(
			Flux.just(MovementEventModel(departure, OUT), MovementEventModel(departure.plusHours(12), IN))
		)

		// Act
		val timeline = service.findVehicleTimeline(defaultProjectId, defaultVehicleId).block()!!

		// Assert
		assertEquals(emptyList<TimelineRangeModel>(), timeline.planned)
		assertEquals(0.5, timeline.usageRate!!, 0.001)
	}

	@Test
	fun `Should findVehicleTimeline only give the time out when no window can be determined`() {
		// Arrange
		val departure = ZonedDateTime.of(2026, 7, 1, 8, 0, 0, 0, ZoneOffset.UTC)
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultVehicle()))
		whenever(projectPort.findById(any(), anyOrNull())).thenReturn(Mono.just(ProjectModel()))
		whenever(movementPort.findEventsByVehicleId(any(), any())).thenReturn(
			Flux.just(MovementEventModel(departure, OUT), MovementEventModel(departure.plusHours(2), IN))
		)

		// Act
		val timeline = service.findVehicleTimeline(defaultProjectId, defaultVehicleId).block()!!

		// Assert
		assertEquals(2 * 3600L, timeline.outSeconds)
		assertNull(timeline.usageRate)
	}

	@Test
	fun `Should findVehicleTimeline throw when the vehicle does not exist`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())
		whenever(projectPort.findById(any(), anyOrNull())).thenReturn(Mono.just(ProjectModel()))

		// Act
		val result = service.findVehicleTimeline(defaultProjectId, defaultVehicleId)

		// Assert
		StepVerifier.create(result).expectError(RegistryException::class.java).verify()
		verifyNoInteractions(movementPort)
	}

	@Test
	fun `Should findVehicleById call port findById`() {
		// Arrange
		val expected = defaultVehicle()
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(expected))

		// Act
		val result = service.findVehicleById(defaultProjectId, defaultVehicleId, onlyVisible).block()

		// Assert
		assertEquals(expected, result)
		verify(port).findById(defaultProjectId, defaultVehicleId, onlyVisible)
	}

	@Test
	fun `Should findVehicleById call port findById throw on empty result`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findVehicleById(defaultProjectId, defaultVehicleId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(defaultVehicleId.toString(), result.args?.first())

		verify(port).findById(defaultProjectId, defaultVehicleId, onlyVisible)
	}

	@Test
	fun `Should findVehicleMovementsPage call port findPageByVehicleId`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(type = MovementTypeEnum.IN)

		whenever(movementPort.findPageByVehicleId(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		val result = service.findVehicleMovementsPage(defaultProjectId, uuid, pageable, params).block()

		// Assert
		assertEquals(PageModel(1, 2, 3, 4, emptyList<Any>()), result)
		verify(movementPort).findPageByVehicleId(defaultProjectId, uuid, pageable, params)
	}

	@Test
	fun `Should createVehicle check date and call port create`() {
		// Arrange
		val vehicle = defaultVehicle()

		whenever(projectDates.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(defaultProjectId))

		whenever(port.create(any())).thenReturn(Mono.just(vehicle))

		// Act
		val result = service.createVehicle(currentUser(), vehicle).block()

		// Assert
		assertEquals(vehicle, result)
		verify(projectDates).validateDateTimes(
			defaultProjectId,
			start = null,
			end = null,
			VEHICLE_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)

		verify(port).create(vehicle)
	}

	@Test
	fun `Should updateVehicleById check date, check existing vehicle, call port updateVehicle`() {
		// Arrange
		val vehicle = defaultVehicle()

		whenever(projectDates.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(defaultProjectId))

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(vehicle))
		whenever(port.update(any())).thenReturn(Mono.just(vehicle))

		// Act
		val result = service.updateVehicleById(currentUser(), defaultProjectId, defaultVehicleId, vehicle).block()

		// Assert
		assertEquals(vehicle, result)
		verify(projectDates).validateDateTimes(
			defaultProjectId,
			start = null,
			end = null,
			VEHICLE_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)

		verify(port).findById(defaultProjectId, defaultVehicleId, isVisible = null)
		verify(port).update(vehicle)
	}

	@Test
	fun `Should disableVehicleById call existing vehicle and call port update`() {
		// Arrange
		val vehicle = defaultVehicle()

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(vehicle))
		whenever(port.update(any())).thenReturn(Mono.just(vehicle))

		// Act
		val result = service.disableVehicleById(currentUser(), defaultProjectId, defaultVehicleId).block()

		// Assert
		assertEquals(vehicle, result)
		verify(port).findById(defaultProjectId, defaultVehicleId, isVisible = true)
		verify(port).update(vehicle.apply { visible = false })
	}

	@Test
	fun `Should enableVehicleById call existing vehicle and call port update`() {
		// Arrange
		val vehicle = defaultVehicle().apply { visible = false }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(vehicle))
		whenever(port.update(any())).thenReturn(Mono.just(vehicle))

		// Act
		val result = service.enableVehicleById(currentUser(), defaultProjectId, defaultVehicleId).block()

		// Assert
		assertEquals(vehicle, result)
		verify(port).findById(defaultProjectId, defaultVehicleId, isVisible = false)
		verify(port).update(vehicle.apply { visible = true })
	}

	@Test
	fun `Should deleteVehicleById call existing vehicle, check no movement, and call port deleteById`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultVehicle()))
		whenever(movementPort.countAllByVehicleId(any(), any(), any())).thenReturn(Mono.just(0))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteVehicleById(currentUser(), defaultProjectId, defaultVehicleId).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultVehicleId, isVisible = null)
		verify(movementPort).countAllByVehicleId(defaultProjectId, defaultVehicleId, MovementSearchParamModel())
verify(rowLockPort).lock(VEHICLE, defaultVehicleId)
		verify(port).deleteById(defaultVehicleId)
	}

	@Test
	fun `Should deleteVehicleById call existing vehicle, throw if movements are linked`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultVehicle()))
		whenever(movementPort.countAllByVehicleId(any(), any(), any())).thenReturn(Mono.just(1))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.deleteVehicleById(currentUser(), defaultProjectId, defaultVehicleId).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(VEHICLE_DELETE_HAS_MOVEMENT, result.message)

		verify(port).findById(defaultProjectId, defaultVehicleId, isVisible = null)
		verify(movementPort).countAllByVehicleId(defaultProjectId, defaultVehicleId, MovementSearchParamModel())
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
		val result = service.purgeVehiclesIfNecessary(date, false).collectList().block()

		// Assert
		assertEquals(listOf(uuid1, uuid2), result)
		verify(port).findUnusedSince(date)
		verify(port).deleteById(uuid1)
		verify(port).deleteById(uuid2)
	}

	@Test
	fun `Should purgeVehiclesIfNecessary call unused vehicle since a date, and not call port deleteById because of dryRun`() {
		// Arrange
		val expected = UUID.randomUUID()
		val date = LocalDate.EPOCH

		whenever(port.findUnusedSince(any())).thenReturn(Flux.just(expected))

		// Act
		val result = service.purgeVehiclesIfNecessary(date, true).collectList().block()

		// Assert
		assertEquals(listOf(expected), result)
		verify(port).findUnusedSince(date)
		verify(port, never()).deleteById(any())
	}
}
