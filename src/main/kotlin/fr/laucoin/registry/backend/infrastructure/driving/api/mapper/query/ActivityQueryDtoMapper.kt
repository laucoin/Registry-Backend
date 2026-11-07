package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ActivityMovementQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ActivityQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.DurationFilterHelper.parseDurationFilter
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = DtoMapperConfig::class)
interface ActivityQueryDtoMapper : PageQueryDtoMapper {
	/** Écrit à la main : MapStruct ne sait pas passer les durées Kotlin (value class) au constructeur du modèle. */
	fun toSearchParams(dto: ActivityQueryDto, isVisible: Boolean?): ActivitySearchParamModel = ActivitySearchParamModel(
		query = dto.query,
		isVisible = isVisible,
		isAvailable = dto.isAvailable,
		at = dto.at,
		minDuration = parseDurationFilter(dto.minDuration),
		maxDuration = parseDurationFilter(dto.maxDuration),
		participants = dto.participants,
		sort = dto.sort.toSortModel(ActivitySearchParamModel.SORT_FIELDS),
	)

	@Mapping(target = "sort", source = "dto.sort", qualifiedByName = ["activityMovementSort"])
	@Mapping(target = "isVisible", source = "isVisible")
	@Mapping(target = "hasActivity", ignore = true)
	@Mapping(target = "activityId", ignore = true)
	@Mapping(target = "visible", ignore = true)
	fun toMovementSearchParams(dto: ActivityMovementQueryDto, isVisible: Boolean?): MovementSearchParamModel

	@Named("activityMovementSort")
	fun activityMovementSort(sort: String?): SortModel? = sort.toSortModel(MovementSearchParamModel.SORT_FIELDS)
}
