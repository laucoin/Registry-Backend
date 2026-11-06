package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import org.mockito.kotlin.argThat
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.StatusLabelMappingHelper
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.test.ModelExt.defaultGroupId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.stream.Stream
import kotlin.test.assertEquals

class GroupReaderDtoMapperTest {
	private val statusHelper: StatusLabelMappingHelper = mock()
	private val memberMapper: ParticipantReaderDtoMapper = mock()
	private val mapper = GroupReaderDtoMapperImpl(statusHelper, memberMapper)

	private companion object {
		private val participantDto = ParticipantReaderDto()
		private val activityStatus = LabelDto(value = AVAILABLE.name, label = "Available")

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
			id = defaultGroupId
			visible = true
			creation = HistoryModel(ZonedDateTime.now(ZoneOffset.UTC))
			lastEdition = HistoryModel(ZonedDateTime.now(ZoneOffset.UTC))
		}

		private val dto = GroupReaderDto().apply {
			name = "Group 1"
			status = activityStatus
			startAvailability = CustomDateTimeModel.MIN
			endAvailability = CustomDateTimeModel.MAX
			membersCount = 1
			insideMembersCount = 2
			outsideMembersCount = 3
			id = defaultGroupId
			members = listOf(participantDto)
			visible = true
			creation = HistoryModel(ZonedDateTime.now(ZoneOffset.UTC))
			lastEdition = HistoryModel(ZonedDateTime.now(ZoneOffset.UTC))
		}

		@JvmStatic
		fun `GroupModel to GroupReaderDto data`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(model, dto),
				Arguments.of(GroupModel(), GroupReaderDto(members = listOf(participantDto))),
			)
		}
	}

	@BeforeEach
	fun setup() {
		whenever(memberMapper.toDtoList(any())).thenReturn(listOf(participantDto))
		whenever(statusHelper.groupStatus(argThat { status != null })).thenReturn(activityStatus)
	}

	@ParameterizedTest
	@MethodSource("GroupModel to GroupReaderDto data")
	fun `Should toDto convert GroupModel to GroupReaderDto`(
		model: GroupModel,
		dto: GroupReaderDto,
	) {
		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(dto, result)

		verify(memberMapper).toDtoList(model.members)
		verify(statusHelper).groupStatus(model)

	}

	@ParameterizedTest
	@MethodSource("GroupModel to GroupReaderDto data")
	fun `Should toDto convert GroupModel list to GroupReaderDto list`(
		model: GroupModel,
		dto: GroupReaderDto,
	) {
		// Arrange
		val models = listOf(model)
		val dtos = listOf(dto)

		// Act
		val result = mapper.toDtoList(models)

		// Assert
		assertEquals(dtos, result)

		verify(memberMapper).toDtoList(model.members)
		verify(statusHelper).groupStatus(model)

	}

	@ParameterizedTest
	@MethodSource("GroupModel to GroupReaderDto data")
	fun `Should toDto convert GroupModel page to GroupReaderDto page`(
		model: GroupModel,
		dto: GroupReaderDto,
	) {
		// Arrange
		val modelPage = PageModel(
			page = 0,
			size = 10,
			totalPages = 1,
			totalElements = 1,
			content = listOf(model),
		)
		val dtoPage = PageModel(
			page = 0,
			size = 10,
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
		verify(statusHelper).groupStatus(model)

	}
}
