package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ParticipantWriterDto
import java.util.Objects
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ParticipantWriterDtoMapper(
    private val customDateTimeMapper: CustomDateTimeWriterDtoMapper,
): IGenericProjectWriterDtoMapper<ParticipantModel, ParticipantWriterDto> {
    override fun toModel(dto: ParticipantWriterDto, projectId: UUID): ParticipantModel {
        return ParticipantModel().apply {
            firstName = dto.firstName
            lastName = dto.lastName
            birthday = dto.birthday
            type = REGISTERED
            startAvailability =
                if (Objects.nonNull(dto.startAvailability)) customDateTimeMapper.toModel(dto.startAvailability !!) else null
            endAvailability = if (Objects.nonNull(dto.endAvailability)) customDateTimeMapper.toModel(dto.endAvailability !!) else null
            user = if (Objects.nonNull(dto.userId)) UserModel().apply { id = dto.userId } else null
            groups = if (Objects.nonNull(dto.groupIds)) dto.groupIds !!.map { GroupModel().apply { id = it } } else emptyList()
            project = ProjectModel().apply { id = projectId }
        }
    }
}
