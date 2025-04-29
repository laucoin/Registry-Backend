package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectProfileReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class ProjectProfileReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
    private val projectMapper: ProjectReaderDtoMapper,
    private val partialUserMapper: PartialUserReaderDtoMapper,
): IGenericReaderDtoMapper<ProjectProfileModel, ProjectProfileReaderDto> {
    override fun toDto(model: ProjectProfileModel, locale: Locale): ProjectProfileReaderDto {
        return ProjectProfileReaderDto(
            user = if (Objects.nonNull(model.user)) partialUserMapper.toDto(model.user !!, locale) else null,
            role = if (Objects.nonNull(model.role)) LabelDto(
                model.role !!,
                translateService.getMessage("$PROJECT_PROFILE_ROLE_PREFIX${model.role}", null, locale),
            ) else null,
            status = buildStatus(model, locale),
            startAccess = model.startAccess,
            endAccess = model.endAccess,
        ).apply {
            id = model.id
            project = if (Objects.nonNull(model.project)) projectMapper.toDto(model.project !!, locale) else null
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
    }

    private fun buildStatus(model: ProjectProfileModel, locale: Locale): LabelDto? {
        val originalStatus = translateService.getMessage("$PROJECT_PROFILE_STATUS_PREFIX${model.status}", null, locale)
        if (! model.visible) {
            return LabelDto(
                BLOCKED.name,
                if (Objects.nonNull(model.status)) translateService.getMessage(
                    "$PROJECT_PROFILE_STATUS_PREFIX${BLOCKED}_WITH_STATUS",
                    arrayOf(originalStatus),
                    locale
                )
                else translateService.getMessage("$PROJECT_PROFILE_STATUS_PREFIX$BLOCKED", null, locale),
            )
        }

        return if (Objects.nonNull(model.status)) LabelDto(
            model.status !!.name,
            originalStatus,
        ) else null
    }
}
