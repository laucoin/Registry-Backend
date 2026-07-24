package fr.laucoin.registry.backend.domain.service.impl

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
import fr.laucoin.registry.backend.test.ModelExt.activityId
import fr.laucoin.registry.backend.test.ModelExt.commonActivity
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.LocalDate
import java.util.UUID
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

class ActivityServiceTest {
	private val port: IActivityPort = mock()
	private val projectService: IProjectService = mock()
	private val movementPort: IMovementPort = mock()
	private val service: IActivityService = ActivityService(projectService, port, movementPort)

	@Test
	fun `Should findActivitiesPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ActivitySearchParamModel()
		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findActivitiesPage(projectId, pageable, params).block()

		// Assert
		verify(port).findPage(projectId, pageable, params)
	}

	@Test
	fun `Should findActivityById call port findById`() {
		// Arrange
		val onlyVisible = true
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonActivity()))

		// Act
		service.findActivityById(projectId, activityId, onlyVisible).block()

		// Assert
		verify(port).findById(projectId, activityId, onlyVisible)
	}

	@Test
	fun `Should findActivityById throw on empty result`() {
		// Arrange
		val onlyVisible = true
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findActivityById(projectId, activityId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(activityId.toString(), result.args?.first())

		verify(port).findById(projectId, activityId, onlyVisible)
	}

	@Test
	fun `Should findActivityMovementsPage call port findPageByActivityId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(typeSearched = MovementTypeEnum.IN)

		whenever(movementPort.findPageByActivityId(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findActivityMovementsPage(projectId, activityId, pageable, params).block()

		// Assert
		verify(movementPort).findPageByActivityId(projectId, activityId, pageable, params)
	}

	@Test
	fun `Should createActivity check date and call port create`() {
		// Arrange
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(projectId))
		whenever(port.create(any())).thenReturn(Mono.just(commonActivity()))

		// Act
		service.createActivity(currentUser(), commonActivity()).block()

		// Assert
		verify(projectService).validateDateTimes(
			projectId,
			start = null,
			end = null,
			ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		)
		verify(port).create(commonActivity())
	}

	@Test
	fun `Should updateActivityById check date, check existing activity, call port updateActivity`() {
		// Arrange
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(projectId))
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonActivity()))
		whenever(port.update(any())).thenReturn(Mono.just(commonActivity()))

		// Act
		service.updateActivityById(currentUser(), projectId, activityId, commonActivity()).block()

		// Assert
		verify(projectService).validateDateTimes(
			projectId,
			start = null,
			end = null,
			ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(port).findById(projectId, activityId, visibilitySearched = null)
		verify(port).update(commonActivity())
	}

	@Test
	fun `Should disableActivityById call existing activity and call port update`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonActivity()))
		whenever(port.update(any())).thenReturn(Mono.just(commonActivity()))

		// Act
		service.disableActivityById(currentUser(), projectId, activityId).block()

		// Assert
		verify(port).findById(projectId, activityId, visibilitySearched = true)
		verify(port).update(commonActivity().apply { visible = false })
	}

	@Test
	fun `Should enableActivityById call existing activity and call port update`() {
		// Arrange
		val activity = commonActivity().apply { visible = false }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(activity))
		whenever(port.update(any())).thenReturn(Mono.just(activity))

		// Act
		service.enableActivityById(currentUser(), projectId, activityId).block()

		// Assert
		verify(port).findById(projectId, activityId, visibilitySearched = false)
		verify(port).update(commonActivity())
	}

	@Test
	fun `Should deleteActivityById call existing activity, check no movement, and call port deleteById`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonActivity()))
		whenever(movementPort.countAllByActivityId(any(), any(), any())).thenReturn(Mono.just(0))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteActivityById(currentUser(), projectId, activityId).block()

		// Assert
		verify(port).findById(projectId, activityId, visibilitySearched = null)
		verify(movementPort).countAllByActivityId(projectId, activityId, MovementSearchParamModel())
		verify(port).deleteById(activityId)
	}

	@Test
	fun `Should deleteActivityById call existing activity, throw if movements are linked`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonActivity()))
		whenever(movementPort.countAllByActivityId(any(), any(), any())).thenReturn(Mono.just(1))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.deleteActivityById(currentUser(), projectId, activityId).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(ACTIVITY_DELETE_HAS_MOVEMENT, result.message)

		verify(port).findById(projectId, activityId, visibilitySearched = null)
		verify(movementPort).countAllByActivityId(projectId, activityId, MovementSearchParamModel())
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

		whenever(port.findUnusedSince(any())).thenReturn(Flux.just(activityId))

		// Act
		service.purgeActivitiesIfNecessary(date, true).collectList().block()

		// Assert
		verify(port).findUnusedSince(date)
		verify(port, never()).deleteById(any())
	}
}
