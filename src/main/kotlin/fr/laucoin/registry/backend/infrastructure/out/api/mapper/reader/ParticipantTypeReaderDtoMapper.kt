package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PARTICIPANT_TYPE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.springframework.stereotype.Component

@Component
class ParticipantTypeReaderDtoMapper(
	translateService: ITranslateService,
) : GenericEnumLabelReaderDtoMapper<ParticipantTypeEnum>(translateService, PARTICIPANT_TYPE_PREFIX)
