package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DURATION_FORMAT_FAILED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ActivityWriterDto
import java.util.Optional
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
				Optional.ofNullable(dto.duration).map { Duration.parse(it) }.orElse(null)
			} catch (e: Exception) {
				throw RegistryException(
					status = BAD_REQUEST,
					code = ACTIVITY_DURATION_FORMAT_FAILED,
				)
			}
			allowedParticipants =
				Optional.ofNullable(dto.allowedParticipants).map { numericRangeMapper.toModel(it) }.orElse(null)
			startAvailability =
				Optional.ofNullable(dto.startAvailability).map { customDateTimeMapper.toModel(it) }.orElse(null)
			endAvailability =
				Optional.ofNullable(dto.endAvailability).map { customDateTimeMapper.toModel(it) }.orElse(null)
			project = ProjectModel().apply { id = projectId }
		}
	}
}
