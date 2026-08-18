package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.infrastructure.out.api.controller.IMetadataV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.FeaturesReaderDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.RestController

@RestController
class MetadataV2Controller(
	@param:Value($$"${registry.feature.light-user.enabled:true}")
	private val lightUserEnabled: Boolean,
) : IMetadataV2Controller {
	override fun getFeatures(): FeaturesReaderDto {
		return FeaturesReaderDto(lightUser = lightUserEnabled)
	}
}
