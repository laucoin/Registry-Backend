package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PARTICIPANT_TYPE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import org.springframework.stereotype.Component

@Component
class ParticipantTypeReaderDtoMapper(
	private val translateService: ITranslateService,
): IGenericReaderDtoMapper<ParticipantTypeEnum, LabelDto> {
	override fun toDto(model: ParticipantTypeEnum): LabelDto {
		return LabelDto(
			model.name,
			translateService.getMessage(code = "$PARTICIPANT_TYPE_PREFIX$model"),
		)
	}
}
