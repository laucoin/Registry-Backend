package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.MOVEMENT_TYPE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.util.Locale
import org.springframework.stereotype.Component

@Component
class MovementTypeReaderDtoMapper(
    private val translateService: ITranslateService,
) : IGenericReaderDtoMapper<MovementTypeEnum, LabelDto> {
    override fun toDto(model: MovementTypeEnum, locale: Locale): LabelDto {
        return LabelDto(
            model.name,
            translateService.getMessage(code = "$MOVEMENT_TYPE_PREFIX$model", locale = locale),
        )
    }
}
