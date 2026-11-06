package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CurrentUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.AuthorityMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [AuthorityMappingHelper::class, PreferenceReaderDtoMapper::class, UserRoleReaderDtoMapper::class])
interface CurrentUserReaderDtoMapper: IGenericReaderDtoMapper<CurrentUserModel, CurrentUserReaderDto> {
	@Mapping(target = "authorities", source = "model", qualifiedByName = ["authorityNames"])
	override fun toDto(model: CurrentUserModel): CurrentUserReaderDto
}
