package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.AgeMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.StatusLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [PartialUserReaderDtoMapper::class, ParticipantTypeReaderDtoMapper::class, StatusLabelMappingHelper::class, GroupWithoutMemberReaderDtoMapper::class, AgeMappingHelper::class])
interface ParticipantReaderDtoMapper: IGenericReaderDtoMapper<ParticipantModel, ParticipantReaderDto> {
	@Mapping(target = "major", source = "model", qualifiedByName = ["major"])
	@Mapping(target = "status", source = "model", qualifiedByName = ["participantStatus"])
	override fun toDto(model: ParticipantModel): ParticipantReaderDto
}
