package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PURGE_REPORTED_IDS_MAX
import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectContentPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import java.util.UUID
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = DtoMapperConfig::class)
interface ProjectContentPurgeReaderDtoMapper: IGenericReaderDtoMapper<ProjectContentPurgeModel, ProjectContentPurgeReaderDto> {
	@Mapping(target = "movements", qualifiedByName = ["reportedIds"])
	@Mapping(target = "alerts", qualifiedByName = ["reportedIds"])
	@Mapping(target = "communications", qualifiedByName = ["reportedIds"])
	override fun toDto(model: ProjectContentPurgeModel): ProjectContentPurgeReaderDto

	@Named("reportedIds")
	fun reportedIds(ids: List<UUID>): List<UUID> = ids.take(PURGE_REPORTED_IDS_MAX)
}
