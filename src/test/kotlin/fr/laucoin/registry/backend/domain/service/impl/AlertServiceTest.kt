package fr.laucoin.registry.backend.domain.service.impl

import java.time.Clock
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.ALERT
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_D
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_COMMUNICATION_R
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
import fr.laucoin.registry.backend.test.ModelExt.defaultAlert
import fr.laucoin.registry.backend.test.ModelExt.defaultAlertId
import fr.laucoin.registry.backend.test.ModelExt.defaultCommunication
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class AlertServiceTest {
	private val port: IAlertPort = mock()
	private val projectService: IProjectService = mock()
	private val communicationPort: ICommunicationPort = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val rowLockPort: IRowLockPort = mock()
	init {
		whenever(rowLockPort.lock(any(), any())).thenReturn(Mono.just(Unit))
	}

	private val service: IAlertService = AlertService(projectService, port, communicationPort, rowLockPort, transactionalOperator, Clock.systemUTC())

	@BeforeEach
	fun setup() {
		whenever(
			projectService.validateDateTime(
				any(),
				anyOrNull(),
				anyOrNull()
			)
		).thenReturn(Mono.just(defaultProjectId))
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
		service.findAlertsPage(currentUser(), defaultProjectId, pageable, params).block()

		// Assert
		verify(port).findPage(defaultProjectId, pageable, params)
	}

	@Test
	fun `Should findAlertById call port findById`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultAlert()))

		// Act
		service.findAlertById(defaultProjectId, defaultAlertId, onlyVisible).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultAlertId, onlyVisible)
	}

	@Test
	fun `Should findAlertById throw on empty result`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findAlertById(defaultProjectId, defaultAlertId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(defaultAlertId.toString(), result.args?.first())

		verify(port).findById(defaultProjectId, defaultAlertId, onlyVisible)
	}

	@Test
	fun `Should findAlertCommunicationsPage call communication port findPageByAlertId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = CommunicationSearchParamModel()

		whenever(communicationPort.findPageByAlertId(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findAlertCommunicationsPage(defaultProjectId, defaultAlertId, pageable, params).block()

		// Assert
		verify(communicationPort).findPageByAlertId(defaultProjectId, defaultAlertId, pageable, params)
	}

	@Test
	fun `Should createAlert check date and call port create`() {
		// Arrange
		val alert = defaultAlert().apply { communications = listOf(CommunicationModel()) }

		whenever(port.create(any())).thenReturn(Mono.just(alert))
		whenever(communicationPort.create(any())).thenReturn(Mono.just(defaultCommunication()))

		// Act
		service.createAlert(currentUser(), alert).block()

		// Assert
		verify(projectService).validateDateTime(
			defaultProjectId,
			CustomDateTimeModel(alert.dateTime),
			ALERT_DATETIME_OUT_OF_PROJECT_DATE_RANGE,
		)
		verify(communicationPort).create(any())
		verify(port).create(alert)
	}

	@Test
	fun `Should createAlert expose the initial communication as last communication`() {
		// Arrange
		val alert = defaultAlert().apply { communications = listOf(CommunicationModel()) }
		val createdCommunication = defaultCommunication()

		whenever(port.create(any())).thenReturn(Mono.just(alert))
		whenever(communicationPort.create(any())).thenReturn(Mono.just(createdCommunication))

		// Act
		val result = service.createAlert(currentUser(buildAuthority(REGISTRY_PROJECT_ALERT_COMMUNICATION_R)), alert).block()!!

		// Assert
		assertEquals(createdCommunication, result.lastCommunication)
	}

	@Test
	fun `Should createAlert hide the last communication from a user who cannot read communications`() {
		// Arrange
		val alert = defaultAlert().apply { communications = listOf(CommunicationModel()) }

		whenever(port.create(any())).thenReturn(Mono.just(alert))
		whenever(communicationPort.create(any())).thenReturn(Mono.just(defaultCommunication()))

		// Act
		val result = service.createAlert(currentUser(), alert).block()!!

		// Assert
		assertNull(result.lastCommunication)
	}

	@Test
	fun `Should findAlertsPage hide the last communication of every alert from a user who cannot read communications`() {
		// Arrange
		val alerts = listOf(
			defaultAlert().apply { lastCommunication = defaultCommunication() },
			defaultAlert().apply { lastCommunication = defaultCommunication() },
		)
		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(PageableModel(0, 10), totalElements = 2, alerts)))

		// Act
		service.findAlertsPage(currentUser(), defaultProjectId, PageableModel(0, 10), AlertSearchParamModel()).block()

		// Assert
		assertTrue(alerts.all { it.lastCommunication == null })
	}

	@Test
	fun `Should findAlertsPage keep the last communication for a user who can read communications`() {
		// Arrange
		val alert = defaultAlert().apply { lastCommunication = defaultCommunication() }
		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(PageableModel(0, 10), totalElements = 1, listOf(alert))))

		// Act
		service.findAlertsPage(
			currentUser(buildAuthority(REGISTRY_PROJECT_ALERT_COMMUNICATION_R)),
			defaultProjectId,
			PageableModel(0, 10),
			AlertSearchParamModel(),
		).block()

		// Assert
		assertNotNull(alert.lastCommunication)
	}

	@Test
	fun `Should findAlertForUser hide the last communication from a user who cannot read communications`() {
		// Arrange
		val alert = defaultAlert().apply { lastCommunication = defaultCommunication() }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(alert))

		// Act
		val result = service.findAlertForUser(currentUser(), defaultProjectId, defaultAlertId).block()!!

		// Assert
		assertNull(result.lastCommunication)
		verify(port).findById(defaultProjectId, defaultAlertId, true)
	}

	@Test
	fun `Should findAlertForUser read a hidden alert only for a user who can delete alerts`() {
		// Arrange
		val alert = defaultAlert()
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(alert))

		// Act
		service.findAlertForUser(currentUser(buildAuthority(REGISTRY_PROJECT_ALERT_D)), defaultProjectId, defaultAlertId).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultAlertId, null)
	}

	@Test
	fun `Should findAlertForUser keep the last communication for a user who can read communications`() {
		// Arrange
		val alert = defaultAlert().apply { lastCommunication = defaultCommunication() }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(alert))

		// Act
		val result = service
			.findAlertForUser(currentUser(buildAuthority(REGISTRY_PROJECT_ALERT_COMMUNICATION_R)), defaultProjectId, defaultAlertId)
			.block()!!

		// Assert
		assertNotNull(result.lastCommunication)
	}

	@Test
	fun `Should updateAlertById check date, check existing alert, call port updateAlert`() {
		// Arrange
		val oldAlert =
			defaultAlert().apply { status = IN_PROGRESS; dateTime = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10) }
		val updatedAlert =
			defaultAlert().apply { status = IN_PROGRESS; dateTime = ZonedDateTime.now(ZoneOffset.UTC) }

		val expectedCommunicationSearch = CommunicationSearchParamModel(
			query = null,
			isVisible = null,
			from = null,
			to = updatedAlert.dateTime,
		)

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldAlert))
		whenever(communicationPort.countAllByAlertId(any(), any(), any())).thenReturn(Mono.just(0L))
		whenever(port.update(any())).thenReturn(Mono.just(updatedAlert))

		// Act
		service.updateAlertById(currentUser(), defaultProjectId, defaultAlertId, updatedAlert).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultAlertId, isVisible = null)
		verify(communicationPort).countAllByAlertId(defaultProjectId, defaultAlertId, expectedCommunicationSearch)
		verify(port).update(any())
	}

	@Test
	fun `Should updateAlertById throw because of not in progress status`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultAlert()))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateAlertById(currentUser(), defaultProjectId, defaultAlertId, defaultAlert()).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(ALERT_STATUS_IS_NOT_UPDATABLE, result.code)

		verify(projectService).validateDateTime(
			defaultProjectId,
			CustomDateTimeModel(defaultAlert().dateTime),
			ALERT_DATETIME_OUT_OF_PROJECT_DATE_RANGE,
		)
		verify(port).findById(defaultProjectId, defaultAlertId, isVisible = null)
		verify(port, never()).update(defaultAlert())
	}

	@Test
	fun `Should updateAlertById throw because of communication conflict`() {
		// Arrange
		val oldAlert =
			defaultAlert().apply { status = IN_PROGRESS; dateTime = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10) }
		val updatedAlert =
			defaultAlert().apply { status = IN_PROGRESS; dateTime = ZonedDateTime.now(ZoneOffset.UTC) }

		val expectedCommunicationSearch = CommunicationSearchParamModel(
			query = null,
			isVisible = null,
			from = null,
			to = updatedAlert.dateTime,
		)

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldAlert))
		whenever(communicationPort.countAllByAlertId(any(), any(), any())).thenReturn(Mono.just(1L))
		whenever(port.update(any())).thenReturn(Mono.just(updatedAlert))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateAlertById(currentUser(), defaultProjectId, defaultAlertId, updatedAlert).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(ALERT_COMMUNICATION_OUT_OF_ALERT_DATETIME, result.code)

		verify(port).findById(defaultProjectId, defaultAlertId, isVisible = null)
		verify(communicationPort).countAllByAlertId(defaultProjectId, defaultAlertId, expectedCommunicationSearch)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should updateAlertStatusById check existing alert, call port updateAlert`() {
		// Arrange
		val alert = defaultAlert().apply { status = IN_PROGRESS }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(alert))
		whenever(port.update(any())).thenReturn(Mono.just(alert))

		// Act
		service.updateAlertStatusById(currentUser(), defaultProjectId, defaultAlertId, RESOLVED).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultAlertId, isVisible = null)
		verify(port).update(alert.apply { status = RESOLVED })
	}

	@Test
	fun `Should disableAlertById call existing alert and call port update`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultAlert()))
		whenever(port.update(any())).thenReturn(Mono.just(defaultAlert()))

		// Act
		service.disableAlertById(currentUser(), defaultProjectId, defaultAlertId).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultAlertId, isVisible = true)
		verify(port).update(defaultAlert().apply { visible = false })
	}

	@Test
	fun `Should enableAlertById call existing alert and call port update`() {
		// Arrange
		val alert = defaultAlert().apply { visible = false }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(alert))
		whenever(port.update(any())).thenReturn(Mono.just(alert))

		// Act
		service.enableAlertById(currentUser(), defaultProjectId, defaultAlertId).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultAlertId, isVisible = false)
		verify(port).update(defaultAlert())
	}

	@Test
	fun `Should deleteAlertById call existing alert, check no movement, and call port deleteById`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultAlert()))
		whenever(communicationPort.countAllByAlertId(any(), any(), any())).thenReturn(Mono.just(0))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteAlertById(currentUser(), defaultProjectId, defaultAlertId).block()

		// Assert
		verify(port).findById(defaultProjectId, defaultAlertId, isVisible = null)
		verify(communicationPort).countAllByAlertId(defaultProjectId, defaultAlertId, CommunicationSearchParamModel())
verify(rowLockPort).lock(ALERT, defaultAlertId)
		verify(port).deleteById(defaultAlertId)
	}

	@Test
	fun `Should deleteAlertById call existing alert, throw if movements are linked`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(defaultAlert()))
		whenever(communicationPort.countAllByAlertId(any(), any(), any())).thenReturn(Mono.just(1))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.deleteAlertById(currentUser(), defaultProjectId, defaultAlertId).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(ALERT_DELETE_HAS_COMMUNICATION, result.message)

		verify(port).findById(defaultProjectId, defaultAlertId, isVisible = null)
		verify(communicationPort).countAllByAlertId(defaultProjectId, defaultAlertId, CommunicationSearchParamModel())
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

		whenever(port.findOlderThanAndUncommentedSince(any())).thenReturn(Flux.just(defaultAlertId))

		// Act
		service.purgeAlertsIfNecessary(date, true).collectList().block()

		// Assert
		verify(port).findOlderThanAndUncommentedSince(date)
		verify(port, never()).deleteById(any())
	}
}
