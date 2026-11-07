package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.VehicleQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = DtoMapperConfig::class)
interface VehicleQueryDtoMapper : ElementMovementQueryDtoMapper {
	@Mapping(target = "sort", source = "dto.sort", qualifiedByName = ["vehicleSort"])
	@Mapping(target = "isVisible", source = "isVisible")
	@Mapping(target = "visible", ignore = true)
	@Mapping(target = "available", ignore = true)
	@Mapping(target = "present", ignore = true)
	fun toSearchParams(dto: VehicleQueryDto, isVisible: Boolean?): VehicleSearchParamModel

	@Named("vehicleSort")
	fun vehicleSort(sort: String?): SortModel? = sort.toSortModel(VehicleSearchParamModel.SORT_FIELDS)
}
