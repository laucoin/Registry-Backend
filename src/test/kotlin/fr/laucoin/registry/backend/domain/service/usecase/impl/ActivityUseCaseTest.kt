package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.ActivityStatisticsModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.core.IActivityService
import fr.laucoin.registry.backend.domain.service.usecase.IActivityUseCase
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono

class ActivityUseCaseTest {
	private val service: IActivityService = mock()
	private val useCase: IActivityUseCase = ActivityUseCase(service)

	@Test
	fun `Should findActivitiesPage delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: ActivitySearchParamModel = mock()
		val expected: Mono<PageModel<ActivityModel>> = Mono.empty()
		whenever(service.findActivitiesPage(projectId, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findActivitiesPage(projectId, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(service).findActivitiesPage(projectId, pageable, searchParams)
	}

	@Test
	fun `Should findActivityStatistics delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<ActivityStatisticsModel> = Mono.empty()
		whenever(service.findActivityStatistics(projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.findActivityStatistics(projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).findActivityStatistics(projectId, id)
	}

	@Test
	fun `Should findActivityById delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val isVisible: Boolean? = null
		val expected: Mono<ActivityModel> = Mono.empty()
		whenever(service.findActivityById(projectId, id, isVisible)).thenReturn(expected)

		// Act
		val result = useCase.findActivityById(projectId, id, isVisible)

		// Assert
		assertSame(expected, result)
		verify(service).findActivityById(projectId, id, isVisible)
	}

	@Test
	fun `Should findActivityMovementsPage delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: MovementSearchParamModel = mock()
		val expected: Mono<PageModel<MovementModel>> = Mono.empty()
		whenever(service.findActivityMovementsPage(projectId, id, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findActivityMovementsPage(projectId, id, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(service).findActivityMovementsPage(projectId, id, pageable, searchParams)
	}

	@Test
	fun `Should createActivity delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val activity: ActivityModel = mock()
		val expected: Mono<ActivityModel> = Mono.empty()
		whenever(service.createActivity(currentUser, activity)).thenReturn(expected)

		// Act
		val result = useCase.createActivity(currentUser, activity)

		// Assert
		assertSame(expected, result)
		verify(service).createActivity(currentUser, activity)
	}

	@Test
	fun `Should updateActivityById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val activity: ActivityModel = mock()
		val expected: Mono<ActivityModel> = Mono.empty()
		whenever(service.updateActivityById(currentUser, projectId, id, activity)).thenReturn(expected)

		// Act
		val result = useCase.updateActivityById(currentUser, projectId, id, activity)

		// Assert
		assertSame(expected, result)
		verify(service).updateActivityById(currentUser, projectId, id, activity)
	}

	@Test
	fun `Should disableActivityById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<ActivityModel> = Mono.empty()
		whenever(service.disableActivityById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.disableActivityById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).disableActivityById(currentUser, projectId, id)
	}

	@Test
	fun `Should enableActivityById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<ActivityModel> = Mono.empty()
		whenever(service.enableActivityById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.enableActivityById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).enableActivityById(currentUser, projectId, id)
	}

	@Test
	fun `Should deleteActivityById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<Unit> = Mono.empty()
		whenever(service.deleteActivityById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.deleteActivityById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).deleteActivityById(currentUser, projectId, id)
	}
}
