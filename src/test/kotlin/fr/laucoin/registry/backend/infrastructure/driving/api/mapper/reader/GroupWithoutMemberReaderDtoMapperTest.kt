package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupWithoutMemberReaderDto
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

class GroupWithoutMemberReaderDtoMapperTest {
	private val availabilityMapper: AvailabilityStatusReaderDtoMapper = mock()
	private val mapper = GroupWithoutMemberReaderDtoMapper(availabilityMapper)

	private companion object {
		private val activityStatus = LabelDto(value = AVAILABLE.name, label = "Available")

		private val model = GroupModel(
			members = listOf(ParticipantModel(), ParticipantModel(), ParticipantModel()),
		).apply {
			name = "Group 1"
			status = AVAILABLE
			startAvailability = CustomDateTimeModel.MIN
			endAvailability = CustomDateTimeModel.MAX
			membersCount = 1
			insideMembersCount = 2
			outsideMembersCount = 3
			id = groupId
			isVisible = true
			creation = HistoryModel()
			lastEdition = HistoryModel()
		}

		private val dto = GroupWithoutMemberReaderDto().apply {
			name = "Group 1"
			status = activityStatus
			startAvailability = CustomDateTimeModel.MIN
			endAvailability = CustomDateTimeModel.MAX
			membersCount = 1
			insideMembersCount = 2
			outsideMembersCount = 3
			id = groupId
			isVisible = true
			creation = HistoryModel()
			lastEdition = HistoryModel()
		}

		@JvmStatic
		fun `GroupModel to GroupWithoutMemberReaderDto data`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(model, dto, 1),
				Arguments.of(GroupModel(), GroupWithoutMemberReaderDto(), 0),
			)
		}
	}

	@BeforeEach
	fun setup() {
		whenever(availabilityMapper.toDto(any(), anyOrNull(), anyOrNull())).thenReturn(activityStatus)
	}

	@ParameterizedTest
	@MethodSource("GroupModel to GroupWithoutMemberReaderDto data")
	fun `Should toDto convert GroupModel to GroupWithoutMemberReaderDto`(
		model: GroupModel,
		dto: GroupWithoutMemberReaderDto,
		expectedAvailabilityCast: Int,
	) {
		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(dto.name, result.name)
		assertEquals(dto.status, result.status)
		assertEquals(dto.startAvailability, result.startAvailability)
		assertEquals(dto.endAvailability, result.endAvailability)
		assertEquals(dto.membersCount, result.membersCount)
		assertEquals(dto.insideMembersCount, result.insideMembersCount)
		assertEquals(dto.outsideMembersCount, result.outsideMembersCount)
		assertEquals(dto.id, result.id)
		assertEquals(dto.isVisible, result.isVisible)

		verify(availabilityMapper, times(expectedAvailabilityCast))
			.toDto(model.status ?: AVAILABLE, model.startAvailability, model.endAvailability)
	}

	@ParameterizedTest
	@MethodSource("GroupModel to GroupWithoutMemberReaderDto data")
	fun `Should toDto convert GroupModel list to GroupWithoutMemberReaderDto list`(
		model: GroupModel,
		dto: GroupWithoutMemberReaderDto,
		expectedAvailabilityCast: Int,
	) {
		// Arrange
		val models = listOf(model)
		val dtos = listOf(dto)

		// Act
		val result = mapper.toDtoList(models)

		// Assert
		assertEquals(dtos.size, result.size)

		verify(availabilityMapper, times(expectedAvailabilityCast))
			.toDto(model.status ?: AVAILABLE, model.startAvailability, model.endAvailability)
	}
}
