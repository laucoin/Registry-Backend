package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.infrastructure.driving.api.helper.PurgeReportHelper.reported
import java.util.UUID
import org.mapstruct.Named

interface PurgeReportDtoMapper {
	@Named("reportedIds")
	fun reportedIds(ids: List<UUID>): List<UUID> = ids.reported()
}
