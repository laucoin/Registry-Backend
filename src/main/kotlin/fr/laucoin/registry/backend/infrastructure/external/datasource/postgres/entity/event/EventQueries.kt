package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_END

object EventQueries {
    const val IN_DATE_RANGE_CLAUSE = """
        (:startDateTime IS NULL OR :startDateTime <= t.$EVENT_END) AND
        (:endDateTime IS NULL OR :endDateTime >= t.$EVENT_BEGIN)
    """
}
