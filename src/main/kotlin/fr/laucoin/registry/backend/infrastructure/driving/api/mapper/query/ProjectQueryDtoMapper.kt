package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel.Companion.SORT_FIELDS
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ProjectQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = DtoMapperConfig::class)
interface ProjectQueryDtoMapper : PageQueryDtoMapper {
	@Mapping(target = "sort", source = "sort", qualifiedByName = ["projectSort"])
	@Mapping(target = "isVisible", expression = "java(dto.isVisible())")
	@Mapping(target = "isFavorite", expression = "java(dto.isFavorite())")
	@Mapping(target = "visible", ignore = true)
	@Mapping(target = "favorite", ignore = true)
	@Mapping(target = "userId", ignore = true)
	@Mapping(target = "hiddenReadableIds", ignore = true)
	fun toSearchParams(dto: ProjectQueryDto): ProjectSearchParamModel

	@Named("projectSort")
	fun projectSort(sort: String?): SortModel? = sort.toSortModel(SORT_FIELDS)
}
