package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.springframework.stereotype.Component

@Component
class ProjectProfileStatusReaderDtoMapper(
	translateService: ITranslateService,
) : GenericEnumLabelReaderDtoMapper<ProfileStatusEnum>(translateService, PROJECT_PROFILE_STATUS_PREFIX)
