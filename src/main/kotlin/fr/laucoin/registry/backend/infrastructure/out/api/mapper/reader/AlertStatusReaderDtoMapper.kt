package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.springframework.stereotype.Component

@Component
class AlertStatusReaderDtoMapper(
	translateService: ITranslateService,
) : GenericEnumLabelReaderDtoMapper<AlertStatusEnum>(translateService, ALERT_STATUS_PREFIX)
