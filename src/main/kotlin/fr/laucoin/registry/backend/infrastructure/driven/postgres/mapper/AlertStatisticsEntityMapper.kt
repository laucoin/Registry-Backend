package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.AlertStatisticsModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertStatisticsEntity
import kotlin.math.roundToLong
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = PostgresMapperConfig::class)
interface AlertStatisticsEntityMapper {
	@Mapping(target = "averageResolutionSeconds", qualifiedByName = ["roundedSeconds"])
	fun toModel(entity: AlertStatisticsEntity): AlertStatisticsModel

	@Named("roundedSeconds")
	fun roundedSeconds(seconds: Double): Long = seconds.roundToLong()
}
