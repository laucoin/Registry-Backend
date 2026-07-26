package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.NumericRangeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.test.ModelExt.activityId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.time.Duration

class ActivityReaderDtoMapperTest {
	private val projectMapper: ProjectReaderDtoMapper = mock()
	private val availabilityMapper: AvailabilityStatusReaderDtoMapper = mock()
	private val mapper = ActivityReaderDtoMapper(projectMapper, availabilityMapper)

	private companion object {
		private val activityStatus = LabelDto(value = AVAILABLE.name, label = "Available")
		private val dtoProject = ProjectReaderDto()

		private val model = ActivityModel().apply {
			name = "Activity 1"
			status = AVAILABLE
			description = "Activity 1 description"
			duration = Duration.ZERO
			allowedParticipants = NumericRangeModel(lower = 1, upper = 10)
			startAvailability = CustomDateTimeModel.MIN
			endAvailability = CustomDateTimeModel.MAX
			id = activityId
			project = ProjectModel()
			visible = true
			creation = HistoryModel()
			lastEdition = HistoryModel()
		}

		private val dto = ActivityReaderDto().apply {
			name = "Activity 1"
			status = activityStatus
			description = "Activity 1 description"
			duration = LabelDto(value = Duration.ZERO.toIsoString(), label = Duration.ZERO.toString())
			allowedParticipants = NumericRangeModel(lower = 1, upper = 10)
			startAvailability = CustomDateTimeModel.MIN
			endAvailability = CustomDateTimeModel.MAX
			id = activityId
			project = ProjectReaderDto()
			visible = true
			creation = HistoryModel()
			lastEdition = HistoryModel()
		}

		@JvmStatic
		fun `ActivityModel to ActivityReaderDto data`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(model, dto, 1, 1),
				Arguments.of(ActivityModel(), ActivityReaderDto(), 0, 0),
			)
		}
	}

	@BeforeEach
	fun setup() {
		whenever(availabilityMapper.toDto(any(), anyOrNull(), anyOrNull())).thenReturn(activityStatus)
		whenever(projectMapper.toDto(any())).thenReturn(dtoProject)
	}

	@ParameterizedTest
	@MethodSource("ActivityModel to ActivityReaderDto data")
	fun `Should toDto convert ActivityModel to ActivityReaderDto`(
		model: ActivityModel,
		dto: ActivityReaderDto,
		expectedAvailabilityCast: Int,
		expectedProjectCast: Int,
	) {
		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(dto, result)

		verify(availabilityMapper, times(expectedAvailabilityCast))
			.toDto(model.status ?: AVAILABLE, model.startAvailability, model.endAvailability)

		verify(projectMapper, times(expectedProjectCast)).toDto(model.project ?: ProjectModel())
	}

	@ParameterizedTest
	@MethodSource("ActivityModel to ActivityReaderDto data")
	fun `Should toDto convert ActivityModel list to ActivityReaderDto list`(
		model: ActivityModel,
		dto: ActivityReaderDto,
		expectedAvailabilityCast: Int,
		expectedProjectCast: Int,
	) {
		// Arrange
		val models = listOf(model)
		val dtos = listOf(dto)

		// Act
		val result = mapper.toDtoList(models)

		// Assert
		assertEquals(dtos, result)

		verify(availabilityMapper, times(expectedAvailabilityCast))
			.toDto(model.status ?: AVAILABLE, model.startAvailability, model.endAvailability)

		verify(projectMapper, times(expectedProjectCast)).toDto(model.project ?: ProjectModel())
	}

	@ParameterizedTest
	@MethodSource("ActivityModel to ActivityReaderDto data")
	fun `Should toDto convert ActivityModel page to ActivityReaderDto page`(
		model: ActivityModel,
		dto: ActivityReaderDto,
		expectedAvailabilityCast: Int,
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
		val result = mapper.toDtoPage(modelPage)

		// Assert
		assertEquals(dtoPage, result)

		verify(availabilityMapper, times(expectedAvailabilityCast))
			.toDto(model.status ?: AVAILABLE, model.startAvailability, model.endAvailability)

		verify(projectMapper, times(expectedProjectCast)).toDto(model.project ?: ProjectModel())
	}
}
