package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.annotation.HttpCacheable
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.FeaturesReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Metadata (v2)", description = "API for global metadata")
@RequestMapping("/api/v2/metadata")
interface IMetadataV2Controller {
	@Operation(
		summary = "Get feature switches",
		description = "Get the deployment feature switches the UI mirrors",
	)
	@HttpCacheable
	@GetMapping("/features")
	fun getFeatures(): FeaturesReaderDto
}
