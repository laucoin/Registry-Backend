package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel.Companion.SORT_FIELDS
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.UserQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = DtoMapperConfig::class)
interface UserQueryDtoMapper : PageQueryDtoMapper {
	@Mapping(target = "sort", source = "sort", qualifiedByName = ["userSort"])
	@Mapping(target = "isVisible", expression = "java(dto.isVisible())")
	@Mapping(target = "memberOfProjectId", ignore = true)
	@Mapping(target = "visible", ignore = true)
	fun toSearchParams(dto: UserQueryDto): UserSearchParamModel

	@Named("userSort")
	fun userSort(sort: String?): SortModel? = sort.toSortModel(SORT_FIELDS)
}
