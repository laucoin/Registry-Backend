package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_LAST_MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_LAST_MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_START_AVAILABILITY_TIME

object VehicleQueries {
    private const val VEHICLE_PREFIX = "vehicle_"
    const val WITH_VEHICLE_LAST_MOVEMENT = """
        last_movement AS (
            SELECT plm.$VEHICLE_LAST_MOVEMENT_DATE_TIME, plm.$VEHICLE_PREFIX$ID, t.$MOVEMENT_TYPE
            FROM $MOVEMENT_TABLE t
            LEFT JOIN (
                SELECT MAX(t.$MOVEMENT_DATE_TIME) as $VEHICLE_LAST_MOVEMENT_DATE_TIME, t.$LINKED_PROJECT_ID, $MOVEMENT_CONTENT_TABLE.$VEHICLE_PREFIX$ID
                FROM $MOVEMENT_TABLE t
                INNER JOIN $MOVEMENT_CONTENT_TABLE ON $MOVEMENT_CONTENT_TABLE.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID
                GROUP BY $MOVEMENT_CONTENT_TABLE.$VEHICLE_PREFIX$ID, t.$LINKED_PROJECT_ID
            ) AS plm ON plm.$VEHICLE_LAST_MOVEMENT_DATE_TIME = t.$MOVEMENT_DATE_TIME
            WHERE t.$VISIBLE IS TRUE AND plm.$VEHICLE_PREFIX$ID IS NOT NULL AND t.$LINKED_PROJECT_ID = :projectId
        )
    """

    const val SELECT_LAST_MOVEMENT = """
        last_movement.type AS $VEHICLE_LAST_MOVEMENT_TYPE,
        last_movement.$VEHICLE_LAST_MOVEMENT_DATE_TIME
    """

    const val LAST_MOVEMENT_JOIN = """
        LEFT JOIN last_movement ON last_movement.$VEHICLE_PREFIX$ID = t.$ID
    """

    const val SELECT_VEHICLE_SEARCH = """
        CASE
            WHEN :textSearched IS NULL THEN 1
            ELSE similarity(t.search_text, :textSearched)
        END AS similarity_score
    """

    const val VEHICLE_TEXT_SEARCH_CLAUSE = "(:textSearched IS NULL OR similarity(t.search_text, :textSearched) > 0)"

    const val VEHICLE_AVAILABILITY_CLAUSE = """
        (
            :availabilitySearched IS NULL OR :availabilitySearched = (
                (
                    COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) < CURRENT_DATE
                    OR (COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$VEHICLE_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CURRENT_TIME)
                ) AND
                (
                    COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) > CURRENT_DATE
                    OR (COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$VEHICLE_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CURRENT_TIME)
                )
            )
        )
    """

    const val VEHICLE_PRESENCE_CLAUSE = "(:presenceSearched IS NULL OR :presenceSearched = (last_movement.type = 'IN'))"

    const val DATE_IN_VEHICLE_DATES_RANGE_CLAUSE = """
        (
            :dateTimeSearched IS NULL OR (
                (
                    COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) < CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(t.$VEHICLE_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CAST(:dateTimeSearched AS TIME))
                ) AND
                (
                    COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) > CAST(:dateTimeSearched AS DATE)
                    OR (COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) = CAST(:dateTimeSearched AS DATE) AND COALESCE(t.$VEHICLE_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CAST(:dateTimeSearched AS TIME))
                )
            )
        )
    """
}
