package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_CRITICALITY_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import org.springframework.stereotype.Component

@Component
class AlertCriticalityReaderDtoMapper(
	private val translateService: ITranslateService,
): IGenericReaderDtoMapper<AlertCriticalityEnum, LabelDto> {
	override fun toDto(model: AlertCriticalityEnum): LabelDto {
		return LabelDto(
			model.name,
			translateService.getMessage(code = "$ALERT_CRITICALITY_PREFIX$model"),
		)
	}
}
