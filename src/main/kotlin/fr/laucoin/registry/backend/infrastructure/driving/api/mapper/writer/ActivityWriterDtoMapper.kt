package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.ActivityModel
import java.util.UUID
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.ActivityDurationMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ActivityWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [NumericRangeWriterDtoMapper::class, CustomDateTimeWriterDtoMapper::class, ActivityDurationMappingHelper::class])
interface ActivityWriterDtoMapper: IGenericProjectWriterDtoMapper<ActivityModel, ActivityWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "name", source = "dto.name")
	@Mapping(target = "description", source = "dto.description")
	@Mapping(target = "allowedParticipants", source = "dto.allowedParticipants")
	@Mapping(target = "startAvailability", source = "dto.startAvailability")
	@Mapping(target = "endAvailability", source = "dto.endAvailability")
	@Mapping(target = "projectId", source = "projectId")
	override fun toModel(dto: ActivityWriterDto, projectId: UUID): ActivityModel
}
