package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleFields.VEHICLE_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleFields.VEHICLE_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleFields.VEHICLE_LAST_MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleFields.VEHICLE_LAST_MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleFields.VEHICLE_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleFields.VEHICLE_START_AVAILABILITY_TIME

object VehicleQueries {
	private const val VEHICLE_PREFIX = "vehicle_"

	/**
	 * Same two corrections the participant CTE already carries. The outer movement
	 * MUST be correlated back to the vehicle rather than matched on the timestamp
	 * alone — two movements recorded for the same minute otherwise lend each other
	 * their direction, and a vehicle that never moved inherits somebody else's.
	 * And the outer movement MUST be filtered on visibility: a DISABLED movement
	 * sharing that timestamp was still allowed to supply the type, so hiding a
	 * mistaken exit left the vehicle counted as out on the dashboard. The inner
	 * MAX() already ignored invisible movements, which is exactly what made the
	 * discrepancy hard to see — the date came from a visible movement and the
	 * direction from a hidden one.
	 */
	const val WITH_VEHICLE_LAST_MOVEMENT = """
        last_movement AS (
            SELECT DISTINCT plm.$VEHICLE_LAST_MOVEMENT_DATE_TIME, plm.$VEHICLE_PREFIX$ID, t.$MOVEMENT_TYPE
            FROM $MOVEMENT_TABLE t
            INNER JOIN $MOVEMENT_CONTENT_TABLE mc ON mc.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID
            INNER JOIN (
                SELECT MAX(t.$MOVEMENT_DATE_TIME) as $VEHICLE_LAST_MOVEMENT_DATE_TIME, $MOVEMENT_CONTENT_TABLE.$VEHICLE_PREFIX$ID
                FROM $MOVEMENT_TABLE t
                INNER JOIN $MOVEMENT_CONTENT_TABLE ON $MOVEMENT_CONTENT_TABLE.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID
                WHERE t.$VISIBLE IS TRUE
                GROUP BY $MOVEMENT_CONTENT_TABLE.$VEHICLE_PREFIX$ID
            ) AS plm
                ON plm.$VEHICLE_LAST_MOVEMENT_DATE_TIME = t.$MOVEMENT_DATE_TIME
                AND plm.$VEHICLE_PREFIX$ID = mc.$VEHICLE_PREFIX$ID
            WHERE t.$VISIBLE IS TRUE
        )
    """

	const val SELECT_LAST_MOVEMENT = """
        last_movement.$MOVEMENT_TYPE AS $VEHICLE_LAST_MOVEMENT_TYPE,
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

	const val VEHICLE_PRESENCE_CLAUSE =
		"(:presenceSearched IS NULL OR :presenceSearched != (last_movement.type IS NULL OR last_movement.type = 'OUT'))"

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
