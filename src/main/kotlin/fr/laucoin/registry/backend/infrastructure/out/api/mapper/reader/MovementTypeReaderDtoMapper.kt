package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.MOVEMENT_TYPE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.springframework.stereotype.Component

@Component
class MovementTypeReaderDtoMapper(
	translateService: ITranslateService,
) : GenericEnumLabelReaderDtoMapper<MovementTypeEnum>(translateService, MOVEMENT_TYPE_PREFIX)
