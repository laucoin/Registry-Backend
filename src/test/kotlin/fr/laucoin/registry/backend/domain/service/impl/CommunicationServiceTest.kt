package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_ALERT_IS_AFTER_COMMUNICATION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_ALERT_IS_NOT_COMPATIBLE_WITH_COMMUNICATION_CREATION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_ALERT_NOT_FOUND_IN_COMMUNICATION_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_ALERT_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_CONTENT_TYPE_NOT_REGISTERED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_IS_AFTER_COMMUNICATION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_NOT_FOUND_IN_COMMUNICATION_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_TYPE_NOT_OUT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.CANCELED
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.service.ICommunicationService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.test.ModelExt.alertId
import fr.laucoin.registry.backend.test.ModelExt.commonAlert
import fr.laucoin.registry.backend.test.ModelExt.commonCommunication
import fr.laucoin.registry.backend.test.ModelExt.commonMovement
import fr.laucoin.registry.backend.test.ModelExt.communicationId
import fr.laucoin.registry.backend.test.ModelExt.movementId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream

class CommunicationServiceTest {
	private val port: ICommunicationPort = mock()
	private val projectService: IProjectService = mock()
	private val movementPort: IMovementPort = mock()
	private val alertPort: IAlertPort = mock()
	private val service: ICommunicationService = CommunicationService(
		projectService, port, movementPort, alertPort, MAX_ACTIVITIES, MAX_ALERTS
	)

