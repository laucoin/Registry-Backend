package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.model.ActivityModel
import kotlin.test.assertEquals
import org.mockito.kotlin.argumentCaptor
import java.time.Clock
import java.time.ZoneOffset
import java.time.Instant
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.ACTIVITY
import org.springframework.transaction.reactive.TransactionalOperator
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.test.ModelExt.defaultActivity
import fr.laucoin.registry.backend.test.ModelExt.defaultActivityId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
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
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class ActivityServiceTest {
	private val port: IActivityPort = mock()
	private val projectService: IProjectService = mock()
	private val movementPort: IMovementPort = mock()
	private val rowLockPort: IRowLockPort = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	init {
		whenever(rowLockPort.lock(any(), any())).thenReturn(Mono.just(Unit))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Any>(0) }
	}

	private val service: IActivityService = ActivityService(projectService, port, movementPort, rowLockPort, transactionalOperator, Clock.systemUTC())

	@Test
	fun `Should findActivitiesPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ActivitySearchParamModel()
		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findActivitiesPage(defaultProjectId, pageable, params).block()

		// Assert
		verify(port).findPage(defaultProjectId, pageable, params)
	}

	@Test
	fun `Should findActivityById call port findById`() {
		// Arrange
		val onlyVisible = true
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultActivity()))

		// Act
		service.findActivityById(defaultProjectId, defaultActivityId, onlyVisible).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultActivityId, onlyVisible)
	}

	@Test
	fun `Should findActivityById throw on empty result`() {
		// Arrange
		val onlyVisible = true
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findActivityById(defaultProjectId, defaultActivityId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(defaultActivityId.toString(), result.args?.first())

		verify(port).findById(defaultProjectId, defaultActivityId, onlyVisible)
	}

	@Test
	fun `Should findActivityMovementsPage call port findPageByActivityId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(type = MovementTypeEnum.IN)

		whenever(movementPort.findPageByActivityId(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findActivityMovementsPage(defaultProjectId, defaultActivityId, pageable, params).block()

		// Assert
		verify(movementPort).findPageByActivityId(defaultProjectId, defaultActivityId, pageable, params)
	}

	@Test
	fun `Should createActivity check date and call port create`() {
		// Arrange
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(defaultProjectId))
		whenever(port.create(any())).thenReturn(Mono.just(defaultActivity()))

		// Act
		service.createActivity(currentUser(), defaultActivity()).block()

		// Assert
		verify(projectService).validateDateTimes(
			defaultProjectId,
			start = null,
			end = null,
			ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		)
		verify(port).create(defaultActivity())
	}

	@Test
	fun `Should updateActivityById check date, check existing activity, call port updateActivity`() {
		// Arrange
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(defaultProjectId))
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultActivity()))
		whenever(port.update(any())).thenReturn(Mono.just(defaultActivity()))

		// Act
		service.updateActivityById(currentUser(), defaultProjectId, defaultActivityId, defaultActivity()).block()

		// Assert
		verify(projectService).validateDateTimes(
			defaultProjectId,
			start = null,
			end = null,
			ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(port).findById(defaultProjectId, defaultActivityId, isVisible = null)
		verify(port).update(defaultActivity())
	}

	@Test
	fun `Should disableActivityById call existing activity and call port update`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultActivity()))
		whenever(port.update(any())).thenReturn(Mono.just(defaultActivity()))

		// Act
		service.disableActivityById(currentUser(), defaultProjectId, defaultActivityId).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultActivityId, isVisible = true)
		verify(port).update(defaultActivity().apply { visible = false })
	}

	@Test
	fun `Should enableActivityById call existing activity and call port update`() {
		// Arrange
		val activity = defaultActivity().apply { visible = false }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(activity))
		whenever(port.update(any())).thenReturn(Mono.just(activity))

		// Act
		service.enableActivityById(currentUser(), defaultProjectId, defaultActivityId).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultActivityId, isVisible = false)
		verify(port).update(defaultActivity())
	}

	@Test
	fun `Should deleteActivityById call existing activity, check no movement, and call port deleteById`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultActivity()))
		whenever(movementPort.countAllByActivityId(any(), any(), any())).thenReturn(Mono.just(0))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteActivityById(currentUser(), defaultProjectId, defaultActivityId).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultActivityId, isVisible = null)
		verify(movementPort).countAllByActivityId(defaultProjectId, defaultActivityId, MovementSearchParamModel())
verify(rowLockPort).lock(ACTIVITY, defaultActivityId)
		verify(port).deleteById(defaultActivityId)
	}

	@Test
	fun `Should deleteActivityById call existing activity, throw if movements are linked`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultActivity()))
		whenever(movementPort.countAllByActivityId(any(), any(), any())).thenReturn(Mono.just(1))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.deleteActivityById(currentUser(), defaultProjectId, defaultActivityId).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(ACTIVITY_DELETE_HAS_MOVEMENT, result.message)

		verify(port).findById(defaultProjectId, defaultActivityId, isVisible = null)
		verify(movementPort).countAllByActivityId(defaultProjectId, defaultActivityId, MovementSearchParamModel())
		verify(port, never()).deleteById(any())
	}

	@Test
	fun `Should purgeActivitiesIfNecessary call unused activity since a date, and call port deleteById`() {
		// Arrange
		val date = LocalDate.EPOCH
		val activityId1 = UUID.randomUUID()
		val activityId2 = UUID.randomUUID()

		whenever(port.findUnusedSince(any())).thenReturn(Flux.just(activityId1, activityId2))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.purgeActivitiesIfNecessary(date, false).collectList().block()

		// Assert
		verify(port).findUnusedSince(date)
		verify(port).deleteById(activityId1)
		verify(port).deleteById(activityId2)
	}

	@Test
	fun `Should purgeActivitiesIfNecessary call unused activity since a date, and not call port deleteById because of dryRun`() {
		// Arrange
		val date = LocalDate.EPOCH

		whenever(port.findUnusedSince(any())).thenReturn(Flux.just(defaultActivityId))

		// Act
		service.purgeActivitiesIfNecessary(date, true).collectList().block()

		// Assert
		verify(port).findUnusedSince(date)
		verify(port, never()).deleteById(any())
	}

	@Test
	fun `Should createActivity stamp the creation with the injected clock`() {
		// Arrange
		val clock = Clock.fixed(Instant.parse("2030-01-02T03:04:05Z"), ZoneOffset.UTC)
		val clockedService = ActivityService(projectService, port, movementPort, rowLockPort, transactionalOperator, clock)
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(defaultProjectId))
		whenever(port.create(any())).thenReturn(Mono.just(defaultActivity()))

		// Act
		clockedService.createActivity(currentUser(), defaultActivity()).block()

		// Assert
		val captor = argumentCaptor<ActivityModel>()
		verify(port).create(captor.capture())
		assertEquals(ZonedDateTime.now(clock), captor.firstValue.creation!!.dateTime)
		assertEquals(ZonedDateTime.now(clock), captor.firstValue.lastEdition!!.dateTime)
	}
}
