package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ActivityReaderDto
import java.util.Locale
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class ActivityReaderDtoMapper(
	private val projectMapper: ProjectReaderDtoMapper,
	private val availabilityStatusMapper: AvailabilityStatusReaderDtoMapper,
): IGenericReaderDtoMapper<ActivityModel, ActivityReaderDto> {
	override fun toDto(model: ActivityModel, locale: Locale): ActivityReaderDto {
		return ActivityReaderDto(
			name = model.name,
			status = Optional.ofNullable(model.status)
				.map { availabilityStatusMapper.toDto(it, locale, model.startAvailability, model.endAvailability) }
				.orElse(null),
			description = model.description,
			duration = Optional.ofNullable(model.duration).map {
				LabelDto(
					label = it.toString(),
					value = it.toIsoString()
				)
			}.orElse(null),
			allowedParticipants = model.allowedParticipants,
			startAvailability = model.startAvailability,
			endAvailability = model.endAvailability,
		).apply {
			id = model.id
			project = Optional.ofNullable(model.project).map { projectMapper.toDto(it, locale) }.orElse(null)
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
