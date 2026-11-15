package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel.Companion.PROJECT_SORT_FIELDS
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel.Companion.SORT_FIELDS
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ProjectProfileQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.UserProjectProfileQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = DtoMapperConfig::class)
interface ProjectProfileQueryDtoMapper : PageQueryDtoMapper {
	@Mapping(target = "sort", source = "dto.sort", qualifiedByName = ["projectProfileSort"])
	@Mapping(target = "isVisible", source = "isVisible")
	@Mapping(target = "isAvailable", expression = "java(dto.isAvailable())")
	@Mapping(target = "visible", ignore = true)
	@Mapping(target = "available", ignore = true)
	fun toSearchParams(dto: ProjectProfileQueryDto, isVisible: Boolean?): ProjectProfileSearchParamModel

	@Mapping(target = "sort", source = "sort", qualifiedByName = ["userProjectProfileSort"])
	@Mapping(target = "isVisible", ignore = true)
	@Mapping(target = "isAvailable", expression = "java(dto.isAvailable())")
	@Mapping(target = "visible", ignore = true)
	@Mapping(target = "available", ignore = true)
	fun toSearchParams(dto: UserProjectProfileQueryDto): ProjectProfileSearchParamModel

	@Named("projectProfileSort")
	fun projectProfileSort(sort: String?): SortModel? = sort.toSortModel(SORT_FIELDS)

	@Named("userProjectProfileSort")
	fun userProjectProfileSort(sort: String?): SortModel? = sort.toSortModel(PROJECT_SORT_FIELDS)
}
