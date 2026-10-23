package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementFields.MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleFields.VEHICLE_BRAND
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleFields.VEHICLE_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleFields.VEHICLE_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleFields.VEHICLE_LAST_MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleFields.VEHICLE_LAST_MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleFields.VEHICLE_LICENSE_PLATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleFields.VEHICLE_MODEL
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleFields.VEHICLE_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleFields.VEHICLE_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.TEXT_SEARCH_MIN_SCORE

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
                WHERE t.$VISIBLE IS TRUE
                GROUP BY $MOVEMENT_CONTENT_TABLE.$VEHICLE_PREFIX$ID, t.$LINKED_PROJECT_ID
            ) AS plm ON plm.$VEHICLE_LAST_MOVEMENT_DATE_TIME = t.$MOVEMENT_DATE_TIME
            WHERE plm.$VEHICLE_PREFIX$ID IS NOT NULL
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
            WHEN :query IS NULL THEN 1
            ELSE word_similarity(:query, t.search_text)
        END AS similarity_score
    """

	const val VEHICLE_TEXT_SEARCH_CLAUSE = "(:query IS NULL OR word_similarity(:query, t.search_text) >= $TEXT_SEARCH_MIN_SCORE)"

	const val VEHICLE_BRAND_CLAUSE =
		"(CAST(:brand AS TEXT) IS NULL OR LOWER(UNACCENT(t.$VEHICLE_BRAND)) = LOWER(UNACCENT(CAST(:brand AS TEXT))))"

	const val VEHICLE_MODEL_CLAUSE =
		"(CAST(:model AS TEXT) IS NULL OR LOWER(UNACCENT(t.$VEHICLE_MODEL)) = LOWER(UNACCENT(CAST(:model AS TEXT))))"

	const val VEHICLE_AVAILABILITY_CLAUSE = """
        (
            :isAvailable IS NULL OR :isAvailable = (
                (
                    (COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) + COALESCE(t.$VEHICLE_START_AVAILABILITY_TIME, '00:00:00.000000'::TIMETZ)) <= CURRENT_TIMESTAMP
                ) AND
                (
                    (COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) + COALESCE(t.$VEHICLE_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) >= CURRENT_TIMESTAMP
                )
            )
        )
    """

	const val VEHICLE_PRESENCE_CLAUSE =
		"(:isPresent IS NULL OR :isPresent != (last_movement.type IS NULL OR last_movement.type = 'OUT'))"

	const val DATE_IN_VEHICLE_DATES_RANGE_CLAUSE = """
        (
            :at IS NULL OR (
                (
                    (COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) + COALESCE(t.$VEHICLE_START_AVAILABILITY_TIME, '00:00:00.000000'::TIMETZ)) <= CAST(:at AS TIMESTAMPTZ)
                ) AND
                (
                    (COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) + COALESCE(t.$VEHICLE_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) >= CAST(:at AS TIMESTAMPTZ)
                )
            )
        )
    """

	const val VEHICLE_SORT = """
        CASE WHEN CAST(:sortField AS TEXT) = 'licensePlate' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$VEHICLE_LICENSE_PLATE END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'licensePlate' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$VEHICLE_LICENSE_PLATE END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'brand' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$VEHICLE_BRAND END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'brand' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$VEHICLE_BRAND END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'model' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$VEHICLE_MODEL END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'model' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$VEHICLE_MODEL END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'startAvailability' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) + COALESCE(t.$VEHICLE_START_AVAILABILITY_TIME, '00:00:00.000000'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'startAvailability' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$VEHICLE_START_AVAILABILITY_DATE, '-infinity'::DATE) + COALESCE(t.$VEHICLE_START_AVAILABILITY_TIME, '00:00:00.000000'::TIMETZ)) END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'endAvailability' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) + COALESCE(t.$VEHICLE_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'endAvailability' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$VEHICLE_END_AVAILABILITY_DATE, '+infinity'::DATE) + COALESCE(t.$VEHICLE_END_AVAILABILITY_TIME, '23:59:59.999999'::TIMETZ)) END DESC NULLS LAST,
    """
}
