package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.OpenAlertProjectModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.OpenAlertProjectReaderDto
import org.springframework.stereotype.Component

@Component
class OpenAlertProjectReaderDtoMapper {
	fun toDto(model: OpenAlertProjectModel): OpenAlertProjectReaderDto {
		return OpenAlertProjectReaderDto(
			id = model.project?.id,
			name = model.project?.name,
			openAlertCount = model.openAlertCount,
		)
	}
}
