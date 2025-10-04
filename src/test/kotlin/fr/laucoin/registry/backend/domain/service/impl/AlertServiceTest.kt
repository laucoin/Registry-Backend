package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_COMMUNICATION_OUT_OF_ALERT_DATETIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_DELETE_HAS_COMMUNICATION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AlertError.ALERT_STATUS_IS_NOT_UPDATABLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.RESOLVED
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.service.IAlertService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.test.ModelExt.alertId
import fr.laucoin.registry.backend.test.ModelExt.commonAlert
import fr.laucoin.registry.backend.test.ModelExt.commonCommunication
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class AlertServiceTest {
	private val port: IAlertPort = mock()
	private val projectService: IProjectService = mock()
	private val communicationPort: ICommunicationPort = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val service: IAlertService = AlertService(projectService, port, communicationPort, transactionalOperator)

	@BeforeEach
	fun setup() {
		whenever(projectService.validateDateTime(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(projectId))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }
	}

	@Test
	fun `Should findAlertsPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = AlertSearchParamModel()

		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findAlertsPage(projectId, pageable, params).block()

		// Assert
		verify(port).findPage(projectId, pageable, params)
	}

	@Test
	fun `Should findAlertById call port findById`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonAlert()))

		// Act
		service.findAlertById(projectId, alertId, onlyVisible).block()

		// Assert
		verify(port).findById(projectId, alertId, onlyVisible)
	}

	@Test
	fun `Should findAlertById throw on empty result`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findAlertById(projectId, alertId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(alertId.toString(), result.args?.first())

		verify(port).findById(projectId, alertId, onlyVisible)
	}

	@Test
	fun `Should findAlertCommunicationsPage call communication port findPageByAlertId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = CommunicationSearchParamModel()

		whenever(communicationPort.findPageByAlertId(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findAlertCommunicationsPage(projectId, alertId, pageable, params).block()

		// Assert
		verify(communicationPort).findPageByAlertId(projectId, alertId, pageable, params)
	}

	@Test
	fun `Should createAlert check date and call port create`() {
		// Arrange
		val alert = commonAlert().apply { communications = listOf(CommunicationModel()) }

		whenever(port.create(any())).thenReturn(Mono.just(alert))
		whenever(communicationPort.create(any())).thenReturn(Mono.just(commonCommunication()))

		// Act
		service.createAlert(currentUser(), alert).block()

		// Assert
		verify(projectService).validateDateTime(
			projectId,
			CustomDateTimeModel(alert.dateTime),
			ALERT_DATETIME_OUT_OF_PROJECT_DATE_RANGE,
		)
		verify(communicationPort).create(any())
		verify(port).create(alert)
	}

	@Test
	fun `Should updateAlertById check date, check existing alert, call port updateAlert`() {
		// Arrange
		val oldAlert =
			commonAlert().apply { status = IN_PROGRESS; dateTime = ZonedDateTime.now().minusDays(10) }
		val updatedAlert =
			commonAlert().apply { status = IN_PROGRESS; dateTime = ZonedDateTime.now() }

		val expectedCommunicationSearch = CommunicationSearchParamModel(
			textSearched = null,
			visibilitySearched = null,
			startDateTimeSearched = null,
			endDateTimeSearched = updatedAlert.dateTime,
		)

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldAlert))
		whenever(communicationPort.countAllByAlertId(any(), any(), any())).thenReturn(Mono.just(0L))
		whenever(port.update(any())).thenReturn(Mono.just(updatedAlert))

		// Act
		service.updateAlertById(currentUser(), projectId, alertId, updatedAlert).block()

		// Assert
		verify(port).findById(projectId, alertId, visibilitySearched = null)
		verify(communicationPort).countAllByAlertId(projectId, alertId, expectedCommunicationSearch)
		verify(port).update(any())
	}

	@Test
	fun `Should updateAlertById throw because of not in progress status`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonAlert()))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateAlertById(currentUser(), projectId, alertId, commonAlert()).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_ENTITY, result.status)
		assertEquals(ALERT_STATUS_IS_NOT_UPDATABLE, result.code)

		verify(projectService).validateDateTime(
			projectId,
			CustomDateTimeModel(commonAlert().dateTime),
			ALERT_DATETIME_OUT_OF_PROJECT_DATE_RANGE,
		)
		verify(port).findById(projectId, alertId, visibilitySearched = null)
		verify(port, never()).update(commonAlert())
	}

	@Test
	fun `Should updateAlertById throw because of communication conflict`() {
		// Arrange
		val oldAlert =
			commonAlert().apply { status = IN_PROGRESS; dateTime = ZonedDateTime.now().minusDays(10) }
		val updatedAlert =
			commonAlert().apply { status = IN_PROGRESS; dateTime = ZonedDateTime.now() }

		val expectedCommunicationSearch = CommunicationSearchParamModel(
			textSearched = null,
			visibilitySearched = null,
			startDateTimeSearched = null,
			endDateTimeSearched = updatedAlert.dateTime,
		)

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldAlert))
		whenever(communicationPort.countAllByAlertId(any(), any(), any())).thenReturn(Mono.just(1L))
		whenever(port.update(any())).thenReturn(Mono.just(updatedAlert))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateAlertById(currentUser(), projectId, alertId, updatedAlert).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_ENTITY, result.status)
		assertEquals(ALERT_COMMUNICATION_OUT_OF_ALERT_DATETIME, result.code)

		verify(port).findById(projectId, alertId, visibilitySearched = null)
		verify(communicationPort).countAllByAlertId(projectId, alertId, expectedCommunicationSearch)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should updateAlertStatusById check existing alert, call port updateAlert`() {
		// Arrange
		val alert = commonAlert().apply { status = IN_PROGRESS }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(alert))
		whenever(port.update(any())).thenReturn(Mono.just(alert))

		// Act
		service.updateAlertStatusById(currentUser(), projectId, alertId, RESOLVED).block()

		// Assert
		verify(port).findById(projectId, alertId, visibilitySearched = null)
		verify(port).update(alert.apply { status = RESOLVED })
	}

	@Test
	fun `Should disableAlertById call existing alert and call port update`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonAlert()))
		whenever(port.update(any())).thenReturn(Mono.just(commonAlert()))

		// Act
		service.disableAlertById(currentUser(), projectId, alertId).block()

		// Assert
		verify(port).findById(projectId, alertId, visibilitySearched = true)
		verify(port).update(commonAlert().apply { visible = false })
	}

	@Test
	fun `Should enableAlertById call existing alert and call port update`() {
		// Arrange
		val alert = commonAlert().apply { visible = false }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(alert))
		whenever(port.update(any())).thenReturn(Mono.just(alert))

		// Act
		service.enableAlertById(currentUser(), projectId, alertId).block()

		// Assert
		verify(port).findById(projectId, alertId, visibilitySearched = false)
		verify(port).update(commonAlert())
	}

	@Test
	fun `Should deleteAlertById call existing alert, check no movement, and call port deleteById`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonAlert()))
		whenever(communicationPort.countAllByAlertId(any(), any(), any())).thenReturn(Mono.just(0))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteAlertById(currentUser(), projectId, alertId).block()

		// Assert
		verify(port).findById(projectId, alertId, visibilitySearched = null)
		verify(communicationPort).countAllByAlertId(projectId, alertId, CommunicationSearchParamModel())
		verify(port).deleteById(alertId)
	}

	@Test
	fun `Should deleteAlertById call existing alert, throw if movements are linked`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonAlert()))
		whenever(communicationPort.countAllByAlertId(any(), any(), any())).thenReturn(Mono.just(1))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.deleteAlertById(currentUser(), projectId, alertId).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_ENTITY, result.status)
		assertEquals(ALERT_DELETE_HAS_COMMUNICATION, result.message)

		verify(port).findById(projectId, alertId, visibilitySearched = null)
		verify(communicationPort).countAllByAlertId(projectId, alertId, CommunicationSearchParamModel())
		verify(port, never()).deleteById(any())
	}

	@Test
	fun `Should purgeAlertsIfNecessary call unused alert since a date, and call port deleteById`() {
		// Arrange
		val date = LocalDate.EPOCH
		val alertId1 = UUID.randomUUID()
		val alertId2 = UUID.randomUUID()

		whenever(port.findOlderThanAndUncommentedSince(any())).thenReturn(Flux.just(alertId1, alertId2))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.purgeAlertsIfNecessary(date, false).collectList().block()

		// Assert
		verify(port).findOlderThanAndUncommentedSince(date)
		verify(port).deleteById(alertId1)
		verify(port).deleteById(alertId2)
	}

	@Test
	fun `Should purgeAlertsIfNecessary call unused alert since a date, and not call port deleteById because of dryRun`() {
		// Arrange
		val date = LocalDate.EPOCH

		whenever(port.findOlderThanAndUncommentedSince(any())).thenReturn(Flux.just(alertId))

		// Act
		service.purgeAlertsIfNecessary(date, true).collectList().block()

		// Assert
		verify(port).findOlderThanAndUncommentedSince(date)
		verify(port, never()).deleteById(any())
	}
}
