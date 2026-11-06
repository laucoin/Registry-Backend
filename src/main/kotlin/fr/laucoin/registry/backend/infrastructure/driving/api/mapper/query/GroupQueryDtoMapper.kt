package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel.Companion.SORT_FIELDS
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.GroupQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import java.util.UUID
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = DtoMapperConfig::class)
interface GroupQueryDtoMapper : PageQueryDtoMapper {
	@Mapping(target = "sort", source = "dto.sort", qualifiedByName = ["groupSort"])
	@Mapping(target = "isVisible", source = "isVisible")
	@Mapping(target = "isPresent", expression = "java(dto.isPresent())")
	@Mapping(target = "memberUserId", source = "memberUserId")
	@Mapping(target = "visible", ignore = true)
	@Mapping(target = "present", ignore = true)
	fun toSearchParams(dto: GroupQueryDto, isVisible: Boolean?, memberUserId: UUID?): GroupSearchParamModel

	@Named("groupSort")
	fun groupSort(sort: String?): SortModel? = sort.toSortModel(SORT_FIELDS)
}
