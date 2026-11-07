package fr.laucoin.registry.backend.domain.service.core.impl

import org.junit.jupiter.api.Assertions.assertEquals
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.test.ModelExt.defaultAlert
import fr.laucoin.registry.backend.test.ModelExt.defaultMovement
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.domain.model.CommunicationFeatureModel
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.service.core.ICommunicationSearchService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux

class CommunicationSearchServiceTest {
	private val movementPort: IMovementPort = mock()
	private val alertPort: IAlertPort = mock()
	private val service: ICommunicationSearchService = CommunicationSearchService(
		movementPort,
		alertPort,
		CommunicationFeatureModel(CommunicationFeatureModel.Searched(maxActivityResult = MAX_ACTIVITIES, maxAlertResult = MAX_ALERTS)),
	)

	@Test
	fun `Should searchOutMovementWithActivityByText call port findActivityWithLimit`() {
		// Arrange
		val expected = defaultMovement()
		val query = "search"
		val search = ActivitySearchParamModel(
			query = query,
			isVisible = true,
			isAvailable = true,
			at = null,
		)

		whenever(movementPort.findActivityWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(expected))

		// Act
		val result = service.searchOutMovementWithActivityByText(defaultProjectId, query).collectList().block()

		// Assert
		assertEquals(listOf(expected), result)
		verify(movementPort).findActivityWithLimit(MAX_ACTIVITIES, defaultProjectId, search)
	}

	@Test
	fun `Should searchAlertByText call port findWithLimit`() {
		// Arrange
		val expected = defaultAlert()
		val query = "search"
		val search = AlertSearchParamModel(
			query = query,
			isVisible = true,
			status = IN_PROGRESS,
		)

		whenever(alertPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(expected))

		// Act
		val result = service.searchAlertByText(defaultProjectId, query).collectList().block()

		// Assert
		assertEquals(listOf(expected), result)
		verify(alertPort).findWithLimit(MAX_ALERTS, defaultProjectId, search)
	}

	private companion object {
		private const val MAX_ACTIVITIES = 1
		private const val MAX_ALERTS = 2
	}
}
