package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.CREATED_AT
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
	 * The vehicle twin of the participant CTE, with the same corrections: one row
	 * per vehicle, its latest VISIBLE movement, ties broken on the recording order.
	 * Matching on `MAX(date_time)` let two movements recorded for the same minute
	 * lend each other their direction, and let a DISABLED movement sharing that
	 * timestamp supply the type — so hiding a mistaken exit left the vehicle
	 * counted as out. The inner MAX() already ignored invisible movements, which is
	 * exactly what made the discrepancy hard to see: the date came from a visible
	 * movement and the direction from a hidden one.
	 */
	const val WITH_VEHICLE_LAST_MOVEMENT = """
        last_movement AS (
            SELECT DISTINCT ON (mc.$VEHICLE_PREFIX$ID)
                mc.$VEHICLE_PREFIX$ID,
                t.$MOVEMENT_TYPE,
                t.$MOVEMENT_DATE_TIME AS $VEHICLE_LAST_MOVEMENT_DATE_TIME
            FROM $MOVEMENT_TABLE t
            INNER JOIN $MOVEMENT_CONTENT_TABLE mc ON mc.$MOVEMENT_CONTENT_MOVEMENT_ID = t.$ID
            WHERE t.$VISIBLE IS TRUE AND mc.$VEHICLE_PREFIX$ID IS NOT NULL
            ORDER BY mc.$VEHICLE_PREFIX$ID, t.$MOVEMENT_DATE_TIME DESC, t.$CREATED_AT DESC
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

	const val VEHICLE_AVAILABLE_EXPRESSION = """
        (
            (
                COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) < CURRENT_DATE
                OR (COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$VEHICLE_START_AVAILABILITY_TIME, '00:00:00.000000'::TIME) <= CURRENT_TIME)
            ) AND
            (
                COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) > CURRENT_DATE
                OR (COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$VEHICLE_END_AVAILABILITY_TIME, '23:59:59.999999'::TIME) >= CURRENT_TIME)
            )
        )
    """

	const val VEHICLE_AVAILABILITY_CLAUSE =
		"(:availabilitySearched IS NULL OR :availabilitySearched = $VEHICLE_AVAILABLE_EXPRESSION)"

	/**
	 * The SQL twin of `AvailabilityElementExt.status` for vehicles, which have no
	 * exit reason and therefore never reach DEPARTED — the register cannot tell a
	 * vehicle that left for good from one that is simply out. The rule is otherwise
	 * identical: a recorded movement decides, and the window only speaks for a
	 * vehicle no movement describes.
	 */
	const val VEHICLE_STATUS_EXPRESSION = """
        (
            CASE
                WHEN last_movement.type = 'IN' THEN 'IN'
                WHEN last_movement.type IS NULL AND NOT $VEHICLE_AVAILABLE_EXPRESSION THEN 'UNAVAILABLE'
                ELSE 'OUT'
            END
        )
    """

	const val VEHICLE_STATUS_CLAUSE =
		"(:statusSearched IS NULL OR :statusSearched = $VEHICLE_STATUS_EXPRESSION)"

	const val VEHICLE_WARNING_EXPRESSION =
		"(last_movement.type IS NOT NULL AND NOT $VEHICLE_AVAILABLE_EXPRESSION)"

	const val VEHICLE_WARNED_CLAUSE =
		"(:warnedSearched IS NULL OR :warnedSearched = $VEHICLE_WARNING_EXPRESSION)"

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
