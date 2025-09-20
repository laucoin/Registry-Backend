package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import java.util.Locale
import org.springframework.stereotype.Component

@Component
class AlertStatusReaderDtoMapper(
	private val translateService: ITranslateService,
): IGenericReaderDtoMapper<AlertStatusEnum, LabelDto> {
	override fun toDto(model: AlertStatusEnum, locale: Locale): LabelDto {
		return LabelDto(
			model.name,
			translateService.getMessage(code = "$ALERT_STATUS_PREFIX$model", locale = locale),
		)
	}
}
