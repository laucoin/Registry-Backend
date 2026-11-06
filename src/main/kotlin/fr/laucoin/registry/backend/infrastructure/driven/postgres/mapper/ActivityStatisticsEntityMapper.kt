package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ActivityStatisticsModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.ActivityStatisticsEntity
import kotlin.math.roundToLong
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = PostgresMapperConfig::class)
interface ActivityStatisticsEntityMapper {
	@Mapping(target = "movementSeconds", qualifiedByName = ["roundedSeconds"])
	@Mapping(target = "manSeconds", qualifiedByName = ["roundedSeconds"])
	fun toModel(entity: ActivityStatisticsEntity): ActivityStatisticsModel

	@Named("roundedSeconds")
	fun roundedSeconds(seconds: Double): Long = seconds.roundToLong()
}
