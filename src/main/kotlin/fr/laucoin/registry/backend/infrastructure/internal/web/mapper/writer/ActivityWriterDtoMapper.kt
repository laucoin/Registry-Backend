package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DURATION_FORMAT_FAILED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ActivityWriterDto
import java.util.Objects
import java.util.UUID
import kotlin.time.Duration
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.stereotype.Component

@Component
class ActivityWriterDtoMapper(
    private val numericRangeMapper: NumericRangeWriterDtoMapper,
    private val customDateTimeMapper: CustomDateTimeWriterDtoMapper,
): IGenericProjectWriterDtoMapper<ActivityModel, ActivityWriterDto> {
    override fun toModel(dto: ActivityWriterDto, projectId: UUID): ActivityModel {
        return ActivityModel().apply {
            name = dto.name
            description = dto.description
            duration = try {
                if (Objects.nonNull(dto.duration)) Duration.parse(dto.duration !!) else null
            } catch (e: Exception) {
                throw RegistryException(
                    status = BAD_REQUEST,
                    code = ACTIVITY_DURATION_FORMAT_FAILED,
                )
            }
            allowedParticipants =
                if (Objects.nonNull(dto.allowedParticipants)) numericRangeMapper.toModel(dto.allowedParticipants !!) else null
            startAvailability =
                if (Objects.nonNull(dto.startAvailability)) customDateTimeMapper.toModel(dto.startAvailability !!) else null
            endAvailability = if (Objects.nonNull(dto.endAvailability)) customDateTimeMapper.toModel(dto.endAvailability !!) else null
            project = ProjectModel().apply { id = projectId }
        }
    }
}
