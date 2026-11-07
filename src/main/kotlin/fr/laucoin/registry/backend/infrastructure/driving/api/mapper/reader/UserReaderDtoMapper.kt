package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class, uses = [UserRoleReaderDtoMapper::class])
interface UserReaderDtoMapper: IGenericReaderDtoMapper<UserModel, UserReaderDto> {
	override fun toDto(model: UserModel): UserReaderDto
}
