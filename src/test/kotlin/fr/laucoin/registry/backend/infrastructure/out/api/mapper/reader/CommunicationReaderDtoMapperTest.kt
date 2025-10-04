package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AlertReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.test.ModelExt.communicationId
import java.time.ZonedDateTime
import java.util.Locale
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
	private val projectMapper: ProjectReaderDtoMapper = mock()
	private val movementMapper: MovementReaderDtoMapper = mock()
	private val alertMapper: AlertReaderDtoMapper = mock()
	private val mapper = CommunicationReaderDtoMapper(projectMapper, movementMapper, alertMapper)

	private companion object {
		private val now = ZonedDateTime.now()
		private val dtoMovement = MovementReaderDto(dateTime = now, contentType = REGISTERED)
		private val dtoAlert = AlertReaderDto(dateTime = now)
		private val dtoProject = ProjectReaderDto()

		private val model = CommunicationModel().apply {
			dateTime = now
			message = "Communication message"
			movement = MovementModel(dateTime = now)
			alert = AlertModel(dateTime = now)
			id = communicationId
			project = ProjectModel()
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
			project = ProjectReaderDto()
			visible = true
			creation = HistoryModel()
			lastEdition = HistoryModel()
		}

		@JvmStatic
		fun `CommunicationModel to CommunicationReaderDto data`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(model, dto, 1, 1, 1),
				Arguments.of(CommunicationModel(dateTime = now), CommunicationReaderDto(dateTime = now), 0, 0, 0),
			)
		}
	}

	@BeforeEach
	fun setup() {
		whenever(movementMapper.toDto(any(), any())).thenReturn(dtoMovement)
		whenever(alertMapper.toDto(any(), any())).thenReturn(dtoAlert)
		whenever(projectMapper.toDto(any(), any())).thenReturn(dtoProject)
	}

	@ParameterizedTest
	@MethodSource("CommunicationModel to CommunicationReaderDto data")
	fun `Should toDto convert CommunicationModel to CommunicationReaderDto`(
		model: CommunicationModel,
		dto: CommunicationReaderDto,
		expectedMovementCast: Int,
		expectedAlertCast: Int,
		expectedProjectCast: Int,
	) {
		// Act
		val result = mapper.toDto(model, Locale.getDefault())

		// Assert
		assertEquals(dto, result)

		verify(movementMapper, times(expectedMovementCast))
			.toDto(model.movement ?: MovementModel(), Locale.getDefault())

		verify(alertMapper, times(expectedAlertCast)).toDto(model.alert ?: AlertModel(), Locale.getDefault())
		verify(projectMapper, times(expectedProjectCast)).toDto(model.project ?: ProjectModel(), Locale.getDefault())
	}

	@ParameterizedTest
	@MethodSource("CommunicationModel to CommunicationReaderDto data")
	fun `Should toDto convert CommunicationModel list to CommunicationReaderDto list`(
		model: CommunicationModel,
		dto: CommunicationReaderDto,
		expectedMovementCast: Int,
		expectedAlertCast: Int,
		expectedProjectCast: Int,
	) {
		// Arrange
		val models = listOf(model)
		val dtos = listOf(dto)

		// Act
		val result = mapper.toDtoList(models, Locale.getDefault())

		// Assert
		assertEquals(dtos, result)

		verify(movementMapper, times(expectedMovementCast))
			.toDto(model.movement ?: MovementModel(), Locale.getDefault())

		verify(alertMapper, times(expectedAlertCast)).toDto(model.alert ?: AlertModel(), Locale.getDefault())
		verify(projectMapper, times(expectedProjectCast)).toDto(model.project ?: ProjectModel(), Locale.getDefault())
	}

	@ParameterizedTest
	@MethodSource("CommunicationModel to CommunicationReaderDto data")
	fun `Should toDto convert CommunicationModel page to CommunicationReaderDto page`(
		model: CommunicationModel,
		dto: CommunicationReaderDto,
		expectedMovementCast: Int,
		expectedAlertCast: Int,
		expectedProjectCast: Int,
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
		val result = mapper.toDtoPage(modelPage, Locale.getDefault())

		// Assert
		assertEquals(dtoPage, result)

		verify(movementMapper, times(expectedMovementCast))
			.toDto(model.movement ?: MovementModel(), Locale.getDefault())

		verify(alertMapper, times(expectedAlertCast)).toDto(model.alert ?: AlertModel(), Locale.getDefault())
		verify(projectMapper, times(expectedProjectCast)).toDto(model.project ?: ProjectModel(), Locale.getDefault())
	}
}
