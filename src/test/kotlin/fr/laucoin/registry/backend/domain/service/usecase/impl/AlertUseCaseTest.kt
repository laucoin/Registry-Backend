package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.AlertStatisticsModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.core.IAlertService
import fr.laucoin.registry.backend.domain.service.usecase.IAlertUseCase
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono

class AlertUseCaseTest {
	private val service: IAlertService = mock()
	private val useCase: IAlertUseCase = AlertUseCase(service)

	@Test
	fun `Should findAlertsPage delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: AlertSearchParamModel = mock()
		val expected: Mono<PageModel<AlertModel>> = Mono.empty()
		whenever(service.findAlertsPage(currentUser, projectId, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findAlertsPage(currentUser, projectId, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(service).findAlertsPage(currentUser, projectId, pageable, searchParams)
	}

	@Test
	fun `Should findAlertStatistics delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val expected: Mono<AlertStatisticsModel> = Mono.empty()
		whenever(service.findAlertStatistics(projectId)).thenReturn(expected)

		// Act
		val result = useCase.findAlertStatistics(projectId)

		// Assert
		assertSame(expected, result)
		verify(service).findAlertStatistics(projectId)
	}

	@Test
	fun `Should findAlertById delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val isVisible: Boolean? = null
		val expected: Mono<AlertModel> = Mono.empty()
		whenever(service.findAlertById(projectId, id, isVisible)).thenReturn(expected)

		// Act
		val result = useCase.findAlertById(projectId, id, isVisible)

		// Assert
		assertSame(expected, result)
		verify(service).findAlertById(projectId, id, isVisible)
	}

	@Test
	fun `Should findAlertForUser delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<AlertModel> = Mono.empty()
		whenever(service.findAlertForUser(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.findAlertForUser(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).findAlertForUser(currentUser, projectId, id)
	}

	@Test
	fun `Should findAlertCommunicationsPage delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: CommunicationSearchParamModel = mock()
		val expected: Mono<PageModel<CommunicationModel>> = Mono.empty()
		whenever(service.findAlertCommunicationsPage(projectId, id, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findAlertCommunicationsPage(projectId, id, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(service).findAlertCommunicationsPage(projectId, id, pageable, searchParams)
	}

	@Test
	fun `Should createAlert delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val alert: AlertModel = mock()
		val expected: Mono<AlertModel> = Mono.empty()
		whenever(service.createAlert(currentUser, alert)).thenReturn(expected)

		// Act
		val result = useCase.createAlert(currentUser, alert)

		// Assert
		assertSame(expected, result)
		verify(service).createAlert(currentUser, alert)
	}

	@Test
	fun `Should updateAlertById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val alert: AlertModel = mock()
		val expected: Mono<AlertModel> = Mono.empty()
		whenever(service.updateAlertById(currentUser, projectId, id, alert)).thenReturn(expected)

		// Act
		val result = useCase.updateAlertById(currentUser, projectId, id, alert)

		// Assert
		assertSame(expected, result)
		verify(service).updateAlertById(currentUser, projectId, id, alert)
	}

	@Test
	fun `Should updateAlertStatusById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val status: AlertStatusEnum = AlertStatusEnum.entries.first()
		val expected: Mono<AlertModel> = Mono.empty()
		whenever(service.updateAlertStatusById(currentUser, projectId, id, status)).thenReturn(expected)

		// Act
		val result = useCase.updateAlertStatusById(currentUser, projectId, id, status)

		// Assert
		assertSame(expected, result)
		verify(service).updateAlertStatusById(currentUser, projectId, id, status)
	}

	@Test
	fun `Should disableAlertById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<AlertModel> = Mono.empty()
		whenever(service.disableAlertById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.disableAlertById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).disableAlertById(currentUser, projectId, id)
	}

	@Test
	fun `Should enableAlertById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<AlertModel> = Mono.empty()
		whenever(service.enableAlertById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.enableAlertById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).enableAlertById(currentUser, projectId, id)
	}

	@Test
	fun `Should deleteAlertById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<Unit> = Mono.empty()
		whenever(service.deleteAlertById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.deleteAlertById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).deleteAlertById(currentUser, projectId, id)
	}
}
