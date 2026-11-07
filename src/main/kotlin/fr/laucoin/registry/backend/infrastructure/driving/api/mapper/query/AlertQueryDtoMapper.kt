package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel.Companion.SORT_FIELDS
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.AlertQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = DtoMapperConfig::class)
interface AlertQueryDtoMapper : PageQueryDtoMapper {
	@Mapping(target = "sort", source = "dto.sort", qualifiedByName = ["alertSort"])
	@Mapping(target = "isVisible", source = "isVisible")
	@Mapping(target = "visible", ignore = true)
	fun toSearchParams(dto: AlertQueryDto, isVisible: Boolean?): AlertSearchParamModel

	@Named("alertSort")
	fun alertSort(sort: String?): SortModel? = sort.toSortModel(SORT_FIELDS)
}
