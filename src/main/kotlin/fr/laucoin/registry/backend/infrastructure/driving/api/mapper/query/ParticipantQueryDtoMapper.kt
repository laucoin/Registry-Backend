package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ParticipantQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = DtoMapperConfig::class)
interface ParticipantQueryDtoMapper : ElementMovementQueryDtoMapper {
	@Mapping(target = "sort", source = "dto.sort", qualifiedByName = ["participantSort"])
	@Mapping(target = "isVisible", source = "isVisible")
	@Mapping(target = "isMajor", expression = "java(dto.isMajor())")
	@Mapping(target = "isOnSite", source = "dto.onSite")
	@Mapping(target = "presence", ignore = true)
	@Mapping(target = "visible", ignore = true)
	@Mapping(target = "major", ignore = true)
	@Mapping(target = "available", ignore = true)
	@Mapping(target = "onSite", ignore = true)
	fun toSearchParams(dto: ParticipantQueryDto, isVisible: Boolean?): ParticipantSearchParamModel

	@Named("participantSort")
	fun participantSort(sort: String?): SortModel? = sort.toSortModel(ParticipantSearchParamModel.SORT_FIELDS)
}
