package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectProfileReaderDto
import java.util.Locale
import java.util.Objects
import java.util.Optional
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class ProjectProfileReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
    private val projectMapper: ProjectReaderDtoMapper,
    private val availabilityStatusMapper: AvailabilityStatusReaderDtoMapper,
    private val partialUserMapper: PartialUserReaderDtoMapper,
): IGenericReaderDtoMapper<ProjectProfileModel, ProjectProfileReaderDto> {
    override fun toDto(model: ProjectProfileModel, locale: Locale): ProjectProfileReaderDto {
        return ProjectProfileReaderDto(
            user = Optional.ofNullable(model.user).map { partialUserMapper.toDto(it, locale) }.orElse(null),
            role = Optional.ofNullable(model.role).map {
                LabelDto(
                    it,
                    translateService.getMessage("$PROJECT_PROFILE_ROLE_PREFIX$it", null, locale),
                )
            }.orElse(null),
            availabilityStatus = Optional.ofNullable(model.availabilityStatus)
                .map { availabilityStatusMapper.toDto(it, locale, model.startAccess, model.endAccess) }.orElse(null),
            status = buildStatus(model, locale),
            startAccess = model.startAccess,
            endAccess = model.endAccess,
        ).apply {
            id = model.id
            project = Optional.ofNullable(model.project).map { projectMapper.toDto(it, locale) }.orElse(null)
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
                Optional.ofNullable(model.status).map {
                    translateService.getMessage(
                        "$PROJECT_PROFILE_STATUS_PREFIX${BLOCKED}_WITH_STATUS",
                        arrayOf(originalStatus),
                        locale
                    )
                }.orElse(translateService.getMessage("$PROJECT_PROFILE_STATUS_PREFIX$BLOCKED", null, locale))
            )
        }

        return if (Objects.nonNull(model.status)) LabelDto(
            model.status !!.name,
            originalStatus,
        ) else null
    }
}
