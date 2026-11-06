package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.ActivityLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.StatusLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [StatusLabelMappingHelper::class, ActivityLabelMappingHelper::class])
interface ActivityReaderDtoMapper: IGenericReaderDtoMapper<ActivityModel, ActivityReaderDto> {
	@Mapping(target = "status", source = "model", qualifiedByName = ["activityStatus"])
	@Mapping(target = "duration", source = "model", qualifiedByName = ["activityDuration"])
	override fun toDto(model: ActivityModel): ActivityReaderDto
}
