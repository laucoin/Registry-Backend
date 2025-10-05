package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.test.ModelExt.groupId
import java.util.stream.Stream
import kotlin.test.assertEquals
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

class GroupReaderDtoMapperTest {
	private val projectMapper: ProjectReaderDtoMapper = mock()
	private val availabilityMapper: AvailabilityStatusReaderDtoMapper = mock()
	private val memberMapper: ParticipantReaderDtoMapper = mock()
	private val mapper = GroupReaderDtoMapper(projectMapper, availabilityMapper, memberMapper)

	private companion object {
		private val participantDto = ParticipantReaderDto()
		private val activityStatus = LabelDto(value = AVAILABLE.name, label = "Available")
		private val dtoProject = ProjectReaderDto()

		private val model = GroupModel(
			members = listOf(ParticipantModel()),
		).apply {
			name = "Group 1"
			status = AVAILABLE
			startAvailability = CustomDateTimeModel.MIN
			endAvailability = CustomDateTimeModel.MAX
			membersCount = 1
			insideMembersCount = 2
			outsideMembersCount = 3
			id = groupId
			project = ProjectModel()
			visible = true
			creation = HistoryModel()
			lastEdition = HistoryModel()
		}

		private val dto = GroupReaderDto().apply {
			name = "Group 1"
			status = activityStatus
			startAvailability = CustomDateTimeModel.MIN
			endAvailability = CustomDateTimeModel.MAX
			membersCount = 1
			insideMembersCount = 2
			outsideMembersCount = 3
			id = groupId
			project = dtoProject
			members = listOf(participantDto)
			visible = true
			creation = HistoryModel()
			lastEdition = HistoryModel()
		}

		@JvmStatic
		fun `GroupModel to GroupReaderDto data`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(model, dto, 1, 1),
				Arguments.of(GroupModel(), GroupReaderDto(members = listOf(participantDto)), 0, 0),
			)
		}
	}

	@BeforeEach
	fun setup() {
		whenever(memberMapper.toDtoList(any())).thenReturn(listOf(participantDto))
		whenever(availabilityMapper.toDto(any(), anyOrNull(), anyOrNull())).thenReturn(activityStatus)
		whenever(projectMapper.toDto(any())).thenReturn(dtoProject)
	}

	@ParameterizedTest
	@MethodSource("GroupModel to GroupReaderDto data")
	fun `Should toDto convert GroupModel to GroupReaderDto`(
		model: GroupModel,
		dto: GroupReaderDto,
		expectedAvailabilityCast: Int,
		expectedProjectCast: Int,
	) {
		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(dto, result)

		verify(memberMapper).toDtoList(model.members)
		verify(availabilityMapper, times(expectedAvailabilityCast))
			.toDto(model.status ?: AVAILABLE, model.startAvailability, model.endAvailability)

		verify(projectMapper, times(expectedProjectCast)).toDto(model.project ?: ProjectModel())
	}

	@ParameterizedTest
	@MethodSource("GroupModel to GroupReaderDto data")
	fun `Should toDto convert GroupModel list to GroupReaderDto list`(
		model: GroupModel,
		dto: GroupReaderDto,
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

		verify(memberMapper).toDtoList(model.members)
		verify(availabilityMapper, times(expectedAvailabilityCast))
			.toDto(model.status ?: AVAILABLE, model.startAvailability, model.endAvailability)

		verify(projectMapper, times(expectedProjectCast)).toDto(model.project ?: ProjectModel())
	}

	@ParameterizedTest
	@MethodSource("GroupModel to GroupReaderDto data")
	fun `Should toDto convert GroupModel page to GroupReaderDto page`(
		model: GroupModel,
		dto: GroupReaderDto,
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

		verify(memberMapper).toDtoList(model.members)
		verify(availabilityMapper, times(expectedAvailabilityCast))
			.toDto(model.status ?: AVAILABLE, model.startAvailability, model.endAvailability)

		verify(projectMapper, times(expectedProjectCast)).toDto(model.project ?: ProjectModel())
	}
}
