package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel.Companion.SORT_FIELDS
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.MovementQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = DtoMapperConfig::class)
interface MovementQueryDtoMapper : PageQueryDtoMapper {
	@Mapping(target = "sort", source = "dto.sort", qualifiedByName = ["movementListSort"])
	@Mapping(target = "isVisible", source = "isVisible")
	@Mapping(target = "visible", ignore = true)
	fun toSearchParams(dto: MovementQueryDto, isVisible: Boolean?): MovementSearchParamModel

	@Named("movementListSort")
	fun movementListSort(sort: String?): SortModel? = sort.toSortModel(SORT_FIELDS)
}
