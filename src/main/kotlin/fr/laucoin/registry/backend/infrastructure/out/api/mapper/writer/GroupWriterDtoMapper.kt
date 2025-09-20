package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GroupWriterDto
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class GroupWriterDtoMapper(
	private val customDateTimeMapper: CustomDateTimeWriterDtoMapper,
): IGenericProjectWriterDtoMapper<GroupModel, GroupWriterDto> {
	override fun toModel(dto: GroupWriterDto, projectId: UUID): GroupModel {
		return GroupModel().apply {
			name = dto.name!!
			startAvailability =
				Optional.ofNullable(dto.startAvailability).map { customDateTimeMapper.toModel(it) }.orElse(null)
			endAvailability =
				Optional.ofNullable(dto.endAvailability).map { customDateTimeMapper.toModel(it) }.orElse(null)
			endAvailability =
				Optional.ofNullable(dto.endAvailability).map { customDateTimeMapper.toModel(it) }.orElse(null)
			members = dto.members!!.map { ParticipantModel().apply { id = it } }
			project = ProjectModel().apply { id = projectId }
		}
	}
}