	private companion object {
		private const val MAX_ACTIVITIES = 1
		private const val MAX_ALERTS = 2
		private val past = ZonedDateTime.now().minusDays(10L)
		private val now = ZonedDateTime.now()

		@JvmStatic
		fun `Should createCommunication check date and call port create`(): Stream<Arguments> {
			val comMovement = commonMovement().apply {
				type = OUT
				contentType = REGISTERED
				dateTime = now
			}

			val comAlert = commonAlert().apply {
				dateTime = now
				status = IN_PROGRESS
			}

			return Stream.of(
				Arguments.of(
					commonCommunication().apply { dateTime = now; movement = comMovement },
					comMovement,
					null,
					1,
					0,
				),
				Arguments.of(
					commonCommunication().apply { dateTime = now },
					null,
					null,
					0,
					0,
				),
				Arguments.of(
					commonCommunication().apply { dateTime = now; alert = comAlert },
					null,
					comAlert,
					0,
					1,
				),
			)
		}

		@JvmStatic
		fun `Should createCommunication throw not eligible movement link`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					commonCommunication().apply { dateTime = past; movement = commonMovement() },
					null,
					NOT_FOUND,
					COMMUNICATION_MOVEMENT_NOT_FOUND_IN_COMMUNICATION_PROJECT,
				),
				Arguments.of(
					commonCommunication().apply { dateTime = past; movement = commonMovement() },
					commonMovement().apply { dateTime = past; visible = false },
					NOT_FOUND,
					COMMUNICATION_MOVEMENT_NOT_VISIBLE,
				),
				Arguments.of(
					commonCommunication().apply { dateTime = past; movement = commonMovement() },
					commonMovement().apply { dateTime = now },
					UNPROCESSABLE_CONTENT,
					COMMUNICATION_MOVEMENT_IS_AFTER_COMMUNICATION,
				),
				Arguments.of(
					commonCommunication().apply { dateTime = now; movement = commonMovement() },
					commonMovement().apply { dateTime = past; contentType = GUEST },
					UNPROCESSABLE_CONTENT,
					COMMUNICATION_MOVEMENT_CONTENT_TYPE_NOT_REGISTERED,
				),
				Arguments.of(
					commonCommunication().apply { dateTime = now; movement = commonMovement() },
					commonMovement().apply { dateTime = past; type = IN },
					UNPROCESSABLE_CONTENT,
					COMMUNICATION_MOVEMENT_TYPE_NOT_OUT,
				),
			)
		}

		@JvmStatic
		fun `Should createCommunication throw not eligible alert link`(): Stream<Arguments> {
			val currentUserWithRight = currentUser(projectId.toString() + "_" + REGISTRY_PROJECT_OPTION_ALERT)
			val currentUserWithoutRight = currentUser()
			return Stream.of(
				Arguments.of(
					commonCommunication().apply { dateTime = past; alert = commonAlert() },
					currentUserWithRight,
					null,
					1,
					NOT_FOUND,
					COMMUNICATION_ALERT_NOT_FOUND_IN_COMMUNICATION_PROJECT,
				),
				Arguments.of(
					commonCommunication().apply { dateTime = past; alert = commonAlert() },
					currentUserWithRight,
					commonAlert().apply { dateTime = past; visible = false },
					1,
					NOT_FOUND,
					COMMUNICATION_ALERT_NOT_VISIBLE,
				),
				Arguments.of(
					commonCommunication().apply { dateTime = past; alert = commonAlert() },
					currentUserWithRight,
					commonAlert().apply { dateTime = now },
					1,
					UNPROCESSABLE_CONTENT,
					COMMUNICATION_ALERT_IS_AFTER_COMMUNICATION,
				),
				Arguments.of(
					commonCommunication().apply { dateTime = now; alert = commonAlert() },
					currentUserWithRight,
					commonAlert().apply { dateTime = now; status = CANCELED },
					1,
					UNPROCESSABLE_CONTENT,
					COMMUNICATION_ALERT_IS_NOT_COMPATIBLE_WITH_COMMUNICATION_CREATION,
				),
				Arguments.of(
					commonCommunication().apply { dateTime = now; alert = commonAlert() },
					currentUserWithoutRight,
					null,
					0,
					FORBIDDEN,
					NOT_ENOUGH_PERMISSION,
				),
			)
		}
	}

	@Test
	fun `Should findCommunicationsPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = CommunicationSearchParamModel()

		whenever(port.findPage(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findCommunicationPage(projectId, pageable, params).block()

		// Assert
		verify(port).findPage(projectId, pageable, params, emptyList())
	}

	@Test
	fun `Should findCommunicationById call port findById`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonCommunication()))

		// Act
		service.findCommunicationById(projectId, communicationId, onlyVisible).block()

		// Assert
		verify(port).findById(projectId, communicationId, onlyVisible)
	}

	@Test
	fun `Should findCommunicationById call port findById throw on empty result`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findCommunicationById(projectId, communicationId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(communicationId.toString(), result.args?.first())

		verify(port).findById(projectId, communicationId, onlyVisible)
	}

	@Test
	fun `Should searchOutMovementWithActivityByText call port findActivityWithLimit`() {
		// Arrange
		val textSearched = "search"
		val search = ActivitySearchParamModel(
			textSearched = textSearched,
			visibilitySearched = true,
			availabilitySearched = true,
			dateTimeSearched = null,
		)

		whenever(movementPort.findActivityWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(commonMovement()))

		// Act
		service.searchOutMovementWithActivityByText(projectId, textSearched).collectList().block()

		// Assert
		verify(movementPort).findActivityWithLimit(MAX_ACTIVITIES, projectId, search)
	}

	@Test
	fun `Should searchAlertByText call port findWithLimit`() {
		// Arrange
		val textSearched = "search"
		val search = AlertSearchParamModel(
			textSearched = textSearched,
			visibilitySearched = true,
			statusSearched = IN_PROGRESS,
		)

		whenever(alertPort.findWithLimit(any(), any(), anyOrNull())).thenReturn(Flux.just(commonAlert()))

		// Act
		service.searchAlertByText(projectId, textSearched).collectList().block()

		// Assert
		verify(alertPort).findWithLimit(MAX_ALERTS, projectId, search)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createCommunication check date and call port create`(
		communication: CommunicationModel,
		movement: MovementModel?,
		alert: AlertModel?,
		callToMovement: Int,
		callToAlert: Int,
	) {
		// Arrange
		whenever(projectService.validateDateTime(any(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
		whenever(movementPort.findById(any(), anyOrNull(), anyOrNull()))
			.thenReturn(if (Objects.nonNull(movement)) Mono.just(movement!!) else Mono.empty())
		whenever(alertPort.findById(any(), anyOrNull(), anyOrNull()))
			.thenReturn(if (Objects.nonNull(alert)) Mono.just(alert!!) else Mono.empty())
		whenever(port.create(any())).thenReturn(Mono.just(communication))

		// Act
		service.createCommunication(
			currentUser(projectId.toString() + "_" + REGISTRY_PROJECT_OPTION_ALERT),
			communication
		).block()

		// Assert
		verify(projectService).validateDateTime(
			eq(projectId), any(), eq(COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE)
		)
		verify(movementPort, times(callToMovement)).findById(projectId, movementId, visibilitySearched = null)
		verify(alertPort, times(callToAlert)).findById(projectId, alertId, visibilitySearched = null)
		verify(port).create(communication)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createCommunication throw not eligible movement link`(
		communication: CommunicationModel,
		movement: MovementModel?,
		status: HttpStatus,
		errorMessage: String,
	) {
		// Arrange
		whenever(projectService.validateDateTime(any(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
		whenever(movementPort.findById(any(), anyOrNull(), anyOrNull()))
			.thenReturn(if (Objects.isNull(movement)) Mono.empty() else Mono.just(movement!!))
		whenever(port.create(any())).thenReturn(Mono.just(communication))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createCommunication(currentUser(), communication).block()
		}) as RegistryException

		// Assert
		assertEquals(status, result.status)
		assertEquals(errorMessage, result.message)

		verify(projectService).validateDateTime(
			eq(projectId), any(), eq(COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE)
		)
		verify(movementPort).findById(eq(projectId), any(), eq(null))
		verifyNoInteractions(port)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createCommunication throw not eligible alert link`(
		communication: CommunicationModel,
		currentUser: CurrentUserModel,
		alert: AlertModel?,
		callAlertPort: Int,
		status: HttpStatus,
		errorMessage: String,
	) {
		// Arrange
		whenever(projectService.validateDateTime(any(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
		whenever(alertPort.findById(any(), anyOrNull(), anyOrNull()))
			.thenReturn(if (Objects.nonNull(alert)) Mono.just(alert!!) else Mono.empty())
		whenever(port.create(any())).thenReturn(Mono.just(communication))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createCommunication(currentUser, communication).block()
		}) as RegistryException

		// Assert
		assertEquals(status, result.status)
		assertEquals(errorMessage, result.message)
		verify(projectService).validateDateTime(
			eq(projectId), any(), eq(COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE)
		)
		verify(alertPort, times(callAlertPort)).findById(
			eq(projectId), any(), eq(null),
		)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should updateCommunicationById check date, check existing communication, call port updateCommunication`() {
		// Arrange
		val now = ZonedDateTime.now()
		val communication = commonCommunication().apply { dateTime = now; movement = commonMovement() }

		whenever(projectService.validateDateTime(any(), anyOrNull(), any())).thenReturn(Mono.just(projectId))
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(communication))
		whenever(movementPort.findById(any(), anyOrNull(), anyOrNull()))
			.thenReturn(Mono.just(MovementModel().apply { visible = true }))
		whenever(port.update(any())).thenReturn(Mono.just(communication))

		// Act
		service.updateCommunicationById(currentUser(), projectId, communicationId, communication).block()

		// Assert
		verify(projectService).validateDateTime(
			projectId, CustomDateTimeModel(now), COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(port).findById(projectId, communicationId, visibilitySearched = null)
		verify(movementPort, never()).findById(projectId, movementId, visibilitySearched = null)
		verify(port).update(communication)
	}

	@Test
	fun `Should disableCommunicationById call existing communication and call port update`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonCommunication()))
		whenever(port.update(any())).thenReturn(Mono.just(commonCommunication()))

		// Act
		service.disableCommunicationById(currentUser(), projectId, communicationId).block()

		// Assert
		verify(port).findById(projectId, communicationId, visibilitySearched = true)
		verify(port).update(commonCommunication().apply { visible = false })
	}

	@Test
	fun `Should enableCommunicationById call existing communication and call port update`() {
		// Arrange
		val communication = commonCommunication().apply { visible = false }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(communication))
		whenever(port.update(any())).thenReturn(Mono.just(communication))

		// Act
		service.enableCommunicationById(currentUser(), projectId, communicationId).block()

		// Assert
		verify(port).findById(projectId, communicationId, visibilitySearched = false)
		verify(port).update(commonCommunication())
	}

	@Test
	fun `Should deleteCommunicationById call existing communication, check no movement, and call port deleteById`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonCommunication()))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteCommunicationById(currentUser(), projectId, communicationId).block()

		// Assert
		verify(port).findById(projectId, communicationId, visibilitySearched = null)
		verify(port).deleteById(communicationId)
	}

	@Test
	fun `Should purgeOrphanCommunications call orphan communication since a date, and call port deleteById`() {
		// Arrange
		val communicationId1 = UUID.randomUUID()
		val communicationId2 = UUID.randomUUID()
		val movements = listOf(UUID.randomUUID())
		val alerts = listOf(UUID.randomUUID())

		whenever(port.findOrphan(any(), any())).thenReturn(Flux.just(communicationId1, communicationId2))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.purgeOrphanCommunications(movements, alerts, false).collectList().block()

		// Assert
		verify(port).findOrphan(movements, alerts)
		verify(port).deleteById(communicationId1)
		verify(port).deleteById(communicationId2)
	}

	@Test
	fun `Should purgeOrphanCommunications call orphan communication since a date, and not call port deleteById because of dryRun`() {
		// Arrange
		val movements = listOf(UUID.randomUUID())
		val alerts = listOf(UUID.randomUUID())

		whenever(port.findOrphan(any(), any())).thenReturn(Flux.just(UUID.randomUUID()))

		// Act
		service.purgeOrphanCommunications(movements, alerts, true).collectList().block()

		// Assert
		verify(port).findOrphan(movements, alerts)
		verify(port, never()).deleteById(any())
	}
}
