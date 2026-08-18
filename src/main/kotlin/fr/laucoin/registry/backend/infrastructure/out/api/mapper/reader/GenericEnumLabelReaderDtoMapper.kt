package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto

abstract class GenericEnumLabelReaderDtoMapper<E : Enum<E>>(
	private val translateService: ITranslateService,
	private val translationKeyPrefix: String,
) : IGenericReaderDtoMapper<E, LabelDto> {
	override fun toDto(model: E): LabelDto {
		return LabelDto(
			model.name,
			translateService.getMessage(code = "$translationKeyPrefix$model"),
		)
	}
}
