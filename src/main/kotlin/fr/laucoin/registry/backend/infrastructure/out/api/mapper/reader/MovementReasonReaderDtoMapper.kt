package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.MOVEMENT_REASON_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonKindEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReasonsReaderDto
import org.springframework.stereotype.Component

@Component
class MovementReasonReaderDtoMapper(
	private val translateService: ITranslateService,
): IGenericReaderDtoMapper<MovementReasonEnum, MovementReasonsReaderDto> {
	override fun toDto(model: MovementReasonEnum): MovementReasonsReaderDto {
		return MovementReasonsReaderDto(
			label = translateService.getMessage(code = "$MOVEMENT_REASON_PREFIX$model"),
			value = model.name,
			type = model.type,
			kind = MovementReasonKindEnum.REASON,
		)
	}
}
