package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.model.VehicleTimelineModel
import fr.laucoin.registry.backend.domain.service.core.IVehicleService
import fr.laucoin.registry.backend.domain.service.usecase.IVehicleUseCase
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono

class VehicleUseCaseTest {
	private val service: IVehicleService = mock()
	private val useCase: IVehicleUseCase = VehicleUseCase(service)

	@Test
	fun `Should findVehiclesPage delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: VehicleSearchParamModel = mock()
		val expected: Mono<PageModel<VehicleModel>> = Mono.empty()
		whenever(service.findVehiclesPage(projectId, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findVehiclesPage(projectId, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(service).findVehiclesPage(projectId, pageable, searchParams)
	}

	@Test
	fun `Should findVehicleTimeline delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<VehicleTimelineModel> = Mono.empty()
		whenever(service.findVehicleTimeline(projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.findVehicleTimeline(projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).findVehicleTimeline(projectId, id)
	}

	@Test
	fun `Should findVehicleById delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val isVisible: Boolean? = null
		val expected: Mono<VehicleModel> = Mono.empty()
		whenever(service.findVehicleById(projectId, id, isVisible)).thenReturn(expected)

		// Act
		val result = useCase.findVehicleById(projectId, id, isVisible)

		// Assert
		assertSame(expected, result)
		verify(service).findVehicleById(projectId, id, isVisible)
	}

	@Test
	fun `Should findVehicleMovementsPage delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: MovementSearchParamModel = mock()
		val expected: Mono<PageModel<MovementModel>> = Mono.empty()
		whenever(service.findVehicleMovementsPage(projectId, id, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findVehicleMovementsPage(projectId, id, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(service).findVehicleMovementsPage(projectId, id, pageable, searchParams)
	}

	@Test
	fun `Should createVehicle delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val vehicle: VehicleModel = mock()
		val expected: Mono<VehicleModel> = Mono.empty()
		whenever(service.createVehicle(currentUser, vehicle)).thenReturn(expected)

		// Act
		val result = useCase.createVehicle(currentUser, vehicle)

		// Assert
		assertSame(expected, result)
		verify(service).createVehicle(currentUser, vehicle)
	}

	@Test
	fun `Should updateVehicleById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val vehicle: VehicleModel = mock()
		val expected: Mono<VehicleModel> = Mono.empty()
		whenever(service.updateVehicleById(currentUser, projectId, id, vehicle)).thenReturn(expected)

		// Act
		val result = useCase.updateVehicleById(currentUser, projectId, id, vehicle)

		// Assert
		assertSame(expected, result)
		verify(service).updateVehicleById(currentUser, projectId, id, vehicle)
	}

	@Test
	fun `Should disableVehicleById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<VehicleModel> = Mono.empty()
		whenever(service.disableVehicleById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.disableVehicleById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).disableVehicleById(currentUser, projectId, id)
	}

	@Test
	fun `Should enableVehicleById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<VehicleModel> = Mono.empty()
		whenever(service.enableVehicleById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.enableVehicleById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).enableVehicleById(currentUser, projectId, id)
	}

	@Test
	fun `Should deleteVehicleById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<Unit> = Mono.empty()
		whenever(service.deleteVehicleById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.deleteVehicleById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).deleteVehicleById(currentUser, projectId, id)
	}
}
