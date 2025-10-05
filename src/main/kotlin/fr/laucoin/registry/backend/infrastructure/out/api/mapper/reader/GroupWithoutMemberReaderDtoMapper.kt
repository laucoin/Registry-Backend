package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupWithoutMemberReaderDto
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class GroupWithoutMemberReaderDtoMapper(
	private val projectMapper: ProjectReaderDtoMapper,
	private val availabilityStatusMapper: AvailabilityStatusReaderDtoMapper,
): IGenericReaderDtoMapper<GroupModel, GroupWithoutMemberReaderDto> {
	override fun toDto(model: GroupModel): GroupWithoutMemberReaderDto {
		return GroupWithoutMemberReaderDto(
			name = model.name,
			status = Optional.ofNullable(model.status)
				.map { availabilityStatusMapper.toDto(it, model.startAvailability, model.endAvailability) }
				.orElse(null),
			startAvailability = model.startAvailability,
			endAvailability = model.endAvailability,
			membersCount = model.membersCount,
			insideMembersCount = model.insideMembersCount,
			outsideMembersCount = model.outsideMembersCount,
		).apply {
			id = model.id
			project = Optional.ofNullable(model.project).map(projectMapper::toDto).orElse(null)
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
