package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.RefreshAuthenticationInfoModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.RefreshAuthenticationInfoWriterDto
import org.springframework.stereotype.Component

@Component
class RefreshAuthenticationInfoWriterDtoMapper :
	IGenericWriterDtoMapper<RefreshAuthenticationInfoModel, RefreshAuthenticationInfoWriterDto> {
	override fun toModel(dto: RefreshAuthenticationInfoWriterDto): RefreshAuthenticationInfoModel {
		return RefreshAuthenticationInfoModel(
			refreshToken = dto.refreshToken,
		)
	}
}
