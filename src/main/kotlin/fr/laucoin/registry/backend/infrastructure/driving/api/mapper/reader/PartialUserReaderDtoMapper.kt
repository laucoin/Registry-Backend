package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class)
interface PartialUserReaderDtoMapper: IGenericReaderDtoMapper<UserModel, PartialUserReaderDto> {
	override fun toDto(model: UserModel): PartialUserReaderDto
}
