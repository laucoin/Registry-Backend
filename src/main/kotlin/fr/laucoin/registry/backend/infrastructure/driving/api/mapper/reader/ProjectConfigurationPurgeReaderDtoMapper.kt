package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PURGE_REPORTED_IDS_MAX
import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectConfigurationPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import java.util.UUID
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = DtoMapperConfig::class)
interface ProjectConfigurationPurgeReaderDtoMapper: IGenericReaderDtoMapper<ProjectConfigurationPurgeModel, ProjectConfigurationPurgeReaderDto> {
	@Mapping(target = "vehicles", qualifiedByName = ["reportedIds"])
	@Mapping(target = "participants", qualifiedByName = ["reportedIds"])
	@Mapping(target = "activities", qualifiedByName = ["reportedIds"])
	@Mapping(target = "groups", qualifiedByName = ["reportedIds"])
	override fun toDto(model: ProjectConfigurationPurgeModel): ProjectConfigurationPurgeReaderDto

	@Named("reportedIds")
	fun reportedIds(ids: List<UUID>): List<UUID> = ids.take(PURGE_REPORTED_IDS_MAX)
}
