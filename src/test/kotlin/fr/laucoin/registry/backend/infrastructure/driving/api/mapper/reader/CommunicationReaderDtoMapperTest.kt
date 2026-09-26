package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.test.ModelExt.communicationId
import java.time.ZonedDateTime
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CommunicationReaderDtoMapperTest {
	private val movementMapper: MovementReaderDtoMapper = mock()
	private val alertMapper: AlertReaderDtoMapper = mock()
	private val mapper = CommunicationReaderDtoMapper(movementMapper, alertMapper)

	private companion object {
		private val now = ZonedDateTime.now()
		private val dtoMovement = MovementReaderDto(dateTime = now, contentType = REGISTERED)
		private val dtoAlert = AlertReaderDto(dateTime = now)

		private val model = CommunicationModel().apply {
			dateTime = now
			message = "Communication message"
			movement = MovementModel(dateTime = now)
			alert = AlertModel(dateTime = now)
			id = communicationId
			visible = true
			creation = HistoryModel()
			lastEdition = HistoryModel()
		}

		private val dto = CommunicationReaderDto().apply {
			dateTime = now
			message = "Communication message"
			movement = dtoMovement
			alert = dtoAlert
			id = communicationId
			visible = true
			creation = HistoryModel()
			lastEdition = HistoryModel()
		}

		@JvmStatic
		fun `CommunicationModel to CommunicationReaderDto data`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(model, dto, 1, 1),
				Arguments.of(CommunicationModel(dateTime = now), CommunicationReaderDto(dateTime = now), 0, 0),
			)
		}
	}

	@BeforeEach
	fun setup() {
		whenever(movementMapper.toDto(any())).thenReturn(dtoMovement)
		whenever(alertMapper.toDto(any())).thenReturn(dtoAlert)
	}

	@ParameterizedTest
	@MethodSource("CommunicationModel to CommunicationReaderDto data")
	fun `Should toDto convert CommunicationModel to CommunicationReaderDto`(
		model: CommunicationModel,
		dto: CommunicationReaderDto,
		expectedMovementCast: Int,
		expectedAlertCast: Int,
	) {
		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(dto, result)

		verify(movementMapper, times(expectedMovementCast))
			.toDto(model.movement ?: MovementModel())

		verify(alertMapper, times(expectedAlertCast)).toDto(model.alert ?: AlertModel())
	}

	@ParameterizedTest
	@MethodSource("CommunicationModel to CommunicationReaderDto data")
	fun `Should toDto convert CommunicationModel list to CommunicationReaderDto list`(
		model: CommunicationModel,
		dto: CommunicationReaderDto,
		expectedMovementCast: Int,
		expectedAlertCast: Int,
	) {
		// Arrange
		val models = listOf(model)
		val dtos = listOf(dto)

		// Act
		val result = mapper.toDtoList(models)

		// Assert
		assertEquals(dtos, result)

		verify(movementMapper, times(expectedMovementCast))
			.toDto(model.movement ?: MovementModel())

		verify(alertMapper, times(expectedAlertCast)).toDto(model.alert ?: AlertModel())
	}

	@ParameterizedTest
	@MethodSource("CommunicationModel to CommunicationReaderDto data")
	fun `Should toDto convert CommunicationModel page to CommunicationReaderDto page`(
		model: CommunicationModel,
		dto: CommunicationReaderDto,
		expectedMovementCast: Int,
		expectedAlertCast: Int,
	) {
		// Arrange
		val modelPage = PageModel(
			pageNumber = 0,
			pageSize = 10,
			totalPages = 1,
			totalElements = 1,
			content = listOf(model),
		)
		val dtoPage = PageModel(
			pageNumber = 0,
			pageSize = 10,
			totalPages = 1,
			totalElements = 1,
			content = listOf(dto),
			lastRefresh = modelPage.lastRefresh,
		)

		// Act
		val result = mapper.toDtoPage(modelPage)

		// Assert
		assertEquals(dtoPage, result)

		verify(movementMapper, times(expectedMovementCast))
			.toDto(model.movement ?: MovementModel())

		verify(alertMapper, times(expectedAlertCast)).toDto(model.alert ?: AlertModel())
	}
}
