package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.MOVEMENT_REASON_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonKindEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReasonsReaderDto
import org.springframework.stereotype.Component

@Component
class MovementActivityReasonReaderDtoMapper(
	private val translateService: ITranslateService,
) : IGenericReaderDtoMapper<ActivityModel, MovementReasonsReaderDto> {
	override fun toDto(model: ActivityModel): MovementReasonsReaderDto {
		return MovementReasonsReaderDto(
			label = "${model.name} (${translateService.getMessage(code = "$MOVEMENT_REASON_PREFIX$ACTIVITY")})",
			value = model.id!!.toString(),
			kind = MovementReasonKindEnum.ACTIVITY,
			duration = model.duration?.toIsoString(),
		)
	}
}
