package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel.Companion.SORT_FIELDS
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.CommunicationQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = DtoMapperConfig::class)
interface CommunicationQueryDtoMapper : PageQueryDtoMapper {
	@Mapping(target = "sort", source = "dto.sort", qualifiedByName = ["communicationSort"])
	@Mapping(target = "isVisible", source = "isVisible")
	@Mapping(target = "visible", ignore = true)
	fun toSearchParams(dto: CommunicationQueryDto, isVisible: Boolean?): CommunicationSearchParamModel

	@Named("communicationSort")
	fun communicationSort(sort: String?): SortModel? = sort.toSortModel(SORT_FIELDS)
}
