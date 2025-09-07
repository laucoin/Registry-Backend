package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.util.Locale
import org.springframework.stereotype.Component

@Component
class ProjectProfileStatusReaderDtoMapper(
    private val translateService: ITranslateService
) : IGenericReaderDtoMapper<ProfileStatusEnum, LabelDto> {
    override fun toDto(model: ProfileStatusEnum, locale: Locale): LabelDto {
        return LabelDto(
            model.name,
            translateService.getMessage("$PROJECT_PROFILE_STATUS_PREFIX$model", locale),
        )
    }
}
