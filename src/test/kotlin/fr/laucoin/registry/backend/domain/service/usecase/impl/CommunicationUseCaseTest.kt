package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.service.core.ICommunicationService
import fr.laucoin.registry.backend.domain.service.usecase.ICommunicationUseCase
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class CommunicationUseCaseTest {
	private val service: ICommunicationService = mock()
	private val useCase: ICommunicationUseCase = CommunicationUseCase(service)

	@Test
	fun `Should findCommunicationById delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val isVisible: Boolean? = null
		val expected: Mono<CommunicationModel> = Mono.empty()
		whenever(service.findCommunicationById(projectId, id, isVisible)).thenReturn(expected)

		// Act
		val result = useCase.findCommunicationById(projectId, id, isVisible)

		// Assert
		assertSame(expected, result)
		verify(service).findCommunicationById(projectId, id, isVisible)
	}

	@Test
	fun `Should searchOutMovementWithActivityByText delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val query: String? = null
		val expected: Flux<MovementModel> = Flux.empty()
		whenever(service.searchOutMovementWithActivityByText(projectId, query)).thenReturn(expected)

		// Act
		val result = useCase.searchOutMovementWithActivityByText(projectId, query)

		// Assert
		assertSame(expected, result)
		verify(service).searchOutMovementWithActivityByText(projectId, query)
	}

	@Test
	fun `Should searchAlertByText delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val query: String? = null
		val expected: Flux<AlertModel> = Flux.empty()
		whenever(service.searchAlertByText(projectId, query)).thenReturn(expected)

		// Act
		val result = useCase.searchAlertByText(projectId, query)

		// Assert
		assertSame(expected, result)
		verify(service).searchAlertByText(projectId, query)
	}

	@Test
	fun `Should createCommunication delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val communication: CommunicationModel = mock()
		val expected: Mono<CommunicationModel> = Mono.empty()
		whenever(service.createCommunication(currentUser, communication)).thenReturn(expected)

		// Act
		val result = useCase.createCommunication(currentUser, communication)

		// Assert
		assertSame(expected, result)
		verify(service).createCommunication(currentUser, communication)
	}

	@Test
	fun `Should updateCommunicationById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val communication: CommunicationModel = mock()
		val expected: Mono<CommunicationModel> = Mono.empty()
		whenever(service.updateCommunicationById(currentUser, projectId, id, communication)).thenReturn(expected)

		// Act
		val result = useCase.updateCommunicationById(currentUser, projectId, id, communication)

		// Assert
		assertSame(expected, result)
		verify(service).updateCommunicationById(currentUser, projectId, id, communication)
	}

	@Test
	fun `Should disableCommunicationById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<CommunicationModel> = Mono.empty()
		whenever(service.disableCommunicationById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.disableCommunicationById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).disableCommunicationById(currentUser, projectId, id)
	}

	@Test
	fun `Should enableCommunicationById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<CommunicationModel> = Mono.empty()
		whenever(service.enableCommunicationById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.enableCommunicationById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).enableCommunicationById(currentUser, projectId, id)
	}

	@Test
	fun `Should deleteCommunicationById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<Unit> = Mono.empty()
		whenever(service.deleteCommunicationById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.deleteCommunicationById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).deleteCommunicationById(currentUser, projectId, id)
	}
}
