package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.GroupWriterDto
import java.util.Objects
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class GroupWriterDtoMapper(
    private val customDateTimeMapper: CustomDateTimeWriterDtoMapper,
): IGenericProjectWriterDtoMapper<GroupModel, GroupWriterDto> {
    override fun toModel(dto: GroupWriterDto, projectId: UUID): GroupModel {
        return GroupModel().apply {
            name = dto.name !!
            startAvailability =
                if (Objects.nonNull(dto.startAvailability)) customDateTimeMapper.toModel(dto.startAvailability !!) else null
            endAvailability = if (Objects.nonNull(dto.endAvailability)) customDateTimeMapper.toModel(dto.endAvailability !!) else null
            members = dto.members !!.map { ParticipantModel().apply { id = it } }
            project = ProjectModel().apply { id = projectId }
        }
    }
}
