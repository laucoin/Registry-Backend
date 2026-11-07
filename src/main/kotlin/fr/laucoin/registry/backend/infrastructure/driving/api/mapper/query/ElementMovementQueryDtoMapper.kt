package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ElementMovementQueryDto
import org.mapstruct.Mapping
import org.mapstruct.Named

interface ElementMovementQueryDtoMapper : PageQueryDtoMapper {
	@Mapping(target = "sort", source = "dto.sort", qualifiedByName = ["elementMovementSort"])
	@Mapping(target = "isVisible", source = "isVisible")
	@Mapping(target = "activityId", ignore = true)
	@Mapping(target = "visible", ignore = true)
	fun toMovementSearchParams(dto: ElementMovementQueryDto, isVisible: Boolean?): MovementSearchParamModel

	@Named("elementMovementSort")
	fun elementMovementSort(sort: String?): SortModel? = sort.toSortModel(MovementSearchParamModel.SORT_FIELDS)
}
