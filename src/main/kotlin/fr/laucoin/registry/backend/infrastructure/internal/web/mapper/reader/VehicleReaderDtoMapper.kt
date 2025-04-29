package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USABLE_ELEMENT_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.VehicleReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class VehicleReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
    private val projectMapper: ProjectReaderDtoMapper,
): IGenericReaderDtoMapper<VehicleModel, VehicleReaderDto> {
    override fun toDto(model: VehicleModel, locale: Locale): VehicleReaderDto {
        return VehicleReaderDto(
            licensePlate = model.licensePlate,
            brand = model.brand,
            model = model.model,
            status = if (Objects.nonNull(model.status)) LabelDto(
                model.status !!.name,
                translateService.getMessage("$USABLE_ELEMENT_STATUS_PREFIX${model.status}", null, locale),
            ) else null,
            startAvailability = model.startAvailability,
            endAvailability = model.endAvailability,
        ).apply {
            id = model.id
            project = if (Objects.nonNull(model.project)) projectMapper.toDto(model.project !!, locale) else null
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
    }
}
